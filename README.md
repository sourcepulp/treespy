# treespy
Simplified, zero dependency recursive directory watching API for Java SE 7+
    
    TreeSpy spy = SpyFactory.getSpy();
    
    String directory = "/home/wfaithfull"
	
	File home = new File(directory);
	
	spy.watch(home, (changedFile, eventType) -> {
		System.out.println(String.format("File %s changed", changedFile));
	});
