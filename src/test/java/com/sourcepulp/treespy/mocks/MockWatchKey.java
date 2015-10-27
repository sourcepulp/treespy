package com.sourcepulp.treespy.mocks;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.ArrayList;
import java.util.List;

public class MockWatchKey implements WatchKey {
	
	private Path root;
	
	private boolean valid;
	private boolean resettable;
	private WatchEventGenerator generator;
	
	public MockWatchKey(Path root, WatchEventGenerator generator, boolean valid, boolean resettable) {
		this.root = root;
		this.generator = generator;
		generator.setRoot(root);
		this.valid = valid;
		this.resettable = resettable;
	}

	@Override
	public void cancel() {
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public List<WatchEvent<?>> pollEvents() {
		List<WatchEvent<?>> events = new ArrayList<>();
		events.add(generator.generate());
		return events;
	}

	@Override
	public boolean reset() {
		return resettable;
	}

	@Override
	public Watchable watchable() {
		return this.root;
	}

	public void setResettable(boolean resettable) {
		this.resettable = resettable;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}
	
	public static class WatchKeyBuilder {
		
		private WatchEventGenerator generator;
		private boolean valid;
		private boolean resettable;
		private Path root;
		
		public WatchKeyBuilder() {
			
		}
		
		public WatchKeyBuilder withGenerator(WatchEventGenerator generator) {
			this.generator = generator;
			return this;
		}
		
		public WatchKeyBuilder withValid(boolean valid) {
			this.valid = valid;
			return this;
		}
		
		public WatchKeyBuilder withResettable(boolean resettable) {
			this.resettable = resettable;
			return this;
		}
		
		public WatchKeyBuilder withRoot(Path root) {
			this.root = root;
			return this;
		}
		
		public WatchKey build() {
			return new MockWatchKey(this.root, this.generator, this.valid, this.resettable);
		}
		
	}

}
