package functionaltests;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.ow2.proactive.resourcemanager.common.event.RMEventType;
import org.ow2.proactive.resourcemanager.common.event.RMInitialState;
import org.ow2.proactive.resourcemanager.common.event.RMNodeSourceEvent;
import org.ow2.proactive.resourcemanager.frontend.ResourceManager;
import org.ow2.tests.Consecutive;
import org.ow2.tests.FunctionalTest;


@Consecutive
public class RMConsecutive extends FunctionalTest {

    @Before
    public void prepareForTest() throws Exception {
        String urlProperty = System.getProperty("url");
        boolean consecutiveMode = urlProperty != null && !urlProperty.equals("${url}");

        if (consecutiveMode) {
            System.out.println("Cleaning the RM before the test execution");
            // clean the state of the RM
            RMTHelper helper = RMTHelper.getDefaultInstance();
            ResourceManager rm = helper.getResourceManager(null, "admin", "admin");
            int nodeNumber = rm.getState().getTotalNodesNumber();

            List<String> sources = new ArrayList<String>();
            RMInitialState state = rm.getMonitoring().getState();
            for (RMNodeSourceEvent sourceEvent : state.getNodeSource()) {
                sources.add(sourceEvent.getSourceName());
            }

            for (String source : sources) {
                rm.removeNodeSource(source, true);
                helper.waitForNodeSourceEvent(RMEventType.NODESOURCE_REMOVED, source);
            }

            for (int i = 0; i < nodeNumber; i++) {
                helper.waitForAnyNodeEvent(RMEventType.NODE_REMOVED);
            }
        }

        super.prepareForTest();
    }
}
