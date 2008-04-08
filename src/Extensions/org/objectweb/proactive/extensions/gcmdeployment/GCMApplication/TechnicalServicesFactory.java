package org.objectweb.proactive.extensions.gcmdeployment.GCMApplication;

import java.util.Map;

import org.objectweb.proactive.core.descriptor.services.TechnicalService;
import org.objectweb.proactive.extensions.gcmdeployment.GCMDeploymentLoggers;


public class TechnicalServicesFactory {

    static public TechnicalService create(String className, Map<String, String> args) {

        try {
            Class<?> tc;
            tc = Class.forName(className);
            TechnicalService ts = (TechnicalService) tc.newInstance();
            (ts).init(args);

            return ts;

        } catch (Exception e) {
            GCMDeploymentLoggers.GCM_NODEMAPPER_LOGGER.warn(
                    "Exception when try to create technical service " + className, e);
            return null;
        }
    }
}
