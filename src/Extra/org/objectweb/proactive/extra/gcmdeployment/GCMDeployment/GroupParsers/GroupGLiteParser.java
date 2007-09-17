package org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers;

import java.util.StringTokenizer;

import javax.xml.xpath.XPath;

import org.objectweb.proactive.extra.gcmdeployment.GCMParserHelper;
import org.objectweb.proactive.extra.gcmdeployment.PathElement;
import org.objectweb.proactive.extra.gcmdeployment.process.group.AbstractGroup;
import org.objectweb.proactive.extra.gcmdeployment.process.group.GroupGLite;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class GroupGLiteParser extends AbstractGroupParser {
    private static final String NODE_NAME = "gliteGroup";
    private static final String NODE_NAME_ARGUMENTS = "arguments";
    private static final String NODE_NAME_OUTPUT_SANDBOX = "outputSandbox";
    private static final String NODE_NAME_INPUT_SANDBOX = "inputSandbox";
    private static final String NODE_NAME_CONFIG_FILE = "configFile";
    private static final String NODE_NAME_JDL_REMOTE_FILE_PATH = "JDLRemoteFilePath";
    private static final String NODE_NAME_JDL_FILE_PATH = "JDLFilePath";
    private static final String ATTR_STORAGE_INDEX = "storageIndex";
    private static final String ATTR_DATA_ACCESS_PROTOCOL = "dataAccessProtocol";
    private static final String NODE_NAME_INPUT_DATA = "inputData";
    private static final String NODE_NAME_RANK = "rank";
    private static final String NODE_NAME_REQUIREMENTS = "requirements";
    private static final String NODE_NAME_ENVIRONMENT = "environment";
    private static final String ATTR_NODE_NUMBER = "nodeNumber";
    private static final String ATTR_MY_PROXY_SERVER = "myProxyServer";
    private static final String ATTR_RETRY_COUNT = "retryCount";
    private static final String ATTR_VIRTUAL_ORGANISATION = "virtualOrganisation";
    private static final String ATTR_OUTPUT_SE = "outputse";
    private static final String ATTR_STD_ERROR = "stdError";
    private static final String ATTR_STD_INPUT = "stdInput";
    private static final String ATTR_STD_OUTPUT = "stdOutput";
    private static final String ATTR_EXECUTABLE = "executable";
    private static final String ATTR_HOSTNAME = "hostname";
    private static final String ATTR_JDL_FILE_NAME = "JDLFileName";
    private static final String ATTR_JOB_TYPE = "jobType";
    private static final String ATTR_TYPE = "Type";

    @Override
    public AbstractGroup createGroup() {
        return new GroupGLite();
    }

    public String getNodeName() {
        return NODE_NAME;
    }

    @Override
    public void parseGroupNode(Node groupNode, XPath xpath) {
        super.parseGroupNode(groupNode, xpath);

        GroupGLite gliteGroup = (GroupGLite) getGroup();

        String t = GCMParserHelper.getAttributeValue(groupNode, ATTR_TYPE);
        gliteGroup.setJobType(t);

        t = GCMParserHelper.getAttributeValue(groupNode, ATTR_JOB_TYPE);
        gliteGroup.setJobJobType(t);

        t = GCMParserHelper.getAttributeValue(groupNode, ATTR_JDL_FILE_NAME);
        gliteGroup.setFileName(t);

        t = GCMParserHelper.getAttributeValue(groupNode, ATTR_HOSTNAME);
        gliteGroup.setNetServer(t);

        t = GCMParserHelper.getAttributeValue(groupNode, ATTR_EXECUTABLE);
        gliteGroup.setJobExecutable(t);
        gliteGroup.setCommandPath(t);

        t = GCMParserHelper.getAttributeValue(groupNode, ATTR_STD_OUTPUT);
        gliteGroup.setJobStdOutput(t);

        t = GCMParserHelper.getAttributeValue(groupNode, ATTR_STD_INPUT);
        gliteGroup.setJobStdInput(t);

        t = GCMParserHelper.getAttributeValue(groupNode, ATTR_STD_ERROR);
        gliteGroup.setJobStdError(t);

        t = GCMParserHelper.getAttributeValue(groupNode, ATTR_OUTPUT_SE);
        gliteGroup.setJobOutputStorageElement(t);

        t = GCMParserHelper.getAttributeValue(groupNode,
                ATTR_VIRTUAL_ORGANISATION);
        gliteGroup.setJobVO(t);

        t = GCMParserHelper.getAttributeValue(groupNode, ATTR_RETRY_COUNT);
        gliteGroup.setJobRetryCount(t);

        t = GCMParserHelper.getAttributeValue(groupNode, ATTR_MY_PROXY_SERVER);
        gliteGroup.setJobMyProxyServer(t);

        t = GCMParserHelper.getAttributeValue(groupNode, ATTR_NODE_NUMBER);
        gliteGroup.setJobNodeNumber(Integer.parseInt(t));

        NodeList childNodes = groupNode.getChildNodes();
        for (int j = 0; j < childNodes.getLength(); ++j) {
            Node child = childNodes.item(j);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = child.getNodeName();
            String nodeValue = GCMParserHelper.getElementValue(child);
            if (nodeName.equals(NODE_NAME_ENVIRONMENT)) {
                gliteGroup.setJobEnvironment(nodeValue);
            } else if (nodeName.equals(NODE_NAME_REQUIREMENTS)) {
                gliteGroup.setJobRequirements(nodeValue);
            } else if (nodeName.equals(NODE_NAME_RANK)) {
                gliteGroup.setJobRank(nodeValue);
            } else if (nodeName.equals(NODE_NAME_INPUT_DATA)) {
                t = GCMParserHelper.getAttributeValue(child,
                        ATTR_DATA_ACCESS_PROTOCOL);
                gliteGroup.setJobDataAccessProtocol(t);

                t = GCMParserHelper.getAttributeValue(child, ATTR_STORAGE_INDEX);
                gliteGroup.setJobStorageIndex(t);
            } else if (nodeName.equals("gLiteOptions")) {
                NodeList optionChildNodes = child.getChildNodes();
                for (int i = 0; i < optionChildNodes.getLength(); ++i) {
                    Node optionChild = optionChildNodes.item(i);
                    if (optionChild.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    nodeName = optionChild.getNodeName();
                    if (nodeName.equals(NODE_NAME_JDL_FILE_PATH)) {
                        PathElement path = GCMParserHelper.parsePathElementNode(optionChild);
                        gliteGroup.setFilePath(path);
                    } else if (nodeName.equals(NODE_NAME_JDL_REMOTE_FILE_PATH)) {
                        PathElement path = GCMParserHelper.parsePathElementNode(optionChild);
                        gliteGroup.setRemoteFilePath(path);
                        gliteGroup.setJdlRemote(true);
                    } else if (nodeName.equals(NODE_NAME_CONFIG_FILE)) {
                        PathElement path = GCMParserHelper.parsePathElementNode(optionChild);
                        gliteGroup.setConfigFile(path);
                        gliteGroup.setConfigFileOption(true);
                    } else {
                        nodeValue = GCMParserHelper.getElementValue(optionChild);
                        if (nodeName.equals(NODE_NAME_INPUT_SANDBOX)) {
                            String sandbox = nodeValue;
                            StringTokenizer st = new StringTokenizer(sandbox);
                            while (st.hasMoreTokens()) {
                                gliteGroup.addInputSBEntry(st.nextToken());
                            }
                        } else if (nodeName.equals(NODE_NAME_OUTPUT_SANDBOX)) {
                            String sandbox = nodeValue;
                            StringTokenizer st = new StringTokenizer(sandbox);
                            while (st.hasMoreTokens()) {
                                gliteGroup.addOutputSBEntry(st.nextToken());
                            }
                        } else if (nodeName.equals(NODE_NAME_ARGUMENTS)) {
                            gliteGroup.setJobArgument(nodeValue);
                        }
                    }
                }
            }
        }
    }
}
