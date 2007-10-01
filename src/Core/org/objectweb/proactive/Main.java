package org.objectweb.proactive;

import org.objectweb.proactive.core.config.PAProperties;


public class Main {

    /**
     * Returns the version number
     *
     * @return String
     */
    public static String getProActiveVersion() {
        return "$Id$";
    }

    public static void main(String[] args) {
        System.out.println("ProActive " + getProActiveVersion());

        System.out.println("Available properties:");

        for (PAProperties p : PAProperties.values()) {
            String type = p.isBoolean() ? "Boolean" : "String";
            System.out.println(type + " " + p.getKey() + " [" + p.getValue() +
                "]");
            // TODO Add a short description here
        }
    }
}
