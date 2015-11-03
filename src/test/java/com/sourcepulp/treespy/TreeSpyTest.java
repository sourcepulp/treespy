package com.sourcepulp.treespy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public class TreeSpyTest {

	public TreeSpyTest(int param) {	}

	private final static Logger log = LoggerFactory.getLogger(TreeSpyTest.class);

	final static int WAIT_TIME_MILLIS = 500;
	private final static int NUM_RUNS = 5;	

	private final CountDownLatch countDownLatch = new CountDownLatch(1);
	private File modifyFile;
	private List<CaughtFileEvent> caughtEvents;

	private class CaughtFileEvent {

		private Path path;
		private Events event;
		private LocalDateTime timestamp;

		public CaughtFileEvent(Path file, Events event) {
			this.path = file;
			this.event = event;
			this.timestamp = LocalDateTime.now();
		}

		@SuppressWarnings("unused")
		public Path getPath() {
			return path;
		}

		public Events getEvent() {
			return event;
		}

		public String getFileName() {
			return path.getFileName().toString();
		}

		@SuppressWarnings("unused")
		public LocalDateTime getTimeStamp() {
			return timestamp;
		}
	}

	@Parameters
	public static Collection<Object[]> generateParams() {
		List<Object[]> params = new ArrayList<Object[]>();
		for (int i = 1; i <= NUM_RUNS; i++) {
			params.add(new Object[] { i });
		}
		return params;
	}

	@Before
	public void setup() throws IOException {
		caughtEvents = new ArrayList<CaughtFileEvent>();
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
			setCaughtEvent(f, t);
		});

		try {
			write(modifyFile.toPath(), "hello");
			await();

			Assert.assertNotEquals(0, caughtEvents.size());
			caughtEvents.forEach(f -> {
				Assert.assertEquals(modifyFile.getName(), f.getFileName());
				Assert.assertEquals(Events.MODIFY, f.getEvent());
			});
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
			setCaughtEvent(f, t);
		});
		try {
			newFile.createNewFile();
			await();

			Assert.assertNotEquals(0, caughtEvents.size());
			caughtEvents.forEach(f -> {
				Assert.assertEquals(newFileName, f.getFileName());
				Assert.assertEquals(Events.CREATE, f.getEvent());
			});
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
			setCaughtEvent(f, t);
		} , "*.txt");
		try {
			newFile.createNewFile();
			await();

			Assert.assertNotEquals(0, caughtEvents.size());
			caughtEvents.forEach(f -> {
				Assert.assertEquals(newFileName, f.getFileName());
				Assert.assertEquals(Events.CREATE, f.getEvent());
			});

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
			setCaughtEvent(f, t);

		} , "}}}+==--$%566&(6");
		try {
			newFile.createNewFile();
			await();

			Assert.assertEquals(0, caughtEvents.size());
		} finally {
			newFile.delete();
			spy.stop();
		}
	}

	@Test
	public void testDelete() throws IOException, InterruptedException {
		TreeSpy spy = SpyFactory.getSpy();

		spy.watchRecursive(modifyFile.getAbsoluteFile().getParentFile(), (f, t) -> {
			setCaughtEvent(f, t);
		});
		try {
			modifyFile.delete();
			await();
			Assert.assertNotEquals(0, caughtEvents.size());

			CaughtFileEvent f = caughtEvents.get(caughtEvents.size() - 1);
			Assert.assertEquals(modifyFile.getName(), f.getFileName());
			Assert.assertEquals(Events.DELETE, f.getEvent());
		} finally {
			spy.stop();
			clearCaughtEvent();
		}
	}

	private void setCaughtEvent(Path file, Events eventType) {
		caughtEvents.add(new CaughtFileEvent(file, eventType));
	}

	private void clearCaughtEvent() {
		caughtEvents.clear();
	}

	private void write(Path file, String s) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(file, Charset.defaultCharset())) {
			bw.write(s);
		}
	}

	private void logCaughtEvents() {
		caughtEvents.forEach(f -> {
			log.info(String.format("%s => %s", f.getFileName(), f.event.toString()));
		});
	}

	/**
	 * Asyncronously wait a fixed amount of time for the events to be
	 * registered.
	 * 
	 * @throws InterruptedException
	 */
	private void await() throws InterruptedException {
		countDownLatch.await(WAIT_TIME_MILLIS, TimeUnit.MILLISECONDS);
		logCaughtEvents();
	}

}
