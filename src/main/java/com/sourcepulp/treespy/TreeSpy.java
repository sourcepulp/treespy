package com.sourcepulp.treespy;

import java.io.File;
import java.io.IOException;

public interface TreeSpy {

	public void watch(File directory, TreeSpyListener callback) throws IOException;

	public void watch(File directory, TreeSpyListener callback, boolean recurse) throws IOException;
	
	public void start();
	
	public void stop();

}
