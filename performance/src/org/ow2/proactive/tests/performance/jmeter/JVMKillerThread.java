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
package org.ow2.proactive.tests.performance.jmeter;

import org.apache.jmeter.JMeter;
import org.objectweb.proactive.core.runtime.RuntimeFactory;


public class JVMKillerThread extends Thread {

    private final long killDelay;

    public JVMKillerThread(long killDelay) {
        this.killDelay = killDelay;
    }

    public void run() {
        try {
            System.out.println(String.format("JVMKillerThread: sleeping for %d millis", killDelay));
            Thread.sleep(killDelay);
            System.out.println("JVMKillerThread: killing JVM");
            try {
                RuntimeFactory.getDefaultRuntime().killRT(false);
            } finally {
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startKillerThreadIfNonGUIMode(long killDelay) {
        if (JMeter.isNonGUI()) {
            new JVMKillerThread(killDelay).start();
        }
    }
}
