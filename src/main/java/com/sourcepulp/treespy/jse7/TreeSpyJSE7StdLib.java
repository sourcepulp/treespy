package com.sourcepulp.treespy.jse7;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
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

	private final Logger log = LoggerFactory.getLogger(TreeSpyJSE7StdLib.class);

	private WatchService watcher;

	private ConcurrentMap<WatchKey, Path> watchKeysToDirectories;
	private ConcurrentMap<Path, Set<TreeSpyListener>> directoriesToListeners;

	private AtomicBoolean running = new AtomicBoolean(false);

	private ThreadFactory threadFactory;

	public TreeSpyJSE7StdLib() throws IOException {
		this(new TreeSpyThreadFactory());
	}

	/**
	 * Constructs a directory spy using the provided threadfactory. This
	 * constructor is provided should somebody wish to maintain greater control
	 * over the background thread used for watching.
	 * 
	 * @param threadFactory
	 * @throws IOException
	 */
	public TreeSpyJSE7StdLib(ThreadFactory threadFactory) throws IOException {
		this.threadFactory = threadFactory;
		reset();
	}

	public void reset() throws IOException {
		stop();

		watcher = FileSystems.getDefault().newWatchService();
		watchKeysToDirectories = new ConcurrentHashMap<WatchKey, Path>();
		directoriesToListeners = new ConcurrentHashMap<Path, Set<TreeSpyListener>>();
	}

	public void watch(File directory, TreeSpyListener callback) throws IOException {
		this.watch(directory, callback, true);
	}

	public void watch(File directory, TreeSpyListener callback, boolean recurse) throws IOException {
		if (!directory.isDirectory())
			throw new IllegalArgumentException("Path must be a directory");

		Path path = directory.toPath();
		if (!isWatched(path))
			register(path, callback, recurse);

		if (!running.get())
			start();

		log.info(String.format("Watching %s%s", path.toString(), recurse ? " and subdirectories." : "."));
	}

	public boolean isWatched(Path directory) {
		return watchKeysToDirectories.containsValue(directory);
	}

	public boolean isListened(Path directory) {
		return directoriesToListeners.containsKey(directory);
	}

	private void register(Path path, TreeSpyListener listener, boolean all) throws IOException {

		FileVisitor<Path> visitor = makeVisitor(listener);

		if (all)
			Files.walkFileTree(path, visitor);
		else
			visitor.preVisitDirectory(path, Files.readAttributes(path, BasicFileAttributes.class));
	}

	private FileVisitor<Path> makeVisitor(final TreeSpyListener listener) {
		return new SimpleFileVisitor<Path>() {

			TreeSpyListener innerListener = listener;

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW);
				watchKeysToDirectories.put(key, dir);
				if (!isListened(dir))
					directoriesToListeners.put(dir, new LinkedHashSet<TreeSpyListener>());

				directoriesToListeners.get(dir).add(innerListener);

				log.info("Registering " + dir.toString());
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

	private void notifyAll(WatchKey key) {
		Path directory = watchKeysToDirectories.get(key);

		Set<TreeSpyListener> listeners = directoriesToListeners.get(directory);

		for (WatchEvent<?> event : key.pollEvents()) {
			@SuppressWarnings("unchecked")
			WatchEvent<Path> ev = (WatchEvent<Path>) event;
			Path filename = ev.context();
			Path child = directory.resolve(filename);

			@SuppressWarnings("rawtypes")
			Kind kind = event.kind();

			boolean newDirectory = kind == ENTRY_CREATE && Files.isDirectory(child, NOFOLLOW_LINKS);

			for (TreeSpyListener listener : listeners) {
				listener.onChange(child, Events.kindToEvent(kind));

				if (newDirectory) {
					try {
						register(child, listener, true);
					} catch (IOException ex) {

					}
				}
			}

		}
	}

	public void start() {
		Thread t = threadFactory.newThread(new WatchServiceRunnable(this));
		t.start();
		running.set(true);
		log.info("TreeSpy started spying.");
	}

	public void stop() {
		if (running.get()) {
			running.set(false);
			log.info("TreeSpy stopped spying.");
		}
	}

	private class WatchServiceRunnable implements Runnable {

		TreeSpyJSE7StdLib spy;

		public WatchServiceRunnable(TreeSpyJSE7StdLib spy) {
			this.spy = spy;
		}

		public void run() {
			while (running.get()) {
				WatchKey key;
				try {
					key = watcher.take();
				} catch (InterruptedException ex) {
					return;
				}

				spy.notifyAll(key);
			}

			running.set(false);
		}

	}
}
