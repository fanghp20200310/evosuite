package org.evosuite.runtime.agent;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolsJarLocator {

	private static final Logger logger = LoggerFactory.getLogger(ToolsJarLocator.class);


	/**
	 * Full name of a class in tools.jar that is used inside EvoSuite
	 */
	private static final String EXAMPLE_CLASS =  "com.sun.tools.attach.VirtualMachine";

    private String locationNotOnClasspath;

    private String manuallySpecifiedToolLocation;

    public ToolsJarLocator(String manuallySpecifiedToolLocation) {
        this.manuallySpecifiedToolLocation = manuallySpecifiedToolLocation;
    }

    /**
	 * Try to locate tools.jar and return a classloader for it
	 * 
	 * @throws RuntimeException  if it was not possible to locate/load tools.jar
	 */
	public ClassLoader getLoaderForToolsJar() throws RuntimeException{

		try {
			Class.forName(EXAMPLE_CLASS,true,ClassLoader.getSystemClassLoader());
			logger.info("Tools.jar already on system classloader");
			return ClassLoader.getSystemClassLoader(); //if this code is reached, the tools.jar is available on system classpath
		} catch (ClassNotFoundException e) {
			//OK, it is missing, so lets try to locate it
		}

		try {
			Class.forName(EXAMPLE_CLASS);
			logger.info("Tools.jar already on current classloader");
			return ToolsJarLocator.class.getClassLoader(); //if this code is reached, the tools.jar is available on classpath
		} catch (ClassNotFoundException e) {
			//OK, it is missing, so lets try to locate it
		}
		
		if(manuallySpecifiedToolLocation != null){
			//if defined, then use it, and throws exception if it is not valid
			return considerPathInProperties();
		}

		String javaHome = System.getProperty("java.home");
		String[] locations = new String[]{
				javaHome+"/../lib/tools.jar",
				javaHome+"/lib/tools.jar",
				javaHome+"/../Classes/classes.jar" /* this for example happens in Mac */
		}; 

		for(String location : locations){
			File file = new File(location);
			if(file.exists()){
				return validateAndGetLoader(location);
			}
		}

		throw new RuntimeException("Did not manage to automatically find tools.jar. Use -Dtools_jar_location=<path> property");
	}

	private  ClassLoader considerPathInProperties() {
		if(manuallySpecifiedToolLocation.endsWith(".jar")){
			throw new RuntimeException("Property tools_jar_location does not point to a jar file: "+manuallySpecifiedToolLocation);
		}

		return validateAndGetLoader(manuallySpecifiedToolLocation);
	}

	private  ClassLoader validateAndGetLoader(String location) {

		ClassLoader loader = null;
		try {
			loader = URLClassLoader.newInstance(
					new URL[] { new File(location).toURI().toURL() },
					ClassLoader.getSystemClassLoader());
		} catch (MalformedURLException e) {
			throw new RuntimeException("Malformed URL: "+location,e);
		}

		try {
			Class.forName(EXAMPLE_CLASS, true, loader);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Missing "+EXAMPLE_CLASS+" in "+location);
		}

        locationNotOnClasspath = location;

		return loader;
	}

    public String getLocationNotOnClasspath() {
        return locationNotOnClasspath;
    }
}