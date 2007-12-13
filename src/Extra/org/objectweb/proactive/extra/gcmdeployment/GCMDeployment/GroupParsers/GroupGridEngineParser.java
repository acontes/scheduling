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

import javax.xml.xpath.XPath;

import org.objectweb.proactive.extra.gcmdeployment.GCMParserHelper;
import org.objectweb.proactive.extra.gcmdeployment.process.group.AbstractGroup;
import org.objectweb.proactive.extra.gcmdeployment.process.group.GroupGridEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class GroupGridEngineParser extends AbstractGroupParser {
    private static final String NODE_NAME_WALL_TIME = NODE_EXT_NAMESPACE + "wallTime";
    private static final String NODE_NAME_PARALLEL_ENVIRONMENT = NODE_EXT_NAMESPACE + "parallelEnvironment";
    private static final String NODE_NAME_HOSTS_NUMBER = NODE_EXT_NAMESPACE + "hostsNumber";
    private static final String NODE_NAME_STDOUT = NODE_EXT_NAMESPACE + "stdout";
    private static final Object NODE_NAME_STDERR = NODE_EXT_NAMESPACE + "stderr";
    private static final Object NODE_NAME_DIRECTORY = NODE_EXT_NAMESPACE + "directory";
    private static final String NODE_NAME = "gridEngineGroup";
    private static final String ATTR_QUEUE = "queue";

    @Override
    public AbstractGroup createGroup() {
        return new GroupGridEngine();
    }

    @Override
    public String getBaseNodeName() {
        return NODE_NAME;
    }

    @Override
    public AbstractGroup parseGroupNode(Node groupNode, XPath xpath) {
        GroupGridEngine gridGroup = (GroupGridEngine) super.parseGroupNode(groupNode, xpath);

        String queueName = GCMParserHelper.getAttributeValue(groupNode, ATTR_QUEUE);
        gridGroup.setQueue(queueName);

        NodeList childNodes = groupNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = childNode.getNodeName();
            String nodeValue = GCMParserHelper.getElementValue(childNode);
            if (nodeName.equals(NODE_NAME_HOSTS_NUMBER)) {
                gridGroup.setHostsNumber(nodeValue);
            } else if (nodeName.equals(NODE_NAME_PARALLEL_ENVIRONMENT)) {
                gridGroup.setParallelEnvironment(nodeValue);
            } else if (nodeName.equals(NODE_NAME_WALL_TIME)) {
                gridGroup.setWallTime(nodeValue);
            } else if (nodeName.equals(NODE_NAME_DIRECTORY)) {
                gridGroup.setDirectory(nodeValue);
            } else if (nodeName.equals(NODE_NAME_STDOUT)) {
                gridGroup.setStdout(nodeValue);
            } else if (nodeName.equals(NODE_NAME_STDERR)) {
                gridGroup.setStderr(nodeValue);
            }
        }

        return gridGroup;
    }
}
