package org.objectweb.proactive.extra.gcmdeployment.GCMApplication.ApplicationParsers;

import java.io.IOException;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.objectweb.proactive.extra.gcmdeployment.GCMApplication.GCMApplicationParser;
import org.objectweb.proactive.extra.gcmdeployment.GCMDeploymentLoggers;
import org.objectweb.proactive.extra.gcmdeployment.GCMParserHelper;
import org.objectweb.proactive.extra.gcmdeployment.PathElement;
import org.objectweb.proactive.extra.gcmdeployment.process.CommandBuilder;
import org.objectweb.proactive.extra.gcmdeployment.process.commandbuilder.CommandBuilderProActive;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class ApplicationParserProactive extends AbstractApplicationParser {
    private static final String XPATH_JAVA = "pa:java";
    private static final String XPATH_CONFIGURATION = "pa:configuration";
    private static final String XPATH_PROACTIVE_CLASSPATH = "pa:proactiveClasspath";
    private static final String XPATH_APPLICATION_CLASSPATH = "pa:applicationClasspath";
    private static final String XPATH_SECURITY_POLICY = "pa:securityPolicy";
    private static final String XPATH_LOG4J_PROPERTIES = "pa:log4jProperties";
    private static final String XPATH_USER_PROPERTIES = "pa:userProperties";
    protected static final String NODE_NAME = "proactive";

    @Override
    protected CommandBuilder createCommandBuilder() {
        return new CommandBuilderProActive();
    }

    public String getNodeName() {
        return NODE_NAME;
    }

    @Override
    public void parseApplicationNode(Node paNode,
        GCMApplicationParser applicationParser, XPath xpath)
        throws XPathExpressionException, SAXException, IOException {
        super.parseApplicationNode(paNode, applicationParser, xpath);

        CommandBuilderProActive commandBuilderProActive = (CommandBuilderProActive) commandBuilder;

        String relPath = GCMParserHelper.getAttributeValue(paNode, "relpath");
        commandBuilderProActive.setProActivePath(relPath);

        try {
            Node configNode = (Node) xpath.evaluate(XPATH_CONFIGURATION,
                    paNode, XPathConstants.NODE);

            if (configNode != null) {
                parseProActiveConfiguration(xpath, commandBuilderProActive,
                    configNode);
            }

            commandBuilderProActive.setVirtualNodes(applicationParser.getVirtualNodes());
        } catch (XPathExpressionException e) {
            GCMDeploymentLoggers.GCMA_LOGGER.fatal(e.getMessage(), e);
        }
    }

    protected void parseProActiveConfiguration(XPath xpath,
        CommandBuilderProActive commandBuilderProActive, Node configNode)
        throws XPathExpressionException {
        // Optional: java
        Node javaNode = (Node) xpath.evaluate(XPATH_JAVA, configNode,
                XPathConstants.NODE);
        if (javaNode != null) {
            PathElement pe = GCMParserHelper.parsePathElementNode(javaNode);
            commandBuilderProActive.setJavaPath(pe);
        }

        Node classPathNode;
        // Optional: proactiveClasspath
        classPathNode = (Node) xpath.evaluate(XPATH_PROACTIVE_CLASSPATH,
                configNode, XPathConstants.NODE);
        if (classPathNode != null) {
            String type = GCMParserHelper.getAttributeValue(classPathNode,
                    "type");
            List<PathElement> proactiveClassPath = GCMParserHelper.parseClasspath(xpath,
                    classPathNode);
            commandBuilderProActive.setProActiveClasspath(proactiveClassPath);
            if ("overwrite".equals(type)) {
                commandBuilderProActive.setOverwriteClasspath(true);
            } else {
                commandBuilderProActive.setOverwriteClasspath(false);
            }
        }

        // Optional: applicationClasspath
        classPathNode = (Node) xpath.evaluate(XPATH_APPLICATION_CLASSPATH,
                configNode, XPathConstants.NODE);
        if (classPathNode != null) {
            List<PathElement> applicationClassPath = GCMParserHelper.parseClasspath(xpath,
                    classPathNode);
            commandBuilderProActive.setApplicationClasspath(applicationClassPath);
        }

        // Optional: security policy
        Node securityPolicyNode = (Node) xpath.evaluate(XPATH_SECURITY_POLICY,
                configNode, XPathConstants.NODE);
        if (securityPolicyNode != null) {
            PathElement pathElement = GCMParserHelper.parsePathElementNode(securityPolicyNode);
            commandBuilderProActive.setSecurityPolicy(pathElement);
        }

        // Optional: log4j properties
        Node log4jPropertiesNode = (Node) xpath.evaluate(XPATH_LOG4J_PROPERTIES,
                configNode, XPathConstants.NODE);
        if (log4jPropertiesNode != null) {
            PathElement pathElement = GCMParserHelper.parsePathElementNode(log4jPropertiesNode);
            commandBuilderProActive.setLog4jProperties(pathElement);
        }

        // Optional: user properties
        Node userPropertiesNode = (Node) xpath.evaluate(XPATH_USER_PROPERTIES,
                configNode, XPathConstants.NODE);
        if (userPropertiesNode != null) {
            PathElement pathElement = GCMParserHelper.parsePathElementNode(userPropertiesNode);
            commandBuilderProActive.setUserProperties(pathElement);
        }
    }
}
