/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2007 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@objectweb.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version
 * 2 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.core.remoteobject;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlException;
import java.security.PublicKey;

import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.body.future.MethodCallResult;
import org.objectweb.proactive.core.body.reply.Reply;
import org.objectweb.proactive.core.body.request.Request;
import org.objectweb.proactive.core.mop.ClassNotReifiableException;
import org.objectweb.proactive.core.mop.MOP;
import org.objectweb.proactive.core.mop.MethodCallExecutionFailedException;
import org.objectweb.proactive.core.mop.ReifiedCastException;
import org.objectweb.proactive.core.mop.StubObject;
import org.objectweb.proactive.core.remoteobject.adapter.Adapter;
import org.objectweb.proactive.core.security.PolicyServer;
import org.objectweb.proactive.core.security.ProActiveSecurityManager;
import org.objectweb.proactive.core.security.SecurityContext;
import org.objectweb.proactive.core.security.TypedCertificate;
import org.objectweb.proactive.core.security.crypto.KeyExchangeException;
import org.objectweb.proactive.core.security.crypto.SessionException;
import org.objectweb.proactive.core.security.exceptions.RenegotiateSessionException;
import org.objectweb.proactive.core.security.exceptions.SecurityNotAvailableException;
import org.objectweb.proactive.core.security.securityentity.Entities;
import org.objectweb.proactive.core.security.securityentity.Entity;


/**
 *         Implementation of a remote object.
 *
 *
 */
public class RemoteObjectImpl<T> implements RemoteObject, Serializable {
    protected Object target;
    protected String className;
    protected String proxyClassName;
    protected Adapter<T> adapter;
    protected ProActiveSecurityManager psm;

    public RemoteObjectImpl(String className, T target) {
        this(className, target, null);
    }

    public RemoteObjectImpl(String className, T target, Adapter<T> adapter) {
        this(className, target, adapter, null);
    }

    public RemoteObjectImpl(String className, T target, Adapter<T> adapter, ProActiveSecurityManager psm) {
        this.target = target;
        this.className = className;
        this.proxyClassName = SynchronousProxy.class.getName();
        this.adapter = adapter;
        this.psm = psm;
    }

    public Reply receiveMessage(Request message) throws RenegotiateSessionException, ProActiveException {
        try {
            if (message.isCiphered() && (this.psm != null)) {
                message.decrypt(this.psm);
            }
            Object o;

            if (message instanceof RemoteObjectRequest) {
                o = message.getMethodCall().execute(this);
            } else {
                o = (message).getMethodCall().execute(this.target);
            }

            return new SynchronousReplyImpl(new MethodCallResult(o, null));
        } catch (MethodCallExecutionFailedException e) {
            //            e.printStackTrace();
            throw new ProActiveException(e);
        } catch (InvocationTargetException e) {
            return new SynchronousReplyImpl(new MethodCallResult(null, e.getCause()));
        }
    }

