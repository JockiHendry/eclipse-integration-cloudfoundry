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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.DeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryCallback;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.RepublishModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleManager;
import org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel.CaldecottUIHelper;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryApplicationWizard;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryCredentialsWizard;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.DeleteServicesWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IModule;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 */
public class CloudFoundryUiCallback extends CloudFoundryCallback {

	@Override
	public void applicationStarted(CloudFoundryServer server, ApplicationModule cloudModule) {
		for (int i = 0; i < cloudModule.getApplication().getInstances(); i++) {
			ConsoleManager.getInstance().startConsole(server, cloudModule.getApplication(), i, i == 0);
		}
	}

	@Override
	public void deleteApplication(ApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		for (int i = 0; i < cloudModule.getApplication().getInstances(); i++) {
			ConsoleManager.getInstance().stopConsole(cloudServer.getServer(), cloudModule.getApplication(), i);
		}
	}

	public void displayCaldecottTunnelConnections(CloudFoundryServer cloudServer,
			List<CaldecottTunnelDescriptor> descriptors) {

		if (descriptors != null && !descriptors.isEmpty()) {
			List<String> serviceNames = new ArrayList<String>();

			for (CaldecottTunnelDescriptor descriptor : descriptors) {
				serviceNames.add(descriptor.getServiceName());
			}
			new CaldecottUIHelper(cloudServer).displayCaldecottTunnels(serviceNames);
		}
	}

	@Override
	public void applicationStopping(CloudFoundryServer server, ApplicationModule cloudModule) {
		// CloudApplication application = cloudModule.getApplication();
		// if (application != null) {
		// consoleManager.stopConsole(application);
		// }
	}

	@Override
	public void disconnecting(CloudFoundryServer server) {
		ConsoleManager.getInstance().stopConsoles();
	}

	@Override
	public void getCredentials(final CloudFoundryServer server) {
		Display.getDefault().syncExec(new Runnable() {

			public void run() {
				CloudFoundryCredentialsWizard wizard = new CloudFoundryCredentialsWizard(server);
				WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getModalDialogShellProvider()
						.getShell(), wizard);
				dialog.open();
			}
		});

		if (server.getUsername() == null || server.getUsername().length() == 0 || server.getPassword() == null
				|| server.getPassword().length() == 0 || server.getUrl() == null || server.getUrl().length() == 0) {
			throw new OperationCanceledException();
		}
	}

	@Override
	public DeploymentDescriptor prepareForDeployment(final CloudFoundryServer server,
			final ApplicationModule appModule, final IProgressMonitor monitor) {

		DeploymentDescriptor descriptor = null;
		CloudApplication existingApp = appModule.getApplication();
		if (existingApp != null) {
			descriptor = new DeploymentDescriptor();
			descriptor.applicationInfo = new ApplicationInfo(existingApp.getName());
			descriptor.deploymentInfo = new DeploymentInfo();
			descriptor.deploymentInfo.setUris(existingApp.getUris());
			descriptor.deploymentMode = ApplicationAction.START;
			descriptor.standaloneWithContainer = appModule.getStandaloneWithContainerInfo();

			// FIXNS_STANDALONE: uncomment when CF client supports staging
			// descriptor.staging = getStaging(appModule);

			DeploymentInfo lastDeploymentInfo = appModule.getLastDeploymentInfo();
			if (lastDeploymentInfo != null) {
				descriptor.deploymentInfo.setServices(lastDeploymentInfo.getServices());
			}
		}
		else {
			IModule module = appModule.getLocalModule();

			RepublishModule repModule = CloudFoundryPlugin.getModuleCache().getData(server.getServerOriginal())
					.untagForAutomaticRepublish(module);

			// First check if the module is set for automatic republish. This is
			// different that a
			// start or update restart, as the latter two require the module to
			// already be published.
			// Automatic republish are for modules that are configured for
			// deployment but failed to
			// publish the first time. In this case it is unnecessary to open
			// the
			// deployment wizard
			// as all the configuration is already available in an existing
			// descriptor
			if (repModule != null) {
				descriptor = repModule.getDeploymentDescriptor();
			}

			if (!isValidDescriptor(descriptor)) {
				final DeploymentDescriptor[] depDescriptors = new DeploymentDescriptor[1];
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						CloudFoundryApplicationWizard wizard = new CloudFoundryApplicationWizard(server, appModule);
						WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getModalDialogShellProvider()
								.getShell(), wizard);
						int status = dialog.open();
						if (status == Dialog.OK) {
							DeploymentDescriptor descriptorToUpdate = new DeploymentDescriptor();
							descriptorToUpdate.applicationInfo = wizard.getApplicationInfo();
							descriptorToUpdate.deploymentInfo = wizard.getDeploymentInfo();
							descriptorToUpdate.deploymentMode = wizard.getDeploymentMode();
							descriptorToUpdate.standaloneWithContainer = wizard.getStandaloneWithContainer();

							descriptorToUpdate.staging = wizard.getStaging();
							// First add any new services to the server
							final List<CloudService> addedServices = wizard.getAddedCloudServices();

							if (!addedServices.isEmpty()) {
								IProgressMonitor subMonitor = new SubProgressMonitor(monitor, addedServices.size());
								try {
									server.getBehaviour().createService(addedServices.toArray(new CloudService[0]),
											subMonitor);
								}
								catch (CoreException e) {
									CloudFoundryPlugin.logError(e);
								}
								finally {
									subMonitor.done();
								}
							}

							// Now set any selected services, which may
							// include
							// past
							// deployed services
							List<String> selectedServices = wizard.getSelectedCloudServicesID();

							descriptorToUpdate.deploymentInfo.setServices(selectedServices);
							depDescriptors[0] = descriptorToUpdate;
						}
					}
				});
				descriptor = depDescriptors[0];
			}
		}

		if (descriptor == null || descriptor.deploymentInfo == null) {
			throw new OperationCanceledException();
		}
		return descriptor;
	}

	@Override
	public void deleteServices(final List<String> services, final CloudFoundryServer cloudServer) {
		if (services == null || services.isEmpty()) {
			return;
		}

		Display.getDefault().syncExec(new Runnable() {

			public void run() {
				DeleteServicesWizard wizard = new DeleteServicesWizard(cloudServer, services);
				WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
				dialog.open();
			}

		});
	}

	protected boolean isValidDescriptor(DeploymentDescriptor descriptor) {
		if (descriptor == null || descriptor.deploymentMode == null) {
			return false;
		}

		ApplicationInfo info = descriptor.applicationInfo;
		if (info == null || info.getAppName() == null || info.getFramework() == null) {
			return false;
		}

		DeploymentInfo deploymentInfo = descriptor.deploymentInfo;

		if (deploymentInfo == null || deploymentInfo.getDeploymentName() == null || deploymentInfo.getMemory() <= 0
		// If it is not standalone app (not having staging is consider
		// non-standalone), then URIs must be set to be a valid
		// descriptor
				|| ((descriptor.staging == null || !CloudApplication.STANDALONE.equals(descriptor.staging
						.getFramework())) && (deploymentInfo.getUris() == null || deploymentInfo.getUris().isEmpty()))) {
			return false;
		}

		return true;
	}
}
