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
package org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment;

import static org.objectweb.proactive.core.mop.Utils.makeDeepCopy;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.objectweb.proactive.core.util.OperatingSystem;
import org.objectweb.proactive.core.xml.VariableContractImpl;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeploymentLoggers;
import org.objectweb.proactive.extensions.gcmdeployment.GCMParserHelper;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.acquisition.P2PEntry;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.bridge.AbstractBridge;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.bridge.Bridge;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.bridge.BridgeOARSHParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.bridge.BridgeParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.bridge.BridgeRSHParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.bridge.BridgeSSHParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.AbstractGroup;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.Group;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupARCParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupCCSParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupCGSPParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupFuraParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupGLiteParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupGlobusParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupGridBusParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupGridEngineParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupLSFParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupLoadLevelerParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupOARParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupOARSHParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupPBSParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupPrunParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupRSHParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.group.GroupSSHParser;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.hostinfo.HostInfo;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.hostinfo.HostInfoImpl;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeployment.hostinfo.Tool;
import org.objectweb.proactive.extensions.gcmdeployment.environment.Environment;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * Parser for a GCM Deployment descriptor Sample use :
 * 
 * <pre>
 * File descriptor = new File(&quot;descriptor.xml&quot;);
 * GCMDeploymentParserImpl parser = new GCMDeploymentParserImpl(descriptor);
 * parser.parseEnvironment();
 * parser.parseInfrastructure();
 * parser.parseResources();
 * </pre>
 * 
 * It is also possible to register your own custom group/bridge parsers
 * 
 * @author The ProActive Team
 * 
 */
public class GCMDeploymentParserImpl implements GCMDeploymentParser {
    private static final String PA_HOST = "host";
    private static final String PA_GROUP = "group";
    private static final String PA_BRIDGE = "bridge";
    private static final String PA_LOOKUP = "lookup";
    private static final String PA_P2P = "p2p";
    private static final String PA_NODE_ASKED = "nodesAsked";
    private static final String PA_LOCAL_CLIENT = "localClient";
    private static final String PA_PEER_SET = "peerSet";
    private static final String PA_PEER = "peer";
    private static final String PA_PROTOCOL = "protocol";
    private static final String PA_PORT = "port";
    private static final String XPATH_GCMDEPLOYMENT = "/dep:GCMDeployment/";
    private static final String XPATH_ACQUISITION = XPATH_GCMDEPLOYMENT + "dep:acquisition";
    private static final String XPATH_INFRASTRUCTURE = XPATH_GCMDEPLOYMENT + "dep:infrastructure";
    private static final String XPATH_RESOURCES = XPATH_GCMDEPLOYMENT + "dep:resources";
    private static final String XPATH_TOOL = "dep:tool";
    private static final String XPATH_HOME_DIRECTORY = "dep:homeDirectory";
    private static final String XPATH_NETWORK_INTERFACE = "dep:networkInterface";
    private static final String XPATH_BRIDGES = "dep:bridges/*";
    private static final String XPATH_GROUPS = "dep:groups/*";
    private static final String XPATH_HOSTS = "dep:hosts/dep:host";
    private static final String XPATH_HOST = "dep:host";
    private static final String XPATH_DESCRIPTOR_VARIABLE = "dep:descriptorVariable";

    protected DocumentBuilderFactory domFactory;
    protected XPath xpath;
    protected Document document;

    protected List<String> schemas;
    protected Map<String, GroupParser> groupParserMap;
    protected Map<String, BridgeParser> bridgeParserMap;
    protected GCMDeploymentInfrastructure infrastructure;

    // protected GCMDeploymentEnvironment environment;
    protected GCMDeploymentResources resources;
    private VariableContractImpl variableContract;
    private GCMDeploymentAcquisition acquisitions;
    private boolean parsedAcquisitions = false;
    private boolean parsedResource = false;
    private boolean parsedInfrastructure = false;
    private URL descriptor;

    public GCMDeploymentParserImpl(URL descriptor, VariableContractImpl vContract) throws IOException,
            SAXException, XPathExpressionException, TransformerException, ParserConfigurationException {
        this(descriptor, vContract, null);
    }

