# treespy
Simplified, zero dependency recursive directory watching API for Java SE 7+

### Watch a directory recursively
    
    TreeSpy spy = SpyFactory.getSpy();
    
    String directory = "/home/wfaithfull"
	
	File home = new File(directory);
	
	spy.watchRecursive(home, (changedFile, eventType) -> {
		System.out.println(String.format("File %s changed", changedFile));
	});

### Watch a directory recursively with a glob pattern

	TreeSpy spy = SpyFactory.getSpy();
    
    String directory = "/home/wfaithfull"
	
	File home = new File(directory);
	
	spy.watchRecursive(home, (changedFile, eventType) -> {
		System.out.println(String.format("File %s changed", changedFile));
	}, "glob:*.java", "glob:*.txt");
