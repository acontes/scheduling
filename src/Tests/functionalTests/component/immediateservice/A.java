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
package functionalTests.component.immediateservice;

import org.objectweb.proactive.Body;
import org.objectweb.proactive.ProActive;
import org.objectweb.proactive.core.component.body.ComponentInitActive;
import org.objectweb.proactive.core.util.wrapper.StringWrapper;


public class A implements Itf, ComponentInitActive {
    private boolean condition = true;

    public void initComponentActivity(Body body) {
        ProActive.setImmediateService("immediateMethod",
            new Class[] { String.class });
        ProActive.setImmediateService("immediateStopLoopMethod");
        //ProActive.setImmediateService("startFc");
    }

    public StringWrapper immediateMethod(String arg) {
        System.err.println("COMPONENT: immediateMethod: " + arg);
        StringWrapper res = new StringWrapper(arg + "success");
        condition = false;
        return res;
    }

    public void loopQueueMethod() {
        System.err.println("COMPONENT: loopQueueMethod: BEGINNING");
        while (condition)
            ;
        System.err.println("COMPONENT: loopQueueMethod: END");
    }

    public void immediateStopLoopMethod() {
        System.err.println("COMPONENT: immediateStopLoopMethod:");
        condition = false;
    }
}
