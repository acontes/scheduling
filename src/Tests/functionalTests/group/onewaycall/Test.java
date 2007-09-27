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
package functionalTests.group.onewaycall;

import java.util.Iterator;

import org.junit.Before;
import org.objectweb.proactive.api.ProGroup;
import org.objectweb.proactive.core.group.Group;
import org.objectweb.proactive.core.node.Node;

import functionalTests.FunctionalTest;
import functionalTests.descriptor.defaultnodes.TestNodes;
import functionalTests.group.A;
import static junit.framework.Assert.assertTrue;

/**
 * do a oneway call on a previously created group
 *
 * @author Laurent Baduel
 */
public class Test extends FunctionalTest {
    private static final long serialVersionUID = -881236704381741691L;
    private A typedGroup = null;

    @org.junit.Test
    public void action() throws Exception {
        this.typedGroup.onewayCall();

        boolean allOnewayCallDone = true;
        Group group = ProGroup.getGroup(this.typedGroup);
        Iterator it = group.iterator();
        while (it.hasNext()) {
            allOnewayCallDone &= ((A) it.next()).isOnewayCallReceived();
        }
        assertTrue(allOnewayCallDone);
    }

    @Before
    public void preConditions() throws Exception {
        new TestNodes().action();

        Object[][] params = {
                { "Agent0" },
                { "Agent1" },
                { "Agent2" }
            };
        Node[] nodes = {
                TestNodes.getSameVMNode(), TestNodes.getLocalVMNode(),
                TestNodes.getRemoteVMNode()
            };
        this.typedGroup = (A) ProGroup.newGroup(A.class.getName(), params, nodes);
        ProGroup.getGroup(this.typedGroup).setRatioMemberToThread(1);

        boolean NoOnewayCallDone = true;
        Group group = ProGroup.getGroup(this.typedGroup);
        Iterator it = group.iterator();
        while (it.hasNext()) {
            NoOnewayCallDone &= !((A) it.next()).isOnewayCallReceived();
        }
        assertTrue(NoOnewayCallDone);
    }
}
