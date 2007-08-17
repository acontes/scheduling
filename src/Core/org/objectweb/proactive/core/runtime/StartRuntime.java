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
package org.objectweb.proactive.core.runtime;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.objectweb.proactive.core.Constants;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.config.ProActiveConfiguration;
import org.objectweb.proactive.core.util.UrlBuilder;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;


/**
 * <i><font size="-1" color="#FF0000">**For internal use only** </font></i><br>
 * <p>
 * This class is a utility class allowing to start a ProActiveRuntime with a JVM.
 * </p><p>
 * This class is mainly used with ProActiveDescriptor to start a ProActiveRuntime
 * on a local or remote JVM.
 * </p>
 *
 * @author  ProActive Team
 * @version 1.0,  2002/08/29
 * @since   ProActive 0.9
 *
 */
public class StartRuntime {
    static Logger logger = ProActiveLogger.getLogger(Loggers.RUNTIME);

    /** The VirtualNode that started this ProActive Runtime */
    protected String creatorID;

    /** The URL of the parent ProActive Runtime */
    protected String defaultRuntimeURL;

    /** The protocol to be used by this Runtime */
    protected String protocolId;

    /** Name of the associated VirtualMachine */
    protected String vmName;

    protected StartRuntime() {
    }

    private StartRuntime(String[] args) {
        if (args.length != 0) {
            this.creatorID = args[0].trim();
            this.defaultRuntimeURL = UrlBuilder.removeUsername(args[1]);
            this.protocolId = args[2];
            this.vmName = args[3];
        }
    }

    public static void main(String[] args) {
        if ("true".equals(System.getProperty("log4j.defaultInitOverride")) &&
                (System.getProperty("log4j.configuration") != null)) {
            // configure log4j here to avoid classloading problems with log4j classes
            try {
                String log4jConfiguration = System.getProperty(
                        "log4j.configuration");
                File f = new File(log4jConfiguration);
                PropertyConfigurator.configure(new URL(f.getPath()));
            } catch (IOException e) {
                System.out.println(
                    "Error : incorrect path for log4j configuration : " +
                    System.getProperty("log4j.configuration"));
            }
        }

        ProActiveConfiguration.load();

        try {
            logger.info("**** Starting jvm on " +
                UrlBuilder.getHostNameorIP(java.net.InetAddress.getLocalHost()));

            if (logger.isDebugEnabled()) {
                logger.debug("**** Starting jvm with classpath " +
                    System.getProperty("java.class.path"));
                logger.debug("****              with bootclasspath " +
                    System.getProperty("sun.boot.class.path"));
            }
        } catch (java.net.UnknownHostException e) {
            e.printStackTrace();
        }

        new StartRuntime(args).run();
        if (System.getProperty("proactive.runtime.stayalive") != null) {
            Object o = new Object();
            synchronized (o) {
                try {
                    o.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * <i><font size="-1" color="#FF0000">**For internal use only** </font></i>
     * Runs the complete creation and registration of a ProActiveRuntime and creates a
     * node once the creation is completed.
     */
    private void run() {
        ProActiveRuntimeImpl impl = (ProActiveRuntimeImpl) ProActiveRuntimeImpl.getProActiveRuntime();
        impl.getVMInformation().setCreationProtocolID(this.protocolId);

        if (this.defaultRuntimeURL != null) {
            ProActiveRuntime PART;
            try {
                PART = RuntimeFactory.getRuntime(this.defaultRuntimeURL,
                        UrlBuilder.getProtocol(this.defaultRuntimeURL));
                register(PART);
                impl.setParent(PART);

                Object o = new Object();
                synchronized (o) {
                    try {
                        o.wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } catch (ProActiveException e) {
                e.printStackTrace();
                //				 if we cannot get the parent, this jvm is useless
                System.exit(0);
            }
        }
    }

    /**
     * <i><font size="-1" color="#FF0000">**For internal use only** </font></i>
     * Performs the registration of a ProActiveRuntime on the runtime that initiated the creation
     * of ProActiveDescriptor.
     */
    private void register(ProActiveRuntime PART) {
        try {
            ProActiveRuntime proActiveRuntime = RuntimeFactory.getProtocolSpecificRuntime(ProActiveConfiguration.getInstance()
                                                                                                                .getProperty(Constants.PROPERTY_PA_COMMUNICATION_PROTOCOL));

            PART.register(proActiveRuntime, proActiveRuntime.getURL(),
                this.creatorID,
                ProActiveConfiguration.getInstance()
                                      .getProperty(Constants.PROPERTY_PA_COMMUNICATION_PROTOCOL),
                this.vmName);
        } catch (ProActiveException e) {
            e.printStackTrace();

            // if we cannot register, this jvm is useless
            System.exit(0);
        }
    }
}
