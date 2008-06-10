package functionalTests.gcmdeployment.virtualnode;

import java.io.File;
import java.io.Serializable;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.InitActive;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.util.OperatingSystem;
import org.objectweb.proactive.core.xml.VariableContractImpl;
import org.objectweb.proactive.core.xml.VariableContractType;
import org.objectweb.proactive.extensions.gcmdeployment.PAGCMDeployment;
import org.objectweb.proactive.gcmdeployment.GCMApplication;
import org.objectweb.proactive.gcmdeployment.GCMVirtualNode;

import functionalTests.FunctionalTest;
import functionalTests.GCMFunctionalTestDefaultNodes;


public class TestSubscribeAttachmentFromAO extends FunctionalTest {

    @Test
    public void test() throws ActiveObjectCreationException, NodeException, InterruptedException {
        TestSubscribeAttachmentFromAODeployer ao = (TestSubscribeAttachmentFromAODeployer) PAActiveObject
                .newActive(TestSubscribeAttachmentFromAODeployer.class.getName(), new Object[] {});
        ao.deploy();
        Assert.assertTrue(ao.waitUntilCallbackOccur());
    }

    static public class TestSubscribeAttachmentFromAODeployer implements Serializable, InitActive {
        GCMApplication gcma;
        boolean notified = false;

        public TestSubscribeAttachmentFromAODeployer() {

        }

        public TestSubscribeAttachmentFromAODeployer(GCMApplication gcma) {
            this.gcma = gcma;
        }

        public void initActivity(Body body) {
            PAActiveObject.setImmediateService("callback");
        }

        public void deploy() {
            try {
                File appDesc = new File(this.getClass().getResource("/functionalTests/_CONFIG/JunitApp.xml")
                        .getFile());

                VariableContractImpl vContract = new VariableContractImpl();
                vContract.setVariableFromProgram(GCMFunctionalTestDefaultNodes.VAR_OS, OperatingSystem
                        .getOperatingSystem().name(), VariableContractType.DescriptorDefaultVariable);

                GCMApplication gcma = PAGCMDeployment.loadApplicationDescriptor(appDesc, vContract);

                GCMVirtualNode vn = gcma.getVirtualNode("nodes");
                vn.subscribeNodeAttachment(PAActiveObject.getStubOnThis(), "callback", true);

                gcma.startDeployment();
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("EoInitActivity");
        }

        public void callback(Node node, String vn) {
            System.out.println("Callback called");
            notified = true;
        }

        public boolean waitUntilCallbackOccur() throws InterruptedException {
            while (!notified) {
                System.out.println("!notified");
                Thread.sleep(250);
            }

            return true;
        }

    }
}
