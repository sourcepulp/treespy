package com.sourcepulp.treespy;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.util.concurrent.ThreadFactory;

import com.sourcepulp.treespy.concurrent.TreeSpyExecutor;
import com.sourcepulp.treespy.concurrent.TreeSpyThreadFactory;
import com.sourcepulp.treespy.jse7.TreeSpyJSE7StdLib;

public class SpyFactory {

	public static TreeSpy getSpy() throws IOException {
		WatchService watcher = FileSystems.getDefault().newWatchService();
		return new TreeSpyJSE7StdLib(getExecutor(), watcher);
	}
	
	private static ThreadFactory getThreadFactory() {
		return new TreeSpyThreadFactory();
	}
	
	private static TreeSpyExecutor getExecutor() {
		return new TreeSpyExecutor(getThreadFactory());
	}
}
