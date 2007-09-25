package org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.objectweb.proactive.extra.gcmdeployment.GCMDeploymentLoggers;
import org.objectweb.proactive.extra.gcmdeployment.GCMParserHelper;
import org.objectweb.proactive.extra.gcmdeployment.PathElement;
import org.objectweb.proactive.extra.gcmdeployment.process.group.AbstractGroup;
import org.objectweb.proactive.extra.gcmdeployment.process.group.GroupOAR;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class GroupOARParser extends AbstractGroupParser {
    private static final String NODE_NAME_SCRIPT_PATH = "scriptPath";
    private static final String NODE_NAME_RESOURCES = "resources";
    private static final String NODE_NAME_DIRECTORY = "directory";
    private static final String NODE_NAME_STDOUT = "stdout";
    private static final String NODE_NAME_STDERR = "stderr";
    private static final String ATTR_BOOKED_NODES_ACCESS = "bookedNodesAccess";
    private static final String ATTR_QUEUE = "queue";
    private static final String ATTR_INTERACTIVE = "interactive";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_RESOURCES_NODES = "nodes";
    private static final String ATTR_RESOURCES_CPU = "nodes";
    private static final String ATTR_RESOURCES_CORE = "nodes";
    private static final String NODE_NAME = "oarProcess";

    @Override
    public AbstractGroup createGroup() {
        return new GroupOAR();
    }

    public String getNodeName() {
        return NODE_NAME;
    }

    @Override
    public void parseGroupNode(Node groupNode, XPath xpath) {
        super.parseGroupNode(groupNode, xpath);

        GroupOAR oarGroup = (GroupOAR) getGroup();

        String interactive = GCMParserHelper.getAttributeValue(groupNode,
                ATTR_INTERACTIVE);

        if (interactive != null) {
            oarGroup.setInteractive(interactive);
        }

        String queueName = GCMParserHelper.getAttributeValue(groupNode,
                ATTR_QUEUE);

        if (queueName != null) {
            oarGroup.setQueueName(queueName);
        }

        String accessProtocol = GCMParserHelper.getAttributeValue(groupNode,
                ATTR_BOOKED_NODES_ACCESS);

        if (accessProtocol != null) {
            oarGroup.setAccessProtocol(accessProtocol);
        }

        String type = GCMParserHelper.getAttributeValue(groupNode, ATTR_TYPE);
        if (type != null) {
            oarGroup.setType(type);
        }

        //
        // Parse child nodes
        //
        NodeList childNodes = groupNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = childNode.getNodeName();
            if (nodeName.equals(NODE_NAME_RESOURCES)) {
                String elementValue = GCMParserHelper.getElementValue(childNode);
                if (elementValue != null) {
                    oarGroup.setResources(elementValue);
                } else {
                    String nodes = GCMParserHelper.getAttributeValue(childNode,
                            ATTR_RESOURCES_NODES);
                    if (nodes != null) {
                        oarGroup.setNodes(nodes);
                    }
                    String cpu = GCMParserHelper.getAttributeValue(childNode,
                            ATTR_RESOURCES_CPU);
                    if (cpu != null) {
                        oarGroup.setCpu(cpu);
                    }
                    String core = GCMParserHelper.getAttributeValue(childNode,
                            ATTR_RESOURCES_CORE);
                    if (core != null) {
                        oarGroup.setCore(core);
                    }
                }
            } else if (nodeName.equals(NODE_NAME_SCRIPT_PATH)) {
                PathElement path = GCMParserHelper.parsePathElementNode(childNode);
                oarGroup.setScriptLocation(path);
            } else if (nodeName.equals(NODE_NAME_DIRECTORY)) {
                PathElement path = GCMParserHelper.parsePathElementNode(childNode);
                oarGroup.setDirectory(path);
            } else if (nodeName.equals(NODE_NAME_STDOUT)) {
                PathElement path = GCMParserHelper.parsePathElementNode(childNode);
                oarGroup.setStdOutFile(path);
            } else if (nodeName.equals(NODE_NAME_STDERR)) {
                PathElement path = GCMParserHelper.parsePathElementNode(childNode);
                oarGroup.setStdErrFile(path);
            }
        }
    }
}
