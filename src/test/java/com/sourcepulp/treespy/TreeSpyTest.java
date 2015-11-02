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
		if (modifyFile.exists())
			modifyFile.delete();

		this.clearCaughtEvent();
	}

	@Test
	public void testModify() throws IOException, InterruptedException {
		File directory = modifyFile.getAbsoluteFile().getParentFile();

		TreeSpy spy = SpyFactory.getSpy();

		spy.watchRecursive(directory, (f, t) -> {
			log.info("Modification detected");
			setCaughtEvent(f, t);
		});

		try {
			write(modifyFile.toPath(), "hello");
			await();

			Assert.assertEquals(modifyFile.getAbsolutePath(), caughtFile.toString());
			Assert.assertEquals(Events.MODIFY, caughtEvent);
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

		spy.watchRecursive(resourcesDir, (f, t) -> {
			log.info("Creation detected");
			setCaughtEvent(f, t);
		});
		try {
			newFile.createNewFile();
			await();

			Assert.assertEquals(newFileName, caughtFile.getFileName().toString());
			Assert.assertEquals(Events.CREATE, caughtEvent);
		} finally {
			newFile.delete();
			spy.stop();
		}
	}

	@Test
	public void testValidGlobCreate() throws IOException, InterruptedException {

		File resourcesDir = new File(System.getProperty("user.dir"));
		TreeSpy spy = SpyFactory.getSpy();

		final String newFileName = "create.txt";
		File newFile = new File(newFileName);

		spy.watchRecursive(resourcesDir, (f, t) -> {
			log.info("Creation detected");
			setCaughtEvent(f, t);
		} , "*.txt");
		try {
			newFile.createNewFile();
			await();

			Assert.assertNotNull(caughtFile);
			Assert.assertEquals(newFileName, caughtFile.getFileName().toString());
			Assert.assertEquals(Events.CREATE, caughtEvent);
		} finally {
			newFile.delete();
			spy.stop();
		}
	}

	@Test
	public void testInvalidGlobCreate() throws IOException, InterruptedException {

		File resourcesDir = new File(System.getProperty("user.dir"));
		TreeSpy spy = SpyFactory.getSpy();

		final String newFileName = "create.txt";
		File newFile = new File(newFileName);

		spy.watchRecursive(resourcesDir, (f, t) -> {
			log.info("Creation detected");
			setCaughtEvent(f, t);

		} , "}}}+==--$%566&(6");
		try {
			newFile.createNewFile();
			await();

			Assert.assertEquals(null, caughtFile);
			Assert.assertEquals(null, caughtEvent);
		} finally {
			newFile.delete();
			spy.stop();
		}
	}

	@Test
	public void testDelete() throws IOException, InterruptedException {
		TreeSpy spy = SpyFactory.getSpy();

		spy.watchRecursive(modifyFile.getAbsoluteFile().getParentFile(), (f, t) -> {
			log.info("Deletion detected");
			setCaughtEvent(f, t);
		});
		try {
			modifyFile.delete();
			await();
			Assert.assertEquals(modifyFile.getName(), caughtFile.getFileName().toString());
			Assert.assertEquals(Events.DELETE, caughtEvent);
		} finally {
			spy.stop();
			clearCaughtEvent();
		}
	}

	private Path caughtFile;
	private Events caughtEvent;

	private void setCaughtEvent(Path file, Events eventType) {
		this.caughtFile = file;
		this.caughtEvent = eventType;
	}

	private void clearCaughtEvent() {
		this.caughtFile = null;
		this.caughtEvent = null;
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
