package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.io.File;
import java.io.Serializable;

/**
 * Simple DOM class to store information related to standalone framework apps that
 * also deploy a container, such as Tomcat 7.
 * 
 * @author JockiHendry
 *
 */
public class StandaloneWithContainer implements Serializable {
	
	private static final long serialVersionUID = -2842206754223240218L;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((containerDirectory == null) ? 0 : containerDirectory.hashCode());
		result = prime * result + ((deployDirectory == null) ? 0 : deployDirectory.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StandaloneWithContainer other = (StandaloneWithContainer) obj;
		if (containerDirectory == null) {
			if (other.containerDirectory != null)
				return false;
		}
		else if (!containerDirectory.equals(other.containerDirectory))
			return false;
		if (deployDirectory == null) {
			if (other.deployDirectory != null)
				return false;
		}
		else if (!deployDirectory.equals(other.deployDirectory))
			return false;
		return true;
	}
	
}
