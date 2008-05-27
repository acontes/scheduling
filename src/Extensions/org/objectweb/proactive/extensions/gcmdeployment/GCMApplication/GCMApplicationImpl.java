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
package org.objectweb.proactive.extensions.gcmdeployment.GCMApplication;

import static org.objectweb.proactive.extensions.gcmdeployment.GCMDeploymentLoggers.GCMA_LOGGER;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.descriptor.services.TechnicalService;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeFactory;
import org.objectweb.proactive.core.remoteobject.RemoteObjectExposer;
import org.objectweb.proactive.core.remoteobject.RemoteObjectHelper;
import org.objectweb.proactive.core.runtime.ProActiveRuntime;
import org.objectweb.proactive.core.runtime.ProActiveRuntimeImpl;
import org.objectweb.proactive.core.util.ProActiveRandom;
import org.objectweb.proactive.core.xml.VariableContractImpl;
import org.objectweb.proactive.extensions.gcmdeployment.GCMApplication.commandbuilder.CommandBuilder;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.GCMDeploymentDescriptor;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.GCMDeploymentDescriptorImpl;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.GCMDeploymentResources;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.bridge.Bridge;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.Group;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.hostinfo.HostInfo;
import org.objectweb.proactive.extensions.gcmdeployment.core.GCMVirtualNodeImpl;
import org.objectweb.proactive.extensions.gcmdeployment.core.GCMVirtualNodeInternal;
import org.objectweb.proactive.extensions.gcmdeployment.core.GCMVirtualNodeRemoteObjectAdapter;
import org.objectweb.proactive.extensions.gcmdeployment.core.TopologyImpl;
import org.objectweb.proactive.extensions.gcmdeployment.core.TopologyRootImpl;
import org.objectweb.proactive.gcmdeployment.GCMApplication;
import org.objectweb.proactive.gcmdeployment.GCMVirtualNode;
import org.objectweb.proactive.gcmdeployment.Topology;


public class GCMApplicationImpl implements GCMApplicationInternal {
    static private Map<Long, GCMApplication> localDeployments = new HashMap<Long, GCMApplication>();

    /** An unique identifier for this deployment */
    private long deploymentId;

    /** descriptor file */
    private URL descriptor = null;

    /** GCM Application parser (statefull) */
    private GCMApplicationParser parser = null;

    /** All Node Providers referenced by the Application descriptor */
    private Map<String, NodeProvider> nodeProviders = null;

    /** Defined Virtual Nodes */
    private Map<String, GCMVirtualNodeInternal> virtualNodes = null;

    /** The Deployment Tree */
    private TopologyRootImpl deploymentTree;

    /** A mapping to associate deployment IDs to Node Provider */
    private Map<Long, NodeProvider> topologyIdToNodeProviderMapping;

    /** The Command builder to use to start the deployment */
    private CommandBuilder commandBuilder;

    /** The node allocator in charge of Node dispatching */
    private NodeMapper nodeMapper;
    private ArrayList<String> currentDeploymentPath;
    private List<Node> nodes;
    private Object deploymentMutex = new Object();
    private boolean isStarted;

    private VariableContractImpl vContract;

    static public GCMApplication getLocal(long deploymentId) {
        return localDeployments.get(deploymentId);
    }

    public GCMApplicationImpl(String filename) throws ProActiveException, MalformedURLException {
        this(new URL("file", null, filename), null);
    }

    public GCMApplicationImpl(String filename, VariableContractImpl vContract) throws ProActiveException,
            MalformedURLException {
        this(new URL("file", null, filename), vContract);
    }

    public GCMApplicationImpl(URL file) throws ProActiveException {
        this(file, null);
    }

