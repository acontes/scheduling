package org.ow2.proactive.authentication;

public class GroupException extends Exception {

    /**
     * Attaches a message to the Exception
     * @param msg message attached
     */
    public GroupException(String msg) {
        super(msg);
    }
}