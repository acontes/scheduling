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
package org.objectweb.proactive.core;


/**
 * Defines many constants useful across ProActive
 *
 * @author  ProActive Team
 * @version 1.0,  2002/03/21
 * @since   ProActive 0.9
 *
 */
public interface Constants {

    /** The explicit local body default class */
    public static final Class DEFAULT_BODY_CLASS = org.objectweb.proactive.core.body.ActiveBody.class;

    /** The name of the explicit local body default class */
    public static final String DEFAULT_BODY_CLASS_NAME = DEFAULT_BODY_CLASS.getName();

    /** The explicit local body default class */
    public static final Class DEFAULT_BODY_INTERFACE = org.objectweb.proactive.Body.class;

    /** The name of the explicit local body default class */
    public static final String DEFAULT_BODY_INTERFACE_NAME = DEFAULT_BODY_INTERFACE.getName();

    /** The explicit local body default class */
    public static final Class DEFAULT_BODY_PROXY_CLASS = org.objectweb.proactive.core.body.proxy.UniversalBodyProxy.class;

    /** The name of the explicit local body default class */
    public static final String DEFAULT_BODY_PROXY_CLASS_NAME = DEFAULT_BODY_PROXY_CLASS.getName();

    /** The explicit local body default class */
    public static final Class DEFAULT_FUTURE_PROXY_CLASS = org.objectweb.proactive.core.body.future.FutureProxy.class;

    /** The name of the explicit local body default class */
    public static final String DEFAULT_FUTURE_PROXY_CLASS_NAME = DEFAULT_FUTURE_PROXY_CLASS.getName();

    /**
     * The interface implemented by all proxies featuring 'future' semantics,
     * depending on whether they are remoteBodyly-accessible or not
     */
    public static final Class FUTURE_PROXY_INTERFACE = org.objectweb.proactive.core.body.future.Future.class;

    /** rmi protocol identifier */
    public static final String RMI_PROTOCOL_IDENTIFIER = "rmi";

    /** rmi tunneling over ssh protocol identifier */
    public static final String RMISSH_PROTOCOL_IDENTIFIER = "rmissh";

    /** ibis protocol identifier */
    public static final String IBIS_PROTOCOL_IDENTIFIER = "ibis";

    /**xml-http protocol identifier */
    public static final String XMLHTTP_PROTOCOL_IDENTIFIER = "http";

    /** default protocol identifier */
    public static final String DEFAULT_PROTOCOL_IDENTIFIER = RMI_PROTOCOL_IDENTIFIER;

    // list of system properties used within proactive

    /** this property identifies the communication protocol */
    public static final String PROPERTY_PA_COMMUNICATION_PROTOCOL = "proactive.communication.protocol";

    /** this property allows to override the default java behaviour when retrieving the runtime url */
    public static final String PROPERTY_PA_RUNTIME_IPADDRESS = "proactive.runtime.ipaddress";

    /** this property allows to set the hostname of a runtime */
    public static final String PROPERTY_PA_HOSTNAME = "proactive.hostname";

    /** this property indicates to use the ip adress instead of DNS entry for a runtime url */
    public static final String PROPERTY_PA_USE_IP_ADDRESS = "proactive.useIPaddress";

    /** this property identifies the default port used by the RMI transport layer */
    public static final String PROPERTY_PA_RMI_PORT = "proactive.rmi.port";

    /** this property identifies the default port used by the RMISSH transport layer */
    public static final String PROPERTY_PA_RMISSH_PORT = "proactive.ssh.port";

    /** this property identifies the location of the known host file for the RMISSH transport layer */
    public static final String PROPERTY_PA_RMISSH_KNOWN_HOST = "proactive.ssh.known_hosts";

    /** this property identifies the location of RMISSH key directory */
    public static final String PROPERTY_PA_RMISSH_KEY_DIR = "proactive.ssh.key_directory";

    /** this property identifies the default port for the xml-http protocol  */
    public static final String PROPERTY_PA_XMLHTTP_PORT = "proactive.http.port";

    /** this property identifies that when using ssh tunneling, a normal connection should be tried before tunneling */
    public static final String PROPERTY_PA_SSH_TUNNELING_TRY_NORMAL_FIRST = "proactive.tunneling.try_normal_first";

    /** this property identifies if the garbage collector should be turned on when using ssh tunneling */
    public static final String PROPERTY_PA_SSH_TUNNELING_USE_GC = "proactive.tunneling.use_gc";

    /** this property identifies the garbage collector period when using ssh tunneling */
    public static final String PROPERTY_PA_SSH_TUNNELING_GC_PERIOD = "proactive.tunneling.gc_period";

    /** this property identifies the know hosts file location when using ssh tunneling
     *  if undefined, the default value is user.home property concatenated to SSH_TUNNELING_DEFAULT_KNOW_HOSTS
     *  */
    public static final String PROPERTY_PA_SSH_TUNNELING_KNOW_HOSTS = "proactive.ssh.known_hosts";
    public static final String SSH_TUNNELING_DEFAULT_KNOW_HOSTS = "/.ssh/known_hosts";

    /** this property identifies proactive's configuration file */
    public static final String PROPERTY_PA_CONFIGURATION_FILE = "proactive.configuration";

    /** this property identifies the https protocol */
    public static final String HTTPSSH_PROTOCOL_IDENTIFIER = "httpssh";

    /** this property indicates to create for each ProActive object a MBeans */
    public static final String PROPERTY_PA_JMX_MBEAN = "proactive.jmx.mbean";

    /** this property indicates to send JMX notifications */
    public static final String PROPERTY_PA_JMX_NOTIFICATION = "proactive.jmx.notification";

    /** this property indicates to send JMX notifications */
    public static final String PROPERTY_PA_USER_CONFIG_DIR = ".proactive";
}
