package com.sourcepulp.treespy.mocks;

import java.io.IOException;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MockWatchService implements WatchService {
	
	private List<WatchKey> watchKeys;
	private int pointer = 0;
	
	private WatchKey getNext() {
		if(pointer == watchKeys.size() - 1)
			pointer = 0;
		
		// postfix
		return watchKeys.get(pointer++);
	}
	
	public MockWatchService(List<WatchKey> keys) {
		this.watchKeys = keys;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public WatchKey poll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WatchKey take() throws InterruptedException {
		return getNext();
	}
	
	public static class WatchServiceBuilder {
		
		private List<WatchKey> watchKeys;
		
		public WatchServiceBuilder() {
			this.watchKeys = new ArrayList<WatchKey>();
		}
		
		public WatchServiceBuilder withKey(WatchKey key) {
			this.watchKeys.add(key);
			return this;
		}
		
		public WatchService build() {
			return new MockWatchService(this.watchKeys);
		}
	}

}
