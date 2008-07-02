package org.ow2.proactive.scheduler.util.process;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;


/**
 * Environment variables.
 *
 * <p>
 * In Hudson, often we need to build up "environment variable overrides"
 * on master, then to execute the process on slaves. This causes a problem
 * when working with variables like <tt>PATH</tt>. So to make this work,
 * we introduce a special convention <tt>PATH+FOO</tt> &mdash; all entries
 * that starts with <tt>PATH+</tt> are merged and prepended to the inherited
 * <tt>PATH</tt> variable, on the process where a new process is executed. 
 * 
 * @author Kohsuke Kawaguchi
 */
public class EnvVars extends TreeMap<String, String> {

    /**
     * Create a new instance of EnvVars.
     *
     */
    public EnvVars() {
        super(CaseInsensitiveComparator.INSTANCE);
    }

    /**
     * Create a new instance of EnvVars.
     *
     * @param m an already existing map of variables.
     */
    public EnvVars(Map<String, String> m) {
        super(CaseInsensitiveComparator.INSTANCE);
        putAll(m);
    }

    /**
     * Overrides the current entry by the given entry.
     *
     * <p>
     * Handles <tt>PATH+XYZ</tt> notation.
     * @param key 
     * @param value 
     */
    public void override(String key, String value) {
        if (value == null || value.length() == 0) {
            remove(key);
            return;
        }

        int idx = key.indexOf('+');
        if (idx > 0) {
            String realKey = key.substring(0, idx);
            String v = get(realKey);
            if (v == null)
                v = value;
            else
                v = value + File.pathSeparatorChar + v;
            put(realKey, v);
            return;
        }

        put(key, value);
    }

    /**
     * Override all entries.
     *
     * @param all
     */
    public void overrideAll(Map<String, String> all) {
        for (Map.Entry<String, String> e : all.entrySet()) {
            override(e.getKey(), e.getValue());
        }
    }

    /**
     * Takes a string that looks like "a=b" and adds that to this map.
     * @param line the line to add.
     */
    public void addLine(String line) {
        int sep = line.indexOf('=');
        put(line.substring(0, sep), line.substring(sep + 1));
    }

    private static final class GetEnvVars {
        /**
         * Return the environment map.
         *
         * @return the environment map.
         */
        public Map<String, String> call() {
            return new TreeMap<String, String>(EnvVars.masterEnvVars);
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Environmental variables that we've inherited.
     *
     * <p>
     * Despite what the name might imply, this is the environment variable
     * of the current JVM process. And therefore, it is Hudson master's environment
     * variables only when you access this from the master.
     *
     * <p>
     * If you access this field from slaves, then this is the environment
     * variable of the slave agent.
     */
    public static final Map<String, String> masterEnvVars = new EnvVars(System.getenv());
}
