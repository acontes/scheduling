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
package org.objectweb.proactive.extra.gcmdeployment.GCMDeployment.GroupParsers;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.objectweb.proactive.extra.gcmdeployment.GCMDeploymentLoggers;
import org.objectweb.proactive.extra.gcmdeployment.GCMParserHelper;
import org.objectweb.proactive.extra.gcmdeployment.process.group.AbstractGroup;
import org.objectweb.proactive.extra.gcmdeployment.process.group.GroupARC;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class GroupARCParser extends AbstractGroupParser {
    private static final String XPATH_TRANSFER = NODE_EXT_NAMESPACE +
        "transfer";
    private static final String NODE_NAME_NOTIFY = NODE_EXT_NAMESPACE +
        "notify";
    private static final String NODE_NAME_MAX_TIME = NODE_EXT_NAMESPACE +
        "maxTime";
    private static final String NODE_NAME_STDIN = NODE_EXT_NAMESPACE + "stdin";
    private static final String NODE_NAME_STDERR = NODE_EXT_NAMESPACE +
        "stderr";
    private static final String NODE_NAME_STDOUT = NODE_EXT_NAMESPACE +
        "stdout";
    private static final String NODE_NAME_OUTPUT_FILES = NODE_EXT_NAMESPACE +
        "outputFiles";
    private static final String NODE_NAME_INPUT_FILES = NODE_EXT_NAMESPACE +
        "inputFiles";
    private static final String NODE_NAME_ARGUMENTS = NODE_EXT_NAMESPACE +
        "arguments";
    private static final String NODE_NAME_COUNT = NODE_EXT_NAMESPACE + "count";
    private static final String NODE_NAME = "arcGroup";
    private static final String ATTR_JOB_NAME = "jobName";

    @Override
    public AbstractGroup createGroup() {
        return new GroupARC();
    }

    public String getBaseNodeName() {
        return NODE_NAME;
    }

    @Override
    public AbstractGroup parseGroupNode(Node groupNode, XPath xpath) {
        GroupARC arcGroup = (GroupARC) super.parseGroupNode(groupNode, xpath);

        try {
            String t = GCMParserHelper.getAttributeValue(groupNode,
                    ATTR_JOB_NAME);
            arcGroup.setJobName(t);

            NodeList childNodes = groupNode.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); ++j) {
                Node child = childNodes.item(j);
                if (child.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                String nodeName = child.getNodeName();
                String nodeValue = GCMParserHelper.getElementValue(child);

                if (nodeName.equals(NODE_NAME_COUNT)) {
                    arcGroup.setCount(nodeValue);
                } else if (nodeName.equals(NODE_NAME_ARGUMENTS)) {
                    List<String> args = GCMParserHelper.parseArgumentListNode(xpath,
                            child);
                    arcGroup.setArguments(args);
                } else if (nodeName.equals(NODE_NAME_INPUT_FILES)) {
                    List<FileTransfer> inputFilesList = parseFileTransferList(child,
                            xpath);
                    arcGroup.setInputFiles(inputFilesList);
                } else if (nodeName.equals(NODE_NAME_OUTPUT_FILES)) {
                    List<FileTransfer> outputFilesList = parseFileTransferList(child,
                            xpath);
                    arcGroup.setOutputFiles(outputFilesList);
                } else if (nodeName.equals(NODE_NAME_STDOUT)) {
                    arcGroup.setStdout(nodeValue);
                } else if (nodeName.equals(NODE_NAME_STDERR)) {
                    arcGroup.setStderr(nodeValue);
                } else if (nodeName.equals(NODE_NAME_STDIN)) {
                    arcGroup.setStdin(nodeValue);
                } else if (nodeName.equals(NODE_NAME_MAX_TIME)) {
                    arcGroup.setMaxTime(nodeValue);
                } else if (nodeName.equals(NODE_NAME_NOTIFY)) {
                    arcGroup.setNotify(nodeValue);
                }
            }
        } catch (XPathExpressionException e) {
            GCMDeploymentLoggers.GCMD_LOGGER.fatal(e.getMessage());
        }

        return arcGroup;
    }

    public class FileTransfer {
        private static final String ATTR_LOCATION = "location";
        private static final String ATTR_FILENAME = "filename";
        public String filename;
        public String location;

        FileTransfer(Node fileTransferNode) {
            filename = GCMParserHelper.getAttributeValue(fileTransferNode,
                    ATTR_FILENAME);
            location = GCMParserHelper.getAttributeValue(fileTransferNode,
                    ATTR_LOCATION);
        }
    }

    protected List<FileTransfer> parseFileTransferList(
        Node fileTransferListNode, XPath xpath) throws XPathExpressionException {
        List<FileTransfer> res = new ArrayList<FileTransfer>();

        NodeList ftNodes = (NodeList) xpath.evaluate(XPATH_TRANSFER,
                fileTransferListNode, XPathConstants.NODESET);

        for (int i = 0; i < ftNodes.getLength(); ++i) {
            Node ftNode = ftNodes.item(i);
            FileTransfer ft = new FileTransfer(ftNode);
            res.add(ft);
        }
        return res;
    }
}
