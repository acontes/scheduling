/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.resourcemanager.gui.actions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.ow2.proactive.jmx.JMXClientHelper;
import org.ow2.proactive.jmx.naming.JMXTransportProtocol;
import org.ow2.proactive.resourcemanager.authentication.RMAuthentication;


/**
 * This manager handles actions that starts the ChartIt plugin feeded with statistical data 
 * that comes from the Resource Manager through JMX. 
 * <b>
 * The operation of this action can be described as:
 * <ul>
 * <li>1. The Resource Manager GUI asks the user to connect to an existing Resource Manager
 * <li>2. The JMX client tries to connect to the 
 * <li>3. If connection is not established this action does nothing
 * <li>4. Before disconnecting the Resource Manager GUI the jmx client must be disconnected
 * </ul>
 * <b>
 * Actions are enabled if the internal JMX client got a valid JMX connection. 
 *  
 * @author The ProActive Team 
 */
public class JMXActionsManager {

    /** The static reference on the single instance of this class */
    private static JMXActionsManager instance;

    /** An instance of internal jmx client that handles all connection specific issues */
    private JMXClientHelper jmxClient;

    /** All managed actions */
    private List<IAction> actions;

    /**
     * Returns the single instance of this class
     * @return The single instance of this class
     */
    public static JMXActionsManager getInstance() {
        if (JMXActionsManager.instance == null) {
            JMXActionsManager.instance = new JMXActionsManager();
        }
        return JMXActionsManager.instance;
    }

    private JMXActionsManager() {
        this.actions = new ArrayList<IAction>(2);
    }

    public void addAction(final IAction action) {
        this.actions.add(action);
    }

    public void removeAction(final IAction action) {
        this.actions.remove(action);
    }

    /**
     * Initializes the JMX client and if the initialization was successful enables this action if
     * not sets as tool tip a message.
     * @param rmURL the URL of the resource manager
     * @param auth the resource manager authentication interface
     * @param creds the credentials required for authentication 
     */
    public void initJMXClient(final String rmURL, final RMAuthentication auth, final Object[] creds) {
        this.jmxClient = new JMXClientHelper(auth, creds);
        // By default the protocol used for JMX connection 
        // is RO, if the protocol of the resource manager URL is RMI
        // it will be used for JMX connection
        JMXTransportProtocol jmxProtocol = JMXTransportProtocol.RO;
        try {
            final URI uri = new URI(rmURL);
            final String protocol = uri.getScheme().toLowerCase();
            if (JMXTransportProtocol.RMI.toString().toLowerCase().equals(protocol)) {
                jmxProtocol = JMXTransportProtocol.RMI;
            }
        } catch (URISyntaxException e) {
            // Nothing to do, we can still try to connect 
        }
        // Connect the JMX client using the specified protocol
        final boolean isConnected = this.jmxClient.connect(jmxProtocol);
        // If connected enable the actions
        if (isConnected) {
            for (final IAction a : this.actions) {
                a.setEnabled(true);
            }
        } else {
            for (final IAction a : this.actions) {
                // Show a tool tip text in case of connection failure
                a.setToolTipText("Unable to connect the JMX client due to " +
                    this.jmxClient.getLastException());
            }
        }
    }

    /**
     * Disconnects the JMX client and disables this action and closes the associated editor
     */
    public void disconnectJMXClient() {
        // Disable all actions of this manager
        for (final IAction a : this.actions) {
            a.setEnabled(false);
            // Close the corresponding window
            try {
                JMXActionsManager.activateIfFound(a.getId(), false);
            } catch (Exception t) {
                MessageDialog.openError(Display.getDefault().getActiveShell(), "Unable to close the " +
                    a.getId(), t.getMessage());
                t.printStackTrace();
            }
        }
        /*
         * disconnect action requires network access and 
         * potentially it can hang, run it from separate thread 
         * to don't freeze GUI thread
         */
        new Thread() {
        	public void run() {
        		jmxClient.disconnect();
        	}
        }.start();
    }

    public JMXClientHelper getJMXClientHelper() {
        return this.jmxClient;
    }

    /**
     * Activates or closes an editor by name.
     * 
     * @param activate <code>True</code> to activate,
     *         <code>False</code> to close
     * @param name
     *            The name of the editor to activate
     * @return <code>True</code> if the existing editor was activated,
     *         <code>False</code> otherwise
     * @throws PartInitException
     *             Thrown if the part can not be activated
     */
    public static boolean activateIfFound(final String name, final boolean activate) throws PartInitException {
        final IWorkbenchWindow currentWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        // Navigate through EditorReference->EditorInput then find the
        // Editor through ActivePage.findEditor(editorInputRef)
        // First list all EditorReferences
        for (final IEditorReference ref : currentWindow.getActivePage().getEditorReferences()) {
            if (ref.getEditorInput().getName().equals(name)) {
                final IEditorPart editor = currentWindow.getActivePage().findEditor(ref.getEditorInput());
                if (activate) {
                    // If the Editor input was found activate it
                    currentWindow.getActivePage().activate(editor);
                } else {
                    currentWindow.getActivePage().closeEditor(editor, false); // close and don't save
                }
                return true;
            }
        }
        return false;
    }

}