    // implements SecurityEntity ----------------------------------------------
    public TypedCertificate getCertificate() throws SecurityNotAvailableException, IOException {
        if (this.psm != null) {
            return this.psm.getCertificate();
        }
        throw new SecurityNotAvailableException();
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.security.SecurityEntity#getEntities()
     */
    public Entities getEntities() throws SecurityNotAvailableException, IOException {
        if (this.psm != null) {
            return this.psm.getEntities();
        }
        throw new SecurityNotAvailableException();
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.security.SecurityEntity#getPolicy(org.objectweb.proactive.core.security.securityentity.Entities, org.objectweb.proactive.core.security.securityentity.Entities)
     */
    public SecurityContext getPolicy(Entities local, Entities distant) throws SecurityNotAvailableException {
        if (this.psm == null) {
            throw new SecurityNotAvailableException();
        }
        return this.psm.getPolicy(local, distant);
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.security.SecurityEntity#getPublicKey()
     */
    public PublicKey getPublicKey() throws SecurityNotAvailableException, IOException {
        if (this.psm != null) {
            return this.psm.getPublicKey();
        }
        throw new SecurityNotAvailableException();
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.security.SecurityEntity#publicKeyExchange(long, byte[])
     */
    public byte[] publicKeyExchange(long sessionID, byte[] signature) throws SecurityNotAvailableException,
            RenegotiateSessionException, KeyExchangeException, IOException {
        if (this.psm != null) {
            return this.psm.publicKeyExchange(sessionID, signature);
        }
        throw new SecurityNotAvailableException();
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.security.SecurityEntity#randomValue(long, byte[])
     */
    public byte[] randomValue(long sessionID, byte[] clientRandomValue) throws SecurityNotAvailableException,
            RenegotiateSessionException, IOException {
        if (this.psm != null) {
            return this.psm.randomValue(sessionID, clientRandomValue);
        }
        throw new SecurityNotAvailableException();
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.security.SecurityEntity#secretKeyExchange(long, byte[], byte[], byte[], byte[], byte[])
     */
    public byte[][] secretKeyExchange(long sessionID, byte[] encodedAESKey, byte[] encodedIVParameters,
            byte[] encodedClientMacKey, byte[] encodedLockData, byte[] parametersSignature)
            throws SecurityNotAvailableException, RenegotiateSessionException, IOException {
        if (this.psm != null) {
            return this.psm.secretKeyExchange(sessionID, encodedAESKey, encodedIVParameters,
                    encodedClientMacKey, encodedLockData, parametersSignature);
        }
        throw new SecurityNotAvailableException();
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.security.SecurityEntity#startNewSession(long, org.objectweb.proactive.core.security.SecurityContext, org.objectweb.proactive.core.security.TypedCertificate)
     */
    public long startNewSession(long distantSessionID, SecurityContext policy,
            TypedCertificate distantCertificate) throws SecurityNotAvailableException, SessionException {
        if (this.psm != null) {
            return this.psm.startNewSession(distantSessionID, policy, distantCertificate);
        }
        throw new SecurityNotAvailableException();
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.security.SecurityEntity#terminateSession(long)
     */
    public void terminateSession(long sessionID) throws SecurityNotAvailableException, IOException {
        if (this.psm != null) {
            this.psm.terminateSession(sessionID);
        }
        throw new SecurityNotAvailableException();
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.remoteobject.RemoteObject#getObjectProxy()
     */
    public Object getObjectProxy() throws ProActiveException {
        try {
            Object reifiedObjectStub = MOP
                    .createStubObject(this.className, target.getClass(), new Class[] {});
            if (adapter != null) {
                //            	Constructor myConstructor =   adapter.getClass().getConstructor(new Class[] {Class.forName(this.className)});
                //            	Adapter ad = (Adapter) myConstructor.newInstance(new Object[] { MOP.createStubObject(this.className, target.getClass(), new Class[] {})});
                Adapter<Object> ad = adapter.getClass().newInstance();
                ad.setAdapter(reifiedObjectStub);
                return ad;
            } else {
                return reifiedObjectStub;
            }
        } catch (ClassNotReifiableException e) {
            e.printStackTrace();
        } catch (ReifiedCastException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.remoteobject.RemoteObject#getObjectProxy(org.objectweb.proactive.core.remoteobject.RemoteRemoteObject)
     */
    public Object getObjectProxy(RemoteRemoteObject rro) throws ProActiveException {
        try {
            Object reifiedObjectStub = MOP
                    .createStubObject(this.className, target.getClass(), new Class[] {});
            ((StubObject) reifiedObjectStub).setProxy(new SynchronousProxy(null, new Object[] { rro }));
            if (adapter != null) {
                //            	Constructor myConstructor =   adapter.getClass().getConstructor(new Class[] {Class.forName(this.className)});
                //            	Adapter ad = (Adapter) myConstructor.newInstance(new Object[] { MOP.createStubObject(this.className, target.getClass(), new Class[] {})});
                Adapter<Object> ad = adapter.getClass().newInstance();
                ad.setAdapter(reifiedObjectStub);
                return ad;
            } else {
                return reifiedObjectStub;
            }
        } catch (ClassNotReifiableException e) {
            e.printStackTrace();
        } catch (ReifiedCastException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.remoteobject.RemoteObject#getClassName()
     */
    public String getClassName() {
        return this.className;
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.remoteobject.RemoteObject#getProxyName()
     */
    public String getProxyName() {
        return this.proxyClassName;
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.remoteobject.RemoteObject#getTargetClass()
     */
    public Class<?> getTargetClass() {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.remoteobject.RemoteObject#getAdapterClass()
     */
    public Class<?> getAdapterClass() {
        if (adapter != null) {
            return adapter.getClass();
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.security.SecurityEntity#getProActiveSecurityManager(org.objectweb.proactive.core.security.securityentity.Entity)
     */
    public ProActiveSecurityManager getProActiveSecurityManager(Entity user)
            throws SecurityNotAvailableException, AccessControlException {
        if (this.psm == null) {
            throw new SecurityNotAvailableException();
        }
        return this.psm.getProActiveSecurityManager(user);
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.security.SecurityEntity#setProActiveSecurityManager(org.objectweb.proactive.core.security.securityentity.Entity, org.objectweb.proactive.core.security.PolicyServer)
     */
    public void setProActiveSecurityManager(Entity user, PolicyServer policyServer)
            throws SecurityNotAvailableException, AccessControlException {
        if (this.psm == null) {
            throw new SecurityNotAvailableException();
        }
        this.psm.setProActiveSecurityManager(user, policyServer);
    }
}
