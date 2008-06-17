/**
 * 
 */
package org.objectweb.proactive.extensions.scheduler.core.properties;

import java.io.IOException;
import java.util.Properties;
import org.objectweb.proactive.annotation.PublicAPI;
import org.objectweb.proactive.core.config.PAProperties.PAPropertiesType;


/**
 * PASchedulerProperties contains all ProActive Scheduler properties.
 * 
 * You must use provide methods in order to get the Scheduler properties.
 * 
 * @author The ProActiveTeam
 * @date 11 june 08
 * @version 4.0
 * @since ProActive 4.0
 *
 */
@PublicAPI
public enum PASchedulerProperties {

    /* ***************************************************************** */
    /* ********************** SCHEDULER PROPERTIES ********************* */
    /* ***************************************************************** */

    /** Default scheduler node name */
    SCHEDULER_DEFAULT_NAME("pa.scheduler.core.defaultname", PAPropertiesType.STRING),

    /** Scheduler main loop time out */
    SCHEDULER_TIME_OUT("pa.scheduler.core.timeout", PAPropertiesType.INTEGER),

    /** Scheduler node ping frequency in ms. */
    SCHEDULER_NODE_PING_FREQUENCY("pa.scheduler.core.nodepingfrequency", PAPropertiesType.INTEGER),

    /** Delay to wait between getting a job result and removing the job concerned */
    SCHEDULER_REMOVED_JOB_DELAY("pa.scheduler.core.removejobdelay", PAPropertiesType.INTEGER),

    /** Default database configuration file name */
    SCHEDULER_DEFAULT_DBCONFIG_FILE("pa.scheduler.core.database.defaultconfigfile", PAPropertiesType.STRING),

    /** Login default file name */
    SCHEDULER_LOGIN_FILENAME("pa.scheduler.core.defaultloginfilename", PAPropertiesType.STRING),

    /** Group default filename */
    SCHEDULER_GROUP_FILENAME("pa.scheduler.core.defaultgroupfilename", PAPropertiesType.STRING),

    /* ***************************************************************** */
    /* ************************* JOBS PROPERTIES *********************** */
    /* ***************************************************************** */

    /** Default job name */
    JOB_DEFAULT_NAME("pa.scheduler.job.defaultname", PAPropertiesType.STRING),

    /** Multiplicative factor for job id (taskId will be : this_factor*jobID+taskID) */
    JOB_FACTOR("pa.scheduler.job.factor", PAPropertiesType.INTEGER),

    /* ***************************************************************** */
    /* ************************ TASKS PROPERTIES *********************** */
    /* ***************************************************************** */

    /** Default task name */
    TASK_DEFAULT_NAME("pa.scheduler.task.defaultname", PAPropertiesType.STRING),

    /* ***************************************************************** */
    /* ************************* LOGS PROPERTIES *********************** */
    /* ***************************************************************** */

    /** Log forwarder default listening port */
    LOGS_LISTEN_PORT("pa.scheduler.logs.listenport", PAPropertiesType.INTEGER),

    /* ***************************************************************** */
    /* ************************ OTHER PROPERTIES *********************** */
    /* ***************************************************************** */

    /** Name of the environment variable for windows home directory on the common file system. */
    WINDOWS_HOME_ENV_VAR("pa.scheduler.launcher.windowsenv", PAPropertiesType.STRING),

    /** Name of the environment variable for unix home directory on the common file system. */
    UNIX_HOME_ENV_VAR("pa.scheduler.launcher.unixenv", PAPropertiesType.STRING);

    /* ***************************************************************************** */
    /* ***************************************************************************** */
    /** Default properties file for the scheduler configuration */
    private static final String DEFAULT_PROPERTIES_FILE = "PASchedulerProperties.ini";
    /** memory entity of the properties file. */
    private static Properties prop = null;
    /** Key of the specific instance. */
    private String key;
    /** value of the specific instance. */
    private PAPropertiesType type;

    /**
     * Create a new instance of PASchedulerProperties
     *
     * @param str the key of the instance.
     * @param type the real java type of this instance.
     */
    PASchedulerProperties(String str, PAPropertiesType type) {
        this.key = str;
        this.type = type;
    }

    /**
     * Get the properties map or load it if needed.
     * 
     * @return the properties map corresponding to the default property file.
     */
    private static Properties getProperties() {
        if (prop == null) {
            prop = new Properties();
            try {
                prop.load(PASchedulerProperties.class.getResourceAsStream(DEFAULT_PROPERTIES_FILE));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return prop;
    }

    /**
     * Returns the value of this property as an integer.
     * If value is not an integer, an exception will be thrown.
     * 
     * @return the value of this property.
     */
    public int getValueAsInt() {
        String valueS = getValueAsString();
        try {
            int value = Integer.parseInt(valueS);
            return value;
        } catch (NumberFormatException e) {
            RuntimeException re = new IllegalArgumentException(key +
                " is not an integer property. getValueAsInt cannot be called on this property");
            throw re;
        }
    }

    /**
     * Returns the value of this property as a string.
     * 
     * @return the value of this property.
     */
    public String getValueAsString() {
        return getProperties().getProperty(key);
    }

    /**
     * Returns the value of this property as a boolean.
     * If value is not a boolean, an exception will be thrown.<br>
     * The behavior of this method is the same as the {@link java.lang.Boolean.parseBoolean(String s)}. 
     * 
     * @return the value of this property.
     */
    public boolean getValueAsBoolean() {
        return Boolean.parseBoolean(getValueAsString());
    }

    /**
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return getValueAsString();
    }

}
