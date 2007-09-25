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
package org.objectweb.proactive.extensions.scilab.test;

import java.util.List;

import org.objectweb.proactive.extensions.scilab.AbstractData;
import org.objectweb.proactive.extensions.scilab.AbstractGeneralTask;
import org.objectweb.proactive.extensions.scilab.GeneralResult;
import org.objectweb.proactive.extensions.scilab.SciTask;
import org.objectweb.proactive.extensions.scilab.monitor.GenTaskInfo;
import org.objectweb.proactive.extensions.scilab.monitor.SciEvent;
import org.objectweb.proactive.extensions.scilab.monitor.SciEventListener;
import org.objectweb.proactive.extensions.scilab.monitor.ScilabService;


public class SciTest2 {
    private ScilabService scilab;

    public void displayResult(GenTaskInfo scitaskInfo) {
        GeneralResult sciResult = scitaskInfo.getResult();
        List<AbstractData> listResult = sciResult.getList();

        for (AbstractData data : listResult) {
            System.out.println(data);
        }

        scilab.exit();
        System.exit(0);
    }

    public SciTest2(String nameVN, String pathVN) throws Exception {
        scilab = new ScilabService();
        scilab.deployEngine(nameVN, pathVN, new String[] { "Scilab" });

        scilab.addEventListenerTask(new SciEventListener() {
                public void actionPerformed(SciEvent evt) {
                    GenTaskInfo sciTaskInfo = (GenTaskInfo) evt.getSource();

                    if (sciTaskInfo.getState() == GenTaskInfo.SUCCEEDED) {
                        displayResult(sciTaskInfo);
                        return;
                    }
                }
            });

        AbstractGeneralTask task = new SciTask("id");
        task.setJobInit("n = 10;");
        task.addDataOut("n");
        task.setJob("n = n+1;");
        System.out.println("Job : " + task.getJob());
        scilab.sendTask(task);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Invalid number of parameter : " + args.length);
            return;
        }

        new SciTest2(args[0], args[1]);
    }
}
