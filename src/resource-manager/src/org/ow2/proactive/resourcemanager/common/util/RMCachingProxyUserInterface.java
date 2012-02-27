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
 *  Initial developer(s):               The ActiveEon Team
 *                        http://www.activeeon.com/
 *  Contributor(s):
 *
 * ################################################################
 * $$ACTIVEEON_INITIAL_DEV$$
 */
package org.ow2.proactive.resourcemanager.common.util;

import java.io.IOException;
import java.security.KeyException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.login.LoginException;

import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.extensions.annotation.ActiveObject;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.jmx.JMXClientHelper;
import org.ow2.proactive.jmx.provider.JMXProviderUtils;
import org.ow2.proactive.resourcemanager.authentication.RMAuthentication;
import org.ow2.proactive.resourcemanager.common.event.RMEvent;
import org.ow2.proactive.resourcemanager.common.event.RMEventType;
import org.ow2.proactive.resourcemanager.common.event.RMInitialState;
import org.ow2.proactive.resourcemanager.common.event.RMNodeEvent;
import org.ow2.proactive.resourcemanager.common.event.RMNodeSourceEvent;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.frontend.RMConnection;
import org.ow2.proactive.resourcemanager.frontend.RMEventListener;
import org.ow2.proactive.resourcemanager.frontend.RMMonitoringImpl;


/**
 * As  {@link RMProxyUserInterface}, this class implements an active object managing a connection 
 * to the Resource Manager (a proxy to the Resource Manager).
 * This class adds a cache mechanism that maintains the {@link RMInitialState} state of the
 * remote resource manager.
 * You must init the proxy by calling the {@link #init(String, String, String)} method 
 * after having created it
 */
@ActiveObject
public class RMCachingProxyUserInterface extends RMProxyUserInterface implements RMEventListener {

    protected RMAuthentication rmAuth;
    protected RMMonitoringImpl rmMonitoring;
    protected RMInitialState rmInitialState;
    protected RMEventType RMstate;
    protected Credentials credentials;

    protected JMXConnector nodeConnector;
    protected String nodeConnectorUrl;

    public boolean init(String url, Credentials credentials) throws RMException, KeyException, LoginException {

        this.credentials = credentials;
        this.rmAuth = RMConnection.join(url);
        this.target = rmAuth.login(credentials);

        rmInitialState = this.target.getMonitoring().addRMEventListener(
                (RMEventListener) PAActiveObject.getStubOnThis());

        // here we log on using an empty login field to ensure that
        // credentials are used.

        this.jmxClient = new JMXClientHelper(rmAuth, new Object[] { "", credentials });
        this.jmxClient.connect();
        return true;
    }

    /**
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#rmEvent(org.ow2.proactive.resourcemanager.common.event.RMEvent)
     */
    public void rmEvent(RMEvent event) {
        switch (event.getEventType()) {
            case SHUTTING_DOWN:
                RMstate = RMEventType.SHUTTING_DOWN;
                break;
            case SHUTDOWN:
                RMstate = RMEventType.SHUTDOWN;
                break;
        }

    }

    /**
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#nodeSourceEvent(org.ow2.proactive.resourcemanager.common.event.RMNodeSourceEvent)
     */
    public void nodeSourceEvent(RMNodeSourceEvent event) {
        switch (event.getEventType()) {
            case NODESOURCE_CREATED:
                rmInitialState.getNodeSource().add(event);
                break;
            case NODESOURCE_REMOVED:
                for (int i = 0; i < rmInitialState.getNodeSource().size(); i++) {
                    if (rmInitialState.getNodeSource().get(i).getSourceName().equals(event.getSourceName())) {
                        rmInitialState.getNodeSource().remove(i);
                        break;
                    }
                }
                break;
        }

    }

    /**
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#nodeEvent(org.ow2.proactive.resourcemanager.common.event.RMNodeEvent)
     */
    public void nodeEvent(RMNodeEvent event) {
        switch (event.getEventType()) {
            case NODE_REMOVED:
                for (int i = 0; i < rmInitialState.getNodesEvents().size(); i++) {
                    if (event.getNodeUrl().equals(rmInitialState.getNodesEvents().get(i).getNodeUrl())) {
                        rmInitialState.getNodesEvents().remove(i);
                        break;
                    }
                }
                break;
            case NODE_ADDED:
                rmInitialState.getNodesEvents().add(event);
                break;
            case NODE_STATE_CHANGED:
                for (int i = 0; i < rmInitialState.getNodesEvents().size(); i++) {
                    if (event.getNodeUrl().equals(rmInitialState.getNodesEvents().get(i).getNodeUrl())) {
                        rmInitialState.getNodesEvents().set(i, event);
                        break;
                    }
                }
                break;

        }
    }

