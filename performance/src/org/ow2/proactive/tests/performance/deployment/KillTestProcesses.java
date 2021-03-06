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
 *  Initial developer(s):               The ActiveEon Team
 *                        http://www.activeeon.com/
 *  Contributor(s):
 *
 * ################################################################
 * $ACTIVEEON_INITIAL_DEV$
 */
package org.ow2.proactive.tests.performance.deployment;

import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Program killing all java processes started by the test.
 * It tries to kill all java processes started with JVM option -Dorg.ow2.proactive.tests.performance=true 
 * on the given hosts.   
 *  
 * @author ProActive team
 *
 */
public class KillTestProcesses {

    public static void killProcesses(String hostsNamesString) {
        String hosts[] = {};
        if (!hostsNamesString.isEmpty()) {
            hosts = hostsNamesString.split(",");
        }
        Set<String> hostsList = new LinkedHashSet<String>();
        for (String hostName : hosts) {
            if (hostName != null && !hostName.isEmpty()) {
                hostsList.add(hostName.trim());
            }
        }
        System.out.println("Killing test processes on the hosts: " + hostsList);
        DeploymentTestUtils.killTestProcesses(hostsList, TestDeployer.TEST_JVM_OPTION);
    }

    public static void main(String[] args) {
        String hostsNamesString = System.getProperty("testHosts");
        if (hostsNamesString == null) {
            throw new IllegalArgumentException("Property 'testHosts' isn't set");
        }
        killProcesses(hostsNamesString);
    }

}
