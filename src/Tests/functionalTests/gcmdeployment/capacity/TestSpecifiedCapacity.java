package functionalTests.gcmdeployment.capacity;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.proactive.core.runtime.ProActiveRuntimeImpl;
import org.objectweb.proactive.extra.gcmdeployment.core.StartRuntime;

import functionalTests.FunctionalTest;


public class TestSpecifiedCapacity extends FunctionalTest {
    final static long askedCapacity = 12;

    @Test
    public void testSpecifiedCapacity() {
        new Thread() {
                public void run() {
                    StartRuntime.main(new String[] {
                            "--capacity", new Long(askedCapacity).toString()
                        });
                }
            }.start();

        /* Be sure that the StartRuntime thread has been scheduled
         * Otherwise getCapacity will return -1 due to a race condition
         */
        Thread.yield();

        ProActiveRuntimeImpl part = ProActiveRuntimeImpl.getProActiveRuntime();

        long cap = part.getVMInformation().getCapacity();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(askedCapacity, cap);
        Assert.assertEquals(askedCapacity, part.getLocalNodes().size());
    }
}
