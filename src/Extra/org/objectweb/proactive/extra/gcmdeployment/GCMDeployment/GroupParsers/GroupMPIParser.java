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
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.objectweb.proactive.extra.gcmdeployment.GCMDeploymentLoggers;
import org.objectweb.proactive.extra.gcmdeployment.GCMParserHelper;
import org.objectweb.proactive.extra.gcmdeployment.PathElement;
import org.objectweb.proactive.extra.gcmdeployment.process.group.AbstractGroup;
import org.objectweb.proactive.extra.gcmdeployment.process.group.GroupMPI;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Not used
 * @author glaurent
 *
 */
public class GroupMPIParser extends AbstractGroupParser {
    @Override
    public AbstractGroup createGroup() {
        return new GroupMPI();
    }

    public String getNodeName() {
        return "mpiGroup";
    }

    @Override
    public void parseGroupNode(Node groupNode, XPath xpath) {
        super.parseGroupNode(groupNode, xpath);

        GroupMPI mpiGroup = (GroupMPI) getGroup();

        String mpiFileName = GCMParserHelper.getAttributeValue(groupNode,
                "mpiFileName");
        mpiGroup.setMpiFileName(mpiFileName);

        String hostsFileName = GCMParserHelper.getAttributeValue(groupNode,
                "hostsFileName");
        mpiGroup.setHostsFileName(hostsFileName);

        String mpiCommandOptions = GCMParserHelper.getAttributeValue(groupNode,
                "mpiCommandOptions");
        mpiGroup.setMpiCommandOptions(mpiCommandOptions);

        try {
            Node optionNode = (Node) xpath.evaluate("mpiOptions", groupNode,
                    XPathConstants.NODE);
            NodeList childNodes = optionNode.getChildNodes();

            for (int i = 0; i < childNodes.getLength(); ++i) {
                Node childNode = childNodes.item(i);
                if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                String nodeName = childNode.getNodeName();

                if (nodeName.equals("localRelativePath")) {
                    PathElement path = GCMParserHelper.parsePathElementNode(childNode);
                    mpiGroup.setLocalPath(path);
                } else if (nodeName.equals("remoteAbsolutePath")) {
                    PathElement path = GCMParserHelper.parsePathElementNode(childNode);
                    mpiGroup.setRemotePath(path);
                } else if (nodeName.equals("processNumber")) {
                    String nodeExpandedValue = GCMParserHelper.getElementValue(childNode);
                    mpiGroup.setHostsNumber(nodeExpandedValue);
                }
            }
        } catch (XPathExpressionException e) {
            GCMDeploymentLoggers.GCMD_LOGGER.error(e.getMessage(), e);
        }
    }
}
