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
package org.objectweb.proactive.extensions.gcmdeployment.GCMApplication.commandbuilder;

import static org.objectweb.proactive.extensions.gcmdeployment.GCMDeploymentLoggers.GCMD_LOGGER;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.config.PAProperties;
import org.objectweb.proactive.core.runtime.RuntimeFactory;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeploymentLoggers;
import org.objectweb.proactive.extensions.gcmdeployment.PathElement;
import org.objectweb.proactive.extensions.gcmdeployment.GCMApplication.GCMApplicationInternal;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.hostinfo.HostInfo;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.hostinfo.Tool;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.hostinfo.Tools;
import org.objectweb.proactive.extensions.gcmdeployment.PathElement.PathBase;
import org.objectweb.proactive.extensions.gcmdeployment.core.GCMVirtualNodeInternal;
import org.objectweb.proactive.extensions.gcmdeployment.core.StartRuntime;


public class CommandBuilderProActive implements CommandBuilder {

    final static String PROACTIVE_JAR = "ProActive.jar";

    /** Path to the ProActive installation */
    private PathElement proActivePath;

    /** Declared Virtual nodes */
    private Map<String, GCMVirtualNodeInternal> vns;

    /** Path to ${java.home}/bin/java */
    private PathElement javaPath = null;

    /** Arguments to be passed to java */
    private List<String> jvmArgs;

    /**
     * ProActive classpath
     * 
     * If not set, then the default classpath is used
     */
    private List<PathElement> proactiveClasspath;
    private boolean overwriteClasspath;

    /** Application classpath */
    private List<PathElement> applicationClasspath;

    /** Security Policy file */
    private PathElement securityPolicy;

    /** Log4j configuration file */
    private PathElement log4jProperties;

    /** User properties file */
    private PathElement userProperties;

    /** application security policy file */
    private PathElement applicationPolicy;

    /** runtime security policy file */
    private PathElement runtimePolicy;

    public CommandBuilderProActive() {
        GCMD_LOGGER.trace(this.getClass().getSimpleName() + " created");
        vns = new HashMap<String, GCMVirtualNodeInternal>();
        jvmArgs = new ArrayList<String>();
    }

    public void addVirtualNode(GCMVirtualNodeInternal vn) {
        addVirtualNode(vn.getName(), vn);
    }

    public void addVirtualNode(String id, GCMVirtualNodeInternal vn) {
        vns.put(id, vn);
    }

    public void addProActivePath(PathElement pe) {
        if (proactiveClasspath == null) {
            proactiveClasspath = new ArrayList<PathElement>();
        }

        proactiveClasspath.add(pe);
    }

    public void setProActiveClasspath(List<PathElement> pe) {
        proactiveClasspath = pe;
    }

    public void addApplicationPath(PathElement pe) {
        if (applicationClasspath == null) {
            applicationClasspath = new ArrayList<PathElement>();
        }

        applicationClasspath.add(pe);
    }

    public void setApplicationClasspath(List<PathElement> pe) {
        if (GCMD_LOGGER.isTraceEnabled()) {
            GCMD_LOGGER.trace(" Set ApplicationClasspath to:");
            for (PathElement e : pe) {
                GCMD_LOGGER.trace("\t" + e);
            }
        }

        applicationClasspath = pe;
    }

    public void setVirtualNodes(Map<String, GCMVirtualNodeInternal> vns) {
        if (GCMD_LOGGER.isTraceEnabled()) {
            GCMD_LOGGER.trace(" Set VirtualNodes to:");
            for (String vn : vns.keySet()) {
                GCMD_LOGGER.trace("\t" + vn);
            }
        }

        this.vns = vns;
    }

    public void addJVMArg(String arg) {
        if (arg != null) {
            GCMD_LOGGER.trace(" Added " + arg + " to JavaArgs");
            jvmArgs.add(arg);
        }
    }

    public void setJavaPath(PathElement pe) {
        if (pe != null) {
            GCMD_LOGGER.trace(" Set JavaPath to " + pe);
            javaPath = pe;
        }
    }

    public void setLog4jProperties(PathElement pe) {
        if (pe != null) {
            GCMD_LOGGER.trace(" Set log4jProperties relpath to " + pe.getRelPath());
            log4jProperties = pe;
        }
    }