    public GCMDeploymentParserImpl(URL descriptor, VariableContractImpl vContract, List<String> userSchemas)
            throws RuntimeException, SAXException, IOException, TransformerException,
            XPathExpressionException, ParserConfigurationException {
        this.descriptor = descriptor;
        this.infrastructure = new GCMDeploymentInfrastructure();
        this.resources = new GCMDeploymentResources();
        this.acquisitions = new GCMDeploymentAcquisition();
        this.groupParserMap = new HashMap<String, GroupParser>();
        this.bridgeParserMap = new HashMap<String, BridgeParser>();
        this.variableContract = new VariableContractImpl();
        this.schemas = (userSchemas != null) ? new ArrayList<String>(userSchemas) : new ArrayList<String>();

        setupJAXP();

        registerDefaultGroupParsers();
        registerUserGroupParsers();
        registerDefaultBridgeParsers();
        registerUserBridgeParsers();
        try {
            InputSource processedInputSource = Environment.replaceVariables(descriptor, vContract, xpath,
                    GCM_DEPLOYMENT_NAMESPACE_PREFIX);

            // we need to create a new DocumentBuilder before each parsing,
            // otherwise the schemas set in setupJAXP() through
            // JAXP_SCHEMA_SOURCE
            // are ignored, and validation fails
            //
            DocumentBuilder documentBuilder = GCMParserHelper.getNewDocumentBuilder(domFactory);

            document = documentBuilder.parse(processedInputSource);

        } catch (SAXException e) {
            String msg = "parsing problem with document " + descriptor.toExternalForm();
            GCMDeploymentLoggers.GCMD_LOGGER.fatal(msg + " - " + e.getMessage());
            throw new SAXException(msg, e);
        } catch (XPathExpressionException e) {
            GCMDeploymentLoggers.GCMD_LOGGER.fatal(e);
            throw e;
        } catch (TransformerException e) {
            String msg = "problem when evaluating variables with document " + descriptor.toExternalForm();
            GCMDeploymentLoggers.GCMD_LOGGER.fatal(msg + " - " + e.getMessage());
            throw new TransformerException(msg, e);
        }
    }

    /**
     * Register all pre-installed group parsers
     */
    protected void registerDefaultGroupParsers() {
        registerGroupParser(new GroupARCParser());
        registerGroupParser(new GroupCCSParser());
        registerGroupParser(new GroupCGSPParser());
        registerGroupParser(new GroupFuraParser());
        registerGroupParser(new GroupGLiteParser());
        registerGroupParser(new GroupGlobusParser());
        registerGroupParser(new GroupGridBusParser());
        registerGroupParser(new GroupGridEngineParser());
        registerGroupParser(new GroupLSFParser());
        registerGroupParser(new GroupLoadLevelerParser());
        registerGroupParser(new GroupOARParser());
        registerGroupParser(new GroupOARSHParser());
        registerGroupParser(new GroupPBSParser());
        registerGroupParser(new GroupPrunParser());
        registerGroupParser(new GroupRSHParser());
        registerGroupParser(new GroupSSHParser());
        // TODO add other group parsers here
    }

