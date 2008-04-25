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
package org.objectweb.proactive.ic2d.jmxmonitoring.data;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerInvocationHandler;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.jmx.ProActiveConnection;
import org.objectweb.proactive.core.jmx.mbean.NodeWrapperMBean;
import org.objectweb.proactive.core.jmx.mbean.ProActiveRuntimeWrapperMBean;
import org.objectweb.proactive.core.jmx.naming.FactoryName;
import org.objectweb.proactive.core.jmx.util.JMXNotificationManager;
import org.objectweb.proactive.core.util.URIBuilder;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.ic2d.console.Console;
import org.objectweb.proactive.ic2d.jmxmonitoring.Activator;
import org.objectweb.proactive.ic2d.jmxmonitoring.data.listener.RuntimeObjectListener;
import org.objectweb.proactive.ic2d.jmxmonitoring.util.MVCNotification;
import org.objectweb.proactive.ic2d.jmxmonitoring.util.MVCNotificationTag;
import org.objectweb.proactive.p2p.service.util.P2PConstants;


/**
 * Represents a Runtime in the IC2D model.
 * @author The ProActive Team
 */
public class RuntimeObject extends AbstractData {

    /**
     * All the method names used to notify the observers
     */

    //    public enum methodName {RUNTIME_KILLED,
    //        RUNTIME_NOT_RESPONDING,
    //        RUNTIME_NOT_MONITORED;
    //    }
    //    ;
    private final HostObject parent;
    private final String url;

    //private ProActiveConnection connection;
    private final String hostUrlServer;
    private final String serverName;
    private ProActiveRuntimeWrapperMBean proxyMBean;
    private transient Logger logger = ProActiveLogger.getLogger(Loggers.JMX_NOTIFICATION);

    /** JMX Notification listener */
    private final javax.management.NotificationListener listener;

