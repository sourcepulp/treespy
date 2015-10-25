package com.sourcepulp.treespy;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TreeSpyThreadFactory implements ThreadFactory {
	
	private Logger log = LoggerFactory.getLogger(TreeSpyThreadFactory.class);
	private final static ThreadGroup group = new ThreadGroup("TreeSpyThreadGroup");
	
	private UncaughtExceptionHandler handler = new UncaughtExceptionHandler() {
		
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			log.warn(e.getMessage());
		}
	};

	@Override
	public Thread newThread(Runnable arg0) {
		Thread t = new Thread(group, arg0, "TreeSpy");
		t.setUncaughtExceptionHandler(handler);
		return t;
	}

}