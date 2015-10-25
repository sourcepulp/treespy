package com.sourcepulp.treespy;

import java.nio.file.Path;

public interface TreeSpyListener {
	
	void onChange(Path file, Events type);
	
}
