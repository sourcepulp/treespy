package com.sourcepulp.treespy;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.WatchService;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.sourcepulp.treespy.jse7.TreeSpyJSE7StdLib;

/**
 * Unit test for simple App.
 */
public class TreeSpyTest 
{
	TreeSpyJSE7StdLib spy;
	String dirName = System.getProperty("user.dir");
	
	@Before
	public void setup() throws IOException {
		WatchService watcher = Mockito.mock(WatchService.class);
		Executor executor = Mockito.mock(Executor.class);
		spy = new TreeSpyJSE7StdLib(executor, watcher);
	}
	
    @Test
    public void testApp() throws IOException
    {	
    	File directory = new File(dirName);
    	
    	spy.watch(directory, (f,t) -> {
    		if(t == Events.CREATE) {
    			System.out.println(String.format("Created file %s", f.toString()));   			
    		}
    	});
    	
    	// TODO: Work out some way to test concurrently :s
    	
    	assertTrue( true );
    }
}
