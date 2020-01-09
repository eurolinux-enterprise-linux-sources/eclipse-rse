/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir,
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson,
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 *
 * Contributors:
 * Martin Oberhuber (Wind River) - Adapted from LocalShellService.
 * Martin Oberhuber (Wind River) - [186128] Move IProgressMonitor last in all API
 * Martin Oberhuber (Wind River) - [226262] Make IService IAdaptable
 * Martin Oberhuber (Wind River) - [226301][api] IShellService should throw SystemMessageException on error
 * Martin Oberhuber (Wind River) - [170910] Adopt RSE ITerminalService API for SSH
 * Anna Dushistova  (MontaVista) - adapted from SshShellService
 * Anna Dushistova  (MontaVista) - [240523] [rseterminals] Provide a generic adapter factory that adapts any ITerminalService to an IShellService
 * Anna Dushistova  (MontaVista) - [261478] Remove SshShellService, SshHostShell (or deprecate and schedule for removal in 3.2)
 * Martin Oberhuber (Wind River) - [267402] [telnet] "launch shell" takes forever
 *******************************************************************************/
package org.eclipse.rse.internal.services.shells;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;

import org.eclipse.rse.internal.services.RSEServicesMessages;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.shells.AbstractShellService;
import org.eclipse.rse.services.shells.IHostShell;
import org.eclipse.rse.services.terminals.ITerminalService;
import org.eclipse.rse.services.terminals.ITerminalShell;

/**
 * @since 3.1
 */
public class TerminalShellService extends AbstractShellService {

	ITerminalService fTerminalService;

	public TerminalShellService(ITerminalService terminalService) {
		super();
		fTerminalService = terminalService;
	}

	public IHostShell launchShell(String initialWorkingDirectory,
			String encoding, String[] environment, IProgressMonitor monitor)
			throws SystemMessageException {
		return runCommand(initialWorkingDirectory, TerminalServiceHostShell.SHELL_INVOCATION, encoding, environment, monitor);
	}

	public IHostShell runCommand(String initialWorkingDirectory, String command, String encoding, String[] environment, IProgressMonitor monitor)
			throws SystemMessageException {
		// vt100 is the most common kind of terminal, and default for Telnet
		// and SSH: see Commons Net TelnetClient#TelnetClient() and JSch
		// ChannelSession#ttype.
		// We therefore pick vt100 here, and adapt to it by ignoring control
		// sequences in TerminalServiceShellOutputReader line 100. We could
		// also request a "dumb" type here, but testing showed that the
		// prompt is then not detected correctly.
		ITerminalShell terminalShell = fTerminalService.launchTerminal("vt100", //$NON-NLS-1$
				encoding, environment, initialWorkingDirectory, null, monitor);
		TerminalServiceHostShell hostShell = new TerminalServiceHostShell(
				terminalShell, initialWorkingDirectory, command, environment);
		return hostShell;
	}

	/**
	 * Adapt this shell service to different (potentially contributed)
	 * interfaces.
	 *
	 * Asks the adapter manager first whether it got any contributed adapter; if
	 * none is found contributed externally, try to adapt to an
	 * SshTerminalService. That way, clients can easily convert this
	 * IShellService into an ITerminalService:
	 *
	 * <pre>
	 * ITerminalService ts = (ITerminalService) myShellService.getAdapter(ITerminalService.class);
	 * </pre>
	 *
	 * @see IAdaptable
	 * @see PlatformObject#getAdapter(Class)
	 */
	public Object getAdapter(Class adapter) {
		// TODO I'm not sure if this is the right way doing things. First of
		// all, we're holding on to the created terminal service forever if
		// we're asked for it, thus needing extra memory.
		// Second, by asking the adapter manager first, we might get no chance
		// returning what we think is right.
		Object o = super.getAdapter(adapter);
		if (o == null && adapter.isAssignableFrom(ITerminalService.class)) {
			return fTerminalService;
		}
		return o;
	}

	public String getName()
	{
		return RSEServicesMessages.TerminalShellService_name;
	}

	public String getDescription()
	{
		return RSEServicesMessages.TerminalShellService_description;
	}

}
