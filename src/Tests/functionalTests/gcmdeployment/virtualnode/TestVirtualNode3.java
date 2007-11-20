package functionalTests.gcmdeployment.virtualnode;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.proactive.extra.gcmdeployment.API;
import org.objectweb.proactive.extra.gcmdeployment.GCMApplication.GCMApplicationDescriptor;
import org.objectweb.proactive.extra.gcmdeployment.core.VirtualNode;
import org.xml.sax.SAXException;


public class TestVirtualNode3 extends Abstract {
    @Test
    public void test()
        throws IllegalArgumentException, XPathExpressionException,
            FileNotFoundException, SAXException, IOException {
        GCMApplicationDescriptor gcma = API.getGCMApplicationDescriptor(getDescriptor());
        waitAllocation();

        VirtualNode vn = gcma.getVirtualNode("vn");
        Assert.assertEquals(1, vn.getNodes().size());
    }
}
