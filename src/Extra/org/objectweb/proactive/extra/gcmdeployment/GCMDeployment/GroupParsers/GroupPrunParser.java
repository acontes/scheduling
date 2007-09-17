package org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.objectweb.proactive.extra.gcmdeployment.GCMDeploymentLoggers;
import org.objectweb.proactive.extra.gcmdeployment.GCMParserHelper;
import org.objectweb.proactive.extra.gcmdeployment.process.group.AbstractGroup;
import org.objectweb.proactive.extra.gcmdeployment.process.group.GroupPrun;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class GroupPrunParser extends AbstractGroupParser {
    private static final String NODE_NAME_OUTPUT_FILE = "outputFile";
    private static final String NODE_NAME_BOOKING_DURATION = "bookingDuration";
    private static final String NODE_NAME_PROCESSOR_PER_NODE = "processorPerNode";
    private static final String NODE_NAME_HOSTS_NUMBER = "hostsNumber";
    private static final String NODE_NAME_HOSTLIST = "hostlist";
    private static final String XPATH_PRUN_OPTION = "prunOption";
    private static final String ATTR_QUEUE = "queue";
    private static final String NODE_NAME = "prunProcess";

    @Override
    public AbstractGroup createGroup() {
        return new GroupPrun();
    }

    public String getNodeName() {
        return NODE_NAME;
    }

    @Override
    public void parseGroupNode(Node groupNode, XPath xpath) {
        super.parseGroupNode(groupNode, xpath);

        GroupPrun prunGroup = (GroupPrun) getGroup();

        String queueName = GCMParserHelper.getAttributeValue(groupNode,
                ATTR_QUEUE);
        prunGroup.setQueueName(queueName);

        try {
            Node optionNode = (Node) xpath.evaluate(XPATH_PRUN_OPTION,
                    groupNode, XPathConstants.NODE);

            NodeList childNodes = optionNode.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); ++i) {
                Node childNode = childNodes.item(i);
                if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                String nodeName = childNode.getNodeName();
                String nodeExpandedValue = GCMParserHelper.getElementValue(childNode);
                if (nodeName.equals(NODE_NAME_HOSTLIST)) {
                    prunGroup.setHostList(nodeExpandedValue);
                } else if (nodeName.equals(NODE_NAME_HOSTS_NUMBER)) {
                    prunGroup.setHostsNumber(nodeExpandedValue);
                } else if (nodeName.equals(NODE_NAME_PROCESSOR_PER_NODE)) {
                    prunGroup.setProcessorPerNodeNumber(nodeExpandedValue);
                } else if (nodeName.equals(NODE_NAME_BOOKING_DURATION)) {
                    prunGroup.setBookingDuration(nodeExpandedValue);
                } else if (nodeName.equals(NODE_NAME_OUTPUT_FILE)) {
                    prunGroup.setOutputFile(nodeExpandedValue);
                }
            }
        } catch (XPathExpressionException e) {
            GCMDeploymentLoggers.GCMD_LOGGER.error(e.getMessage(), e);
        }
    }
}
