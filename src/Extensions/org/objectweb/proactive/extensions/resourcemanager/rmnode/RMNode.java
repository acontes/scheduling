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
package org.objectweb.proactive.extensions.resourcemanager.rmnode;

import java.util.HashMap;

import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.node.NodeInformation;
import org.objectweb.proactive.extensions.resourcemanager.common.event.RMNodeEvent;
import org.objectweb.proactive.extensions.resourcemanager.nodesource.frontend.NodeSource;
import org.objectweb.proactive.extensions.scheduler.common.scripting.ScriptResult;
import org.objectweb.proactive.extensions.scheduler.common.scripting.SelectionScript;


/**
 * An RMNode is a ProActive node able to execute schedulers tasks.
 * So an RMNode is a representation of a ProActive node object with its associated {@link NodeSource},
 * and its state in the Resource Manager :<BR>
 * -free : node is ready to perform a task.<BR>
 * -busy : node is executing a task.<BR>
 * -to be released : node is busy and have to be removed at the end of the its current task.<BR>
 * -down : node is broken, and not anymore able to perform tasks.<BR><BR>
 *
 * Resource Manager can select nodes that verify criteria. this selection is implemented with
 * {@link SelectionScript} objects. Each node memorize results of executed scripts, in order to
 * answer faster to a selection already asked.
 *
 *
 * @see NodeSource
 * @see SelectionScript
 *
 * @author ProActive team.
 * @version 3.9
 * @since ProActive 3.9
 *
 */
public interface RMNode extends Comparable<RMNode> {

    /**
     * The script has been executed on this node, and the result was negative.
     */
    public static final int NOT_VERIFIED_SCRIPT = 0;

    /**
     * The script has already responded by the negative,
     * but something has been executed on the node since.
     */
    public static final int NO_LONGER_VERIFIED_SCRIPT = 1;

    /**
     * The script has never been tested on this {@link RMNode}.
     */
    public static final int NEVER_TESTED = 2;

    /**
     * The script is verified on this node,
     * but something has been executed since the time it has been tested.
     */
    public static final int ALREADY_VERIFIED_SCRIPT = 3;

    /**
     * The script is verified, and nothing
     * has been executed since the verification.
     */
    public static final int VERIFIED_SCRIPT = 4;

    // SCRIPTING
    /**
     * Execute a {@link SelectionScript} on this {@link RMNode}
     * @return the {@link ScriptResult} corresponding to the script execution.
     */
    public ScriptResult<Boolean> executeScript(SelectionScript script);

    /**
     * Get a map of all selection scripts already tested on this node,
     * and the responses given.
     * @return the map of Script and status
     */
    public HashMap<SelectionScript, Integer> getScriptStatus();

    // ----------------------------------------------------------------------//
    // GET

    /**
     * @return the name of the node
     */
    public String getNodeName();

    /**
     * @return Node
     */
    public Node getNode() throws NodeException;

    /**
     * @return the node information, describing this node
     * @see org.objectweb.proactive.core.node.NodeInformation
     */
    public NodeInformation getNodeInformation() throws NodeException;

    /**
     * @return the name of the virtual node
     */
    public String getVNodeName();

    /**
     * @return the name of the ProActive descriptor
     */
    public String getPADName();

    /**
     * This method call nodeInformation.getHostName();
     *
     * @return the name of the host machine
     */
    public String getHostName();

    /**
     * This method call nodeInformation.getDescriptorVMName();
     *
     * @return the name of the virtual machine
     */
    public String getDescriptorVMName();

    /**
     * @return the {@link NodeSource} name of the source that handle the node
     */
    public String getNodeSourceId();

    /**
     * @return the URL of the node.
     */
    public String getNodeURL();

    /**
     * @return true if the node is free, false otherwise.
     */
    public boolean isFree();

    /**
     * @return true if the node is down, false otherwise.
     */
    public boolean isDown();

    /**
     * @return true if the node has to be released, false otherwise.
     */
    public boolean isToRelease();

    /**
     * @return true if the node is busy, false otherwise.
     */
    public boolean isBusy();

    /**
     * change the node's status to free
     * @throws NodeException
     */
    public void setFree() throws NodeException;

    /**
     * change the node's status to busy.
     * @throws NodeException
     */
    public void setBusy() throws NodeException;

    /**
     * change the node's status to 'to release'.
     * @throws NodeException.
     */
    public void setToRelease() throws NodeException;

    /**
     * change the node's status to down.
     */
    public void setDown();

    /**
     * @return a string describing the RMNode (status, vnode, host, pad, ...)
     */
    public String toString();

    /**
     * Cleaning method : remove all active objects on this node.
     */
    public void clean();

    /**
     * The {@link NodeSource} from where the MNode is issued.
     */
    public NodeSource getNodeSource();

    /**
     * Change the {@link NodeSource} from where the node is.
     * @param nodeSource
     */
    public void setNodeSource(NodeSource nodeSource);

    /**
     * Memorize a result of a selection script,
     * node verify conditions of the selection.
     * @param script script tested.
     */
    public void setVerifyingScript(SelectionScript script);

    /**
     * Memorize a result of a selection script,
     * node does not verify conditions of the selection.
     * @param script script tested.
     */
    public void setNotVerifyingScript(SelectionScript script);

    /**
     *  Builds the RMNodeEvent object for the RMNode
     *  @return the RMNodeEvent object related to the node.
     */
    public RMNodeEvent getNodeEvent();
}