    public GCMApplicationImpl(URL file, VariableContractImpl vContract) throws ProActiveException {
        try {
            deploymentId = ProActiveRandom.nextPosLong();
            localDeployments.put(deploymentId, this);

            currentDeploymentPath = new ArrayList<String>();
            topologyIdToNodeProviderMapping = new HashMap<Long, NodeProvider>();
            nodes = new LinkedList<Node>();
            isStarted = false;

            if (vContract == null) {
                vContract = new VariableContractImpl();
            }
            this.vContract = vContract;

            descriptor = file;
            // vContract will be modified by the Parser to include variable defined in the descriptor
            parser = new GCMApplicationParserImpl(descriptor, this.vContract);
            nodeProviders = parser.getNodeProviders();
            virtualNodes = parser.getVirtualNodes();
            commandBuilder = parser.getCommandBuilder();
            nodeMapper = new NodeMapper(this, virtualNodes.values());

            this.vContract.close();

            // apply Application-wide tech services on local node
            //
            Node defaultNode = NodeFactory.getDefaultNode();
            Node halfBodiesNode = NodeFactory.getHalfBodiesNode();
            TechnicalServicesProperties appTSProperties = parser.getAppTechnicalServices();

            for (Map.Entry<String, HashMap<String, String>> tsp : appTSProperties) {

                TechnicalService ts = TechnicalServicesFactory.create(tsp.getKey(), tsp.getValue());
                if (ts != null) {
                    ts.apply(defaultNode);
                    ts.apply(halfBodiesNode);
                }
            }

            // Export this GCMApplication as a remote object
            RemoteObjectExposer<GCMApplication> roe = new RemoteObjectExposer<GCMApplication>(
                GCMApplication.class.getName(), this, GCMApplicationRemoteObjectAdapter.class);
            URI uri = RemoteObjectHelper.generateUrl(deploymentId + "/GCMApplication");
            roe.activateProtocol(uri);

            // Export all VirtualNodes as remote objects
            for (GCMVirtualNode vn : virtualNodes.values()) {
                RemoteObjectExposer<GCMVirtualNode> vnroe = new RemoteObjectExposer<GCMVirtualNode>(
                    GCMVirtualNode.class.getName(), vn, GCMVirtualNodeRemoteObjectAdapter.class);
                uri = RemoteObjectHelper.generateUrl(deploymentId + "/VirtualNode/" + vn.getName());
                vnroe.activateProtocol(uri);
            }
        } catch (Exception e) {
            GCMA_LOGGER.warn("GCM Application Descriptor cannot be created", e);
            throw new ProActiveException(e);
        }
    }

    /*
     * ----------------------------- GCMApplicationDescriptor interface
     */
    public void startDeployment() {
        synchronized (deploymentMutex) {
            if (isStarted) {
                GCMA_LOGGER.warn("A GCM Application descriptor cannot be started twice", new Exception());
            }

            isStarted = true;

            deploymentTree = buildDeploymentTree();
            for (GCMVirtualNodeInternal virtualNode : virtualNodes.values()) {
                virtualNode.setDeploymentTree(deploymentTree);
            }

            for (NodeProvider nodeProvider : nodeProviders.values()) {
                nodeProvider.start(commandBuilder, this);
            }
        }
    }

    public boolean isStarted() {
        synchronized (deploymentMutex) {
            return isStarted;
        }
    }

    public GCMVirtualNode getVirtualNode(String vnName) {
        return virtualNodes.get(vnName);
    }

    public Map<String, GCMVirtualNode> getVirtualNodes() {
        return new HashMap<String, GCMVirtualNode>(virtualNodes);
    }

    public void kill() {
        synchronized (deploymentMutex) {
            Set<String> cache = new HashSet<String>();

            synchronized (nodes) {
                for (Node node : nodes) {
                    try {
                        ProActiveRuntime part = node.getProActiveRuntime();
                        String url = part.getURL();
                        if (!cache.contains(url)) {
                            cache.add(url);
                            part.killRT(false);
                        }
                    } catch (Exception e) {
                        // Miam Miam Miam
                    }
                }
            }
        }
    }

    public Topology getTopology() {
        if (!virtualNodes.isEmpty())
            throw new IllegalStateException("getTopology cannot be called if a VirtualNode is defined");

        // To not block other threads too long we make a snapshot of the node set
        Set<Node> nodesCopied;
        synchronized (nodes) {
            nodesCopied = new HashSet<Node>(nodes);
        }
        return TopologyImpl.createTopology(deploymentTree, nodesCopied);
    }

    public List<Node> getAllNodes() {
        if (virtualNodes.size() != 0) {
            throw new IllegalStateException("getAllNodes cannot be called if a VirtualNode is defined");
        }

        Set<FakeNode> fakeNodes = nodeMapper.getUnusedNode(true);
        List<Node> nodes = new ArrayList<Node>();
        for (FakeNode fakeNode : fakeNodes) {
            nodes.add(fakeNode.create(GCMVirtualNodeImpl.DEFAULT_VN, null));
        }

        return nodes;
    }

    public String getDebugInformation() {
        Set<FakeNode> fakeNodes = nodeMapper.getUnusedNode(false);
        StringBuilder sb = new StringBuilder();
        sb.append("Number of unmapped nodes: " + fakeNodes.size() + "\n");
        for (FakeNode fakeNode : fakeNodes) {
            sb.append("\t" + fakeNode.getRuntimeURL() + "(capacity=" + fakeNode.getCapacity() + ")\n");
        }
        return sb.toString();
    }

    public void updateTopology(Topology topology) {
        if (!virtualNodes.isEmpty())
            throw new IllegalStateException("updateTopology cannot be called if a VirtualNode is defined");

        // To not block other threads too long we make a snapshot of the node set
        Set<Node> nodesCopied;
        synchronized (nodes) {
            nodesCopied = new HashSet<Node>(nodes);
        }
        TopologyImpl.updateTopology(topology, nodesCopied);
    }

    public VariableContractImpl getVariableContract() {
        return this.vContract;
    }

    public URL getDescriptorURL() {
        return descriptor;
    }

    /*
     * ----------------------------- GCMApplicationDescriptorInternal interface
     */
    public long getDeploymentId() {
        return deploymentId;
    }

