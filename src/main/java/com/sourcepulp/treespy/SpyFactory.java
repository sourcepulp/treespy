package com.sourcepulp.treespy;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import com.sourcepulp.treespy.concurrent.TreeSpyExecutor;
import com.sourcepulp.treespy.concurrent.TreeSpyThreadFactory;
import com.sourcepulp.treespy.jse7.TreeSpyJSE7StdLib;

public class SpyFactory {

	/**
	 * Build a TreeSpy implementation with default settings.
	 * 
	 * @return a TreeSpy using the JSE7 API.
	 * @throws IOException
	 */
	public static TreeSpy getSpy() throws IOException {
		return new TreeSpyJSE7StdLib(getExecutor(), getWatchService());
	}

	/**
	 * Build a TreeSpy implementation with a custom ExecutorService for
	 * processing callbacks. Choose this option if you wish to exert control
	 * over which thread executes the callbacks.
	 * 
	 * @param callbackExecutorService
	 *            The ExecutorService to be used for carrying out the callbacks.
	 * @return a TreeSpy using the JSE7 API using the specified ExecutorService.
	 * @throws IOException
	 */
	public static TreeSpy getSpy(ExecutorService callbackExecutorService) throws IOException {
		return new TreeSpyJSE7StdLib(getExecutor(), getWatchService(), callbackExecutorService);
	}
	
	private static WatchService getWatchService() throws IOException {
		return FileSystems.getDefault().newWatchService();
	}

	private static ThreadFactory getThreadFactory() {
		return new TreeSpyThreadFactory();
	}

	private static TreeSpyExecutor getExecutor() {
		return new TreeSpyExecutor(getThreadFactory());
	}
}
