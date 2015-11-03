package com.sourcepulp.treespy;

import java.nio.file.Path;

public interface TreeSpyListener {

	/**
	 * Method that is called when a file change event is detected.
	 * 
	 * @param file
	 *            The path to the changed file
	 * @param type
	 *            The type of event; CREATE, MODIFY, DELETE or OVERFLOW.
	 */
	void onChange(Path file, Events type);

}
