package org.objectweb.proactive.extra.gcmdeployment.GCMApplication;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.Executor;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GCMDeploymentDescriptor;
import org.objectweb.proactive.extra.gcmdeployment.Helpers;
import org.objectweb.proactive.extra.gcmdeployment.core.VirtualNode;
import org.objectweb.proactive.extra.gcmdeployment.core.VirtualNodeInternal;
import org.objectweb.proactive.extra.gcmdeployment.process.CommandBuilder;


public class GCMApplicationDescriptorImpl implements GCMApplicationDescriptor {

    /** The descriptor file */
    private File gadFile = null;

    /** A parser dedicated to this GCM Application descriptor */
    private GCMApplicationParser gadParser = null;

    /** All the Virtual Nodes defined in this application */
    private Map<String, VirtualNodeInternal> virtualNodes = null;

    public GCMApplicationDescriptorImpl(String filename)
        throws IllegalArgumentException {
        this(new File(filename));
    }

    public GCMApplicationDescriptorImpl(File file)
        throws IllegalArgumentException {
        gadFile = Helpers.checkDescriptorFileExist(file);
        try {
            gadParser = new GCMApplicationParserImpl(gadFile);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        // 1. Load all GCM Deployment Descriptor
        Map<String, GCMDeploymentDescriptor> gdds;
        gdds = gadParser.getResourceProviders();

        // 2. Get Virtual Node and Command Builder
        virtualNodes = gadParser.getVirtualNodes();

        CommandBuilder commandBuilder = gadParser.getCommandBuilder();

        // 4. Select the GCM Deployment Descriptors to be used
        gdds = selectGCMD(virtualNodes, gdds);

        // 5. Start the deployment
        for (GCMDeploymentDescriptor gdd : gdds.values()) {
            gdd.start(commandBuilder);
        }

        /**
         * If this GCMA describes a distributed application. The Runtime has
         * been started and will populate Virtual Nodes etc. We let the user
         * code, interact with its Middleware.
         *
         * if a "script" is described. The command has been started on each
         * machine/VM/core and we can safely return
         */
    }

    /**
     * Select the GCM Deployment descriptor to be used
     *
     * A Virtual Node is a consumer, and a GCM Deployment Descriptor a producer.
     * We try to fulfill the consumers needs with as few as possible producer.
     *
     * @param vns
     *            Virtual Nodes asking for some resources
     * @param gdds
     *            GCM Deployment Descriptor providing some resources
     * @return A
     */
    static private Map<String, GCMDeploymentDescriptor> selectGCMD(
        Map<String, VirtualNodeInternal> vns,
        Map<String, GCMDeploymentDescriptor> gdds) {
        // TODO: Implement this method
        return gdds;
    }

    private long getRequiredCapacity() {
        int cap = 0;
        for (VirtualNodeInternal vn : virtualNodes.values()) {
            cap += vn.getRequiredCapacity();
        }

        return cap;
    }

    public VirtualNode getVirtualNode(String vnName)
        throws IllegalArgumentException {
        VirtualNode ret = virtualNodes.get(vnName);
        if (ret == null) {
            throw new IllegalArgumentException("Virtual Node " + vnName +
                " does not exist");
        }
        return ret;
    }

    public Map<String, ?extends VirtualNode> getVirtualNodes() {
        return virtualNodes;
    }

    public void kill() {
        // TODO Auto-generated method stub
    }

    @SuppressWarnings("unused")
    static public class TestGCMApplicationDescriptorImpl {
    }

    public boolean allProcessExited() {
        // TODO Auto-generated method stub
        return false;
    }

    public void awaitTermination() {
        try {
            Executor.getExecutor().awaitTermination();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
