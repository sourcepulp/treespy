package com.sourcepulp.treespy.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public class TreeSpyExecutor implements Executor {
	
	private ThreadFactory threadFactory;
	
	public TreeSpyExecutor(ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}

	@Override
	public void execute(Runnable arg0) {
		threadFactory.newThread(arg0).start();
	}

}
