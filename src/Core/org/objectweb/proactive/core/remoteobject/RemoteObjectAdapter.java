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
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://www.inria.fr/oasis/ProActive/contacts.html
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.core.remoteobject;

import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.body.reply.Reply;
import org.objectweb.proactive.core.body.request.Request;
import org.objectweb.proactive.core.mop.StubObject;
import org.objectweb.proactive.core.security.Communication;
import org.objectweb.proactive.core.security.SecurityContext;
import org.objectweb.proactive.core.security.crypto.KeyExchangeException;
import org.objectweb.proactive.core.security.exceptions.RenegotiateSessionException;
import org.objectweb.proactive.core.security.exceptions.SecurityNotAvailableException;
import org.objectweb.proactive.core.security.securityentity.Entity;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;


/**
 * @author acontes
 * RemoteObjectAdapter are used to hide the protocol specific part of a remote object ie the RemoteRemoteObject
 */
public class RemoteObjectAdapter implements RemoteObject {
    protected RemoteRemoteObject remoteObject;
    protected Object stub;
    protected URI uri;

    public RemoteObjectAdapter() {
    }

    public RemoteObjectAdapter(RemoteRemoteObject ro) throws ProActiveException {
        this.remoteObject = ro;
        try {
            this.uri = ro.getURI();
        } catch (IOException e) {
            throw new ProActiveException(e);
        }
    }

    public Reply receiveMessage(Request message)
        throws ProActiveException, RenegotiateSessionException, IOException {
        try {
            return this.remoteObject.receiveMessage(message);
        } catch (EOFException e) {
            ProActiveLogger.getLogger(Loggers.REMOTEOBJECT)
                           .debug("EOFException while calling method " +
                message.getMethodName());
            return new SynchronousReplyImpl();
        } catch (ProActiveException e) {
            throw new IOException(e.getMessage());
        } catch (IOException e) {
            ProActiveLogger.getLogger(Loggers.REMOTEOBJECT)
                           .warn("unable to contact remote object at " +
                this.uri + " when calling " + message.getMethodName());
            return new SynchronousReplyImpl(e);
        }

        //        return new SynchronousReplyImpl();
    }

    public X509Certificate getCertificate()
        throws SecurityNotAvailableException, IOException {
        return this.remoteObject.getCertificate();
    }

    public byte[] getCertificateEncoded()
        throws SecurityNotAvailableException, IOException {
        return this.remoteObject.getCertificateEncoded();
    }

    public ArrayList<Entity> getEntities()
        throws SecurityNotAvailableException, IOException {
        return this.remoteObject.getEntities();
    }

    public SecurityContext getPolicy(SecurityContext securityContext)
        throws SecurityNotAvailableException, IOException {
        return this.remoteObject.getPolicy(securityContext);
    }

    public PublicKey getPublicKey()
        throws SecurityNotAvailableException, IOException {
        return this.remoteObject.getPublicKey();
    }

    public byte[][] publicKeyExchange(long sessionID, byte[] myPublicKey,
        byte[] myCertificate, byte[] signature)
        throws SecurityNotAvailableException, RenegotiateSessionException,
            KeyExchangeException, IOException {
        return this.remoteObject.publicKeyExchange(sessionID, myPublicKey,
            myCertificate, signature);
    }

    public byte[] randomValue(long sessionID, byte[] clientRandomValue)
        throws SecurityNotAvailableException, RenegotiateSessionException,
            IOException {
        return this.remoteObject.randomValue(sessionID, clientRandomValue);
    }

    public byte[][] secretKeyExchange(long sessionID, byte[] encodedAESKey,
        byte[] encodedIVParameters, byte[] encodedClientMacKey,
        byte[] encodedLockData, byte[] parametersSignature)
        throws SecurityNotAvailableException, RenegotiateSessionException,
            IOException {
        return this.remoteObject.secretKeyExchange(sessionID, encodedAESKey,
            encodedIVParameters, encodedClientMacKey, encodedLockData,
            parametersSignature);
    }

    public long startNewSession(Communication policy)
        throws SecurityNotAvailableException, RenegotiateSessionException,
            IOException {
        return this.remoteObject.startNewSession(policy);
    }

    public void terminateSession(long sessionID)
        throws SecurityNotAvailableException, IOException {
        this.remoteObject.terminateSession(sessionID);
    }

    public Object getObjectProxy() throws ProActiveException {
        try {
            if (this.stub == null) {
                this.stub = this.remoteObject.getObjectProxy();
                ((SynchronousProxy) ((StubObject) this.stub).getProxy()).setRemoteObject(this);
            }
            return this.stub;
        } catch (IOException e) {
            throw new ProActiveException(e);
        }
    }

    public Object getObjectProxy(RemoteRemoteObject rmo)
        throws ProActiveException {
        try {
            if (this.stub == null) {
                this.stub = this.remoteObject.getObjectProxy();
            }
            return this.stub;
        } catch (IOException e) {
            throw new ProActiveException(e);
        }
    }

    public String getClassName() {
        try {
            return this.remoteObject.getClassName();
        } catch (ProActiveException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public String getProxyName() {
        try {
            return this.remoteObject.getProxyName();
        } catch (ProActiveException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RemoteObjectAdapter) {
            return remoteObject.equals(((RemoteObjectAdapter) o).remoteObject);
        }
        return false;
    }

    public Class getTargetClass() {
        try {
            return remoteObject.getTargetClass();
        } catch (ProActiveException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public Class getAdapterClass() {
        try {
            return this.remoteObject.getAdapterClass();
        } catch (ProActiveException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
