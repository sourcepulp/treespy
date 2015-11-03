package com.sourcepulp.treespy;

import java.io.File;
import java.io.IOException;

public interface TreeSpy {

	/**
	 * Watches the specified directory and all subdirectories, invoking the
	 * callback when a change is signalled.
	 * 
	 * @param directory
	 *            The directory to be watched.
	 * @param callback
	 *            A TreeSpyListener compliant callback.
	 * @throws IOException
	 */
	public void watchRecursive(File directory, TreeSpyListener callback) throws IOException;

	/**
	 * Watches the specified directory and all subdirectories, invoking the
	 * callback when a change is signalled.
	 * 
	 * @param directory
	 *            The directory to be watched.
	 * @param callback
	 *            A TreeSpyListener compliant callback.
	 * @throws IOException
	 */
	public void watchRecursive(File directory, TreeSpyListener callback, String... globs) throws IOException;

	/**
	 * Watches the specified directory, invoking the callback when a change is
	 * signalled. If recurse is set to true, then this will recursively watch
	 * all subdirectories.
	 * 
	 * @param directory
	 *            The directory to be watched.
	 * @param callback
	 *            A TreeSpyListener compliant callback.
	 * @param globs
	 *            Glob expressions with which to restrict this listener.
	 * @throws IOException
	 */
	public void watchJust(File directory, TreeSpyListener callback) throws IOException;

	/**
	 * Watches the specified directory, invoking the callback when a change is
	 * signalled. If recurse is set to true, then this will recursively watch
	 * all subdirectories.
	 * 
	 * @param directory
	 *            The directory to be watched.
	 * @param callback
	 *            A TreeSpyListener compliant callback.
	 * @param globs
	 *            Glob expressions with which to restrict this listener.
	 * @throws IOException
	 */
	public void watchJust(File directory, TreeSpyListener callback, String... globs) throws IOException;

	/**
	 * Manually starts the service.
	 */
	public void start();

	/**
	 * Manually stops the service.
	 */
	public void stop();

	/**
	 * Clears all stored WatchKeys and callbacks.
	 * 
	 * @throws IOException
	 */
	public void reset() throws IOException;

}
