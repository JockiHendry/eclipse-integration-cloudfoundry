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
import org.eclipse.wst.server.core.IModule;
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
	
	private File containerDirectory;
	private List<Entry> containerDirectoryEntries;
	private IModule module;
	
	public StandaloneApplicationArchiveWithContainer(IModule module, List<IModuleResource> resources, 
			File containerDirectory, String moduleTargetDirectoryName, File warFile) {
		
		Assert.notNull(containerDirectory, "Container directory must not null");
		Assert.isTrue(containerDirectory.isDirectory(), "Container directory must reference a directory");
		this.module = module;
		
		// Process entries for container directory
		this.containerDirectory = containerDirectory;
        List<Entry> containerDirectoryEntries = new ArrayList<Entry>();
        collectEntries(containerDirectoryEntries, containerDirectory);
        
        // Proces entries for module directory which is usually inside container directory        
        //if (moduleTargetDirectoryName==null) moduleTargetDirectoryName = "";
        //this.moduleTargetDirectoryName = moduleTargetDirectoryName;
        //collectModuleEntries(containerDirectoryEntries, resources.toArray(new IModuleResource[0]));        
        containerDirectoryEntries.add(new ContainerEntryAdapter(warFile, moduleTargetDirectoryName));

        // Result
        this.containerDirectoryEntries = Collections.unmodifiableList(containerDirectoryEntries);       
	}
	
	/**
	 * Collect container entries
	 */
    private void collectEntries(List<Entry> entries, File directory) {
        for (File child : directory.listFiles()) {
            entries.add(new ContainerEntryAdapter(child));
            if (child.isDirectory()) {
                collectEntries(entries, child);
            }
        }
    }

    public String getFilename() {
    	return module.getName();
	}

    public Iterable<Entry> getEntries() {
        return containerDirectoryEntries;
    }
    	
    private class ContainerEntryAdapter extends AbstractApplicationArchiveEntry {

        private File file;
        private String name;

        /**
         * Process a <code>File</code> that represent module resource.
         * @param file is a <code>File</code> that represent module resource.
         * @param directoryPrefix will be added as prefix to file name.
         */
        public ContainerEntryAdapter(File file, String directoryPrefix) {
        	this.file = file;
        	this.name = directoryPrefix + (directoryPrefix==null?"":"/") + file.getName();
        	if(isDirectory()) {
                this.name = this.name + "/";
            }
        }
        
//        public ContainerEntryAdapter(File file, String moduleName, String directoryPrefix) {
//        	this.file = file;
//        	String absolutePath = file.getAbsolutePath().replace(System.getProperty("file.separator", "/"), "/");
//        	int startIndex = absolutePath.indexOf(moduleName) + moduleName.length() + 1;
//        	this.name = directoryPrefix + (directoryPrefix==null?"":"/") + absolutePath.substring(startIndex);
//        	if(isDirectory()) {
//                this.name = this.name + "/";
//            }
//        }
        
        public ContainerEntryAdapter(File file) {
            this.file = file;
            this.name = file.getAbsolutePath().replace(System.getProperty("file.separator", "/"), "/")
            				.substring(containerDirectory.getAbsolutePath().length()+1);
            if(isDirectory()) {
                this.name = this.name + "/";
            }
        }

        public boolean isDirectory() {
            return file.isDirectory();
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