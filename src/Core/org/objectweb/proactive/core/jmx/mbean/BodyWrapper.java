package org.objectweb.proactive.core.jmx.mbean;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.objectweb.proactive.ProActive;
import org.objectweb.proactive.benchmarks.timit.util.basic.BasicTimer;
import org.objectweb.proactive.core.UniqueID;
import org.objectweb.proactive.core.body.AbstractBody;
import org.objectweb.proactive.core.body.migration.Migratable;
import org.objectweb.proactive.core.body.migration.MigrationException;
import org.objectweb.proactive.core.body.request.Request;
import org.objectweb.proactive.core.gc.GarbageCollector;
import org.objectweb.proactive.core.gc.ObjectGraph;
import org.objectweb.proactive.core.jmx.naming.FactoryName;
import org.objectweb.proactive.core.jmx.notification.NotificationType;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.node.NodeFactory;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.core.util.profiling.TimerProvidable;
import org.objectweb.proactive.core.util.profiling.TimerWarehouse;


/**
 * Implementation of a BodyWrapperMBean
 * @author ProActive Team
 */
public class BodyWrapper extends NotificationBroadcasterSupport
    implements Serializable, BodyWrapperMBean {

    /** JMX Logger */
    private transient Logger logger = ProActiveLogger.getLogger(Loggers.JMX_MBEAN);
    private transient Logger notificationsLogger = ProActiveLogger.getLogger(Loggers.JMX_NOTIFICATION);

    /** ObjectName of this MBean */
    private transient ObjectName objectName;

    /** Unique id of the active object */
    private UniqueID id;

    /** The body wrapped in this MBean */
    private AbstractBody body;

    /** The url of node containing this active object */
    private transient String nodeUrl;

    /** The name of the body of the active object */
    private String bodyName;

    // -- JMX Datas --

    /** To know if we have to send the JMX notifications */
    private boolean shouldNotify = true;

    /** Timeout between updates */
    private long updateFrequence = 300;

    /** Used by the JMX notifications */
    private long counter = 1;

    /** A list of jmx notifications.
     * The current MBean sends a list of notifications in order to not overload the network
     */
    private transient ConcurrentLinkedQueue<Notification> notifications;

    public BodyWrapper() {

        /* Empty Constructor required by JMX */
    }

    /**
     * Creates a new BodyWrapper MBean, representing an active object.
     * @param oname
     * @param body
     */
    public BodyWrapper(ObjectName oname, AbstractBody body, UniqueID id) {
        this.objectName = oname;
        this.id = id;
        this.nodeUrl = body.getNodeURL();
        this.body = body;

        this.notifications = new ConcurrentLinkedQueue<Notification>();
        launchNotificationsThread();
    }

    public UniqueID getID() {
        return this.id;
    }

    public String getName() {
        if (this.bodyName == null) {
            this.bodyName = this.body.getName();
        }
        return this.bodyName;
    }

    public ObjectName getObjectName() {
        if (this.objectName == null) {
            this.objectName = FactoryName.createActiveObjectName(getID());
        }
        return this.objectName;
    }

    public String getNodeUrl() {
        return this.nodeUrl;
    }

    public void sendNotification(String type) {
        this.sendNotification(type, null);
    }

    public void sendNotification(String type, Object userData) {
        ObjectName source = getObjectName();
        if (notificationsLogger.isDebugEnabled()) {
            notificationsLogger.debug("[" + type +
                "]#[BodyWrapper.sendNotification] source=" + source +
                ", userData=" + userData);
        }

        Notification notification = new Notification(type, source, counter++);
        notification.setUserData(userData);

        // If the migration is finished, we need to inform the JMXNotificationManager
        if (type.equals(NotificationType.migrationFinished)) {
            sendNotifications();
            notifications.add(notification);
            sendNotifications(NotificationType.migrationMessage);
        } else {
            notifications.add(notification);
        }
    }

    public Collection<BasicTimer> getTimersSnapshot(String[] timerNames) {
        TimerProvidable container = TimerWarehouse.getTimerProvidable(getID());
        if (container == null) {
            throw new NullPointerException(
                "The timers container is null, the body is not timed.");
        }
        return container.getSnapshot(timerNames);
    }

    public void migrateTo(String nodeUrl) throws MigrationException {
        if (!(body instanceof Migratable)) {
            throw new MigrationException("Object cannot Migrate");
        }
        Node node = null;
        try {
            node = NodeFactory.getNode(nodeUrl);
        } catch (NodeException e) {
            throw new MigrationException("Cannot find node " + nodeUrl, e);
        }
        ProActive.migrateTo(body, node, true,
            Request.NFREQUEST_IMMEDIATE_PRIORITY);
    }

    //
    // -- PRIVATE METHODS -----------------------------------------------------
    //

    /**
     * Creates a new thread which sends JMX notifications.
     * A BodyWrapperMBean keeps all the notifications,
     * and the NotificationsThread sends every 'updateFrequence' a list of notifications.
     */
    private void launchNotificationsThread() {
        new Thread() {
                @Override
                public void run() {
                    while (shouldNotify) {
                        try {
                            Thread.sleep(updateFrequence);
                        } catch (InterruptedException e) {
                            logger.error("The JMX notifications sender thread was interrupted",
                                e);
                        }
                        sendNotifications();
                    }
                }
            }.start();
    }

    /**
     * Sends a notification containing all stored notifications.
     */
    private void sendNotifications() {
        this.sendNotifications(null);
    }

    /**
     * Sends a notification containing all stored notifications.
     * @param userMessage The message to send with the set of notifications.
     */
    private void sendNotifications(String userMessage) {
        if (notifications == null) {
            this.notifications = new ConcurrentLinkedQueue<Notification>();
        }
        synchronized (notifications) {
            if (!notifications.isEmpty()) {
                ObjectName source = getObjectName();
                Notification n = new Notification(NotificationType.setOfNotifications,
                        source, counter++, userMessage);
                n.setUserData(notifications);
                super.sendNotification(n);
                notifications.clear();
            }
        }
    }

    //
    // -- SERIALIZATION METHODS -----------------------------------------------
    //
    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug(
                "[Serialisation.writeObject]#Serialization of the MBean :" +
                objectName);
        }

        // Send the notifications before migrates.
        if (!notifications.isEmpty()) {
            sendNotifications();
        }

        // Unregister the MBean from the MBean Server.
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        if (mbs.isRegistered(objectName)) {
            try {
                mbs.unregisterMBean(objectName);
            } catch (InstanceNotFoundException e) {
                logger.error("The objectName " + objectName +
                    " was not found during the serialization of the MBean", e);
            } catch (MBeanRegistrationException e) {
                logger.error("The MBean " + objectName +
                    " can't be unregistered from the MBean server during the serialization of the MBean",
                    e);
            }
        }

        // Default Serialization
        out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        // Warning loggers is transient
        logger = ProActiveLogger.getLogger(Loggers.JMX_MBEAN);
        notificationsLogger = ProActiveLogger.getLogger(Loggers.JMX_NOTIFICATION);

        if ((logger != null) && logger.isDebugEnabled()) {
            logger.debug(
                "[Serialisation.readObject]#Deserialization of the MBean");
        }

        in.defaultReadObject();

        // Warning objectName is transient
        this.objectName = FactoryName.createActiveObjectName(id);

        // Warning nodeUrl is transient
        // We get the url of the new node.
        this.nodeUrl = this.body.getNodeURL();
        logger.debug("BodyWrapper.readObject() nodeUrl=" + nodeUrl);

        // Warning notifications is transient
        if (notifications == null) {
            this.notifications = new ConcurrentLinkedQueue<Notification>();
        }

        // Register the MBean into the MBean Server
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            mbs.registerMBean(this, objectName);
        } catch (InstanceAlreadyExistsException e) {
            logger.error("A Mean is already registered with this objectName " +
                objectName, e);
        } catch (MBeanRegistrationException e) {
            logger.error("The MBean " + objectName +
                " can't be registered on the MBean server during the deserialization of the MBean",
                e);
        } catch (NotCompliantMBeanException e) {
            logger.error("Execption throws during the deserialization of the MBean",
                e);
        }

        launchNotificationsThread();
    }

    /**
     *  returns a list of outgoing active object references.
     */
    public Collection<UniqueID> getReferenceList() {
        return ObjectGraph.getReferenceList(this.getID());
    }

    public String getDgcState() {
        return GarbageCollector.getDgcState(this.getID());
    }
}
