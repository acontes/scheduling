package unitTests.deployment.descriptorParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.BridgeParsers.AbstractBridgeParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GCMDeploymentParserImpl;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers.AbstractGroupParser;
import org.objectweb.proactive.extra.gcmdeployment.process.bridge.AbstractBridge;
import org.objectweb.proactive.extra.gcmdeployment.process.group.AbstractGroup;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class TestDeploymentDescriptorParser {
    @Test
    public void test()
        throws IOException, XPathExpressionException, SAXException {
        File descriptor = new File(this.getClass()
                                       .getResource("testfiles/deployment.xml")
                                       .getFile());

        System.out.println("Parsing " + descriptor.getAbsolutePath());
        GCMDeploymentParserImpl parser = new GCMDeploymentParserImpl(descriptor);

        parser.parseEnvironment();
        parser.parseInfrastructure();
        parser.parseResources();
    }

    //
    // Examples of custom group & bridge parsers
    //
    protected static class UserGroup extends AbstractGroup {
        @Override
        public List<String> internalBuildCommands() {
            return new ArrayList<String>();
        }
    }

    protected static class UserBridge extends AbstractBridge {
        @Override
        public String internalBuildCommand() {
            return "";
        }
    }

    protected static class UserGroupParser extends AbstractGroupParser {
        @Override
        public AbstractGroup createGroup() {
            return new UserGroup();
        }

        public String getNodeName() {
            return "paext:myGroup";
        }

        @Override
        public void parseGroupNode(Node groupNode, XPath xpath) {
            super.parseGroupNode(groupNode, xpath);

            System.out.println("User Group Parser - someattr value = " +
                groupNode.getAttributes().getNamedItem("someattr").getNodeValue());
        }
    }

    protected static class UserBridgeParser extends AbstractBridgeParser {
        @Override
        public AbstractBridge createBridge() {
            return new UserBridge();
        }

        public String getNodeName() {
            return "paext:myBridge";
        }

        @Override
        public void parseBridgeNode(Node bridgeNode, XPath xpath) {
            super.parseBridgeNode(bridgeNode, xpath);
            System.out.println("User Bridge Parser - someattr value = " +
                bridgeNode.getAttributes().getNamedItem("someattr")
                          .getNodeValue());
        }
    }

    @Test
    public void userSchemaTest()
        throws IOException, XPathExpressionException, SAXException {
        File descriptor = new File(getClass()
                                       .getResource("testfiles/deployment/group_bridge_ext.xml")
                                       .getFile());

        String userSchema = getClass()
                                .getResource("testfiles/deployment/SampleDeploymentExtension.xsd")
                                .toString();

        ArrayList<String> schemas = new ArrayList<String>();
        schemas.add(userSchema);

        System.out.println("Parsing " + descriptor.getAbsolutePath() +
            " with custom schema " + userSchema);
        GCMDeploymentParserImpl parser = new GCMDeploymentParserImpl(descriptor,
                schemas);

        parser.registerGroupParser(new UserGroupParser());
        parser.registerBridgeParser(new UserBridgeParser());

        parser.parseEnvironment();
        parser.parseInfrastructure();
        parser.parseResources();
    }

    protected void idConstraintTest(String descriptorLocation) {
        File descriptor = new File(this.getClass()
                                       .getResource(descriptorLocation).getFile());

        System.out.println("Parsing " + descriptor.getAbsolutePath());
        boolean gotException = false;

        try {
            GCMDeploymentParserImpl parser = new GCMDeploymentParserImpl(descriptor);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (SAXException e) {
            e.printStackTrace();
            gotException = e.getMessage().contains("Duplicate key value");
        }

        Assert.assertTrue(gotException);
    }

    @Test
    public void hostIdConstraintTest() {
        idConstraintTest("testfiles/deployment/duplicateHostId.xml");
    }

    @Test
    public void groupIdConstraintTest() {
        idConstraintTest("testfiles/deployment/duplicateGroupId.xml");
    }

    @Test
    public void bridgeIdConstraintTest() {
        idConstraintTest("testfiles/deployment/duplicateBridgeId.xml");
    }

    protected void refConstraintTest(String descriptorLocation) {
        File descriptor = new File(this.getClass()
                                       .getResource(descriptorLocation).getFile());

        System.out.println("Parsing " + descriptor.getAbsolutePath());
        boolean gotException = false;

        try {
            GCMDeploymentParserImpl parser = new GCMDeploymentParserImpl(descriptor);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (SAXException e) {
            e.printStackTrace();
            gotException = e.getMessage()
                            .contains("not found for identity constraint");
        }

        Assert.assertTrue(gotException);
    }

    @Test
    public void hostRefIdConstraintTest() {
        refConstraintTest("testfiles/deployment/missingHostId.xml");
    }

    @Test
    public void groupRefIdConstraintTest() {
        refConstraintTest("testfiles/deployment/missingGroupId.xml");
    }

    @Test
    public void groupHostRefIdConstraintTest() {
        refConstraintTest("testfiles/deployment/missingGroupHostId.xml");
    }

    @Test
    public void bridgeRefIdConstraintTest() {
        refConstraintTest("testfiles/deployment/missingBridgeId.xml");
    }
}