    /**
     * Register all pre-installed bridge parsers
     */
    protected void registerDefaultBridgeParsers() {
        registerBridgeParser(new BridgeSSHParser());
        registerBridgeParser(new BridgeRSHParser());
        registerBridgeParser(new BridgeOARSHParser());
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

    /**
     * setup xml parser (inserting schemas, setting up xpath query engine)
     * 
     * @throws IOException
     * @throws SAXException
     */
    protected void setupJAXP() throws IOException, SAXException {
        // System.setProperty("jaxp.debug", "1");
        // System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
        // "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");

        domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        domFactory.setIgnoringComments(true);

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        // Must use URLs here so schemas can be fetched from jars
        URL extensionSchemaURL = GCMDeploymentParserImpl.class.getClass().getResource(
                EXTENSION_SCHEMAS_LOCATION);

        schemas.add(0, extensionSchemaURL.toString());

        Source[] schemaSources = new Source[schemas.size()];

        int idx = 0;
        for (String s : schemas) {
            schemaSources[idx++] = new StreamSource(s);
        }

        Schema extensionSchema = schemaFactory.newSchema(schemaSources);

        domFactory.setSchema(extensionSchema);

        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();
        xpath.setNamespaceContext(new GCMParserHelper.ProActiveNamespaceContext());
    }

    /**
     * Parse the &lt; acquisition&gt; node
     *
     * @throws XPathExpressionException
     * @throws IOException
     */

    public void parseAcquisition() throws XPathExpressionException, IOException {
        if (parsedAcquisitions) {
            throw new IllegalStateException("parseAcquisition can only be called once");
        }

        Node acquisitionNode = (Node) xpath.evaluate(XPATH_ACQUISITION, document, XPathConstants.NODE);

        if (acquisitionNode != null) {

            NodeList childNodes = acquisitionNode.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    parseAcquisitionNode(childNode);
                }
            }

            parsedAcquisitions = true;

        }
    }

    /**
     * Parse the &lt; aquisition&gt; node
     * 
     * @throws XPathExpressionException
     * @throws IOException
     */
    private void parseAcquisitionNode(Node acquisitionNode) throws XPathExpressionException, IOException {
        if (acquisitionNode.getNodeName().equals(PA_P2P)) {
            parseP2PNode(acquisitionNode);
        } else if (acquisitionNode.getNodeName().equals(PA_LOOKUP)) {
            // parseLookupNode();
        }
    }

    private void parseP2PNode(Node acquisitionNode) {
        P2PEntry newEntry = new P2PEntry();

        Node attr = acquisitionNode.getAttributes().getNamedItem(PA_NODE_ASKED);

        if (attr != null && attr.getNodeType() == Node.ATTRIBUTE_NODE) {
            Attr attr2 = (Attr) attr;
            newEntry.setNodesToAsk(Integer.parseInt(attr2.getValue()));
        }

        NodeList childNodes = acquisitionNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node chNode = childNodes.item(i);
            if (chNode.getNodeType() == Node.ELEMENT_NODE) {
                if (chNode.getNodeName().equals(PA_LOCAL_CLIENT)) {
                    parseLocalClientNode(chNode, newEntry);
                } else if (chNode.getNodeName().equals(PA_PEER_SET)) {
                    parsePeerSetNode(chNode, newEntry);
                }
            }
        }

        this.acquisitions.getP2PEntries().add(newEntry);
    }

    private void parseLocalClientNode(Node localClientNode, P2PEntry p2pEntry) {
        NamedNodeMap attrs = localClientNode.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr) attrs.item(i);
            if (attr.getName().equals(PA_PROTOCOL)) {
                p2pEntry.getLocalClient().setProtocol(attr.getValue());
            } else if (attr.getName().equals(PA_PORT)) {
                p2pEntry.getLocalClient().setPort(Integer.parseInt(attr.getValue()));
            }
        }
    }

    private void parsePeerSetNode(Node peerSetNode, P2PEntry p2pEntry) {
        NodeList childNodes = peerSetNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node chNode = childNodes.item(i);
            if (chNode.getNodeType() == Node.ELEMENT_NODE) {
                if (chNode.getNodeName().equals(PA_PEER)) {
                    p2pEntry.getHostsList().add(chNode.getTextContent());
                }
            }
        }
    }

    /**
     * Parse the &lt;resources&gt; node
     * 
     * @throws XPathExpressionException
     * @throws IOException
     */
    public void parseResources() throws XPathExpressionException, IOException {
        // TODO - do a no-op and log a warning if called twice, rather than
        // throwing ?
        if (parsedResource) {
            throw new IllegalStateException("parseResources can only be called once");
        }

        if (!parsedInfrastructure) {
            parseInfrastructure();
        }

        Node resourcesNode = (Node) xpath.evaluate(XPATH_RESOURCES, document, XPathConstants.NODE);

        NodeList childNodes = resourcesNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                parseResourceNode(childNode);
            }
        }

        parsedResource = true;
    }

    /**
     * Parse a single child node from the &lt;resources&gt; node
     * 
     * @param resourceNode
     * @throws XPathExpressionException
     * @throws IOException
     */
    protected void parseResourceNode(Node resourceNode) throws XPathExpressionException, IOException {
        String refid = GCMParserHelper.getAttributeValue(resourceNode, "refid");

        String nodeName = resourceNode.getNodeName();

        if (nodeName.equals(PA_BRIDGE)) {
            Bridge bridge = getBridge(refid);
            if (bridge == null) {
                throw new RuntimeException("no bridge with refid " + refid + " has been defined");
            }
            parseBridgeResource(resourceNode, bridge);
            resources.addBridge(bridge);
        } else if (nodeName.equals(PA_GROUP)) {
            Group group = getGroup(refid);
            if (group == null) {
                throw new RuntimeException("no group with refid " + refid + " has been defined");
            }
            parseGroupResource(resourceNode, group);
            resources.addGroup(group);
        } else if (nodeName.equals(PA_HOST)) {
            HostInfo hostInfo = getHostInfo(refid);
            if (hostInfo == null) {
                throw new RuntimeException("no host with refid " + refid + " has been defined");
            }

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

    /**
     * Parse a &lt;group&gt; child node of a &lt;resources&gt; node :
     * 
     * <pre>
     *  
     * &lt;resources&gt;
     *   &lt;group&gt;
     *   &lt;/group&gt;
     * &lt;/resources&gt;
     * </pre>
     * 
     * @param resourceNode
     * @param group
     * @throws XPathExpressionException
     * @throws IOException
     */
    protected void parseGroupResource(Node resourceNode, Group group) throws XPathExpressionException,
            IOException {
        Node hostNode = (Node) xpath.evaluate(XPATH_HOST, resourceNode, XPathConstants.NODE);

        String refid = GCMParserHelper.getAttributeValue(hostNode, "refid");

        HostInfo hostInfo = getHostInfo(refid);
        group.setHostInfo(hostInfo);
    }

    /**
     * Parse a &lt;bridge&gt; child node of a &lt;resources&gt; node :
     * 
     * <pre>
     *  
     * &lt;resources&gt;
     *   &lt;bridge&gt;
     *   &lt;/bridge&gt;
     * &lt;/resources&gt;
     * </pre>
     * 
     * @param resourceNode
     * @param bridge
     * @throws XPathExpressionException
     * @throws IOException
     */
    protected void parseBridgeResource(Node resourceNode, Bridge bridge) throws IOException,
            XPathExpressionException {
        NodeList childNodes = resourceNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String childNodeName = childNode.getNodeName();
            String childRefId = GCMParserHelper.getAttributeValue(childNode, "refid");

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

    /**
     * Parse the &lt;infrastructure&gt; node and build the
     * {@link #infrastructure} member
     * 
     * @throws XPathExpressionException
     */
    public void parseInfrastructure() throws XPathExpressionException {
        // TODO - do a no-op and log a warning if called twice, rather than
        // throwing ?
        if (parsedInfrastructure) {
            throw new IllegalStateException("parseInfrastructure can only be called once");
        }

        Node infrastructureNode = (Node) xpath.evaluate(XPATH_INFRASTRUCTURE, document, XPathConstants.NODE);

        //
        // Hosts
        //
        NodeList hosts = (NodeList) xpath.evaluate(XPATH_HOSTS, infrastructureNode, XPathConstants.NODESET);

        for (int i = 0; i < hosts.getLength(); ++i) {
            HostInfo hostInfo = parseHostNode(hosts.item(i));
            infrastructure.addHost(hostInfo);
        }

        //
        // Groups
        //
        NodeList groups = (NodeList) xpath.evaluate(XPATH_GROUPS, infrastructureNode, XPathConstants.NODESET);

        for (int i = 0; i < groups.getLength(); ++i) {
            Node groupNode = groups.item(i);
            GroupParser groupParser = groupParserMap.get(groupNode.getNodeName());
            if (groupParser == null) {
                GCMDeploymentLoggers.GCMD_LOGGER.warn("No group parser registered for node <" +
                    groupNode.getNodeName() + ">");
            } else {
                AbstractGroup group = groupParser.parseGroupNode(groupNode, xpath);
                infrastructure.addGroup(group);
            }
        }

        //
        // Bridges
        //
        NodeList bridges = (NodeList) xpath.evaluate(XPATH_BRIDGES, infrastructureNode,
                XPathConstants.NODESET);

        for (int i = 0; i < bridges.getLength(); ++i) {
            Node bridgeNode = bridges.item(i);
            BridgeParser bridgeParser = bridgeParserMap.get(bridgeNode.getNodeName());
            if (bridgeParser == null) {
                GCMDeploymentLoggers.GCMD_LOGGER.warn("No bridge parser registered for node <" +
                    bridgeNode.getNodeName() + ">");
            } else {
                AbstractBridge bridge = bridgeParser.parseBridgeNode(bridgeNode, xpath);
                infrastructure.addBrige(bridge);
            }
        }

        parsedInfrastructure = true;
    }

    /**
     * GroupParser registration A GroupParser must be registered to be taken
     * into account when parsing a descriptor.
     * 
     * @param groupParser
     */
    public void registerGroupParser(GroupParser groupParser) {
        if (groupParserMap.containsKey(groupParser.getNodeName())) {
            GCMDeploymentLoggers.GCMD_LOGGER.error("Group parser for '" + groupParser.getNodeName() +
                "' already registered");
        }
        groupParserMap.put(groupParser.getNodeName(), groupParser);
    }

    /**
     * BridgeParser registration A BridgeParser must be registered to be taken
     * into account when parsing a descriptor.
     * 
     * @param bridgeParser
     */
    public void registerBridgeParser(BridgeParser bridgeParser) {
        if (bridgeParserMap.containsKey(bridgeParser.getNodeName())) {
            GCMDeploymentLoggers.GCMD_LOGGER.error("Bridge parser for '" + bridgeParser.getNodeName() +
                "' already registered");
        }
        bridgeParserMap.put(bridgeParser.getNodeName(), bridgeParser);
    }

    /**
     * Parse a &lt;host&gt; node
     * 
     * @param hostNode
     * @return
     * @throws XPathExpressionException
     */
    protected HostInfo parseHostNode(Node hostNode) throws XPathExpressionException {
        HostInfoImpl hostInfo = new HostInfoImpl();

        String id = GCMParserHelper.getAttributeValue(hostNode, "id");
        hostInfo.setId(id);

        String os = GCMParserHelper.getAttributeValue(hostNode, "os");
        if (os.equals("unix") || os.equals("cygwin")) {
            hostInfo.setOs(OperatingSystem.unix);
        } else if (os.equals("windows")) {
            hostInfo.setOs(OperatingSystem.windows);
        }

        String hostCapacityStr = GCMParserHelper.getAttributeValue(hostNode, "hostCapacity");
        if (hostCapacityStr != null) {
            hostInfo.setHostCapacity(Integer.parseInt(hostCapacityStr));

            // If host capacity is specified then VM capacity must be specified
            // too
            String vmCapacityStr = GCMParserHelper.getAttributeValue(hostNode, "vmCapacity");
            // FIXME: Check that both Host Capacity and VM Capacity are set
            hostInfo.setVmCapacity(Integer.parseInt(vmCapacityStr));
        }

        String username = GCMParserHelper.getAttributeValue(hostNode, "username");
        if (username != null) {
            hostInfo.setUsername(username);
        }

        Node homeDirectoryNode = (Node) xpath.evaluate(XPATH_HOME_DIRECTORY, hostNode, XPathConstants.NODE);
        if (homeDirectoryNode != null) {
            hostInfo.setHomeDirectory(GCMParserHelper.getAttributeValue(homeDirectoryNode, "relpath"));
        }

        Node networkInterfaceNode = (Node) xpath.evaluate(XPATH_NETWORK_INTERFACE, hostNode,
                XPathConstants.NODE);
        if (networkInterfaceNode != null) {
            hostInfo.setNetworkInterface(GCMParserHelper.getAttributeValue(networkInterfaceNode, "name"));
        }

        NodeList toolNodes = (NodeList) xpath.evaluate(XPATH_TOOL, hostNode, XPathConstants.NODESET);
        for (int i = 0; i < toolNodes.getLength(); ++i) {
            Node toolNode = toolNodes.item(i);
            Tool tool = new Tool(GCMParserHelper.getAttributeValue(toolNode, "id"), GCMParserHelper
                    .getAttributeValue(toolNode, "path"));
            hostInfo.addTool(tool);
        }

        return hostInfo;
    }

    /**
     * Returns the set of variables/values (if any) specified in the
     * &lt;environment&gt; node of a descriptor
     * 
     * @return the descriptor's VariableContract
     */
    public VariableContractImpl getEnvironment() {
        return variableContract;
    }

    /**
     * Returns the infrastructure of the descriptor (parses it if needed)
     * 
     * @return the infrastructure of the descriptor
     */
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

    /**
     * Returns the resources of the descriptor (parses them if needed)
     * 
     * @return the resources of the descriptor
     */
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

    /**
     * Returns the acquisitions defined in descriptor (parses them if needed) * *
     * 
     * @return the acquisitions of the descriptor
     */
    public GCMDeploymentAcquisition getAcquisitions() {
        if (!parsedAcquisitions) {
            try {
                parseAcquisition();
            } catch (XPathExpressionException e) {
                GCMDeploymentLoggers.GCMD_LOGGER.fatal(e.getMessage());
            } catch (IOException e) {
                GCMDeploymentLoggers.GCMD_LOGGER.fatal(e.getMessage());
            }
        }
        return acquisitions;
    }

    public URL getDescriptorURL() {
        return descriptor;
    }
}
