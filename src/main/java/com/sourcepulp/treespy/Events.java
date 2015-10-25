package com.sourcepulp.treespy;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

public enum Events {
	CREATE,
	MODIFY,
	DELETE,
	OVERFLOW;
	
	public static Events kindToEvent(@SuppressWarnings("rawtypes") WatchEvent.Kind kind) {
		if(kind == StandardWatchEventKinds.OVERFLOW)
			return OVERFLOW;
		else if(kind == ENTRY_CREATE)
			return CREATE;
		else if(kind == ENTRY_DELETE)
			return DELETE;
		else
			return MODIFY;

	}
}
