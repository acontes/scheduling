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
package org.objectweb.proactive.extensions.resourcemanager.nodesource.frontend;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.EndActive;
import org.objectweb.proactive.InitActive;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.body.exceptions.BodyTerminatedException;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.core.util.wrapper.IntWrapper;
import org.objectweb.proactive.extensions.resourcemanager.common.event.RMNodeSourceEvent;
import org.objectweb.proactive.extensions.resourcemanager.core.RMCore;
import org.objectweb.proactive.extensions.resourcemanager.core.RMCoreSourceInterface;
import org.objectweb.proactive.extensions.resourcemanager.exception.AddingNodesException;
import org.objectweb.proactive.extensions.resourcemanager.frontend.RMAdmin;
import org.objectweb.proactive.extensions.resourcemanager.nodesource.dynamic.DynamicNodeSource;
import org.objectweb.proactive.gcmdeployment.GCMApplication;


/**
 * Abstract class designed to manage a NodeSource.
 * A NodeSource active object is designed to manage acquisition, monitoring
 * and removing of a set of {@link Node} objects in the Resource Manager.
 * This set of nodes could be nodes deployed by a ProActive Descriptor,
 * or nodes acquired dynamically from a dynamic source,
 * such as a peer to peer infrastructure, or a cluster.<BR>
 * As the {@link RMCore} manage nodes providing to Scheduler (with nodes selection, and nodes states handling),
 * a NodeSource has just to : acquire nodes, add them to {@link RMCore}, monitor these acquired nodes,
 * and finally remove them from RMCore.
 * NodeSource communications to RMCore are defined in {@link org.objectweb.proactive.extensions.resourcemanager.core.RMCoreSourceInterface}.<BR><BR><BR>
 *
 *
 * There is a mechanism of giving-removing nodes between NodeSource and {@link RMCore} :<BR><BR>
 *
 * 1- Giving to RMCore a new available node :<BR>
 * {@link org.objectweb.proactive.extensions.resourcemanager.nodesource.frontend.NodeSource#addNewAvailableNode(Node,String,String)}
 * (method to call when a new node is available).<BR>
 * The NodeSource add node to the {@link RMCoreSourceInterface} with the method<BR>
 * {@link org.objectweb.proactive.extensions.resourcemanager.core.RMCoreSourceInterface#internalAddNode(Node, String, String, NodeSource)}<BR><BR>
 *
 * 2- NodeSource ask to remove the Node to the {@link RMCoreSourceInterface} with the method : <BR>
 * {@link org.objectweb.proactive.extensions.resourcemanager.core.RMCoreSourceInterface#internalRemoveNode(String, boolean)}<BR><BR>
 *
 * 3- Finally the {@link RMCore} confirm the removing request by calling <BR>
 * {@link org.objectweb.proactive.extensions.resourcemanager.nodesource.frontend.NodeSource#confirmRemoveNode(String)} <BR><BR>
 *
 *
 * A NodeSource has to treat explicit removing and adding nodes Requests asked by {@link RMAdmin} and forwarded by {@link RMCore}<BR>
 * - {@link RMCore} receive an adding nodes request, it just forwards the request to the appropriate NodeSource by calling :<BR>
 *                 {@link org.objectweb.proactive.extensions.resourcemanager.nodesource.frontend.NodeSource#addNodes(ProActiveDescriptor, String)}<BR>
 * - {@link RMCore} receive a removing node request, it just forwards the request to the appropriate NodeSource by calling :<BR>
 *                 {@link org.objectweb.proactive.extensions.resourcemanager.nodesource.frontend.NodeSource#forwardRemoveNode(String, boolean)}.
 * The NodeSource receive the removing node request and perform the removing as the Steps 2 and 3 of the remove mechanism presented above<BR><BR>
 *
 *
 * This class implements a Nodes monitoring mechanism implemented by an inner class {@link Pinger}. This object run a Pinger thread,
 * which will ping  {@link Node} objects each {@link NodeSource#DEFAULT_NODE_SOURCE_PING_FREQUENCY} ms.
 * When a down node is detected, the Pinger inform the NodeSource object about the down node
 * (call @see org.objectweb.proactive.extensions.resourcemanager.nodesource.frontend.NodeSource#detectedPingedDownNode(String);).
 *
 *
 *        @see RMCoreSourceInterface
 *        @see RMCore
 *
 * @author The ProActive Team
 * @version 3.9
 * @since ProActive 3.9
 *
 */
public abstract class NodeSource implements Serializable, InitActive, EndActive {

    /** RM logger */
    protected final static Logger logger = ProActiveLogger.getLogger(Loggers.RM_CORE);

    /** {@link RMCore} interface for the NodeSource */
    protected RMCoreSourceInterface rmCore;

    /** unique id of the source */
    protected String SourceId = null;

    /** Ping frequency of the Node Pinger thread */
    protected static final int DEFAULT_NODE_SOURCE_PING_FREQUENCY = 5000;
    protected Pinger pinger;