    public RuntimeObject(HostObject parent, String runtimeUrl, ObjectName objectName, String hostUrl,
            String serverName) {
        super(objectName);
        this.parent = parent;

        this.url = FactoryName.getCompleteUrl(runtimeUrl);

        this.hostUrlServer = hostUrl;
        this.serverName = serverName;

        this.listener = new RuntimeObjectListener(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public HostObject getParent() {
        return this.parent;
    }

    @Override
    public void explore() {
        findNodes();
    }

    @Override
    public String getKey() {
        return this.url;
    }

    @Override
    public String getType() {
        return "runtime object";
    }

    @Override
    public String getHostUrlServer() {
        return this.hostUrlServer;
    }

    @Override
    protected String getServerName() {
        return this.serverName;
    }

    /**
     * Returns the url of this object.
     * @return An url.
     */
    public String getUrl() {
        return this.url;
    }

    @Override
    public void destroy() {
        //    this.resetCommunications();
        //    System.out.println("Destroying Runtime Representation Object "+this.getObjectName().getCanonicalName());
        //      JMXNotificationManager.getInstance()
        //      .unsubscribe(this.getObjectName(), this.getListener());
        //    System.out.println("Runtime Listener unsubscribed");
        //      
        Iterator<AbstractData> children = this.getMonitoredChildrenAsList().iterator();
        while (children.hasNext()) {
            AbstractData child = children.next();
            child.destroy();
        }

        super.destroy();
    }

    @Override
    public void stopMonitoring(boolean log) {
        super.stopMonitoring(log);
        JMXNotificationManager.getInstance().unsubscribe(this.getObjectName(), this.getListener());
    }

    /**
     * Kill this runtime.
     */
    public void killRuntime() {
        new Thread() {
            @Override
            public void run() {
                Object[] params = {};
                String[] signature = {};
                invokeAsynchronous("killRuntime", params, signature);
                runtimeKilled();
            }
        }.start();
    }

    public void runtimeKilled() {
        setChanged();
        notifyObservers(new MVCNotification(MVCNotificationTag.RUNTIME_OBJECT_RUNTIME_KILLED));
        ;
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(3000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                RuntimeObject.this.destroy();
            }
        }.start();
    }

    /**
     * Updates the set of IC2D's NodeObjects so that it is in sync with the ProActive Nodes on the monitored Host.
     * The update is performed by comparing the existing NodeObjects with the set of ProActive Node Objects
     * returned from <code>ProActiveRuntimeWrapperMBean.getNodes() </code>
     *
     */
    @SuppressWarnings("unchecked")
    private void findNodes() {
        if (this.proxyMBean == null) {
            if (getProActiveConnection() == null)
                return;
            this.proxyMBean = (ProActiveRuntimeWrapperMBean) MBeanServerInvocationHandler.newProxyInstance(
                    getProActiveConnection(), getObjectName(), ProActiveRuntimeWrapperMBean.class, false);
        }

        if (!PAActiveObject.pingActiveObject(getProActiveConnection())) {
            System.out.println("Connection to runtime closed: " + this.getName());
            return;
        }

        try {
            if (!(getProActiveConnection().isRegistered(getObjectName()))) {
                return;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        List<ObjectName> nodeNames = null;
        try {
            nodeNames = proxyMBean.getNodes();
        } catch (ProActiveException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        final Map<String, AbstractData> childrenToRemove = this.getMonitoredChildrenAsMap();

        for (final ObjectName name : nodeNames) {
            // Search if the node is a P2P node
            final String nodeName = name.getKeyProperty(FactoryName.NODE_NAME_PROPERTY);
            if (nodeName.startsWith(P2PConstants.P2P_NODE_NAME) && getWorldObject().isP2PHidden()) {
                // We have to skeep this node because it is a P2PNode
                continue;
            }

            // Build the complete nodeUrl from the hostUrlServer and nodeName 
            final String nodeUrl = this.hostUrlServer + nodeName;

            // If this child is a NOT monitored child.
            if (containsChildInNOTMonitoredChildren(nodeUrl)) {
                continue;
            }

            NodeObject child = (NodeObject) this.getMonitoredChild(nodeUrl);

            // If this child is not monitored.
            if (child == null) {
                // Get the mbean proxy for the current node
                final NodeWrapperMBean proxyNodeMBean = (NodeWrapperMBean) MBeanServerInvocationHandler
                        .newProxyInstance(getProActiveConnection(), name, NodeWrapperMBean.class, false);

                // Get the jobId and the virtualNodeName in one call
                final String[] res = proxyNodeMBean.getJobIdAndVirtualNodeName();

                final String jobId = res[0];
                final String virtualNodeName = res[1];

                if (virtualNodeName == null) {
                    Console
                            .getInstance(Activator.CONSOLE_NAME)
                            .err(
                                    "Problem when getting virtual node name from the remote NodeWrapperNbean for node " +
                                        name);
                    logger
                            .error("Problem when getting virtual node name from the remote NodeWrapperNbean for node " +
                                nodeName + ". A null value was received.");
                    continue;
                }

                // Find the virtualNode if already monitored
                VirtualNodeObject vn = getWorldObject().getVirtualNode(virtualNodeName);

                // This virtual node is not monitored
                if (vn == null) {
                    vn = new VirtualNodeObject(virtualNodeName, jobId, getWorldObject());
                    getWorldObject().addVirtualNode(vn);
                }

                // Once the virtualNode object has been created or found 
                // Create the child node object
                child = new NodeObject(this, nodeUrl, name);
                // Set the already available proxy
                child.setProxyNodeMBean(proxyNodeMBean);
                // Set to the node the parent virtual node.
                child.setVirtualNode(vn);
                vn.addChild(child);
                addChild(child);
            }
            // This child is already monitored, but this child maybe contains some not monitord objects.
            //else {
            child.explore();
            //}
            // Removes from the model the not monitored or terminated nodes.
            childrenToRemove.remove(child.getKey());
        }

        // Some child have to be removed
        for (final AbstractData child : childrenToRemove.values()) {
            child.destroy();
        }
    }

    @Override
    public String getName() {
        return URIBuilder.getNameFromURI(getUrl());
    }

    /**
     * Returns the ProActiveConnection for this Runtime
     */
    @Override
    public ProActiveConnection getProActiveConnection() {
        return JMXNotificationManager.getInstance().getConnection(getUrl());
    }

    @Override
    public String toString() {
        return "Runtime: " + getUrl();
    }

    public NotificationListener getListener() {
        return this.listener;
    }
}
