/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2007 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@objectweb.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version
 * 2 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.extensions.scheduler.gui;

import java.net.UnknownHostException;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.objectweb.proactive.core.util.ProActiveInet;
import org.objectweb.proactive.extensions.scheduler.util.logforwarder.SimpleLoggerServer;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 * 
 * @author The ProActive Team
 */
public class Activator extends AbstractUIPlugin {

    /** The plug-in ID */
    public static final String PLUGIN_ID = "Scheduler_Plugin";

    // The shared instance
    private static Activator plugin;
    private static String hostname = null;
    private static SimpleLoggerServer simpleLoggerServer = null;
    private static Thread simpleLoggerServerThread = null;

    /**
     * The constructor
     */
    public Activator() {
    }

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        // start the log server
        simpleLoggerServer = SimpleLoggerServer.createLoggerServer();
        hostname = ProActiveInet.getInstance().getHostname();
    }

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        // FIXME cdelbe
        // simpleLoggerServer.stop();
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Returns the hostname
     * 
     * @return the hostname
     */
    public static String getHostname() {
        return hostname;
    }

    /**
     * Return the port on which logs are listened.
     * 
     * @return the port on which logs are listened.
     */
    public static int getListenPortNumber() {
        if (simpleLoggerServer != null) {
            return simpleLoggerServer.getPort();
        } else {
            throw new RuntimeException("Logger server is not created yet");
        }
    }
}
