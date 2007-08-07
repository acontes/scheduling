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
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://www.inria.fr/oasis/ProActive/contacts.html
 *  Contributor(s):
 *
 * ################################################################
 */
package functionalTests.ft.cic;

import org.objectweb.proactive.ProActive;
import org.objectweb.proactive.core.descriptor.data.ProActiveDescriptor;
import org.objectweb.proactive.core.descriptor.data.VirtualNode;
import org.objectweb.proactive.core.exceptions.body.ServiceFailedCalleeNFE;
import org.objectweb.proactive.core.exceptions.manager.NFEListener;
import org.objectweb.proactive.core.exceptions.manager.TypedNFEListener;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.process.JVMProcessImpl;

import functionalTests.FunctionalTest;
import functionalTests.ft.Agent;
import functionalTests.ft.Collector;
import functionalTests.ft.ReInt;
import static junit.framework.Assert.assertTrue;

/**
 * AO fails during the computation, and is restarted.
 * Communications between passive object, non-ft active object and ft active object.
 */
public class Test extends FunctionalTest {
    private static final long serialVersionUID = 924490397928151804L;
    private int result = 0;
    private JVMProcessImpl server;
    private static String FT_XML_LOCATION_UNIX = Test.class.getResource(
            "/functionalTests/ft/testFT_CIC.xml").getPath();
    private static int AWAITED_RESULT = 1771014405;

    @org.junit.Test
    public void action() throws Exception {
        // deployer le FTServer !
        this.server = new JVMProcessImpl(new org.objectweb.proactive.core.process.AbstractExternalProcess.StandardOutputMessageLogger());
        // this.server = new JVMProcessImpl(new org.objectweb.proactive.core.process.AbstractExternalProcess.NullMessageLogger());
        this.server.setJvmOptions(FunctionalTest.JVM_PARAMETERS +
            " -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8005 ");

        this.server.setClassname(
            "org.objectweb.proactive.core.body.ft.servers.StartFTServer");
        this.server.startProcess();
        Thread.sleep(3000);

        // ProActive descriptor
        ProActiveDescriptor pad;
        VirtualNode vnode;

        //	create nodes
        pad = ProActive.getProactiveDescriptor(Test.FT_XML_LOCATION_UNIX);
        pad.activateMappings();
        vnode = pad.getVirtualNode("Workers");
        Node[] nodes = vnode.getNodes();

        Agent a = (Agent) ProActive.newActive(Agent.class.getName(),
                new Object[0], nodes[0]);
        Agent b = (Agent) ProActive.newActive(Agent.class.getName(),
                new Object[0], nodes[1]);

        // not ft !
        Collector c = (Collector) ProActive.newActive(Collector.class.getName(),
                new Object[0]);

        // a *normal* ServiceFailedCalleeNFE could appear; ignore ot for test
        ProActive.addNFEListenerOnAO(a,
            new TypedNFEListener(ServiceFailedCalleeNFE.class,
                NFEListener.NOOP_LISTENER));
        ProActive.addNFEListenerOnAO(b,
            new TypedNFEListener(ServiceFailedCalleeNFE.class,
                NFEListener.NOOP_LISTENER));

        a.initCounter(1);
        b.initCounter(1);
        a.setNeighbour(b);
        b.setNeighbour(a);
        a.setLauncher(c);

        c.go(a, 1000);

        //failure in 11 sec...
        Thread.sleep(11000);
        try {
            nodes[1].getProActiveRuntime().killRT(false);
        } catch (Exception e) {
            //e.printStackTrace();
        }

        ReInt r = c.getResult();
        this.result = r.getValue();

        //cleaning
        this.server.stopProcess();
        pad.killall(false);

        //System.out.println(" ---------> RES = " + r.getValue()); //1771014405
        assertTrue(this.result == Test.AWAITED_RESULT);
    }
}
