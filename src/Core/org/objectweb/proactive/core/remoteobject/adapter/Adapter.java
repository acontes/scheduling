package org.objectweb.proactive.core.remoteobject.adapter;

import java.io.Serializable;

import org.objectweb.proactive.core.mop.Proxy;
import org.objectweb.proactive.core.mop.StubObject;


/**
 * @author acontes
 * Remote Object Adapter is a mechanism that allow to insert an interposition object.
 * Thus it is possible to insert personnalized mechanisms within the remote objects like a cache mechanism
 * @param <T>
 */
public abstract class Adapter<T> implements Serializable, StubObject {

    /**
     * the generated stub
     */
    protected T target;

    public Adapter() {
    }

    /**
     * @param target the generated stub
     */
    public Adapter(T target) {
        this.target = target;
        construct();
    }

    /**
     * a method that allows to change the default target of the adapter.
     * Setting a new adapter could invalid some of the treatment done when this adapter has been constructed,
     * that why construct() is called once again.
     * @param target the new target of this adapter
     */
    public void setAdapter(T target) {
        this.target = target;
        construct();
    }

    /**
     * @return return the current target of this adapter
     */
    public T getAdapter() {
        return target;
    }

    /**
     * a method called during the constructor call.
     * If some treatment has to be done during the constructor call, Adapters have to
     * override this method
     */
    protected abstract void construct();

    /**
    * set the proxy to the active object
    */
    public void setProxy(Proxy p) {
        ((StubObject) target).setProxy(p);
    }

    /**
     * return the proxy to the active object
     */
    public Proxy getProxy() {
        return ((StubObject) target).getProxy();
    }
}
