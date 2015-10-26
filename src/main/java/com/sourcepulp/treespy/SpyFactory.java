package com.sourcepulp.treespy;

import java.io.IOException;
import java.util.concurrent.ThreadFactory;

import com.sourcepulp.treespy.jse7.*;

public class SpyFactory {

	public static TreeSpy getSpy() throws IOException {
		return new TreeSpyJSE7StdLib(getExecutor());
	}
	
	private static ThreadFactory getThreadFactory() {
		return new TreeSpyThreadFactory();
	}
	
	private static TreeSpyExecutor getExecutor() {
		return new TreeSpyExecutor(getThreadFactory());
	}
}
