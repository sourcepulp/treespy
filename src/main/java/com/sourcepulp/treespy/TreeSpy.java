package com.sourcepulp.treespy;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
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

/**
 * 
 * @author Will Faithfull
 *
 */
public class TreeSpy {

	private final Logger log = LoggerFactory.getLogger(TreeSpy.class);

	private WatchService watcher;
	private FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
			watchKeysToDirectories.put(key, dir);
			log.info("Registering " + dir.toString());
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			// TODO Auto-generated method stub
			return FileVisitResult.CONTINUE;
		}
	};

	private ConcurrentMap<WatchKey, Path> watchKeysToDirectories;
	private ConcurrentMap<Path, Set<TreeSpyListener>> directoriesToListeners;

	private AtomicBoolean running = new AtomicBoolean(false);

	private ThreadFactory threadFactory;

	public TreeSpy() throws IOException {
		this(new TreeSpyThreadFactory());
	}
	
	public TreeSpy(ThreadFactory threadFactory) throws IOException {
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
			register(path, recurse);

		if (!isListened(path))
			directoriesToListeners.put(path, new LinkedHashSet<TreeSpyListener>());

		directoriesToListeners.get(path).add(callback);

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

	private void register(Path path, boolean all) throws IOException {
		if (all)
			Files.walkFileTree(path, visitor);
		else
			visitor.preVisitDirectory(path, Files.readAttributes(path, BasicFileAttributes.class));
	}

	private void notifyAll(WatchKey key) {
		Path directory = watchKeysToDirectories.get(key);
		if(!directoriesToListeners.containsKey(directory))
			return;
		
		Set<TreeSpyListener> listeners = directoriesToListeners.get(directory);

		for (WatchEvent<?> event : key.pollEvents()) {
			@SuppressWarnings("unchecked")
			WatchEvent<Path> ev = (WatchEvent<Path>) event;
			Path filename = ev.context();
			Path child = directory.resolve(filename);

			@SuppressWarnings("rawtypes")
			Kind kind = event.kind();

			for (TreeSpyListener listener : listeners) {
				log.info("notifying listener");
				listener.onChange(child, Events.kindToEvent(kind));
			}

			if (kind == ENTRY_CREATE && Files.isDirectory(child, NOFOLLOW_LINKS)) {
				try {
					register(child, true);
				} catch (IOException ex) {

				}
			}
		}
	}

	public void start() {
		threadFactory.newThread(new WatchServiceRunnable(this));
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

		TreeSpy spy;

		public WatchServiceRunnable(TreeSpy spy) {
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
