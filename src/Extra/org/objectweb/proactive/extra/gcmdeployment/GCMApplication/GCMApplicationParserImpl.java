package org.objectweb.proactive.extra.gcmdeployment.GCMApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.objectweb.proactive.extra.gcmdeployment.GCMApplication.ApplicationParsers.ApplicationParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMApplication.ApplicationParsers.ApplicationParserExecutable;
import org.objectweb.proactive.extra.gcmdeployment.GCMApplication.ApplicationParsers.ApplicationParserProactive;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GCMDeploymentDescriptor;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GCMDeploymentDescriptorFactory;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GCMDeploymentDescriptorParams;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeploymentLoggers;
import org.objectweb.proactive.extra.gcmdeployment.GCMParserConstants;
import org.objectweb.proactive.extra.gcmdeployment.GCMParserHelper;
import org.objectweb.proactive.extra.gcmdeployment.core.VirtualNode;
import org.objectweb.proactive.extra.gcmdeployment.core.VirtualNodeImpl;
import org.objectweb.proactive.extra.gcmdeployment.core.VirtualNodeInternal;
import org.objectweb.proactive.extra.gcmdeployment.process.CommandBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/*
 * FIXME: Improvements needed
 *  - Refactoring & Cleanup
 *         - Put all "magic strings" in a warehouse
 *  - Write some comment to explain how it works
 */
public class GCMApplicationParserImpl implements GCMApplicationParser {
    private static final String XPATH_GCMAPP = "/pa:GCMApplication/";
    private static final String XPATH_VIRTUAL_NODE = XPATH_GCMAPP +
        "pa:application/pa:proactive/pa:virtualNode";
    private static final String XPATH_RESOURCE_PROVIDERS = XPATH_GCMAPP +
        "pa:resources/pa:resourceProvider";
    private static final String XPATH_APPLICATION = XPATH_GCMAPP +
        "pa:application";
    private static final String XPATH_RESOURCE_PROVIDER = "pa:resourceProvider";
    private static final String XPATH_FILETRANSFER = "pa:filetransfer";
    private static final String XPATH_FILE = "pa:file";
    protected File descriptor;
    protected Document document;
    protected DocumentBuilderFactory domFactory;
    protected List<String> schemas;
    protected XPath xpath;
    protected DocumentBuilder documentBuilder;
    protected CommandBuilder commandBuilder;
    protected Map<String, GCMDeploymentDescriptor> resourceProvidersMap;
    protected Map<String, VirtualNodeInternal> virtualNodes;
    protected Map<String, ApplicationParser> applicationParsersMap;

    public GCMApplicationParserImpl(File descriptor) throws IOException {
        this(descriptor, null);
    }

    public GCMApplicationParserImpl(File descriptor, List<String> userSchemas)
        throws IOException {
        this.descriptor = descriptor;
        resourceProvidersMap = null;
        virtualNodes = null;
        schemas = (userSchemas != null) ? new ArrayList<String>(userSchemas)
                                        : new ArrayList<String>();
        applicationParsersMap = new HashMap<String, ApplicationParser>();

        registerDefaultApplicationParsers();
        registerUserApplicationParsers();

        setup();
        InputSource inputSource = new InputSource(new FileInputStream(
                    descriptor));
        try {
            document = documentBuilder.parse(inputSource);
        } catch (SAXException e) {
            GCMDeploymentLoggers.GCMA_LOGGER.fatal(e.getMessage());
        }
    }

    /**
     * override me
     */
    protected void registerUserApplicationParsers() {
    }

    public void registerApplicationParser(ApplicationParser applicationParser) {
        applicationParsersMap.put(applicationParser.getNodeName(),
            applicationParser);
    }

    private void registerDefaultApplicationParsers() {
        registerApplicationParser(new ApplicationParserProactive());
        registerApplicationParser(new ApplicationParserExecutable());
    }

    public void setup() throws IOException {
        domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        domFactory.setValidating(true);
        domFactory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);

        String deploymentSchema = getClass()
                                      .getResource(APPLICATION_DESC_LOCATION)
                                      .toString();

        String commonTypesSchema = getClass()
                                       .getResource(COMMON_TYPES_DESC_LOCATION)
                                       .toString();

        schemas.add(0, deploymentSchema);
        schemas.add(0, commonTypesSchema);

        domFactory.setAttribute(JAXP_SCHEMA_SOURCE, schemas.toArray());

