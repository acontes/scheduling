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
package functionalTests.descriptor.variablecontract.javapropertiesProgram;

import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.objectweb.proactive.core.descriptor.legacyparser.ProActiveDescriptorConstants;
import org.objectweb.proactive.core.xml.VariableContractImpl;
import org.objectweb.proactive.core.xml.VariableContractType;
import org.objectweb.proactive.extensions.gcmdeployment.PAGCMDeployment;
import org.objectweb.proactive.gcmdeployment.GCMApplication;

import functionalTests.FunctionalTest;


/**
 * Tests conditions for variables of type JavaPropertiesProgram
 */
public class Test extends FunctionalTest {
    private static String XML_LOCATION = Test.class.getResource(
            "/functionalTests/descriptor/variablecontract/javapropertiesProgram/Test.xml").getPath();
    GCMApplication gcma;
    boolean bogusFromProgram;
    boolean bogusFromDescriptor;

    @Before
    public void initTest() throws Exception {
        bogusFromDescriptor = true;
        bogusFromProgram = true;
    }

    @org.junit.Test
    public void action() throws Exception {
        VariableContractImpl variableContract = new VariableContractImpl();

        //Setting from Program
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("user.home", "/home/userprogram");
        variableContract.setVariableFromProgram(map, VariableContractType
                .getType(ProActiveDescriptorConstants.VARIABLES_JAVAPROPERTY_PROGRAM_TAG));
        assertTrue(variableContract.getValue("user.home").equals(System.getProperty("user.home")));

        boolean bogus = false;
        try {
            variableContract.setVariableFromProgram("bogus.property", "", VariableContractType
                    .getType(ProActiveDescriptorConstants.VARIABLES_JAVAPROPERTY_PROGRAM_TAG));
            bogus = true; //shouldn't reach this line
        } catch (Exception e) {
        }
        assertTrue(!bogus);

        variableContract.setVariableFromProgram("bogus.property", "bogus_value", VariableContractType
                .getType(ProActiveDescriptorConstants.VARIABLES_JAVAPROPERTY_PROGRAM_TAG));
        assertTrue(variableContract.getValue("bogus.property").equals("bogus_value"));

        //Setting from Descriptor
        variableContract.setDescriptorVariable("user.home", "/home/userdesc", VariableContractType
                .getType(ProActiveDescriptorConstants.VARIABLES_JAVAPROPERTY_PROGRAM_TAG));
        assertTrue(variableContract.getValue("user.home").equals(System.getProperty("user.home")));

        try {
            bogus = false;
            variableContract.setDescriptorVariable("${ilegal.var.name}", "ilegalvariablename",
                    VariableContractType
                            .getType(ProActiveDescriptorConstants.VARIABLES_JAVAPROPERTY_PROGRAM_TAG));
            bogus = true; //shouldn't reach this line
        } catch (Exception e) {
        }
        assertTrue(!bogus);

        //Setting bogus from program
        variableContract.setDescriptorVariable("bogus.property", "", VariableContractType
                .getType(ProActiveDescriptorConstants.VARIABLES_JAVAPROPERTY_PROGRAM_TAG));
        Assert.assertEquals("bogus_value", variableContract.getValue("bogus.property"));

        gcma = PAGCMDeployment.loadApplicationDescriptor(new File(XML_LOCATION), variableContract);
        variableContract = (VariableContractImpl) gcma.getVariableContract();

        variableContract.getValue("user.home").equals(System.getProperty("user.home"));

        //Empty value in descriptor should have less priority, and not set to empty
        assertTrue(variableContract.isClosed());
    }
}
