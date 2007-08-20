package org.objectweb.proactive.extra.gcmdeployment.GCMDeployment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import static org.objectweb.proactive.core.mop.Utils.makeDeepCopy;
import org.objectweb.proactive.core.util.OperatingSystem;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.BridgeParsers.BridgeParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.BridgeParsers.BridgeRSHParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.BridgeParsers.BridgeSSHParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers.GroupBSubParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers.GroupGLiteParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers.GroupGlobusParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers.GroupGridEngineParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers.GroupMPIParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers.GroupOARGridParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers.GroupOARParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers.GroupPBSParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers.GroupParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers.GroupRSHParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers.GroupSSHParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeploymentLoggers;
import org.objectweb.proactive.extra.gcmdeployment.GCMParserHelper;
import org.objectweb.proactive.extra.gcmdeployment.process.Bridge;
import org.objectweb.proactive.extra.gcmdeployment.process.CommandBuilder;
import org.objectweb.proactive.extra.gcmdeployment.process.Group;
import org.objectweb.proactive.extra.gcmdeployment.process.HostInfo;
import org.objectweb.proactive.extra.gcmdeployment.process.hostinfo.HostInfoImpl;
import org.objectweb.proactive.extra.gcmdeployment.process.hostinfo.Tool;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class GCMDeploymentParserImpl implements GCMDeploymentParser {
    protected Document document;
    protected DocumentBuilderFactory domFactory;
    protected XPath xpath;
    protected DocumentBuilder documentBuilder;
    protected CommandBuilder commandBuilder;
    protected Map<String, GroupParser> groupParserMap;
    protected Map<String, BridgeParser> bridgeParserMap;
    protected GCMDeploymentInfrastructure infrastructure;
    protected GCMDeploymentEnvironment environment;
    protected GCMDeploymentResources resources;
    private boolean parsedResource = false;
    private boolean parsedInfrastructure = false;
    protected List<String> schemas;

    public GCMDeploymentParserImpl(File descriptor) throws IOException {
        this(descriptor, null);
    }

    public GCMDeploymentParserImpl(File descriptor, List<String> userSchemas)
        throws IOException {
        infrastructure = new GCMDeploymentInfrastructure();
        resources = new GCMDeploymentResources();
        groupParserMap = new HashMap<String, GroupParser>();
        bridgeParserMap = new HashMap<String, BridgeParser>();
        environment = new GCMDeploymentEnvironment();
        schemas = (userSchemas != null) ? new ArrayList<String>(userSchemas)
                                        : new ArrayList<String>();

        setup();
        registerDefaultGroupParsers();
        registerUserGroupParsers();
        registerDefaultBridgeParsers();
        registerUserBridgeParsers();
        InputSource inputSource = new InputSource(new FileInputStream(
                    descriptor));
        try {
            document = documentBuilder.parse(inputSource);
        } catch (SAXException e) {
            GCMDeploymentLoggers.GCMD_LOGGER.fatal(e.getMessage());
        }
    }

    protected void registerDefaultGroupParsers() {
        registerGroupParser(new GroupSSHParser());
        registerGroupParser(new GroupRSHParser());
        registerGroupParser(new GroupOARParser());
        registerGroupParser(new GroupOARGridParser());
        registerGroupParser(new GroupPBSParser());
        registerGroupParser(new GroupBSubParser());
        registerGroupParser(new GroupGLiteParser());
        registerGroupParser(new GroupGlobusParser());
        registerGroupParser(new GroupGridEngineParser());
        registerGroupParser(new GroupMPIParser());
        // TODO add other group parsers here 
    }

    protected void registerDefaultBridgeParsers() {
        registerBridgeParser(new BridgeSSHParser());
        registerBridgeParser(new BridgeRSHParser());
        // TODO add other bridge parsers here 
    }

    /**
     * Override this
     */
    protected void registerUserGroupParsers() {
    }

    /**
     * Override this
     */
    protected void registerUserBridgeParsers() {
    }

    public void setup() throws IOException {
        domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        domFactory.setValidating(true);
        domFactory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);

        String deploymentSchema = getClass()
                                      .getResource(DEPLOYMENT_DESC_LOCATION)
                                      .toString();

        schemas.add(0, deploymentSchema);

        domFactory.setAttribute(JAXP_SCHEMA_SOURCE, schemas.toArray());

        try {
            documentBuilder = domFactory.newDocumentBuilder();
            documentBuilder.setErrorHandler(new GCMParserHelper.MyDefaultHandler());

            XPathFactory factory = XPathFactory.newInstance();
            xpath = factory.newXPath();
            xpath.setNamespaceContext(new GCMParserHelper.ProActiveNamespaceContext(
                    DEPLOYMENT_DESCRIPTOR_NAMESPACE));
        } catch (ParserConfigurationException e) {
            GCMDeploymentLoggers.GCMD_LOGGER.fatal(e.getMessage());
        }
    }

    public void parseEnvironment() throws XPathExpressionException {
        Node environmentNode = (Node) xpath.evaluate("/pa:GCMDeployment/pa:environment",
                document, XPathConstants.NODE);

        NodeList descriptorVarNodes = (NodeList) xpath.evaluate("pa:descriptorVariable",
                environmentNode, XPathConstants.NODESET);

        for (int i = 0; i < descriptorVarNodes.getLength(); ++i) {
            Node descVarNode = descriptorVarNodes.item(i);
            String varName = GCMParserHelper.getAttributeValue(descVarNode,
                    "name");
            String varValue = GCMParserHelper.getAttributeValue(descVarNode,
                    "value");

            environment.addValue(varName, varValue);
        }
    }

    public void parseResources() throws XPathExpressionException, IOException {
        if (parsedResource) {
            throw new IllegalStateException(
                "parseResources can only be called once");
        }

        if (!parsedInfrastructure) {
            parseInfrastructure();
        }

        Node resourcesNode = (Node) xpath.evaluate("/pa:GCMDeployment/pa:resources",
                document, XPathConstants.NODE);

        NodeList childNodes = resourcesNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                parseResourceNode(childNode);
            }
        }

        parsedResource = true;
    }

    protected void parseResourceNode(Node resourceNode)
        throws XPathExpressionException, IOException {
        String refid = GCMParserHelper.getAttributeValue(resourceNode, "refid");

        String nodeName = resourceNode.getNodeName();

        if (nodeName.equals("bridge")) {
            Bridge bridge = getBridge(refid);
            parseBridgeResource(resourceNode, bridge);
            resources.addBridge(bridge);
        } else if (nodeName.equals("group")) {
            Group group = getGroup(refid);
            parseGroupResource(resourceNode, group);
            resources.addGroup(group);
        } else if (nodeName.equals("host")) {
            HostInfo hostInfo = getHostInfo(refid);
            resources.setHostInfo(hostInfo);
        }
    }

    protected HostInfo getHostInfo(String refid) throws IOException {
        HostInfo hostInfo = infrastructure.getHosts().get(refid);
        return (HostInfo) makeDeepCopy(hostInfo);
    }

    protected Group getGroup(String refid) throws IOException {
        Group group = infrastructure.getGroups().get(refid);
        return (Group) makeDeepCopy(group);
    }

    protected Bridge getBridge(String refid) throws IOException {
        Bridge bridge = infrastructure.getBridges().get(refid);
        return (Bridge) makeDeepCopy(bridge);
    }

    protected void parseGroupResource(Node resourceNode, Group group)
        throws XPathExpressionException, IOException {
        Node hostNode = (Node) xpath.evaluate("pa:host", resourceNode,
                XPathConstants.NODE);

        String refid = GCMParserHelper.getAttributeValue(hostNode, "refid");
        HostInfo hostInfo = getHostInfo(refid);
        group.setHostInfo(hostInfo);
    }

    protected void parseBridgeResource(Node resourceNode, Bridge bridge)
        throws IOException, XPathExpressionException {
        NodeList childNodes = resourceNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String childNodeName = childNode.getNodeName();
            String childRefId = GCMParserHelper.getAttributeValue(childNode,
                    "refid");

            if (childNodeName.equals("group")) {
                Group group = getGroup(childRefId);
                parseGroupResource(childNode, group);
                bridge.addGroup(group);
            } else if (childNodeName.equals("host")) {
                HostInfo hostInfo = getHostInfo(childRefId);
                bridge.setHostInfo(hostInfo);
            } else if (childNodeName.equals("bridge")) {
                Bridge childBridge = getBridge(childRefId);
                parseBridgeResource(childNode, childBridge);
                bridge.addBridge(childBridge);
            }
        }
    }

    public void parseInfrastructure() throws XPathExpressionException {
        if (parsedInfrastructure) {
            throw new IllegalStateException(
                "parseInfrastructure can only be called once");
        }

        Node infrastructureNode = (Node) xpath.evaluate("/pa:GCMDeployment/pa:infrastructure",
                document, XPathConstants.NODE);

        //
        // Hosts
        //
        NodeList hosts = (NodeList) xpath.evaluate("pa:hosts/pa:host",
                infrastructureNode, XPathConstants.NODESET);

        for (int i = 0; i < hosts.getLength(); ++i) {
            HostInfo hostInfo = parseHostNode(hosts.item(i));
            infrastructure.addHost(hostInfo);
        }

        //
        // Groups
        //
        NodeList groups = (NodeList) xpath.evaluate("pa:groups/*",
                infrastructureNode, XPathConstants.NODESET);

        for (int i = 0; i < groups.getLength(); ++i) {
            Node groupNode = groups.item(i);
            GroupParser groupParser = groupParserMap.get(groupNode.getNodeName());
            if (groupParser == null) {
                GCMDeploymentLoggers.GCMA_LOGGER.warn(
                    "No group parser registered for node <" +
                    groupNode.getNodeName() + ">");
            } else {
                groupParser.parseGroupNode(groupNode, xpath);
                infrastructure.addGroup(groupParser.getGroup());
            }
        }

        //
        // Bridges
        //
        NodeList bridges = (NodeList) xpath.evaluate("pa:bridges/*",
                infrastructureNode, XPathConstants.NODESET);

        for (int i = 0; i < bridges.getLength(); ++i) {
            Node bridgeNode = bridges.item(i);
            BridgeParser bridgeParser = bridgeParserMap.get(bridgeNode.getNodeName());
            if (bridgeParser == null) {
                GCMDeploymentLoggers.GCMA_LOGGER.warn(
                    "No bridge parser registered for node <" +
                    bridgeNode.getNodeName() + ">");
            } else {
                bridgeParser.parseBridgeNode(bridgeNode, xpath);
                infrastructure.addBrige(bridgeParser.getBridge());
            }
        }

        parsedInfrastructure = true;
    }

    public void registerGroupParser(GroupParser groupParser) {
        if (groupParserMap.containsKey(groupParser.getNodeName())) {
            GCMDeploymentLoggers.GCMD_LOGGER.error("Group parser for '" +
                groupParser.getNodeName() + "' already registered");
        }
        groupParserMap.put(groupParser.getNodeName(), groupParser);
    }

    public void registerBridgeParser(BridgeParser bridgeParser) {
        if (bridgeParserMap.containsKey(bridgeParser.getNodeName())) {
            GCMDeploymentLoggers.GCMD_LOGGER.error("Bridge parser for '" +
                bridgeParser.getNodeName() + "' already registered");
        }
        bridgeParserMap.put(bridgeParser.getNodeName(), bridgeParser);
    }

    protected HostInfo parseHostNode(Node hostNode)
        throws XPathExpressionException {
        HostInfoImpl hostInfo = new HostInfoImpl();

        String id = GCMParserHelper.getAttributeValue(hostNode, "id");
        hostInfo.setId(id);

        String os = GCMParserHelper.getAttributeValue(hostNode, "os");
        if (os.equals("unix")) {
            hostInfo.setOs(OperatingSystem.unix);
        } else if (os.equals("windows")) {
            hostInfo.setOs(OperatingSystem.windows);
        }

        String hostCapacityStr = GCMParserHelper.getAttributeValue(hostNode,
                "hostCapacity");
        if (hostCapacityStr != null) {
            hostInfo.setHostCapacity(Integer.parseInt(hostCapacityStr));
        }

        String vmCapacityStr = GCMParserHelper.getAttributeValue(hostNode,
                "vmCapacity");
        if (vmCapacityStr != null) {
            hostInfo.setVmCapacity(Integer.parseInt(vmCapacityStr));
        }

        String username = GCMParserHelper.getAttributeValue(hostNode, "username");
        if (username != null) {
            hostInfo.setUsername(username);
        }

        Node homeDirectoryNode = (Node) xpath.evaluate("pa:homeDirectory",
                hostNode, XPathConstants.NODE);

        if (homeDirectoryNode != null) {
            hostInfo.setHomeDirectory(homeDirectoryNode.getFirstChild()
                                                       .getNodeValue());
        }

        NodeList toolNodes = (NodeList) xpath.evaluate("pa:tool", hostNode,
                XPathConstants.NODESET);

        for (int i = 0; i < toolNodes.getLength(); ++i) {
            Node toolNode = toolNodes.item(i);
            Tool tool = new Tool(GCMParserHelper.getAttributeValue(toolNode,
                        "id"),
                    GCMParserHelper.getAttributeValue(toolNode, "path"));
            hostInfo.addTool(tool);
        }

        return hostInfo;
    }

    public GCMDeploymentEnvironment getEnvironment() {
        return environment;
    }

    public GCMDeploymentInfrastructure getInfrastructure() {
        if (!parsedInfrastructure) {
            try {
                parseInfrastructure();
            } catch (XPathExpressionException e) {
                GCMDeploymentLoggers.GCMD_LOGGER.fatal(e.getMessage());
            }
        }
        return infrastructure;
    }

    public GCMDeploymentResources getResources() {
        if (!parsedResource) {
            try {
                parseResources();
            } catch (XPathExpressionException e) {
                GCMDeploymentLoggers.GCMD_LOGGER.fatal(e.getMessage());
            } catch (IOException e) {
                GCMDeploymentLoggers.GCMD_LOGGER.fatal(e.getMessage());
            }
        }

        return resources;
    }
}
