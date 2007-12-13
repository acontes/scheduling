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
package functionalTests.descriptor.coallocation;

import org.objectweb.proactive.api.PADeployment;
import org.objectweb.proactive.core.config.PAProperties;
import org.objectweb.proactive.core.descriptor.data.ProActiveDescriptor;
import org.objectweb.proactive.core.descriptor.data.VirtualMachine;
import org.objectweb.proactive.core.descriptor.data.VirtualNode;
import org.objectweb.proactive.core.node.Node;

import functionalTests.FunctionalTest;
import static junit.framework.Assert.assertTrue;


/**
 * Test coallocation in deployment descriptors
 */
public class Test extends FunctionalTest {
    ProActiveDescriptor proActiveDescriptor;
    private static String AGENT_XML_LOCATION_UNIX = null;

    static {
        if ("ibis".equals(PAProperties.PA_COMMUNICATION_PROTOCOL.getValue())) {
            AGENT_XML_LOCATION_UNIX = Test.class.getResource(
                    "/functionalTests/descriptor/coallocation/coallocationIbis.xml").getPath();
        } else {
            AGENT_XML_LOCATION_UNIX = Test.class.getResource(
                    "/functionalTests/descriptor/coallocation/coallocation.xml").getPath();
        }
    }

    Node node1;
    Node node2;

    @org.junit.Test
    public void action() throws Exception {
        proActiveDescriptor = PADeployment.getProactiveDescriptor("file:" + AGENT_XML_LOCATION_UNIX);
        // We activate the mapping in reverse order
        // when two vns refer to the same vm, the first vn which creates the vm becomes the creator of the vm
        // we want to verify this behavior (in addition to coallocation)
        proActiveDescriptor.activateMapping("covn2");
        proActiveDescriptor.activateMapping("covn1");
        VirtualNode vn1 = proActiveDescriptor.getVirtualNode("covn1");
        VirtualNode vn2 = proActiveDescriptor.getVirtualNode("covn2");
        node1 = vn1.getNode();
        node2 = vn2.getNode();

        vn1 = proActiveDescriptor.getVirtualNode("covn1");
        VirtualMachine vm = vn1.getVirtualNodeInternal().getVirtualMachine();
        assertTrue(node1.getProActiveRuntime().getURL().equals(node2.getProActiveRuntime().getURL()) &&
            vm.getCreatorId().equals("covn2"));
    }

    public static void main(String[] args) {
        Test test = new Test();
        try {
            test.action();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
