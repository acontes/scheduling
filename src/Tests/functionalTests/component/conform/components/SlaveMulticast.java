package functionalTests.component.conform.components;

import java.util.List;


//import org.objectweb.proactive.core.component.type.annotations.multicast.ClassDispatchMetadata;
import org.objectweb.proactive.core.component.type.annotations.multicast.ParamDispatchMetadata;
import org.objectweb.proactive.core.component.type.annotations.multicast.ParamDispatchMode;
import org.objectweb.proactive.core.util.wrapper.StringWrapper;


//@ClassDispatchMetadata(mode = @ParamDispatchMetadata(mode = ParamDispatchMode.ROUND_ROBIN))
public interface SlaveMulticast {
    void compute(
        @ParamDispatchMetadata(mode = ParamDispatchMode.ROUND_ROBIN)
    List<String> args, String other);

    List<StringWrapper> computeAsync(
        @ParamDispatchMetadata(mode = ParamDispatchMode.ROUND_ROBIN)
    List<String> args, String other);

    List<String> computeSync(
        @ParamDispatchMetadata(mode = ParamDispatchMode.ROUND_ROBIN)
    List<String> args, String other);
}
