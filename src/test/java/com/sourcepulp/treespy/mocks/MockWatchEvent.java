package com.sourcepulp.treespy.mocks;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public class MockWatchEvent implements WatchEvent<Path> {
	
	private Path context;
	private int count;
	private WatchEvent.Kind<Path> kind;
	
	public MockWatchEvent(Path context, int count, WatchEvent.Kind<Path> kind) {
		this.context = context;
		this.count = count;
		this.kind = kind;
	}

	@Override
	public Path context() {
		return context;
	}

	@Override
	public int count() {
		return count;
	}

	@Override
	public java.nio.file.WatchEvent.Kind<Path> kind() {
		return kind;
	}

}
