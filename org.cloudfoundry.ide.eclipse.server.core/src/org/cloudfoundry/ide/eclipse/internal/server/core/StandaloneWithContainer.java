package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.io.File;

/**
 * Simple DOM class to store information related to standalone framework apps that
 * also deploy a container, such as Tomcat 7.
 * 
 * @author JockiHendry
 *
 */
public class StandaloneWithContainer {

	/**
	 * This is the location of container.  It is not restricted to project location and
	 * can be anywhere in local disk.
	 */
	private File containerDirectory;
	
	/**
	 * This is the location of project output relative to container directory.
	 * For example:
	 * containerDirectory = "C:/tomcat7"
	 * deployDirectory  = "webapps/ROOT"
	 * Then project output will be deployed in: "C:/tomcat7/webapps/ROOT" 
	 */
	private String deployDirectory;

	public File getContainerDirectory() {
		return containerDirectory;
	}

	public void setContainerDirectory(File containerDirectory) {
		this.containerDirectory = containerDirectory;
	}
	
	public void setContainerDirectory(String containerDirectory) {		
		this.containerDirectory = new File(containerDirectory.replace(System.getProperty("file.separator"), "/"));
	}

	public String getDeployDirectory() {
		return deployDirectory;
	}

	public void setDeployDirectory(String deployDirectory) {
		this.deployDirectory = deployDirectory.replace(System.getProperty("file.separator"), "/");
	}
	
	
}