    public void setSecurityPolicy(PathElement pe) {
        if (pe != null) {
            GCMD_LOGGER.trace(" Set securityPolicy relpath to " + pe.getRelPath());
            securityPolicy = pe;
        }
    }

    public void setApplicationPolicy(PathElement pe) {
        if (pe != null) {
            GCMD_LOGGER.trace(" Set applicationPolicy relpath to " + pe.getRelPath());
            applicationPolicy = pe;
        }
    }

    public void setRuntimePolicy(PathElement pe) {
        if (pe != null) {
            GCMD_LOGGER.trace(" Set runtimePolicy relpath to " + pe.getRelPath());
            runtimePolicy = pe;
        }
    }

    public PathElement getUserProperties() {
        return userProperties;
    }

    public void setUserProperties(PathElement userProperties) {
        if (userProperties != null) {
            GCMD_LOGGER.trace(" Set userProperties relpath to " + userProperties.getRelPath());
            this.userProperties = userProperties;
        }
    }

    /**
     * Returns the java executable to be used
     * 
     * <ol>
     * <li> Uses the java element inside GCMA/proactive/config </li>
     * <li> Uses the java tool defined by the hostInfo </li>
     * <li> returns "java" and lets the $PATH magic occur </li>
     * 
     * @param hostInfo
     * @return the java command to be used for this host
     */
    private String getJava(HostInfo hostInfo) {
        String javaCommand = "java";

        if (javaPath != null) {
            javaCommand = javaPath.getFullPath(hostInfo, this);
        } else {
            Tool javaTool = hostInfo.getTool(Tools.JAVA.id);
            if (javaTool != null) {
                javaCommand = javaTool.getPath();
            }
        }
        return javaCommand;
    }

    /**
     * 
     * ProActive then Application
     * 
     * @param hostInfo
     * @param withCP
     *            whether to include "-cp" in the return string or not
     * @return
     */
    public String getClasspath(HostInfo hostInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("-cp \"");

        if (!overwriteClasspath) {
            // ProActive.jar contains a JAR index
            // see: http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#JAR%20Index
            char fs = hostInfo.getOS().fileSeparator();
            sb.append(getPath(hostInfo));
            sb.append(fs);
            sb.append("dist");
            sb.append(fs);
            sb.append("lib");
            sb.append(fs);
            sb.append(PROACTIVE_JAR);
            sb.append(hostInfo.getOS().pathSeparator());
        }

        if (proactiveClasspath != null) {
            for (PathElement pe : proactiveClasspath) {
                sb.append(pe.getFullPath(hostInfo, this));
                sb.append(hostInfo.getOS().pathSeparator());
            }
        }

        if (applicationClasspath != null) {
            for (PathElement pe : applicationClasspath) {
                sb.append(pe.getFullPath(hostInfo, this));
                sb.append(hostInfo.getOS().pathSeparator());
            }
        }

        // Trailing pathSeparator don't forget to remove it later
        return sb.substring(0, sb.length() - 1) + "\"";
    }

