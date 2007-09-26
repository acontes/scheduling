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
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://www.inria.fr/oasis/ProActive/contacts.html
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.extensions.calcium.monitor;

import org.objectweb.proactive.extensions.calcium.Calcium;
import org.objectweb.proactive.extensions.calcium.statistics.StatsGlobal;


public class SimpleLogMonitor implements Monitor {
    Calcium calcium;
    int frequency;
    MonitoringThread thread;

    public SimpleLogMonitor(Calcium calcium, int frequency) {
        this.calcium = calcium;
        this.frequency = frequency;
        thread = null;
    }

    public void stop() {
        thread.myStop();
        thread.interrupt();
        thread = null;
    }

    public void start() {
        if (thread != null) {
            return;
        }

        thread = new MonitoringThread();
        thread.start();
    }

    class MonitoringThread extends Thread {
        boolean stopped;

        public MonitoringThread() {
            super();
            stopped = false;
        }

        public void myStop() {
            stopped = true;
        }

        @Override
        public void run() {
            while (!stopped) {
                try {
                    Thread.sleep(frequency * 1000);
                } catch (InterruptedException e) {
                    return;
                }

                StatsGlobal stats = calcium.getStatsGlobal();
                System.out.println("[" + System.currentTimeMillis() + "]" +
                    stats);
            }
        }
    }
}
