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

import static org.objectweb.proactive.extensions.gcmdeployment.GCMDeploymentLoggers.GCM_NODEMAPPER_LOGGER;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import javax.management.Notification;
import javax.management.NotificationListener;

import org.objectweb.proactive.core.jmx.notification.GCMRuntimeRegistrationNotificationData;
import org.objectweb.proactive.core.jmx.notification.NotificationType;
import org.objectweb.proactive.core.jmx.util.JMXNotificationManager;
import org.objectweb.proactive.core.runtime.ProActiveRuntime;
import org.objectweb.proactive.core.runtime.ProActiveRuntimeImpl;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.GCMDeploymentDescriptor;
import org.objectweb.proactive.extensions.gcmdeployment.core.GCMVirtualNodeInternal;


public class NodeMapper implements NotificationListener {

    /** The GCM Application Descriptor associated to this Node Allocator */
    final private GCMApplicationInternal gcma;

    /** All Virtual Nodes */
    final private List<GCMVirtualNodeInternal> virtualNodes;

    /** Nodes waiting in Stage 2 */
    final private Map<FakeNode, NodeProvider> stage2Pool;

    /** Nodes waiting in Stage 3 */
    final private Map<FakeNode, NodeProvider> stage3Pool;

    /** A Semaphore to activate stage 2/3 node dispatching on node arrival */
    final private Semaphore semaphore;

    /*
     * Node allocation backend (inside GCMVirtualNode) is not thread safe. This mutex must be take
     * each time a dispatchSx function is called
     * 
     * Anyway, notifications are currently synchronized by JMX. No concurrency should be encountered
     * :-/
     */
    final private Object dispatchMutex;

    public NodeMapper(GCMApplicationImpl gcma, Collection<GCMVirtualNodeInternal> virtualNodes) {
        this.gcma = gcma;

        this.virtualNodes = new LinkedList<GCMVirtualNodeInternal>();
        this.virtualNodes.addAll(virtualNodes);

        this.semaphore = new Semaphore(0);
        this.dispatchMutex = new Object();

        /*
         * Stage2Pool and Stage3Pool need weakly consistent iterators. All this class must be
         * rewritten if fail fast iterators are used
         */
        this.stage2Pool = new ConcurrentHashMap<FakeNode, NodeProvider>();
        this.stage3Pool = new ConcurrentHashMap<FakeNode, NodeProvider>();
        subscribeJMXRuntimeEvent();
        startStage23Thread();
    }

    private void subscribeJMXRuntimeEvent() {
        ProActiveRuntimeImpl part = ProActiveRuntimeImpl.getProActiveRuntime();
        part.addDeployment(gcma.getDeploymentId());
        JMXNotificationManager.getInstance().subscribe(part.getMBean().getObjectName(), this);
    }

    public void handleNotification(Notification notification, Object handback) {
        try {
            String type = notification.getType();

            if (NotificationType.GCMRuntimeRegistered.equals(type)) {
                GCMRuntimeRegistrationNotificationData data = (GCMRuntimeRegistrationNotificationData) notification
                        .getUserData();
                if (data.getDeploymentId() != gcma.getDeploymentId()) {
                    return;
                }

                ProActiveRuntime nodePart = data.getChildRuntime();
                NodeProvider nodeProvider = gcma.getNodeProviderFromTopologyId(data.getTopologyId());

                for (int i = 0; i < nodePart.getVMInformation().getCapacity(); i++) {
                    FakeNode fakeNode = new FakeNode(gcma, nodePart);

                    synchronized (dispatchMutex) {
                        if (!dispatchS1(fakeNode, nodeProvider)) {
                            stage2Pool.put(fakeNode, nodeProvider);
                        }
                    }
                }

                // Wake up the Stage2 / Stage3 dispatcher
                semaphore.release();
            }
        } catch (Exception e) {
            // If not handled by us, JMX eats the Exception !
            GCM_NODEMAPPER_LOGGER.warn(e);
        }
    }

