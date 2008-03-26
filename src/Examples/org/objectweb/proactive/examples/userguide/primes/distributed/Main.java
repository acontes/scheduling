//@snippet-start primes_distributed_main
package org.objectweb.proactive.examples.userguide.primes.distributed;

import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.api.PADeployment;
import org.objectweb.proactive.core.descriptor.data.ProActiveDescriptor;
import org.objectweb.proactive.core.descriptor.data.VirtualNode;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.examples.userguide.cmagent.simple.CMAgent;


/**
 * This class illustrates a distributed version of the sequential algorithm for
 * primality test based on the {@link CMAgent}.
 * <p>
 * Some primes : 3093215881333057, 4398042316799, 63018038201, 2147483647 
 * 
 * @author The ProActive Team
 * 
 */
public class Main {

    public static void main(String[] args) {
        // The default value for the candidate to test (is prime)
        long candidate = 2147483647l;
        // Parse the number from args if there is some
        if (args.length > 1) {
            try {
                candidate = Long.parseLong(args[1]);
            } catch (NumberFormatException numberException) {
                System.err.println("Usage: Main <candidate>");
                System.err.println(numberException.getMessage());
            }
        }

        try {
            VirtualNode[] vNodes = deploy(args[0]);
            VirtualNode vNode = vNodes[0];

            // create the active object on the first node on
            // the first virtual node available
            // start the master
            CMAgentPrimeManager manager = (CMAgentPrimeManager) PAActiveObject.newActive(
                    CMAgentPrimeManager.class.getName(), new Object[] {}, vNode.getNode());

            // iterate through all nodes and deploy
            // a worker per node
            for (Node node : vNode.getNodes()) {
                CMAgentPrimeWorker worker = (CMAgentPrimeWorker) PAActiveObject.newActive(
                        CMAgentPrimeWorker.class.getName(), new Object[] {}, node);
                manager.addWorker(worker);
            }

            // Check the primality (Send a synchronous method call to the manager)
            boolean isPrime = manager.isPrime(candidate);
            // Display the result
            System.out.println("\n" + candidate + (isPrime ? " is prime." : " is not prime.") + "\n");
            // Free all resources
            for (VirtualNode vn : vNodes) {
                vn.killAll(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    private static VirtualNode[] deploy(String descriptor) {
        ProActiveDescriptor pad;
        try {
            pad = PADeployment.getProactiveDescriptor(descriptor);
            // active all Virtual Nodes
            pad.activateMappings();
            // get the first Node available in the first Virtual Node
            // specified in the descriptor file
            return pad.getVirtualNodes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
// @snippet-end primes_distributed_main
