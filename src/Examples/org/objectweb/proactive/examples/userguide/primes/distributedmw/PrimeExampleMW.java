//@snippet-start primes_distributedmw_example
package org.objectweb.proactive.examples.userguide.primes.distributedmw;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.proactive.api.PADeployment;
import org.objectweb.proactive.core.descriptor.data.ProActiveDescriptor;
import org.objectweb.proactive.core.descriptor.data.VirtualNode;
import org.objectweb.proactive.extensions.masterworker.ProActiveMaster;


/**
 * 
 * Some primes : 3093215881333057l, 4398042316799l, 63018038201, 2147483647
 * 
 * @author The ProActive Team
 * 
 */
public class PrimeExampleMW {
    /**
     * Default interval size
     */
    public static final int INTERVAL_SIZE = 100;

    public static void main(String[] args) {
        // The default value for the candidate to test (is prime)
        long candidate = 2147483647l;
        // Parse the number from args if there is some
        if (args.length > 1) {
            try {
                candidate = Long.parseLong(args[1]);
            } catch (NumberFormatException numberException) {
                System.err.println("Usage: PrimeExampleMW <candidate>");
                System.err.println(numberException.getMessage());
            }
        }
        try {
            // Create the Master
            ProActiveMaster<FindPrimeTask, Boolean> master = new ProActiveMaster<FindPrimeTask, Boolean>();
            // Deploy resources
            for (VirtualNode vNode : deploy(args[0])) {
                master.addResources(vNode);
            }
            // Create and submit the tasks
            master.solve(createTasks(candidate));

            /*************************************************/
            /* 3. Wait all results from master */
            /*************************************************/
            // Collect results            
            List<Boolean> results = master.waitAllResults();
            /*************************************************/

            // Test the primality
            boolean isPrime = true;
            for (Boolean result : results) {
                isPrime = isPrime && result;
            }
            // Display the result
            System.out.println("\n" + candidate + (isPrime ? " is prime." : " is not prime.") + "\n");
            // Terminate the master and free all resources
            master.terminate(true);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    /**
     * Creates the prime computation tasks to be solved
     * 
     * @return A list of prime computation tasks
     */
    public static List<FindPrimeTask> createTasks(long number) {
        List<FindPrimeTask> tasks = new ArrayList<FindPrimeTask>();

        // We don't need to check numbers greater than the square-root of the
        // candidate in this algorithm
        long squareRootOfCandidate = (long) Math.ceil(Math.sqrt(number));

        // Begin from 2 the first known prime number
        long begin = 2;

        // The number of intervals       
        long nbOfIntervals = (long) Math.ceil(squareRootOfCandidate / INTERVAL_SIZE);

        // Until the end of the first interval
        long end = INTERVAL_SIZE;

        for (int i = 0; i <= nbOfIntervals; i++) {

            /********************************************************/
            /* 4. Create a new task for the current interval and */
            /* add it to the list of tasks */
            /********************************************************/
            // Adds the task for the current interval to the list of tasks
            tasks.add(new FindPrimeTask(number, begin, end));
            /********************************************************/

            // Update the begin and the end of the interval
            begin = end + 1;
            end += INTERVAL_SIZE;
        }

        return tasks;
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
//@snippet-end primes_distributedmw_example