    /**
     * give access to the cached initial state
     * @return the local version of the initial state
     */
    public RMInitialState getRMInitialState() {
        return rmInitialState;
    }

    /**
     * Retrieves attributes of the specified mbean.
     * 
     * @param sessionId current session
     * @param name of mbean
     * @param nodeJmxUrl mbean server url
     * @param attrs set of mbean attributes
     * 
     * @return mbean attributes values
     */
    public Object getNodeMBeanInfo(String nodeJmxUrl, String objectName, List<String> attrs)
            throws IOException, InstanceNotFoundException, IntrospectionException,
            MalformedObjectNameException, ReflectionException, NullPointerException {

        initNodeConnector(nodeJmxUrl);

        if ((attrs == null) || (attrs.size() == 0)) {
            // no attribute is requested, we return
            // the description of the mbean
            return nodeConnector.getMBeanServerConnection().getMBeanInfo(new ObjectName(objectName));
        } else {

            List<Object> result = new LinkedList<Object>();
            AttributeList attributes = nodeConnector.getMBeanServerConnection().getAttributes(
                    new ObjectName(objectName), attrs.toArray(new String[] {}));

            for (Object attrObj : attributes) {
                if (attrObj instanceof Attribute) {
                    // when data from MXBean is requested
                    // convert the composite data (see MXBean spec) into
                    // readable values
                    Attribute attr = (Attribute) attrObj;
                    Object value = attr.getValue();
                    if (value instanceof CompositeData[]) {
                        CompositeData[] valueArr = (CompositeData[]) value;
                        Object[] converted = new Object[valueArr.length];
                        for (int i = 0; i < valueArr.length; i++) {
                            converted[i] = convertCompositeData(valueArr[i]);
                        }
                        attr = new Attribute(attr.getName(), converted);
                    }
                    if (value instanceof CompositeData) {
                        CompositeData compositeData = (CompositeData) value;
                        attr = new Attribute(attr.getName(), convertCompositeData(compositeData));
                    }

                    result.add(attr);
                }
            }

            return result;
        }
    }

    /**
     * Converts MXBean composite data to json serializable values 
     */
    private Object convertCompositeData(CompositeData compositeData) {
        HashMap<String, Object> vals = new HashMap<String, Object>();
        for (Object key : compositeData.getCompositeType().keySet()) {
            vals.put(key.toString(), compositeData.get(key.toString()));
        }
        return vals;
    }

    /**
     * Retrieves attributes of the specified mbeans.
     * 
     * @param sessionId current session
     * @param objectNames mbean names (@see ObjectName format)
     * @param nodeJmxUrl mbean server url
     * @param attrs set of mbean attributes
     * 
     * @return mbean attributes values
     */
    public Object getNodeMBeansInfo(String nodeJmxUrl, String objectNames, List<String> attrs)
            throws IOException, InstanceNotFoundException, IntrospectionException,
            MalformedObjectNameException, ReflectionException, NullPointerException {

        initNodeConnector(nodeJmxUrl);

        Set<ObjectName> beans = nodeConnector.getMBeanServerConnection().queryNames(
                new ObjectName(objectNames), null);

        HashMap<String, Object> results = new HashMap<String, Object>();
        for (ObjectName bean : beans) {
            results
                    .put(bean.getCanonicalName(),
                            getNodeMBeanInfo(nodeJmxUrl, bean.getCanonicalName(), attrs));
        }

        return results;
    }

    /**
     * Initializes mbean server connection to specified url.
     * If the connection is already established returns existing
     * connection. 
     * 
     * @param url mbean server url
     * @return mbean server connection
     * @throws IOException 
     */
    private void initNodeConnector(String url) throws IOException {

        if (url == null) {
            throw new NullPointerException("nodeJmxUrl cannot be null");
        }

        if (url.equals(nodeConnectorUrl) && nodeConnector != null) {
            // already connected
            return;
        }

        if (nodeConnector != null) {
            nodeConnector.close();
            nodeConnector = null;
        }

        final HashMap<String, Object> env = new HashMap<String, Object>(2);
        env.put(JMXConnector.CREDENTIALS, new Object[] { "", credentials });
        if (url.startsWith("service:jmx:ro")) {
            env.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, JMXProviderUtils.RO_PROVIDER_PKGS);
        }
        try {
            nodeConnector = JMXConnectorFactory.connect(new JMXServiceURL(url), env);
            nodeConnectorUrl = url;
        } catch (IOException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof RuntimeException) {
                throw (RuntimeException) ex.getCause();
            } else {
                throw ex;
            }
        }
    }
}
