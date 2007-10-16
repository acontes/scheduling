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
package unitTests.deployment.descriptorParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.junit.Test;
import org.objectweb.proactive.extra.gcmdeployment.GCMApplication.ApplicationParsers.AbstractApplicationParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMApplication.GCMApplicationDescriptor;
import org.objectweb.proactive.extra.gcmdeployment.GCMApplication.GCMApplicationDescriptorImpl;
import org.objectweb.proactive.extra.gcmdeployment.GCMApplication.GCMApplicationParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMApplication.GCMApplicationParserImpl;
import org.objectweb.proactive.extra.gcmdeployment.process.CommandBuilder;
import org.objectweb.proactive.extra.gcmdeployment.process.commandbuilder.CommandBuilderScript;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class TestApplicationDescriptorParser {
    final static String TEST_APP_DIR = TestApplicationDescriptorParser.class.getClass()
                                                                            .getResource("/unitTests/deployment/descriptorParser/testfiles/application")
                                                                            .getFile();

    //    @Test
    public void test()
        throws IOException, XPathExpressionException, SAXException {
        for (File descriptor : getApplicationDescriptors()) {
            if (descriptor.toString().contains("script_ext")) {
                continue;
            }

            GCMApplicationParserImpl parser = new GCMApplicationParserImpl(descriptor);

            parser.getCommandBuilder();
            parser.getVirtualNodes();
            parser.getResourceProviders();
        }
    }

    /**
     * User application node parser used to demonstrate how to install custom app parsers
     * @author glaurent
     *
     */
    protected static class UserApplicationNodeParser
        extends AbstractApplicationParser {
        @Override
        protected CommandBuilder createCommandBuilder() {
            return new CommandBuilderScript();
        }

        public String getNodeName() {
            return "paext:myapplication";
        }

        @Override
        public void parseApplicationNode(Node paNode,
            GCMApplicationParser applicationParser, XPath xpath)
            throws XPathExpressionException, SAXException, IOException {
            super.parseApplicationNode(paNode, applicationParser, xpath);

            System.out.println("User Application Parser - someattr value = " +
                paNode.getAttributes().getNamedItem("someattr").getNodeValue());
        }
    }

    @Test
    public void userSchemaTest()
        throws IOException, XPathExpressionException, SAXException {
        for (File file : getApplicationDescriptors()) {
            if (!file.toString().contains("script_ext")) {
                continue;
            }
            System.out.println(file);

            String userSchema = getClass()
                                    .getResource("/unitTests/deployment/descriptorParser/testfiles/application/SampleApplicationExtension.xsd")
                                    .toString();

            ArrayList<String> schemas = new ArrayList<String>();
            schemas.add(userSchema);

            GCMApplicationParserImpl parser = new GCMApplicationParserImpl(file,
                    schemas);

            parser.registerApplicationParser(new UserApplicationNodeParser());

            parser.getCommandBuilder();
            parser.getVirtualNodes();
            parser.getResourceProviders();
        }
    }

    //    @Test
    public void doit()
        throws IOException, IllegalArgumentException, SAXException,
            XPathExpressionException {
        for (File file : getApplicationDescriptors()) {
            if (!file.toString().contains("scriptHostname")) {
                continue;
            }
            System.out.println(file);

            GCMApplicationDescriptor gcma = new GCMApplicationDescriptorImpl(file);
            gcma.awaitTermination();
        }
    }

    private List<File> getApplicationDescriptors() {
        List<File> ret = new ArrayList<File>();
        File dir = new File(TEST_APP_DIR);

        for (String file : dir.list()) {
            ret.add(new File(dir, file));
        }
        return ret;
    }
}
