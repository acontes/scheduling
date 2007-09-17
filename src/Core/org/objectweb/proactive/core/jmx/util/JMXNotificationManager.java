package org.objectweb.proactive.core.jmx.util;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.ProActive;
import org.objectweb.proactive.core.jmx.ProActiveConnection;
import org.objectweb.proactive.core.jmx.client.ClientConnector;
import org.objectweb.proactive.core.jmx.naming.FactoryName;
import org.objectweb.proactive.core.jmx.notification.NotificationType;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.util.URIBuilder;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;


/**
 * This class is an utility class.
 * It gives you the possibility to register/unregister a listener to the notifications of a remote JMX MBean.
 * When an active object migrates, the notificationManager subscribes to the new JMX MBean server, and send you the notifications.
 *
 * @author ProActive Team
 * @version 07/28/2007
 * @see org.objectweb.proactive.core.jmx.ProActiveConnection
 * @see org.objectweb.proactive.core.jmx.client.ClientConnector
 * @see org.objectweb.proactive.core.jmx.server.ServerConnector
 */
public class JMXNotificationManager implements NotificationListener {
    private static Logger logger = ProActiveLogger.getLogger(Loggers.JMX);

    /**
     * To find the connection for a given server
     */
    private Map<ServerListened, ProActiveConnection> connections;

    /**
     * To find the server for a given ObjectName
     */
    private Map<ObjectName, ServerListened> servers;

    /**
     * To find the listeners for a given ObjectName
     */
    private Map<ObjectName, ConcurrentLinkedQueue<NotificationListener>> listeners;

    // Singleton
    private static JMXNotificationManager instance;

    /**
     * The active object listener of all notifications.
     */
    private JMXNotificationListener notificationlitener;

    private JMXNotificationManager() {
        connections = new ConcurrentHashMap<ServerListened, ProActiveConnection>();
        servers = new ConcurrentHashMap<ObjectName, ServerListened>();

        listeners = new ConcurrentHashMap<ObjectName, ConcurrentLinkedQueue<NotificationListener>>();

        try {
            this.notificationlitener = (JMXNotificationListener) ProActive.newActive(JMXNotificationListener.class.getName(),
                    new Object[] {  });
        } catch (ActiveObjectCreationException e) {
            logger.error("Can't create the JMX notifications listener active object",
                e);
        } catch (NodeException e) {
            logger.error("Can't create the JMX notifications listener active object",
                e);
        }
    }

    /**
     * Returns the unique instance of the JMXNotificationManager
     * @return Returns the unique instance of the JMXNotificationManager
     */
    public static JMXNotificationManager getInstance() {
        if (instance == null) {
            instance = new JMXNotificationManager();
        }
        return instance;
    }

    /**
     * Subscribes a notification listener to a <b>local</b> JMX MBean Server.
     * @param objectName The name of the MBean on which the listener should
     * be added.
     * @param listener The listener object which will handle the
     * notifications emitted by the registered MBean.
     */
    public void subscribe(ObjectName objectName, NotificationListener listener) {
        subscribe(objectName, listener, (NotificationFilter) null, null);
    }

