package com.sourcepulp.treespy;

import java.io.File;
import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		
		TreeSpy spy = new TreeSpy();
		
		String directory = System.getProperty("user.dir");
		
		File home = new File(directory);
		
		spy.watch(home, (changedFile, eventType) -> {
			System.out.println(String.format("File %s changed", changedFile));
		});
		
		System.in.read();
		
	}
}
