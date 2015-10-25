package com.sourcepulp.treespy;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
	TreeSpy watcher;
	
	@Before
	public void setup() throws IOException {
		watcher = new TreeSpy();
	}
	
    @Test
    public void testApp() throws IOException
    {
    	String homeDir = System.getProperty("user.home");
    	
    	File directory = new File(homeDir);
    	
    	watcher.watch(directory, (f,t) -> {
    		if(t == Events.CREATE) {
    			System.out.println(String.format("Created file %s", f.toString()));   			
    		}
    	});
    	
    	// TODO: Work out some way to test concurrently :s
    	
    	assertTrue( true );
    }
}
