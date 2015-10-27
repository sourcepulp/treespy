package com.sourcepulp.treespy.mocks;

import java.nio.file.WatchEvent;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class MockOverflowEvent implements WatchEvent<Object> {

	@Override
	public Object context() {
		return null;
	}

	@Override
	public int count() {
		return 1;
	}

	@Override
	public java.nio.file.WatchEvent.Kind<Object> kind() {
		return OVERFLOW;
	}

}
