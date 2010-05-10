package xc.mst.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

public class SetupClasspath {
	
	public static void setupClasspath(String dir) {
		if (dir == null) {
			dir = "MetadataServicesToolkit";
		}
    	try {
    		String rootDir = null;
    		if (System.getenv("MST_ROOT_DIR") != null) {
    			rootDir = System.getenv("MST_ROOT_DIR");
    			if (rootDir.indexOf("beluga") != -1) {
    				rootDir = null;
    			}
    		}
    		if (rootDir == null) {
	    		try {
	    			BufferedReader reader = new BufferedReader(new InputStreamReader(
	    					SetupClasspath.class.getClassLoader().getResourceAsStream(
		    			        "env.properties")));
		    		Properties props = new Properties();
		    		props.load(reader);
		    		reader.close();
		    		if (props.getProperty("mst.root.dir") != null) {
		    			rootDir = props.getProperty("mst.root.dir");
		    		}
	    		} catch (Throwable t) {
	    			t.printStackTrace(System.out);
	    			t.printStackTrace(System.err);
	    		}
    		}

    		if (rootDir == null) {
    	    	File workingDir = new File(".");
    	    	rootDir = workingDir.getAbsolutePath();
    	    	rootDir += "/";
    		}
    		MSTConfiguration.rootDir = rootDir;
    		System.out.println("rootDir: "+rootDir);
    		String fileProto = "file:";
    		if (!rootDir.startsWith("/")) {
    			fileProto = fileProto+"/";
    		}
	    	String url = fileProto+rootDir+"MST-instances/"+dir+"/";
	    	url = url.replaceAll("\\\\", "/");
	    	System.out.println("url: "+url);
	    	addURL(new URL(url));
    	} catch (Throwable t) {
    		throw new RuntimeException(t);
    	}
	}
	

	@SuppressWarnings("unchecked")
	public static void addURL(URL u) {

		URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class sysclass = URLClassLoader.class;
		
		for (URL u2 : sysloader.getURLs()) {
			System.out.println("u: "+u2);
		}

		try {
			Method method = sysclass.getDeclaredMethod("addURL",  new Class[]{URL.class});
			method.setAccessible(true);
			method.invoke(sysloader, new Object[]{u});
		} catch (Throwable t) {
			t.printStackTrace();
			throw new RuntimeException("Error, could not add URL to system classloader");
		}

	}

}