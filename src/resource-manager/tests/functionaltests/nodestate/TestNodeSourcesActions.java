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
package functionaltests.nodestate;

import java.io.File;

import junit.framework.Assert;

import org.objectweb.proactive.api.PAFuture;
import org.objectweb.proactive.core.node.Node;
import org.ow2.proactive.resourcemanager.common.NodeState;
import org.ow2.proactive.resourcemanager.common.event.RMEventType;
import org.ow2.proactive.resourcemanager.common.event.RMNodeEvent;
import org.ow2.proactive.resourcemanager.core.properties.PAResourceManagerProperties;
import org.ow2.proactive.resourcemanager.frontend.ResourceManager;
import org.ow2.proactive.resourcemanager.nodesource.infrastructure.LocalInfrastructure;
import org.ow2.proactive.resourcemanager.nodesource.policy.StaticPolicy;
import org.ow2.proactive.utils.FileToBytesConverter;
import org.ow2.proactive.utils.NodeSet;

import functionaltests.RMConsecutive;
import functionaltests.RMTHelper;


/**
 * This class tests actions of adding and removing node sources, particulary the removal
 * of a node source, preemptively or not
 *
 * Add a node source (test 1)
 * put nodes of the nodes in different states ; free, busy, down, to Release,
 * remove the node source preemptively (test 2).
 *
 * Add another node source, and put nodes of the nodes in different states ;
 * free, busy, down, to Release,
 * Remove the node source non preemptively (test 3).
 *
 * @author ProActive team
 */
public class TestNodeSourcesActions extends RMConsecutive {

    /** Actions to be Perform by this test.
     * The method is called automatically by Junit framework.
     * @throws Exception If the test fails.
     */
    @org.junit.Test
    public void action() throws Exception {
        RMTHelper helper = RMTHelper.getDefaultInstance();

        String nodeSourceName = "TestNodeSourcesActions";

        ResourceManager resourceManager = helper.getResourceManager();
        int nodeNumber = 5;

        int pingFrequency = 5000;

        byte[] creds = FileToBytesConverter.convertFileToByteArray(new File(PAResourceManagerProperties
                .getAbsolutePath(PAResourceManagerProperties.RM_CREDS.getValueAsString())));

        resourceManager.createNodeSource(nodeSourceName, LocalInfrastructure.class.getName(), new Object[] {
                "", creds, nodeNumber, RMTHelper.defaultNodesTimeout, "" }, StaticPolicy.class.getName(),
                null);

        //wait for creation of GCM Node Source event, and deployment of its nodes
        helper.waitForNodeSourceEvent(RMEventType.NODESOURCE_CREATED, nodeSourceName);
        resourceManager.setNodeSourcePingFrequency(pingFrequency, nodeSourceName);

        RMTHelper.log("Test 1");
        for (int i = 0; i < nodeNumber; i++) {
            helper.waitForAnyNodeEvent(RMEventType.NODE_ADDED);
            //wait for the nodes to be in free state
            helper.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);
        }

        Assert.assertEquals(nodeNumber, resourceManager.getState().getTotalNodesNumber());
        Assert.assertEquals(nodeNumber, resourceManager.getState().getFreeNodesNumber());

        //book 3 nodes
        NodeSet nodes = resourceManager.getAtMostNodes(3, null);
        PAFuture.waitFor(nodes);

        Assert.assertEquals(3, nodes.size());
        Assert.assertEquals(nodeNumber - 3, resourceManager.getState().getFreeNodesNumber());
        Assert.assertEquals(nodeNumber, resourceManager.getState().getTotalNodesNumber());

