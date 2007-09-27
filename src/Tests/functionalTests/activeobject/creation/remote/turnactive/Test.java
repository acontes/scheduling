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
package functionalTests.activeobject.creation.remote.turnactive;

import org.junit.Before;
import org.objectweb.proactive.api.ProActiveObject;

import functionalTests.FunctionalTest;
import functionalTests.activeobject.creation.A;
import functionalTests.descriptor.defaultnodes.TestNodes;
import static junit.framework.Assert.assertTrue;

/**
 * Test turnActive method on a remote node
 */
public class Test extends FunctionalTest {
    private static final long serialVersionUID = -1029555907940725925L;
    A a;
    String name;
    String nodeUrl;
    String remoteHost;

    @Before
    public void Before() throws Exception {
        new TestNodes().action();
    }

    @org.junit.Test
    public void action() throws Exception {
        assertTrue(TestNodes.getRemoteHostname() != null);

        a = new A("toto");
        a = (A) ProActiveObject.turnActive(a, TestNodes.getRemoteVMNode());
        name = a.getName();
        nodeUrl = a.getNodeUrl();
        assertTrue(name.equals("toto") /*&& (nodeUrl.indexOf(remoteHost) != -1)*/);
    }
}
