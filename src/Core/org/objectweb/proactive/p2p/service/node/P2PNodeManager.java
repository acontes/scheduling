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
package org.objectweb.proactive.p2p.service.node;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.EndActive;
import org.objectweb.proactive.InitActive;
import org.objectweb.proactive.ProActiveInternalObject;
import org.objectweb.proactive.api.ProActiveObject;
import org.objectweb.proactive.api.ProDeployment;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.body.AbstractBody;
import org.objectweb.proactive.core.config.PAProperties;
import org.objectweb.proactive.core.descriptor.data.ProActiveDescriptorInternal;
import org.objectweb.proactive.core.descriptor.data.VirtualNode;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.node.NodeFactory;
import org.objectweb.proactive.core.runtime.ProActiveRuntime;
import org.objectweb.proactive.core.runtime.ProActiveRuntimeImpl;
import org.objectweb.proactive.core.security.ProActiveSecurityManager;
import org.objectweb.proactive.core.util.URIBuilder;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.p2p.service.util.P2PConstants;


/**
 * @author Alexandre di Costanzo
 *
 * Created on Jan 12, 2005
 */
public class P2PNodeManager implements Serializable, InitActive, EndActive,
    P2PConstants, ProActiveInternalObject {
    private static final Logger logger = ProActiveLogger.getLogger(Loggers.P2P_NODES);
    private static final int PROC = Runtime.getRuntime().availableProcessors();
    private Node p2pServiceNode = null;
    private ProActiveRuntime proactiveRuntime = null;
    private final Vector<Node> availbaleNodes = new Vector<Node>();
    private final Vector<Object> bookedNodes = new Vector<Object>();
    private final Vector<Node> usingNodes = new Vector<Node>();
    private int nodeCounter = 0;

    //    private final String descriptorPath = PAProperties.PA_P2P_XML_PATH.getValue();
    private final String descriptorPath = System.getProperty(PAProperties.PA_P2P_XML_PATH.getKey());
    private ProActiveDescriptorInternal pad = null;

    //--------------------------------------------------------------------------
    // Class Constructors
    //--------------------------------------------------------------------------

    /**
     * Empty constructor for new active.
     */
    public P2PNodeManager() {
        // The empty constructor
    }

    //--------------------------------------------------------------------------
    // Public Class methods
    // -------------------------------------------------------------------------

    /**
     * Asking a shared node.
     * @return a <code>P2PNode</code> which contains a node or <code>null</code>
     * if no shared nodes are available.
     */
    public P2PNode askingNode(String nodeFamilyRegexp) {
        logger.debug("Asking a node to the nodes manager");
        if ((nodeFamilyRegexp == null) || (nodeFamilyRegexp.length() == 0) ||
                System.getProperty("os.name").matches(nodeFamilyRegexp)) {
            logger.debug("Family Match");
            if ((this.availbaleNodes.size() == 0) &&
                    (this.bookedNodes.size() == 0) &&
                    (this.usingNodes.size() == 0)) {
                this.deployingDefaultSharedNodes();
            }
            if (this.availbaleNodes.size() > 0) {
                Node node = this.availbaleNodes.remove(0);
                this.bookedNodes.add(new Booking(node));
                logger.debug("Yes the manager has a node");
                return new P2PNode(node,
                    (P2PNodeManager) ProActiveObject.getStubOnThis());
            }
        }

        // All nodes is already assigned
        logger.debug("Sorry no availbale node for the moment");
        return new P2PNode(null, null);
    }

    public Vector<Node> askingAllNodes(String nodeFamilyRegexp) {
        logger.debug("Asking all nodes to the nodes manager");
        if ((nodeFamilyRegexp == null) || (nodeFamilyRegexp.length() == 0) ||
                System.getProperty("os.name").matches(nodeFamilyRegexp)) {
            logger.debug("Family Match");
            if ((this.availbaleNodes.size() == 0) &&
                    (this.bookedNodes.size() == 0) &&
                    (this.usingNodes.size() == 0)) {
                this.deployingDefaultSharedNodes();
            }
            if (this.availbaleNodes.size() > 0) {
                Vector<Node> allNodes = new Vector<Node>(this.availbaleNodes);
                this.availbaleNodes.removeAllElements();
                this.bookedNodes.addAll(allNodes);
                logger.debug("Yes the manager has some nodes");
                return allNodes;
            }
        }

        // All nodes is already assigned
        logger.debug("Sorry no availbale node for the moment");
        return new Vector<Node>();
    }

    public P2PNode askingNode(boolean evenIfItIsShared) {
        if (!evenIfItIsShared) {
            return askingNode(null);
        }
        logger.debug("Asking a node to the nodes manager");
        if ((this.availbaleNodes.size() == 0) &&
                (this.bookedNodes.size() == 0) &&
                (this.usingNodes.size() == 0)) {
            this.deployingDefaultSharedNodes();
        }
        if (this.availbaleNodes.size() > 0) {
            Node node = this.availbaleNodes.remove(0);
            this.bookedNodes.add(new Booking(node));
            logger.debug("Yes, the manager has an empty node");
            return new P2PNode(node,
                (P2PNodeManager) ProActiveObject.getStubOnThis());
        } else if (this.bookedNodes.size() > 0) {
            Node node = ((Booking) this.bookedNodes.get(0)).getNode();
            logger.debug("Yes, the manager has a shared node");
            return new P2PNode(node,
                (P2PNodeManager) ProActiveObject.getStubOnThis());
        } else {
            // All nodes is already assigned
            logger.debug("Sorry no availbale node for the moment");
            return new P2PNode(null, null);
        }
    }

    /**
     * Leave the specified node. The node is killed and new one is created and
     * ready for sharing.
     * @param nodeToFree the node to kill.
     */
    public void leaveNode(Node nodeToFree) {
        String nodeUrl = nodeToFree.getNodeInformation().getURL();
        logger.debug("LeaveNode message received for node @" + nodeUrl);
        this.usingNodes.remove(nodeToFree);
        try {
            // Kill the node
            if (this.descriptorPath == null) {
                this.proactiveRuntime.killNode(nodeUrl);
                logger.info("Node @" + nodeUrl + " left");
                // Creating a new node
                this.createNewNode();
            } else {
                this.availbaleNodes.add(nodeToFree);
            }
        } catch (Exception e) {
            logger.fatal("Coudln't delete or create a shared node", e);
        }
    }

    /**
     * Free a booked node.
     * @param givenNode node given and not used.
     */
    public void noMoreNodeNeeded(Node givenNode) {
        Iterator<Object> it = this.bookedNodes.iterator();
        while (it.hasNext()) {
            Booking current = (Booking) it.next();
            if (current.getNode().equals(givenNode)) {
                this.bookedNodes.remove(current);
                break;
            }
        }
        this.availbaleNodes.add(givenNode);
        if (logger.isInfoEnabled()) {
            logger.info("Booked node " +
                givenNode.getNodeInformation().getURL() + " is now shared");
        }
    }

    //--------------------------------------------------------------------------
    // Active Object methods
    //--------------------------------------------------------------------------

    /**
     * @see org.objectweb.proactive.InitActive#initActivity(org.objectweb.proactive.Body)
     */
    public void initActivity(Body body) {
        logger.debug("Entering initActivity");

        try {
            // Getting reference to the P2P node
            this.p2pServiceNode = NodeFactory.getNode(body.getNodeURL());
            // Getting ProActive runtime
            this.proactiveRuntime = this.p2pServiceNode.getProActiveRuntime();
        } catch (NodeException e) {
            logger.fatal("Couldn't get reference to the local p2pServiceNode", e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("P2P node manager is running at " +
                this.p2pServiceNode.getNodeInformation().getURL());
            logger.debug("ProActiveRuntime at " +
                this.proactiveRuntime.getURL());
        }
        try {
            ProActiveObject.register(ProActiveObject.getStubOnThis(),
                URIBuilder.buildURIFromProperties("localhost", "P2PNodeManager")
                          .toString());
        } catch (IOException e) {
            logger.fatal("Couldn't register the P2P node manager", e);
        }

        // Creating shared nodes
        if (this.descriptorPath == null) {
            this.deployingDefaultSharedNodes();
        } else {
            this.deployingXmlSharedNodes();
        }

        logger.debug("Exiting initActivity");
    }

    /**
     * @see org.objectweb.proactive.EndActive#endActivity(org.objectweb.proactive.Body)
     */
    public void endActivity(Body body) {
        if (this.pad != null) {
            try {
                this.pad.killall(false);
            } catch (ProActiveException e) {
                logger.warn("Couldn't kill deployed nodes", e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private class method
    // -------------------------------------------------------------------------

    /**
     * @return a new shared node.
     * @throws NodeException
     * @throws ProActiveException
     * @throws AlreadyBoundException
     */
    private Node createNewNode()
        throws NodeException, ProActiveException, AlreadyBoundException {
        // security
        ProActiveSecurityManager psm = ((AbstractBody) ProActiveObject.getBodyOnThis()).getProActiveSecurityManager();
        ProActiveSecurityManager newNodeSecurityManager = null;
        if (psm != null) {
            newNodeSecurityManager = psm.generateSiblingCertificate(P2PConstants.VN_NAME);
        } else {
            ProActiveLogger.getLogger(Loggers.SECURITY_NODE)
                           .debug("Node created without security manager");
        }

        Node newNode = NodeFactory.createNode(URIBuilder.buildURI("localhost",
                    P2PConstants.SHARED_NODE_NAME + "_" + this.nodeCounter++,
                    URIBuilder.getProtocol(ProActiveRuntimeImpl.getProActiveRuntime()
                                                               .getURL())
                              .toString(),
                    URIBuilder.getPortNumber(ProActiveRuntimeImpl.getProActiveRuntime()
                                                                 .getURL()))
                                                        .toString(), true,
                newNodeSecurityManager, P2PConstants.VN_NAME, null);
        this.availbaleNodes.add(newNode);
        logger.info("New shared node created @" +
            newNode.getNodeInformation().getURL());
        return newNode;
    }

    /**
     * Starting default shared nodes. One by processor.
     */
    private void deployingDefaultSharedNodes() {
        assert PROC > 0 : "Processor count = 0";
        logger.debug("Number of available processors for this JVM: " + PROC);
        int nodes = PROC;
        if (!PAProperties.PA_P2P_MULTI_PROC_NODES.isTrue()) {
            nodes = 1;
        }

        // No sharing enable
        if (PAProperties.PA_P2P_NO_SHARING.isTrue()) {
            nodes = 0;
        }

        // Starting default shared nodes
        for (int procNumber = 0; procNumber < nodes; procNumber++) {
            try {
                Node node = this.createNewNode();
                logger.debug("Default shared node succefuly created at: " +
                    node.getNodeInformation().getURL());
            } catch (Exception e) {
                logger.warn("Couldn't create default shared node", e);
            }
        }
        logger.info(nodes + " shared nodes deployed");
    }

    /**
     * Deploying shred nodes from a XML descriptor
     */
    private void deployingXmlSharedNodes() {
        try {
            this.pad = ProDeployment.getProactiveDescriptor(this.descriptorPath);
        } catch (ProActiveException e) {
            logger.fatal("Could't get ProActive Descripor at " +
                this.descriptorPath, e);
            return;
        }
        VirtualNode[] virtualNodes = this.pad.getVirtualNodes();
        this.pad.activateMappings();
        for (int i = 0; i < virtualNodes.length; i++) {
            VirtualNode currentVn = virtualNodes[i];
            Node[] nodes;
            try {
                nodes = currentVn.getNodes();
                for (int j = 0; j < nodes.length; j++) {
                    this.availbaleNodes.add(nodes[j]);
                }
            } catch (NodeException e) {
                logger.warn("Problem with nodes for " + currentVn.getName(), e);
            }
        }

        // Killing deployed nodes at the JVM shutdown
        XmlNodeKiller killer = new XmlNodeKiller(this.pad);
        Runtime.getRuntime().addShutdownHook(new Thread(killer));

        logger.info(this.availbaleNodes.size() + " shared nodes deployed");
    }

    // -------------------------------------------------------------------------
    // Inner class
    // -------------------------------------------------------------------------

    /**
     *
     * Representing a booking node.
     *
     * @author Alexandre di Costanzo
     *
     * Created on Jan 14, 2005
     */
    private class Booking {
        private Node node = null;

        /**
         * Construct a new <code>Booking</code> for a node.
         * @param node the node to book.
         */
        public Booking(Node node) {
            this.node = node;
        }

        /**
         * @return Returns the booked node.
         */
        public Node getNode() {
            return this.node;
        }
    }
}