        for (int i = 0; i < 3; i++) {
            RMNodeEvent evt = helper.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);
            Assert.assertEquals(evt.getNodeState(), NodeState.BUSY);
        }

        //put one of the busy node in 'to release' state
        Node n1 = nodes.remove(0);
        resourceManager.removeNode(n1.getNodeInformation().getURL(), false);

        RMNodeEvent evt = helper.waitForNodeEvent(RMEventType.NODE_STATE_CHANGED, n1.getNodeInformation()
                .getURL());
        Assert.assertEquals(evt.getNodeState(), NodeState.TO_BE_REMOVED);

        //put one of the busy node in 'down' state
        Node n2 = nodes.remove(0);

        try {
            n2.getProActiveRuntime().killRT(false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        evt = helper.waitForNodeEvent(RMEventType.NODE_STATE_CHANGED, n2.getNodeInformation().getURL());
        Assert.assertEquals(evt.getNodeState(), NodeState.DOWN);

        //kill preemptively the node source
        resourceManager.removeNodeSource(nodeSourceName, true);

        for (int i = 0; i < nodeNumber; i++) {
            helper.waitForAnyNodeEvent(RMEventType.NODE_REMOVED);
        }

        //wait for the event of the node source removal
        helper.waitForNodeSourceEvent(RMEventType.NODESOURCE_REMOVED, nodeSourceName);

        Assert.assertEquals(0, resourceManager.getState().getFreeNodesNumber());
        Assert.assertEquals(0, resourceManager.getState().getTotalNodesNumber());

        //test the non preemptive node source removal
        RMTHelper.log("Test 2");

        String nodeSourceName2 = "TestNodeSourcesActions2";
        //first im parameter is default rm url
        int expectedNodeNumber = helper.createNodeSource(nodeSourceName2);
        resourceManager.setNodeSourcePingFrequency(pingFrequency, nodeSourceName2);

        Assert.assertEquals(expectedNodeNumber, resourceManager.getState().getTotalNodesNumber());
        Assert.assertEquals(expectedNodeNumber, resourceManager.getState().getFreeNodesNumber());

        //book 3 nodes
        nodes = resourceManager.getAtMostNodes(3, null);
        PAFuture.waitFor(nodes);

        for (int i = 0; i < 3; i++) {
            evt = helper.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);
            Assert.assertEquals(evt.getNodeState(), NodeState.BUSY);
        }

        Assert.assertEquals(3, nodes.size());
        Assert.assertEquals(expectedNodeNumber - 3, resourceManager.getState().getFreeNodesNumber());
        Assert.assertEquals(expectedNodeNumber, resourceManager.getState().getTotalNodesNumber());

        //put one of the busy node in 'to release' state
        n1 = nodes.remove(0);
        resourceManager.removeNode(n1.getNodeInformation().getURL(), false);

        evt = helper.waitForNodeEvent(RMEventType.NODE_STATE_CHANGED, n1.getNodeInformation().getURL());
        Assert.assertEquals(evt.getNodeState(), NodeState.TO_BE_REMOVED);

        //put one of the busy node in 'down' state
        n2 = nodes.remove(0);

        Node n3 = nodes.remove(0);

        try {
            n2.getProActiveRuntime().killRT(false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        evt = helper.waitForNodeEvent(RMEventType.NODE_STATE_CHANGED, n2.getNodeInformation().getURL());
        Assert.assertEquals(evt.getNodeState(), NodeState.DOWN);

        //kill non preemptively the node source
        resourceManager.removeNodeSource(nodeSourceName2, false);

        //the node isn't removed immediately because one its node is
        //in to Release state, and one in busy state

        //the two free nodes and the down node (n2) are removed immediately
        for (int i = 0; i < expectedNodeNumber - 2; i++) {
            helper.waitForAnyNodeEvent(RMEventType.NODE_REMOVED);
        }

        //the 'to release' node (n1) keeps the same state

        //the busy node (n3) becomes a 'to release' node
        evt = helper.waitForNodeEvent(RMEventType.NODE_STATE_CHANGED, n3.getNodeInformation().getURL());
        Assert.assertEquals(evt.getNodeState(), NodeState.TO_BE_REMOVED);

        Assert.assertEquals(0, resourceManager.getState().getFreeNodesNumber());
        Assert.assertEquals(2, resourceManager.getState().getTotalNodesNumber());

        //give back the two nodes in 'to release' state, they are directly removed
        resourceManager.releaseNode(n1);
        resourceManager.releaseNode(n3);

        for (int i = 0; i < 2; i++) {
            helper.waitForAnyNodeEvent(RMEventType.NODE_REMOVED);
        }
        Assert.assertEquals(0, resourceManager.getState().getFreeNodesNumber());
        Assert.assertEquals(0, resourceManager.getState().getTotalNodesNumber());
    }
}
