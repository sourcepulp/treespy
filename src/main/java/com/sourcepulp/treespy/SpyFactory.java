package com.sourcepulp.treespy;

import java.io.IOException;

import com.sourcepulp.treespy.jse7.*;

public class SpyFactory {

	public static TreeSpy getSpy() throws IOException {
		return new TreeSpyJSE7StdLib();
	}
}
