package com.sourcepulp.treespy.mocks;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.Random;

public class WatchEventGenerator {

	float[] frequencies;
	private Random random;

	public WatchEventGenerator(Path root, float cFreq, float mFreq, float dFreq, float oFreq) {
		this(root, new float[] { cFreq, mFreq, dFreq, oFreq });
	}
	
	public WatchEventGenerator(Path root, float... freq) {
		this.setRoot(root);
		frequencies = freq;
		cumulate(frequencies);
		random = new Random();
	}

	private static void cumulate(float[] frequencies) {
		for (int i = 1; i < frequencies.length; i++) {
			frequencies[i] = frequencies[i - 1] + frequencies[i];
		}
	}

	private int choose(float[] cumFreq) {
		float rand = random.nextFloat();
		for (int i = 0; i < cumFreq.length; i++) {
			if (rand > cumFreq[i])
				continue;

			return i;
		}

		return 0;
	}

	private Path root;

	public WatchEvent<?> generate() {

		Kind<Path> kind = null;

		switch (choose(frequencies)) {
		case 0:
			kind = ENTRY_CREATE;
		case 1:
			kind = ENTRY_MODIFY;
		case 2:
			kind = ENTRY_DELETE;
		case 3:
			return new MockOverflowEvent();
		}

		return new MockWatchEvent(getRoot(), 1, kind);
	}

	public Path getRoot() {
		return root;
	}

	public void setRoot(Path root) {
		this.root = root;
	}
}