    public String buildCommand(HostInfo hostInfo, GCMApplicationInternal gcma) {
        if ((proActivePath == null) && (hostInfo.getTool(Tools.PROACTIVE.id) == null)) {
            throw new IllegalStateException(
                "ProActive installation path must be specified with the relpath attribute inside the proactive element (GCMA), or as tool in all hostInfo elements (GCMD). HostInfo=" +
                    hostInfo.getId());
        }

        if (!hostInfo.isCapacitiyValid()) {
            throw new IllegalStateException(
                "To enable capacity autodetection nor VM Capacity nor Host Capacity must be specified. HostInfo=" +
                    hostInfo.getId());
        }

        StringBuilder command = new StringBuilder();
        // Java
        command.append(getJava(hostInfo));
        command.append(" ");

        for (String arg : jvmArgs) {
            command.append(arg);
            command.append(" ");
        }

        if (PAProperties.PA_TEST.isTrue()) {
            command.append(PAProperties.PA_TEST.getCmdLine());
            command.append("true ");
        }

        if (PAProperties.PA_CLASSLOADER.isTrue() ||
            "org.objectweb.proactive.core.classloader.ProActiveClassLoader".equals(System
                    .getProperty("java.system.class.loader"))) {
            command
                    .append(" -Djava.system.class.loader=org.objectweb.proactive.core.classloader.ProActiveClassLoader ");
            // the following allows the deserializing of streams that were annotated with rmi utilities
            command
                    .append(" -Djava.rmi.server.RMIClassLoaderSpi=org.objectweb.proactive.core.classloader.ProActiveRMIClassLoaderSpi");
            // to avoid clashes due to multiple classloader, we initiate the
            // configuration of log4j ourselves 
            // (see StartRuntime.main)
            command.append(" -Dlog4j.defaultInitOverride=true ");
        }

        // Class Path: ProActive then Application
        command.append(getClasspath(hostInfo));
        command.append(" ");

        // Log4j
        if (log4jProperties != null) {
            command.append(PAProperties.LOG4J.getCmdLine());
            command.append("\"");
            command.append("file:");
            command.append(log4jProperties.getFullPath(hostInfo, this));
            command.append("\"");
            command.append(" ");
        }

        // Security Policy
        if (securityPolicy != null) {
            command.append(PAProperties.SECURITY_POLICY.getCmdLine());
            command.append("\"");
            command.append(securityPolicy.getFullPath(hostInfo, this));
            command.append("\"");
            command.append(" ");
        } else {
            command.append(PAProperties.SECURITY_POLICY.getCmdLine());
            command.append("\"");
            command.append(PAProperties.SECURITY_POLICY.getValue());
            command.append("\"");
            command.append(" ");
        }

        if (hostInfo.getNetworkInterface() != null) {
            command.append(PAProperties.PA_NET_INTERFACE.getCmdLine() + hostInfo.getNetworkInterface());
            command.append(" ");
        }

        // Class to be started and its arguments
        command.append(StartRuntime.class.getName());
        command.append(" ");

        String parentURL;
        try {
            parentURL = RuntimeFactory.getDefaultRuntime().getURL();
        } catch (ProActiveException e) {
            GCMD_LOGGER.error(
                    "Cannot determine the URL of this runtime. Childs will not be able to register", e);
            parentURL = "unkownParentURL";
        }
        command.append("-" + StartRuntime.Params.parent.shortOpt() + " " + parentURL);
        command.append(" ");

        if (hostInfo.getVmCapacity() != 0) {
            command.append("-" + StartRuntime.Params.capacity.shortOpt() + " " + hostInfo.getVmCapacity());
            command.append(" ");
        }

        command.append("-" + StartRuntime.Params.topologyId.shortOpt() + " " + hostInfo.getToplogyId());
        command.append(" ");

        command.append("-" + StartRuntime.Params.deploymentId.shortOpt() + " " + gcma.getDeploymentId());
        command.append(" ");

        // TODO cdelbe Check FT properties here
        // was this.ftService.buildParamsLine();

        StringBuilder ret = new StringBuilder();

        if (hostInfo.getHostCapacity() == 0) {
            ret.append(command);
        } else {
            for (int i = 0; i < hostInfo.getHostCapacity(); i++) {
                ret.append(command);
                ret.append(" &");
            }
            ret.deleteCharAt(ret.length() - 1);
        }

        GCMD_LOGGER.trace(ret);
        return ret.toString();
    }

    private PathElement getDefaultSecurityPolicy() {
        // TODO Return the default PathElement for Security Policy
        return null;
    }

    private List<PathElement> getDefaultProActiveClassPath() {
        // TODO Return the default PathElements for ProActive ClassPath
        return null;
    }

    private PathElement getDefaultLog4jProperties() {
        // TODO Return the default PathElemen for log4jProperties
        return null;
    }

    public void setProActivePath(String proActivePath) {
        setProActivePath(proActivePath, PathBase.HOME.name());
    }

    public void setProActivePath(String proActivePath, String base) {
        if (proActivePath != null) {
            this.proActivePath = new PathElement(proActivePath, base);
            GCMD_LOGGER.trace(" Set ProActive relpath to " + this.proActivePath.getRelPath());
        }
    }

    public String getPath(HostInfo hostInfo) {
        if (proActivePath != null) {
            return proActivePath.getFullPath(hostInfo, this);
        }

        return null;
    }

    private static String getAbsolutePath(String path) {
        if (path.startsWith("file:")) {
            //remove file part to build absolute path
            path = path.substring(5);
        }
        try {
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            GCMDeploymentLoggers.GCMA_LOGGER.error(e.getMessage());
            return path;
        }
    }

    public void setOverwriteClasspath(boolean overwriteClasspath) {
        this.overwriteClasspath = overwriteClasspath;
    }

}