    /** HashMap of nodes available and managed by this NodeSource
     * All nodes in this HashMap are registered in {@link RMCore} too */
    public HashMap<String, Node> nodes;
    protected boolean toShutdown = false;

    /**
     * ProActive empty constructor.
     */
    public NodeSource() {
    }

    /**
     * Creates a new NodeSource.
     * @param id unique id of the source.
     * @param rmCore the {@link RMCoreSourceInterface} already created of the Resource Manager.
     */
    public NodeSource(String id, RMCoreSourceInterface rmCore) {
        this.SourceId = id;
        this.nodes = new HashMap<String, Node>();
        this.rmCore = rmCore;
    }

    /**
     * Initialization part of NodeSource Active Object.
     * Create the pinger thread which monitor nodes handled by the source and
     * register itself to the {@link RMCore}.
     */
    public void initActivity(Body body) {
        this.rmCore.internalAddSource((NodeSource) PAActiveObject.getStubOnThis(), this.SourceId);
        // TODO gsigety, cdelbe : giving a stub on the source to Pinger can 
        // lead to a lock if source is blocked
        pinger = new Pinger((NodeSource) PAActiveObject.getStubOnThis());
    }

    /**
     * Shutdown the node source.
     * Method in this abstract class just stop the Pinger thread,
     * so for NodeSource's inherited classes, they must override this method,
     * each node source type has to specify what to perform for the shutting down
     * (certainly remove nodes handled by the source).
     * All nodes are removed from node source and from RMCore
     * @param preempt true Node source doesn't wait tasks end on its handled nodes,
     * false node source wait end of tasks on its nodes before shutting down
     */
    public void shutdown(boolean preempt) {
        this.toShutdown = true;
        this.pinger.shutdown();
    }

    /**
     * Returns the event object representing the NodeSource.
     * <BR>Called by {@link RMCore}.<BR>
     * Create a {@link RMNodeSourceEvent} object representing the NodeSource State.
     * @return {@link RMNodeSourceEvent} object contains properties of the NodeSource.
     */
    public abstract RMNodeSourceEvent getSourceEvent();

    /**
     * Manages an explicit removing node request.
     * <BR>Called by {@link RMCore}.<BR>
     * {@link RMAdmin} asked an explicit removing node request to {@link RMCore}.<BR>
     * {@link RMCore} just forward a removing Node request to the appropriate NodeSource,
     * This abstract method must be defined to perform removing actions of a node.
     * @param nodeUrl URL of the node to remove.
     * @param preempt true the node must removed immediately, without waiting job ending if the node is busy,
     * false the node is removed just after the job ending if the node is busy.
     */
    public abstract void forwardRemoveNode(String nodeUrl, boolean preempt);

    /**
     * Manages an explicit adding nodes request.
     * <BR>Called by {@link RMCore}.<BR>
     * The way to add a static nodes on a NodeSource, those new nodes are deployed or acquired
     * by the NodeSource itself.<BR>
     * This method is useful for static sources only (see {@link GCMNodeSource}),
     * an exception is thrown when this request is asked to a {@link DynamicNodeSource}.
     * @param pad ProActive Deployment descriptor representing nodes to deploy.
     * @param padName a name associated with the ProActive Descriptor.
     * @throws AddingNodesException thrown if this method is asked on a {@link DynamicNodeSource}.
     */
    public abstract void addNodes(GCMApplication app) throws AddingNodesException;

    /**
     * Adds an  already deployed node to the NodeSource.
     * lookup the node an add the node to the Source
     * Operation unavailable on a dynamic node source
     * @param nodeUrl
     * @throws AddingNodesException if lookup has failed
     * or asked to a dynamicnodeSource object.
     */
    public abstract void addNode(String nodeUrl) throws AddingNodesException;

    /**
     * Confirms a removing node request.
     * <BR>Called by {@link RMCore}.<BR>
     * Confirm to a NodeSource a node release :<BR>
     * NodeSource has asked previously a remove of a node to {@link RMCore}, by
     * the {@link RMCore#internalRemoveNode(String, boolean)} method.<BR>
     * So the {@link RMCore} has removed the Node and confirm the action to the NodeSource.
     * This abstract method must be defined to perform last actions for node removing mechanism.
     * @param nodeUrl Url of the node on which RMCore confirm the remove.
     */
    public abstract void confirmRemoveNode(String nodeUrl);

    // Getters --------------------------------------------------------------//

    /**
     * Gives the number of nodes handled by the NodeSource.
     * @return number of nodes handled by the NodeSource
     */
    public IntWrapper getNbNodes() {
        return new IntWrapper(nodes.size());
    }

    /**
     * Give nodes handled by the NodeSource.
     * This method is called by the internal Pinger in order to have an up-to-date nodes list.
     * @return an ArrayList of all {@link Node} Handled by the NodeSource
     */
    public ArrayList<Node> getNodes() {
        return new ArrayList<Node>(nodes.values());
    }

