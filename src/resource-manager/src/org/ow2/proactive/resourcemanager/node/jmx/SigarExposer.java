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
 *  Contributor(s): ActiveEon Team - http://www.activeeon.com
 *
 * ################################################################
 * $$ACTIVEEON_CONTRIBUTOR$$
 */
package org.ow2.proactive.resourcemanager.node.jmx;

import java.net.ServerSocket;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.hyperic.sigar.jmx.SigarRegistry;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.ow2.proactive.jmx.AbstractJMXHelper;


public class SigarExposer extends AbstractJMXHelper {

    private static final Logger LOGGER = ProActiveLogger.getLogger(SigarExposer.class);

    public SigarExposer() {
        super(LOGGER);
    }

    public void registerMBeans(MBeanServer mbs) {
        try {
            // Create the SIGAR registry
            SigarRegistry registry = new SigarRegistry();

            ObjectName name = new ObjectName(registry.getObjectName());
            if (!mbs.isRegistered(name)) {
                mbs.registerMBean(registry, name);
            }

            name = new ObjectName("sigar:Type=Processes");
            SigarProcessesMXBean processes = new SigarProcesses();
            if (!mbs.isRegistered(name)) {
                mbs.registerMBean(processes, name);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to register SigarRegistry mbean", e);
        }
    }

    @Override
    public String getConnectorServerName() {
        return "rmnode";
    }

    @Override
    public int getJMXRMIConnectorServerPort() {
        ServerSocket server;
        try {
            server = new ServerSocket(0);
            int port = server.getLocalPort();
            server.close();
            return port;
        } catch (Exception e) {
            LOGGER.error("", e);
        }

        // in worst case try to return a random port from the range 5000-6000
        return (int) (5000 + (Math.random() * 1000));
    }
}