    public NodeProvider getNodeProviderFromTopologyId(Long topologyId) {
        return topologyIdToNodeProviderMapping.get(topologyId);
    }

    public void addNode(Node node) {
        synchronized (nodes) {
            nodes.add(node);
        }
    }

    /*
     * ----------------------------- Internal Methods
     */
    protected TopologyRootImpl buildDeploymentTree() {
        // make root node from local JVM
        TopologyRootImpl rootNode = new TopologyRootImpl();

        ProActiveRuntimeImpl proActiveRuntime = ProActiveRuntimeImpl.getProActiveRuntime();
        currentDeploymentPath.clear();
        pushDeploymentPath(proActiveRuntime.getVMInformation().getName());

        rootNode.setDeploymentDescriptorPath("none"); // no deployment descriptor here

        rootNode.setApplicationDescriptorPath(descriptor.toExternalForm());

        rootNode.setDeploymentPath(getCurrentdDeploymentPath());
        popDeploymentPath();

        // Build leaf nodes
        for (NodeProvider nodeProvider : nodeProviders.values()) {
            for (GCMDeploymentDescriptor gdd : nodeProvider.getDescriptors()) {
                GCMDeploymentDescriptorImpl gddi = (GCMDeploymentDescriptorImpl) gdd;
                GCMDeploymentResources resources = gddi.getResources();

                HostInfo hostInfo = resources.getHostInfo();
                if (hostInfo != null) {
                    buildHostInfoTreeNode(rootNode, rootNode, hostInfo, nodeProvider, gdd);
                }

                for (Group group : resources.getGroups()) {
                    buildGroupTreeNode(rootNode, rootNode, group, nodeProvider, gdd);
                }

                for (Bridge bridge : resources.getBridges()) {
                    buildBridgeTree(rootNode, rootNode, bridge, nodeProvider, gdd);
                }
            }
        }

        return rootNode;
    }

    /**
     * return a copy of the current deployment path
     * 
     * @return
     */
    private List<String> getCurrentdDeploymentPath() {
        return new ArrayList<String>(currentDeploymentPath);
    }

    private TopologyImpl buildHostInfoTreeNode(TopologyRootImpl rootNode, TopologyImpl parentNode,
            HostInfo hostInfo, NodeProvider nodeProvider, GCMDeploymentDescriptor gcmd) {
        pushDeploymentPath(hostInfo.getId());
        TopologyImpl node = new TopologyImpl();
        node.setDeploymentDescriptorPath(gcmd.getDescriptorURL().toExternalForm());
        node.setApplicationDescriptorPath(rootNode.getApplicationDescriptorPath());
        node.setDeploymentPath(getCurrentdDeploymentPath());
        node.setNodeProvider(nodeProvider.getId());
        hostInfo.setTopologyId(node.getId());
        topologyIdToNodeProviderMapping.put(node.getId(), nodeProvider);
        rootNode.addNode(node, parentNode);
        popDeploymentPath(); // ???
        return node;
    }

    private void buildGroupTreeNode(TopologyRootImpl rootNode, TopologyImpl parentNode, Group group,
            NodeProvider nodeProvider, GCMDeploymentDescriptor gcmd) {
        pushDeploymentPath(group.getId());
        buildHostInfoTreeNode(rootNode, parentNode, group.getHostInfo(), nodeProvider, gcmd);
        popDeploymentPath();
    }

    private void buildBridgeTree(TopologyRootImpl rootNode, TopologyImpl parentNode, Bridge bridge,
            NodeProvider nodeProvider, GCMDeploymentDescriptor gcmd) {
        pushDeploymentPath(bridge.getId());

        TopologyImpl node = parentNode;

        // first look for a host info...
        //
        if (bridge.getHostInfo() != null) {
            HostInfo hostInfo = bridge.getHostInfo();
            node = buildHostInfoTreeNode(rootNode, parentNode, hostInfo, nodeProvider, gcmd);
        }

        // then groups...
        //
        if (bridge.getGroups() != null) {
            for (Group group : bridge.getGroups()) {
                buildGroupTreeNode(rootNode, node, group, nodeProvider, gcmd);
            }
        }

        // then bridges (and recurse)
        if (bridge.getBridges() != null) {
            for (Bridge subBridge : bridge.getBridges()) {
                buildBridgeTree(rootNode, node, subBridge, nodeProvider, gcmd);
            }
        }

        popDeploymentPath();
    }

    private boolean pushDeploymentPath(String pathElement) {
        return currentDeploymentPath.add(pathElement);
    }

    private void popDeploymentPath() {
        currentDeploymentPath.remove(currentDeploymentPath.size() - 1);
    }

    public void waitReady() {
        for (GCMVirtualNode vn : virtualNodes.values()) {
            vn.waitReady();
        }
    }

    public Set<String> getVirtualNodeNames() {
        return new HashSet<String>(virtualNodes.keySet());
    }
}
