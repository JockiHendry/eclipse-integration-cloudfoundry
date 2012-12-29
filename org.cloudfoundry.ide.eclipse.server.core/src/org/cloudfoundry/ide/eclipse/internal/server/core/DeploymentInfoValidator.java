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
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Validates whether a url name value, and optionally a start command if an app
 * is a standalone app, are valid. If a standalone app, URL is optional, which
 * is also checked by this validator
 * <p/>
 * Valid url names should not include the protocol (e.g. http://www.google.com)
 * or queries in the name valid names are:
 * <p/>
 * www.google.com
 * <p/>
 * www$.google.com
 * <p/>
 * www.google.com4
 * <p/>
 * names with trailing or ending spaces, or spaces in between the name segments
 * are invalid.
 * 
 */
public class DeploymentInfoValidator {

	private final String startCommand;

	private final boolean isStandAloneApp;

	private final String url;
	
	private final boolean isDeployWithContainer;
	
	private final String containerDirectory;

	public static final String EMPTY_URL_ERROR = "Enter a deployment name.";

	public static final String INVALID_CHARACTERS_ERROR = "The entered name contains invalid characters.";

	public static final String INVALID_START_COMMAND = "A start command is required when deploying a standalone application.";
	
	public static final String EMPTY_CONTAINER_DIRECTORY = "Enter container directory location.";
	
	public static final String INVALID_CONTAINER_DIRECTORY = "Container directory didn't exists";

	public DeploymentInfoValidator(String url, String startCommand, boolean isStandaloneApplication, 
			boolean isDeployWithContainer, String containerDirectory) {
		this.startCommand = startCommand;
		this.url = url;
		this.isStandAloneApp = isStandaloneApplication;
		this.isDeployWithContainer = isDeployWithContainer;
		this.containerDirectory = containerDirectory;
	}

	public IStatus isValid() {
		// Check URL validity
		String message = null;
		boolean isValid = true;
		if (ValueValidationUtil.isEmpty(url)) {
			if (!isStandAloneApp) {
				message = EMPTY_URL_ERROR;
				isValid = false;
			}
		}
		else if (new URLNameValidation(url).hasInvalidCharacters()) {
			message = INVALID_CHARACTERS_ERROR;
			isValid = false;
		}

		// Check standalone app start command
		if (isValid && isStandAloneApp && ValueValidationUtil.isEmpty(startCommand)) {
			message = INVALID_START_COMMAND;
			isValid = false;
		}
		
		// Check container
		if (isValid && isStandAloneApp && isDeployWithContainer) {
			if (ValueValidationUtil.isEmpty(containerDirectory)) {
				message = EMPTY_CONTAINER_DIRECTORY;
				isValid = false;
			} else if (!Files.exists(Paths.get(containerDirectory))) {
				message = INVALID_CONTAINER_DIRECTORY;
				isValid = false;
			}
		}

		if (!isValid) {
			return CloudFoundryPlugin.getErrorStatus(message != null ? message : "");
		}
		else {
			return Status.OK_STATUS;
		}
	}
}