    /**
     * Try to give the node to a Virtual Node to fulfill a NodeProviderContract
     * 
     * @param fakeNode
     *            The node who registered to the local runtime
     * @param nodeProvider
     *            The {@link GCMDeploymentDescriptor} who created the node
     * @return returns true if a GCMVirtualNode took the Node, false otherwise
     */
    private boolean dispatchS1(FakeNode fakeNode, NodeProvider nodeProvider) {
        GCM_NODEMAPPER_LOGGER.trace("Stage1: " + fakeNode.getRuntimeURL() + " (capacity=" +
            fakeNode.getCapacity() + ")from " + nodeProvider.getId());

        for (GCMVirtualNodeInternal virtualNode : virtualNodes) {
            if (virtualNode.doesNodeProviderNeed(fakeNode, nodeProvider)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Offers node to each GCMVirtualNode to fulfill GCMVirtualNode Capacity requirement.
     * 
     * @param fakeNode
     *            The node who registered to the local runtime
     * @param nodeProvider
     *            The {@link GCMDeploymentDescriptor} who created the node
     * @return returns true if a GCMVirtualNode took the Node, false otherwise
     */
    private boolean dispatchS2(FakeNode fakeNode, NodeProvider nodeProvider) {
        GCM_NODEMAPPER_LOGGER.trace("Stage2: " + fakeNode.getRuntimeURL() + " (capacity=" +
            fakeNode.getCapacity() + ")from " + nodeProvider.getId());

        // Check this Node can be dispatched 
        for (GCMVirtualNodeInternal virtualNode : virtualNodes) {
            if (virtualNode.hasContractWith(nodeProvider) && virtualNode.hasUnsatisfiedContract()) {
                return false;
            }
        }

        for (GCMVirtualNodeInternal virtualNode : virtualNodes) {
            if (virtualNode.doYouNeed(fakeNode, nodeProvider)) {
                stage2Pool.remove(fakeNode);
                return true;
            }
        }

        stage2Pool.remove(fakeNode);
        stage3Pool.put(fakeNode, nodeProvider);
        return false;
    }

    /**
     * 
     * @param fakeNode
     *            The node who registered to the local runtime
     * @param nodeProvider
     *            The {@link GCMDeploymentDescriptor} who created the node
     * @return
     */
    private boolean dispatchS3(FakeNode fakeNode, NodeProvider nodeProvider) {
        GCM_NODEMAPPER_LOGGER.trace("Stage3: " + fakeNode.getRuntimeURL() + " (capacity=" +
            fakeNode.getCapacity() + ")from " + nodeProvider.getId());

        for (GCMVirtualNodeInternal virtualNode : virtualNodes) {
            if (virtualNode.doYouWant(fakeNode, nodeProvider)) {
                stage3Pool.remove(fakeNode);
                virtualNodes.add(virtualNodes.remove(0));
                return true;
            }
        }

        return false;
    }

    private void startStage23Thread() {
        Thread t = new Stage23Dispatcher();
        t.setDaemon(true);
        t.setName("GCM Deployment Stage 2 and 3 dispatcher");
        t.start();
    }

    private class Stage23Dispatcher extends Thread {
        @Override
        public void run() {
            while (true) {
                // Wait for next handleNotification invocation
                try {
                    semaphore.acquire();
                    synchronized (dispatchMutex) {
                        for (FakeNode fakeNode : stage2Pool.keySet())
                            dispatchS2(fakeNode, stage2Pool.get(fakeNode));

                        for (FakeNode fakeNode : stage3Pool.keySet())
                            dispatchS3(fakeNode, stage3Pool.get(fakeNode));
                    }
                } catch (InterruptedException e) {
                    GCM_NODEMAPPER_LOGGER.error("Semaphore", e);
                }
            }
        }
    }

    public Set<FakeNode> getUnusedNode(boolean flush) {
        synchronized (dispatchMutex) {
            // dispatchMutex is a bit coarse grained but getUnusedNode should not be
            // called so often. Adding a new synchronization on stage3Pool is a bit overkill

            Set<FakeNode> ret = stage3Pool.keySet();
            if (flush) {
                stage3Pool.clear();
                GCM_NODEMAPPER_LOGGER.info("Flushed Stage3Pool");
            }
            return ret;
        }
    }

    public long getNbUnusedNode() {
        synchronized (dispatchMutex) {
            return stage3Pool.size();
        }
    }

}
