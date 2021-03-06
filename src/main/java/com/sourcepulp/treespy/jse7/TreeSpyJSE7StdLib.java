package com.sourcepulp.treespy.jse7;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sourcepulp.treespy.Events;
import com.sourcepulp.treespy.TreeSpy;
import com.sourcepulp.treespy.TreeSpyListener;

/**
 * Java SE 7 compliant implementation of a directory watching service.
 * 
 * @author Will Faithfull
 *
 */
public class TreeSpyJSE7StdLib implements TreeSpy {

	private static final Logger log = LoggerFactory.getLogger(TreeSpy.class);

	private WatchService watcher;

	private ConcurrentMap<WatchKey, Path> watchKeysToDirectories;
	private ConcurrentMap<Path, Set<TreeSpyListener>> directoriesToListeners;
	private ConcurrentMap<TreeSpyListener, Set<PathMatcher>> callbacksToGlobMatchers;

	private AtomicBoolean running = new AtomicBoolean(false);

	private Executor daemonExecutor;
	private ExecutorService callbackExecutorService;

	private boolean runCallbacksOnDaemonThread = true;

	/**
	 * Constructs a directory spy using the provided executor to orchestrate the
	 * background task.
	 * 
	 * @param daemonExecutor
	 * @throws IOException
	 */
	public TreeSpyJSE7StdLib(Executor daemonExecutor, WatchService watcher) throws IOException {
		this.daemonExecutor = daemonExecutor;
		this.watcher = watcher;

		// Initialise maps
		this.watchKeysToDirectories = new ConcurrentHashMap<WatchKey, Path>();
		this.directoriesToListeners = new ConcurrentHashMap<Path, Set<TreeSpyListener>>();
		this.callbacksToGlobMatchers = new ConcurrentHashMap<TreeSpyListener, Set<PathMatcher>>();
	}