    /**
     * Subscribes a notification listener to a <b>local</b> JMX MBean Server.
     * @param name The name of the MBean on which the listener should
     * be added.
     * @param listener The listener object which will handle the
     * notifications emitted by the registered MBean.
     * @param filter The filter object. If filter is null, no
     * filtering will be performed before handling notifications.
     * @param handback The context to be sent to the listener when a
     * notification is emitted.
     */
    public void subscribe(ObjectName objectName, NotificationListener listener,
        NotificationFilter filter, Object handback) {
        try {
            ManagementFactory.getPlatformMBeanServer()
                             .addNotificationListener(objectName, listener,
                filter, handback);
        } catch (InstanceNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Subscribes a notification listener to a <b>remote</b> JMX MBean Server.
     * @param objectName The object name of the MBean.
     * @param listener The notification listener.
     * @param hostUrl The url of the remote host.
     * @param serverName The name of the MBean server.
     */
    public void subscribe(ObjectName objectName, NotificationListener listener,
        String hostUrl, String serverName) {
        // We want the complete url 'protocol://host:port/path'
        String completeUrl = FactoryName.getCompleteUrl(hostUrl);

        ServerListened server = new ServerListened(hostUrl, serverName);
        ProActiveConnection connection = connections.get(server);

        // We have to create a new connection
        if (connection == null) {
            // Creation of the new connection
            ClientConnector cc = new ClientConnector(completeUrl, serverName);
            cc.connect();
            connection = cc.getConnection();
            if (server == null) {
                System.err.println(
                    "JMXNotificationManager.subscribe() server is null");
                return;
            }
            if (connection == null) {
                System.err.println(
                    "JMXNotificationManager.subscribe() connection is null");
                return;
            }
            // Updates our maps
            connections.put(server, connection);
            servers.put(objectName, server);
        }

        ConcurrentLinkedQueue<NotificationListener> notificationListeners = listeners.get(objectName);

        // This objectName is already listened
        if (notificationListeners != null) {
            // We add this listener to the listeners of this object.
            notificationListeners.add(listener);

            // Is it useful?
            listeners.put(objectName, notificationListeners);
        }
        // This objectName is not yet listened
        else {
            notificationListeners = new ConcurrentLinkedQueue<NotificationListener>();
            notificationListeners.add(listener);
            listeners.put(objectName, notificationListeners);
            notificationlitener.subscribe(connection, objectName, null, null);
        }
    }

    /**
     * Unsubscribes a notification listener to a local or remote JMX MBean Server.
     * @param objectName The object name if the MBean.
     * @param listener The notification listener.
     */
    public void unsubscribe(ObjectName objectName, NotificationListener listener) {
        // ------------- Try to unsubscribe to a LOCAL MBean Server ------------
        try {
            ManagementFactory.getPlatformMBeanServer()
                             .removeNotificationListener(objectName, listener);
        } catch (InstanceNotFoundException e) {
            //---------- Try to unsubscribe to a REMOTE MBean Server -----------
            ConcurrentLinkedQueue<NotificationListener> notificationListeners = listeners.get(objectName);

            // No listener listen this objectName, so we display an error message.
            if (notificationListeners == null) {
                logger.warn(
                    "JMXNotificationManager.unsubscribe() ObjectName not known");
                return;
            }
            // We have to remove the listener.
            else {
                boolean isRemoved = notificationListeners.remove(listener);

                // The listener didn't be listening this objectName, so we display an error message.
                if (!isRemoved) {
                    logger.warn(
                        "JMXNotificationManager.unsubscribe() Listener not known");
                }

                // If there is no listeners which listen this objectName, we remove this one.
                if (notificationListeners.isEmpty()) {
                    listeners.remove(objectName);
                    ServerListened server = servers.get(objectName);
                    if (server != null) {
                        ProActiveConnection connection = connections.get(server);
                        if (connection != null) {
                            // The connection is not yet closed
                            notificationlitener.unsubscribe(connections.get(
                                    server), objectName, null, null);
                        }
                    }
                    // Updates our maps
                    servers.remove(objectName);
                }
            }
        } catch (ListenerNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void handleNotification(Notification notification, Object handback) {
        String type = notification.getType();
        ObjectName oname = (ObjectName) notification.getSource();

        if (logger.isDebugEnabled()) {
            logger.debug("[" + type + "]\n[JMXNotificationManager] source=" +
                oname);
        }

        if (type.equals(NotificationType.setOfNotifications)) {
            ConcurrentLinkedQueue<Notification> notifications = (ConcurrentLinkedQueue<Notification>) notification.getUserData();
            String msg = notification.getMessage();

            // The active object containing the MBean has migrated, so we have to connect to a new remote host.
            if ((msg != null) && msg.equals(NotificationType.migrationMessage)) {
                Notification notif = notifications.element();
                ObjectName ob = (ObjectName) notif.getSource();

                // The JMX MBean server url
                String runtimeUrl = (String) notif.getUserData();

                String host = URIBuilder.getHostNameFromUrl(runtimeUrl);
                String runtimeName = URIBuilder.getNameFromURI(runtimeUrl);
                String protocol = URIBuilder.getProtocol(runtimeUrl);
                int port = URIBuilder.getPortNumber(runtimeUrl);

                String hostUrl = URIBuilder.buildURI(host, "", protocol, port)
                                           .toString();

                // The JMX MBean Server name
                // Warning: This is a convention used in the ServerConnector
                String serverName = runtimeName;

                // Search in our established connections
                ProActiveConnection connection = connections.get(new ServerListened(
                            hostUrl, serverName));

                // We have to open a new connection
                if (connection == null) {
                    // Creates a new Connection
                    ClientConnector cc = new ClientConnector(hostUrl, serverName);
                    cc.connect();
                    connection = cc.getConnection();
                }

                // Subscribes to the JMX notifications
                notificationlitener.subscribe(connection, ob, null, null);

                // Updates ours map
                ServerListened server = new ServerListened(hostUrl, serverName);
                servers.put(ob, server);
                connections.put(server, connection);
            }
        }

        listeners.get(oname);
        ConcurrentLinkedQueue<NotificationListener> l = listeners.get(oname);
        if (l == null) {
            // No listener listen this objectName
            listeners.remove(oname);
            return;
        }

        // Sends to the listeners the notification
        for (NotificationListener listener : l) {
            listener.handleNotification(notification, handback);
        }
    }

    public ProActiveConnection getConnection(String hostUrl, String serverName) {
        return connections.get(new ServerListened(hostUrl, serverName));
    }

    //
    // ------- INNER CLASS ---------
    //
    private class ServerListened {

        /**
         * The url of the remote host
         */
        private String hostUrl;

        /**
         * The JMX MBean Server name
         */
        private String serverName;

        /**
         * Creates a new ServerListened
         * @param hostUrl The url of the remote host.
         * @param serverName The JMX MBean Server name
         */
        public ServerListened(String hostUrl, String serverName) {
            this.hostUrl = hostUrl;
            this.serverName = serverName;
        }

        /**
         * Returns the url of the remote host.
         * @return the url of the remote host.
         */
        public String getHostUrl() {
            return hostUrl;
        }

        /**
         * Returns the JMX MBean Server name.
         * @return The JMX MBean Server name.
         */
        public String getServerName() {
            return serverName;
        }

        @Override
        public boolean equals(Object anObject) {
            if (!(anObject instanceof ServerListened)) {
                return false;
            }
            ServerListened otherServerListened = (ServerListened) anObject;
            return (this.hostUrl.equals(otherServerListened.getHostUrl()) &&
            this.serverName.equals(otherServerListened.getServerName()));
        }

        @Override
        public int hashCode() {
            return this.hostUrl.hashCode() + this.serverName.hashCode();
        }
    }
}