    /**
     * Gives the source Id of the NodeSource object.
     * @return a {@link String} which represent Id of th NodeSource
     */
    public String getSourceId() {
        return this.SourceId;
    }

    // ----------------------------------------------------------------------//
    // protected methods
    // ----------------------------------------------------------------------//

    /**
     * Gives a node corresponding to an url.
     * Internal method.
     * return the {@link Node} object according to the URL
     * @param url unique name of node
     */
    protected Node getNodebyUrl(String url) {
        return nodes.get(url);
    }

    /**
     * Manages the treatment of a new node available.
     * <BR>Internal method.<BR>
     * Must be called when a new node is available in the NodeSource,
     * so the NodeSource register it to the internal list
     * and provide the new node to the {@link RMCore}
     * @param node new node object available
     * @param VnName VirtualNode name of the node
     * @param PADName ProActiveDescriptor name of the node
     */
    protected void addNewAvailableNode(Node node, String VnName, String PADName) {
        if (logger.isInfoEnabled()) {
            logger.info("[" + this.SourceId + "] new node available : " + node.getNodeInformation().getURL());
        }
        this.nodes.put(node.getNodeInformation().getURL(), node);
        NodeSource s = (NodeSource) PAActiveObject.getStubOnThis();
        this.rmCore.internalAddNode(node, VnName, PADName, s);
    }

    /**
     * Removes the node from the {@link NodeSource#nodes} list.
     * <BR>Internal method.<BR>
     * Removing from internal list a node handled by the NodeSource
     * @param node {@link Node} to remove
     */
    protected void removeFromList(Node node) {
        nodes.remove(node.getNodeInformation().getURL());
    }

    // ----------------------------------------------------------------------//
    // method called by the intern class Pinger 
    // ----------------------------------------------------------------------//

    /**
     * Perform operations to do when a node is down.
     * <BR>Called when the Pinger has detected a down node, each NodeSource
     * has to specify what to do when a down node is detected
     * (certainly inform the RMCore about the broken node,
     * remove the broken node from the list this.nodes
     * and for a dynamic node source, adding a new nice time for example)
     * the method is public because called by the NodeSource stub
     * @param nodeUrl URL of the detected down {@link Node}
     */
    public abstract void detectedPingedDownNode(String nodeUrl);

    /**
     * Inner class which implements the Pinger thread.
     * <BR>This class communicate with its NodeSource upper class by the NodeSource AO stub,
     * not directly, in order to avoid concurrent access.
     * This object ask periodically list of nodes managed by its NodeSource object,
     * verify if nodes are still alive, and warn the {@link NodeSource} object if a node is down by calling
     *  {@link NodeSource#detectedPingedDownNode(String)} method.
     * @see org.objectweb.proactive.extensions.resourcemanager.nodesource.frontend.NodeSource
     *
     */
    public class Pinger extends Thread {

        /** stub of the NodeSource Active Object*/
        private NodeSource nodeSource;

        /** state of the thread, true Pinger "ping", false
         * pinger is stopped */
        private boolean active;

        /**
         * Pinger constructor.
         * Launch the nodes monitoring thread
         * @param source stub of the NodeSource Active Object
         */
        public Pinger(NodeSource source) {
            nodeSource = source;
            this.active = true;
            start();
        }

        /**
         * shutdown the Pinger thread .
         */
        public synchronized void shutdown() {
            this.active = false;
        }

        /**
         * Gives the state the Thread's state.
         * @return boolean indicating thread's state :
         * true, Pinger continues Pinging or
         * false Pinger thread stops.
         */
        public synchronized boolean isActive() {
            return this.active;
        }

        /**
         * Activity thread of the Pinger.
         * <BR>Each {@link NodeSource#DEFAULT_NODE_SOURCE_PING_FREQUENCY} time
         * the Pinger get the NodeList of the NodeSource,
         * and verify if nodes are always reachable
         * if one of them is unreachable, the node will be said "down",
         * and must be removed from the NodeSource.
         * {@link NodeSource#detectedPingedDownNode(String)} is called when a down node is detected.
         */
        @Override
        public void run() {
            while (!isInterrupted() && this.isActive()) {
                try {
                    try {
                        sleep(DEFAULT_NODE_SOURCE_PING_FREQUENCY);
                    } catch (InterruptedException ex) {
                    }
                    if (!this.isActive()) {
                        break;
                    }
                    for (Node node : nodeSource.getNodes()) {
                        // check active between each ping
                        if (!this.isActive()) {
                            break;
                        }
                        String nodeURL = node.getNodeInformation().getURL();
                        try {
                            node.getNumberOfActiveObjects();
                        } catch (Exception e) {
                            this.nodeSource.detectedPingedDownNode(nodeURL);
                        } //catch
                    } //for
                } catch (BodyTerminatedException e) {
                    // node source is terminated 
                    // terminate...
                    break;
                }
            } //while !interrupted
        } //run
    }
}
