package com.sourcepulp.treespy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreeSpyTest {
	private final static Logger log = LoggerFactory.getLogger(TreeSpyTest.class);

	final static int WAIT_TIME_MILLIS = 500;

	private final CountDownLatch countDownLatch = new CountDownLatch(1);
	private File modifyFile;

	@Before
	public void setup() throws IOException {
		modifyFile = new File("testfile.txt");
		modifyFile.createNewFile();
	}

	@After
	public void teardown() {
		if(modifyFile.exists())
			modifyFile.delete();
	}

	@Test
	public void testModify() throws IOException, InterruptedException {
		File directory = modifyFile.getAbsoluteFile().getParentFile();

		TreeSpy spy = SpyFactory.getSpy();

		spy.watch(directory, (f, t) -> {
			if (t == Events.MODIFY) {
				log.info("Modification detected");
				Assert.assertEquals(modifyFile.getAbsolutePath(), f.toString());
			}
		});

		try {
			write(modifyFile.toPath(), "hello");
			await();
		} finally {
			spy.stop();
		}
	}

	@Test
	public void testCreate() throws IOException, InterruptedException {

		File resourcesDir = new File(System.getProperty("user.dir"));
		TreeSpy spy = SpyFactory.getSpy();

		final String newFileName = "create.txt";
		File newFile = new File(newFileName);

		spy.watch(resourcesDir, (f, t) -> {
			if (t == Events.CREATE) {
				log.info("Creation detected");
				Assert.assertEquals(newFileName, f.getFileName().toString());
			}
		});
		try {
			newFile.createNewFile();
			await();
		} finally {
			newFile.delete();
			spy.stop();
		}
	}
	
	@Test
	public void testDelete() throws IOException, InterruptedException {
		TreeSpy spy = SpyFactory.getSpy();
		
		spy.watch(modifyFile.getAbsoluteFile().getParentFile(), (f, t) -> {
			if (t == Events.DELETE) {
				log.info("Deletion detected");
				Assert.assertEquals(modifyFile.getName(), f.getFileName().toString());
			}
		});
		try {
			modifyFile.delete();
			await();
		} finally {
			spy.stop();
		}
	}

	private void write(Path file, String s) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(file, Charset.defaultCharset())) {
			bw.write(s);
		}
	}

	private void await() throws InterruptedException {
		countDownLatch.await(WAIT_TIME_MILLIS, TimeUnit.MILLISECONDS);
	}

}
