# treespy
Simplified, zero dependency recursive directory watching API for Java
    
    TreeSpy spy = new TreeSpy();
    
    String directory = "/home/wfaithfull"
	
	File home = new File(directory);
	
	spy.watch(home, (changedFile, eventType) -> {
		System.out.println(String.format("File %s changed", changedFile));
	});
