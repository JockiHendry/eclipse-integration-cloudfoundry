/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.standalone;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.archive.AbstractApplicationArchiveEntry;
import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.springframework.util.Assert;

/**
 * Generates a deployable archive for standalone applications where the
 * structure of the deployed resources is derived from existing directory,
 * and the target or output directories of the project will be included
 * in one of this directory (configurable by user).
 * 
 */
public class StandaloneApplicationArchiveWithContainer implements ApplicationArchive {
	
	private File containerDirectory, moduleDirectory;	
	private List<Entry> containerDirectoryEntries;
	private IModule[] modules;
	private String moduleTargetDirectoryName;
	
	public StandaloneApplicationArchiveWithContainer(IModule[] modules, List<IModuleResource> resources, 
			File containerDirectory, String moduleTargetDirectoryName, Server server) {
		
		Assert.notNull(containerDirectory, "Container directory must not null");
		Assert.isTrue(containerDirectory.isDirectory(), "Container directory must reference a directory");
		this.modules = modules;
		this.moduleTargetDirectoryName = moduleTargetDirectoryName;
		
		// Process entries for container directory
		this.containerDirectory = containerDirectory;
        List<Entry> containerDirectoryEntries = new ArrayList<Entry>();
        collectEntriesForContainer(containerDirectoryEntries, containerDirectory);

        // Process entries for project's module
        try {
			File temporaryOutput = CloudUtil.createTemporaryModuleOutput(modules, server, null);
			this.moduleDirectory = temporaryOutput;
			collectEntriesForProjectModule(containerDirectoryEntries, temporaryOutput);
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
        
        // Result
        this.containerDirectoryEntries = Collections.unmodifiableList(containerDirectoryEntries);       
	}
	
	/**
	 * Collect container entries ex: tomcat7/bin/ tomcat7/conf 
	 */
    private void collectEntriesForContainer(List<Entry> entries, File directory) {
        for (File child : directory.listFiles()) {
            entries.add(new ContainerEntryAdapter(child, containerDirectory));
            if (child.isDirectory()) {
                collectEntriesForContainer(entries, child);
            }
        }
    }
    
    /**
     * Collect project modules (output) ex: WEB-INF/ META-INF/ classes/ 
     */
    private void collectEntriesForProjectModule(List<Entry> entries, File directory) {
    	for (File child : directory.listFiles()) {
            entries.add(new ContainerEntryAdapter(child, moduleTargetDirectoryName, moduleDirectory));
            if (child.isDirectory()) {
                collectEntriesForProjectModule(entries, child);
            }
        }
    }

    public String getFilename() {
    	return modules[0].getName();
	}

    public Iterable<Entry> getEntries() {
        return containerDirectoryEntries;
    }            
    
    private class ContainerEntryAdapter extends AbstractApplicationArchiveEntry {

        private File file;
        private String name;

        public ContainerEntryAdapter(File file, File container) {
            this.file = file;
            this.name = file.getAbsolutePath().replace(System.getProperty("file.separator", "/"), "/")
            				.substring(container.getAbsolutePath().length()+1);
            if(isDirectory()) {
                this.name = this.name + "/";
            }
        }
        
        public ContainerEntryAdapter(File file, String directoryPrefix, File container) {
        	this.file = file;
        	String fileSeparator = System.getProperty("file.separator", "/");
        	this.name = directoryPrefix.replace(fileSeparator, "/") + 
        		(directoryPrefix.endsWith("/")?"":"/") + file.getAbsolutePath().replace(fileSeparator, "/")
        		.substring(container.getAbsolutePath().length()+1);
            if(isDirectory()) {
                this.name = this.name + "/";
            }
        }
                
        public boolean isDirectory() {        	
            return (file==null) || (file.isDirectory());
        }

        public String getName() {
            return name;
        }

        public InputStream getInputStream() throws IOException {
            if (isDirectory()) {
                return null;
            }
            return new FileInputStream(file);
        }
    }	
}