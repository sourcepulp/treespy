package com.sourcepulp.treespy;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	
	final static Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws IOException {
		
		TreeSpy spy = SpyFactory.getSpy();
		
		String directory = System.getProperty("user.dir");
		
		File home = new File(directory);
		
		spy.watch(home, (changedFile, eventType) -> {
			log.info(String.format("File %s changed, mode %s", changedFile, eventType.toString()));
		});
		
		System.in.read();
		
	}
}