        try {
            documentBuilder = domFactory.newDocumentBuilder();
            documentBuilder.setErrorHandler(new GCMParserHelper.MyDefaultHandler());

            XPathFactory factory = XPathFactory.newInstance();
            xpath = factory.newXPath();
            xpath.setNamespaceContext(new GCMParserHelper.ProActiveNamespaceContext(
                    GCMParserConstants.APPLICATION_DESCRIPTOR_NAMESPACE));
        } catch (ParserConfigurationException e) {
            GCMDeploymentLoggers.GCMA_LOGGER.fatal(e.getMessage());
        }
    }

    synchronized public Map<String, GCMDeploymentDescriptor> getResourceProviders() {
        if (resourceProvidersMap != null) {
            return resourceProvidersMap;
        }

        resourceProvidersMap = new HashMap<String, GCMDeploymentDescriptor>();

        try {
            NodeList nodes;

            nodes = (NodeList) xpath.evaluate(XPATH_RESOURCE_PROVIDERS,
                    document, XPathConstants.NODESET);

            for (int i = 0; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);

                // get Id
                //
                GCMDeploymentDescriptorParams resourceProviderParams = new GCMDeploymentDescriptorParams();
                String id = GCMParserHelper.getAttributeValue(node, "id");
                resourceProviderParams.setId(id);

                // get GCMDescriptor file
                //
                Node fileNode = (Node) xpath.evaluate(XPATH_FILE, node,
                        XPathConstants.NODE);
                if (fileNode != null) {
                    String nodeValue = GCMParserHelper.getAttributeValue(fileNode,
                            "path");

                    // TODO support URL here
                    if (nodeValue.startsWith(File.separator)) {
                        // Absolute path
                        resourceProviderParams.setGCMDescriptor(new File(
                                nodeValue));
                    } else {
                        // Path is relative to this descriptor
                        resourceProviderParams.setGCMDescriptor(new File(
                                descriptor.getParent(), nodeValue));
                    }
                }

                // get fileTransfers
                //
                HashSet<FileTransferBlock> fileTransferBlocks = new HashSet<FileTransferBlock>();
                NodeList fileTransferNodes = (NodeList) xpath.evaluate(XPATH_FILETRANSFER,
                        node, XPathConstants.NODESET);
                for (int j = 0; j < fileTransferNodes.getLength(); ++j) {
                    Node fileTransferNode = fileTransferNodes.item(j);
                    FileTransferBlock fileTransferBlock = GCMParserHelper.parseFileTransferNode(fileTransferNode);
                    fileTransferBlocks.add(fileTransferBlock);
                }

                resourceProvidersMap.put(resourceProviderParams.getId(),
                    GCMDeploymentDescriptorFactory.createDescriptor(
                        resourceProviderParams));
            }
        } catch (XPathExpressionException e) {
            GCMDeploymentLoggers.GCMA_LOGGER.fatal(e.getMessage());
        }

        return resourceProvidersMap;
    }

    public CommandBuilder getCommandBuilder() {
        if (commandBuilder != null) {
            return commandBuilder;
        }

        try {
            Node applicationNode = (Node) xpath.evaluate(XPATH_APPLICATION,
                    document, XPathConstants.NODE);

            NodeList appNodes = applicationNode.getChildNodes();

            for (int i = 0; i < appNodes.getLength(); ++i) {
                Node commandNode = appNodes.item(i);
                if (commandNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                ApplicationParser applicationParser = getApplicationParserForNode(commandNode);
                if (applicationParser == null) {
                    GCMDeploymentLoggers.GCMA_LOGGER.warn(
                        "No application parser registered for node <" +
                        commandNode.getNodeName() + ">");
                } else {
                    applicationParser.parseApplicationNode(commandNode, this,
                        xpath);
                    commandBuilder = applicationParser.getCommandBuilder();
                }
            }
        } catch (XPathExpressionException e) {
            GCMDeploymentLoggers.GCMA_LOGGER.fatal(e.getMessage());
        }

        return commandBuilder;
    }

    private ApplicationParser getApplicationParserForNode(Node commandNode) {
        ApplicationParser applicationParser = applicationParsersMap.get(commandNode.getNodeName());
        return applicationParser;
    }

    synchronized public Map<String, VirtualNodeInternal> getVirtualNodes() {
        if (virtualNodes != null) {
            return virtualNodes;
        }

        try {
            virtualNodes = new HashMap<String, VirtualNodeInternal>();

            // make sure these are parsed
            getResourceProviders();

            NodeList nodes = (NodeList) xpath.evaluate(XPATH_VIRTUAL_NODE,
                    document, XPathConstants.NODESET);

            for (int i = 0; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);

                // get Id
                //
                VirtualNodeImpl virtualNode = new VirtualNodeImpl();

                String id = GCMParserHelper.getAttributeValue(node, "id");
                virtualNode.setId(id);

                // get capacity
                //
                String capacity = GCMParserHelper.getAttributeValue(node,
                        "capacity").trim().toLowerCase();

                long capacityI = 0;
                if (capacity.equals("max")) {
                    capacityI = VirtualNode.MAX_CAPACITY;
                } else {
                    capacityI = Long.parseLong(capacity);
                }
                virtualNode.setRequiredCapacity(capacityI);

                // get resource providers references
                //
                NodeList resourceProviderNodes = (NodeList) xpath.evaluate(XPATH_RESOURCE_PROVIDER,
                        node, XPathConstants.NODESET);
                List<GCMDeploymentDescriptor> providers = new ArrayList<GCMDeploymentDescriptor>();

                for (int j = 0; j < resourceProviderNodes.getLength(); ++j) {
                    Node resProv = resourceProviderNodes.item(j);
                    String refId = GCMParserHelper.getAttributeValue(resProv,
                            "refid");

                    GCMDeploymentDescriptor resourceProvider = resourceProvidersMap.get(refId);
                    providers.add(resourceProvider);
                }

                virtualNode.addProviders(providers);

                virtualNodes.put(virtualNode.getId(), virtualNode);
            }
        } catch (XPathExpressionException e) {
            GCMDeploymentLoggers.GCMA_LOGGER.fatal(e.getMessage(), e);
        }

        return virtualNodes;
    }
}