	/**
	 * Constructs a directory spy using the provided executor to orchestrate the
	 * background task, and the provided ExecutorService to execute callbacks.
	 * 
	 * @param daemonExecutor
	 * @param callbackExecutorService
	 * @throws IOException
	 */
	public TreeSpyJSE7StdLib(Executor daemonExecutor, WatchService watcher, ExecutorService callbackExecutorService)
			throws IOException {
		this(daemonExecutor, watcher);
		this.callbackExecutorService = callbackExecutorService;
		// In the default case, we don't use an executorservice, it needs to be
		// explicitly told.
		runCallbacksOnDaemonThread = false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void reset() throws IOException {
		stop();

		for (WatchKey key : watchKeysToDirectories.keySet()) {
			key.cancel();
		}

		watchKeysToDirectories = new ConcurrentHashMap<WatchKey, Path>();
		directoriesToListeners = new ConcurrentHashMap<Path, Set<TreeSpyListener>>();
		callbacksToGlobMatchers = new ConcurrentHashMap<TreeSpyListener, Set<PathMatcher>>();
	}

	/**
	 * {@inheritDoc}
	 */
	public void watchRecursive(File directory, TreeSpyListener callback) throws IOException {
		this.watch(directory, callback, true);
	}

	/**
	 * {@inheritDoc}
	 */
	public void watchJust(File directory, TreeSpyListener callback) throws IOException {
		this.watch(directory, callback, false);
	}

	/**
	 * {@inheritDoc}
	 */
	public void watchRecursive(File directory, TreeSpyListener callback, String... globs) throws IOException {
		registerGlobs(callback, globs);
		this.watch(directory, callback, true);
	}

	/**
	 * {@inheritDoc}
	 */
	public void watchJust(File directory, TreeSpyListener callback, String... globs) throws IOException {
		registerGlobs(callback, globs);
		this.watch(directory, callback, false);
	}

	/**
	 * Actual impelementation method for directory watching.
	 * 
	 * @param directory
	 *            The directory to watch.
	 * @param callback
	 *            The listener to be notified in case of events.
	 * @param recursive
	 *            Whether or not to recursuively watch subdirectories.
	 * @throws IOException
	 *             If the filesystem objects to registration.
	 */
	private void watch(File directory, TreeSpyListener callback, boolean recursive) throws IOException {
		if (!directory.isDirectory())
			throw new IllegalArgumentException("Path must be a directory");

		Path path = directory.toPath();
		if (!isWatched(path))
			register(path, callback, recursive);

		if (!running.get())
			start();

		log.info(String.format("Watching %s%s", path.toString(), recursive ? " and subdirectories." : "."));
	}

	/**
	 * Registers a mapping between the specified listener and it's set of globs.
	 * 
	 * @param callback
	 *            The listener to be notified pending evaluation of the globs.
	 * @param globs
	 *            The globs with which to restrict this listener.
	 */
	private void registerGlobs(TreeSpyListener callback, String[] globs) {
		if (!callbacksToGlobMatchers.containsKey(callback))
			callbacksToGlobMatchers.put(callback, new HashSet<PathMatcher>());

		Set<PathMatcher> storedMatchers = callbacksToGlobMatchers.get(callback);
		for (String glob : globs) {
			if (!glob.startsWith("glob:"))
				glob = "glob:" + glob;
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher(glob);
			storedMatchers.add(matcher);
		}
	}

	/**
	 * Indicates whether the specified directory already has a matching
	 * WatchKey.
	 * 
	 * @param directory
	 *            The path to check.
	 * @return True if there is an existing WatchKey for the path.
	 */
	private boolean isWatched(Path directory) {
		return watchKeysToDirectories.containsValue(directory);
	}

	/**
	 * Indicates whether the specified directory already has attached listeners.
	 * 
	 * @param directory
	 *            The path to check.
	 * @return True if there are attached listeners to the path.
	 */
	private boolean isListened(Path directory) {
		if (directoriesToListeners.containsKey(directory))
			return !directoriesToListeners.get(directory).isEmpty();
		return false;
	}

	/**
	 * Register the specified listener to the specified path, and recursively
	 * all subdirectories, if specified by the boolean.
	 * 
	 * @param path
	 *            The path to register the listener against.
	 * @param listener
	 *            The listener to be registered.
	 * @param all
	 *            Whether or not to recurse and register the listener against
	 *            all subdirectories of the specified path.
	 * @throws IOException
	 */
	private void register(Path path, TreeSpyListener listener, boolean all) throws IOException {

		FileVisitor<Path> visitor = makeVisitor(listener);

		if (all)
			Files.walkFileTree(path, visitor);
		else
			visitor.preVisitDirectory(path, Files.readAttributes(path, BasicFileAttributes.class));
	}

	/**
	 * Creates a FileVisitor that registers the specified callback at all
	 * directories it visits.
	 * 
	 * @param listener
	 *            A callback to be registered at any directories the visitor
	 *            visits.
	 * @return A FileVisitor implementation that registers the callback where it
	 *         visits.
	 */
	private FileVisitor<Path> makeVisitor(final TreeSpyListener listener) {
		return new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW);
				watchKeysToDirectories.put(key, dir);
				if (!isListened(dir))
					directoriesToListeners.put(dir, new LinkedHashSet<TreeSpyListener>());

				directoriesToListeners.get(dir).add(listener);

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				exc.printStackTrace(pw);
				log.warn(sw.toString());
				return FileVisitResult.SKIP_SUBTREE;
			}
		};
	}

	/**
	 * Attempts to notify all listeners of the specified key.
	 * 
	 * @param key
	 */
	private void notifyAll(WatchKey key) {
		Path directory = watchKeysToDirectories.get(key);

		Set<TreeSpyListener> listeners = directoriesToListeners.get(directory);

		for (WatchEvent<?> event : key.pollEvents()) {
			// Find the directory or file referenced by this WatchEvent
			WatchEvent<Path> ev = cast(event);
			Path filename = ev.context();
			Path child = directory.resolve(filename);

			Kind<?> kind = event.kind();

			// Find out ahead of time if a new directory has been created.
			boolean newDirectory = kind == ENTRY_CREATE && Files.isDirectory(child, NOFOLLOW_LINKS);

			for (TreeSpyListener listener : listeners) {

				Events eventType = Events.kindToEvent(kind);

				// If users have particularly heavy or frequent tasks in
				// callbacks, this provides the option to pass them off to an
				// executor service rather than clogging up the daemon thread.
				if (this.runCallbacksOnDaemonThread) {
					notify(listener, child, eventType);
				} else {
					notifyAsync(listener, child, eventType);
				}

				// If a new directory was created, register it and any
				// subdirectories.
				if (newDirectory) {
					try {
						register(child, listener, true);
					} catch (IOException ex) {
						log.warn(String.format("Could not register %s", child.toString()));
					}
				}
			}

			// Reset key to allow subsequent monitoring.
			boolean valid = key.reset();

			if (!valid) {
				log.warn(String.format("Invalid key - Directory %s is no longer accessible.", directory.toString()));
				watchKeysToDirectories.remove(key);
			}

		}
	}

	/**
	 * Notifies the specified listener with the data specified. The notification
	 * work is done on the thread provided by the callback ExecutorService.
	 * 
	 * @param listener
	 *            The listener to notify.
	 * @param path
	 *            The path with which to notify the listener.
	 * @param eventType
	 *            The type of event with which to notify the listener.
	 */
	private void notifyAsync(final TreeSpyListener listener, final Path path, final Events eventType) {
		callbackExecutorService.execute(new Runnable() {
			@Override
			public void run() {
				TreeSpyJSE7StdLib.this.notify(listener, path, eventType);
			}
		});
	}

	/**
	 * Notifies the specified listener with the data specified. The notification
	 * work is done synchronously, on the calling thread.
	 * 
	 * @param listener
	 *            The listener to notify.
	 * @param path
	 *            The path with which to notify the listener.
	 * @param eventType
	 *            The type of event with which to notify the listener.
	 */
	private void notify(final TreeSpyListener listener, final Path path, final Events eventType) {

		Set<PathMatcher> globs = callbacksToGlobMatchers.get(listener);

		boolean noGlobs = globs == null || globs.isEmpty();

		// LHS evaluated first. If there are NO globs, notify. If there ARE
		// globs, then matches also needs to return true in order to notify.
		if (noGlobs || matches(globs, path)) {
			listener.onChange(path, eventType);
		}
	}

	/**
	 * Checks whether any of the globs match the given path.
	 * 
	 * @param globs
	 *            A set of pathmatchers representing the globs to evaluate.
	 * @param path
	 *            The path to be evaluated.
	 * @return True, if any of the globs match the path.
	 */
	private boolean matches(Set<PathMatcher> globs, Path path) {
		Path file = path.getFileName();
		for (PathMatcher glob : globs) {
			if (glob.matches(file))
				return true;
		}
		return false;
	}

	/**
	 * Unchecked cast method suggested by the Java 7 SE API documentation for
	 * {@link java.nio.file.WatcherService}
	 * 
	 * @param event
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	/**
	 * {@inheritDoc}
	 */
	public void start() {
		daemonExecutor.execute(new WatchServiceRunnable(this));
		running.set(true);
		log.info("TreeSpy started spying.");
	}

	/**
	 * {@inheritDoc}
	 */
	public void stop() {
		if (running.get()) {
			running.set(false);
			log.info("TreeSpy stopped spying.");
		}
	}

	/**
	 * Runnable implementation for the background daemon thread. Can be lazily
	 * killed by setting the AtomicBoolean in the outer class.
	 * 
	 * @author wfaithfull
	 *
	 */
	private class WatchServiceRunnable implements Runnable {

		TreeSpyJSE7StdLib spy;

		public WatchServiceRunnable(TreeSpyJSE7StdLib spy) {
			this.spy = spy;
		}

		public void run() {
			while (running.get()) {

				WatchKey key;
				try {
					key = spy.watcher.take();
				} catch (InterruptedException ex) {
					log.error("Thread was interrupted unexpectedly", ex);
					return;
				}

				spy.notifyAll(key);
			}

			running.set(false);
		}

	}
}
