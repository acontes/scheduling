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
package org.objectweb.proactive.core.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.body.UniversalBody;
import org.objectweb.proactive.core.security.crypto.AuthenticationException;
import org.objectweb.proactive.core.security.crypto.AuthenticationTicket;
import org.objectweb.proactive.core.security.crypto.AuthenticationTicketProperty;
import org.objectweb.proactive.core.security.crypto.ConfidentialityTicket;
import org.objectweb.proactive.core.security.crypto.KeyExchangeException;
import org.objectweb.proactive.core.security.crypto.RandomLongGenerator;
import org.objectweb.proactive.core.security.crypto.Session;
import org.objectweb.proactive.core.security.exceptions.CommunicationForbiddenException;
import org.objectweb.proactive.core.security.exceptions.InvalidPolicyFile;
import org.objectweb.proactive.core.security.exceptions.RenegotiateSessionException;
import org.objectweb.proactive.core.security.exceptions.SecurityNotAvailableException;
import org.objectweb.proactive.core.security.securityentity.Entity;
import org.objectweb.proactive.core.security.securityentity.EntityVirtualNode;
import org.objectweb.proactive.core.util.converter.ObjectToByteConverter;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;


/**
 * The ProActiveSecurityManager handles all security related actions for
 * a given SecurityEntity.
 */
public class ProActiveSecurityManager implements Serializable, SecurityEntity {
    static Logger logger = ProActiveLogger.getLogger(Loggers.SECURITY_MANAGER);

    /* contains all active sessions for the current active object */
    protected Hashtable<Long, Session> sessions;

    /* random generator used for generating sesssion key */
    protected transient RandomLongGenerator randomLongGenerator;

    /* Policy server */
    protected PolicyServer policyServer;

    /* keystore containing entity certificate and key */
    protected KeyStore keyStore;

    //protected XMLPropertiesStore policiesRules;
    protected transient UniversalBody myBody;
    protected String VNName;

    // static private Object synchro = new Object();

    /* pointer to the wrapping SecurityEntity if exists */
    protected transient SecurityEntity parent;
    protected byte[] encodedKeyStore;

    // indicates the type of the secured object (object, node, runtime)
    protected int type;

    /**
     * This a the default constructor to use with the ProActiveSecurityManager
     */
    public ProActiveSecurityManager() {
        this.sessions = new Hashtable<Long, Session>();
        this.policyServer = null;
    }

    public ProActiveSecurityManager(String file)
        throws java.io.IOException, InvalidPolicyFile {
        Provider myProvider = new org.bouncycastle.jce.provider.BouncyCastleProvider();
        Security.addProvider(myProvider);
        this.sessions = new Hashtable<Long, Session>();

        if ((new File(file)).exists()) {
            this.policyServer = ProActiveSecurityDescriptorHandler.createPolicyServer(file);
            this.keyStore = this.policyServer.getKeyStore();
        }
        logger.debug("creating Security Manager using file " + file);
    }

    /**
     * @param server
     */
    public ProActiveSecurityManager(PolicyServer server) {
        this();

        this.policyServer = server;
        this.keyStore = server.getKeyStore();
    }

    /**
     * @param keyStore
     * @param policyServer
     */
    public ProActiveSecurityManager(KeyStore keyStore, PolicyServer policyServer) {
        this();
        this.policyServer = policyServer;
        this.keyStore = keyStore;
    }

    public void setBody(UniversalBody body) {
        this.myBody = body;
    }

    /**
     * Method getPolicyTo.
     * @param securityContext the object certificate we want to get the policy from
     * @return Policy policy attributes
     */
    public SecurityContext getPolicy(SecurityContext securityContext)
        throws SecurityNotAvailableException {
        // asking for our wrapping entity policies
        if (this.parent != null) {
            try {
                securityContext = this.parent.getPolicy(securityContext);
            } catch (SecurityNotAvailableException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // adding our policies
        securityContext = this.policyServer.getPolicy(securityContext);

        return securityContext;
    }

    /**
     * Method getPolicyTo.
     * @return Policy policy attributes
     */
    public Communication getPolicyTo(String type, String from, String to)
        throws SecurityNotAvailableException {
        if (this.policyServer == null) {
            throw new SecurityNotAvailableException();
        }
        return this.policyServer.getPolicyTo(type, from, to);
    }

    /**
     * Method initiateSession. This method is the entry point for an secured communication. We get local and distant policies,
     * compute it, and generate the result policy, then if needed, we start an symmetric key exchange to encrypt the communication.
     * @param distantSecurityEntity
     * @throws CommunicationForbiddenException
     * @throws AuthenticationException
     */
    public void initiateSession(int type, SecurityEntity distantSecurityEntity)
        throws CommunicationForbiddenException,
            org.objectweb.proactive.core.security.crypto.AuthenticationException,
            RenegotiateSessionException, SecurityNotAvailableException {
        // client side
        Communication localPolicy = null;
        Communication distantBodyPolicy = null;

        //        PolicyServer runtimePolicyServer = null;
        X509Certificate distantBodyCertificate = null;
        try {
            distantBodyCertificate = ProActiveSecurity.decodeCertificate(distantSecurityEntity.getCertificateEncoded());
        } catch (SecurityNotAvailableException e3) {
            e3.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //        Communication runtimePolicy;
        //        Communication VNPolicy;
        Communication distantPolicy;

        //        runtimePolicy = VNPolicy = distantBodyPolicy = null;
        ArrayList<Entity> arrayFrom = new ArrayList<Entity>();
        ArrayList<Entity> arrayTo = new ArrayList<Entity>();

        // retrienes entities from source
        arrayFrom = this.getEntities();

        try {
            arrayTo = distantSecurityEntity.getEntities();
        } catch (SecurityNotAvailableException e2) {
            e2.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }

        // retrieve distant policy from local object
        SecurityContext sc = new SecurityContext(SecurityContext.COMMUNICATION_SEND_REQUEST_TO,
                arrayFrom, arrayTo);

        sc = this.policyServer.getPolicy(sc);

        localPolicy = sc.getSendRequest();

        if (!localPolicy.isCommunicationAllowed()) {
            throw new CommunicationForbiddenException(
                "Sending request is denied");
        }

        // retrieve policy from distant object
        SecurityContext scDistant = new SecurityContext(SecurityContext.COMMUNICATION_RECEIVE_REQUEST_FROM,
                arrayFrom, arrayTo);

        try {
            scDistant = distantSecurityEntity.getPolicy(scDistant);
        } catch (SecurityNotAvailableException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        distantPolicy = scDistant.getReceiveRequest();

        if (!distantPolicy.isCommunicationAllowed()) {
            throw new CommunicationForbiddenException(
                "Receiving request denied ");
        }

        if (distantBodyPolicy == null) {
            distantBodyPolicy = new Communication();
        }

        //compute policy
        Communication resultPolicy = Communication.computeCommunication(localPolicy,
                distantBodyPolicy);

        long sessionID = 0;
        Session session = null;
        boolean sessionAccepted = false;

        try {
            while (!sessionAccepted) {
                // getting sessionID from target entity
                sessionID = distantSecurityEntity.startNewSession(resultPolicy);
                Long longId = new Long(sessionID);

                if ((session = this.sessions.get(longId)) == null) {
                    session = new Session(sessionID, resultPolicy);
                    session.setDistantOACertificate(distantBodyCertificate);
                    this.sessions.put(new Long(sessionID), session);
                    sessionAccepted = true;
                    ProActiveLogger.getLogger(Loggers.SECURITY_MANAGER)
                                   .debug("adding new session " + sessionID);
                } else if (this.getCertificate().equals(distantBodyCertificate)) {
                    // send a secured message to myself ... why not
                    //session.distantSecureEntity = distantSecurityEntity;
                    session.setDistantOACertificate(distantBodyCertificate);
                    sessionAccepted = true;
                    ProActiveLogger.getLogger(Loggers.SECURITY_MANAGER)
                                   .debug("adding new session : " + sessionID);
                }

                //   }
            }
        } catch (Exception e) {
            logger.warn("can't start a new session");
            e.printStackTrace();
            throw new org.objectweb.proactive.core.security.crypto.AuthenticationException();
        }

        try {
            if (distantBodyCertificate != null) {
                session.setDistantOAPublicKey(distantBodyCertificate.getPublicKey());
            } else {
                ProActiveLogger.getLogger(Loggers.SECURITY_MANAGER)
                               .debug("WARNING remote object scertificate is null");
                session.setDistantOAPublicKey(distantSecurityEntity.getPublicKey());
            }

            ProActiveLogger.getLogger(Loggers.SECURITY_MANAGER)
                           .debug("adding new session " + sessionID +
                " distant object is " +
                distantSecurityEntity.getCertificate().getSubjectDN() +
                "\n local object is " + this.getCertificate().getSubjectDN());

            keyNegociationSenderSide(distantSecurityEntity, sessionID);

            // session is ready, we can validate it
            session.setSessionValidated(true);
        } catch (KeyExchangeException e) {
            logger.warn("Key exchange exception ");
            e.printStackTrace();
            throw new CommunicationForbiddenException();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public X509Certificate getCertificate() {
        try {
            if (this.keyStore == null) {
                return null;
            }

            return (X509Certificate) this.keyStore.getCertificate(SecurityConstants.KEYSTORE_ENTITY_PATH);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void terminateSession(UniversalBody body, long sessionID) {
        terminateSession(sessionID);
    }

    public void terminateSession(long sessionID) {
        synchronized (this.sessions) {
            this.sessions.remove(new Long(sessionID));
        }
    }

    /**
     * @param communicationPolicy
     * @return an identifier for the session
     */
    public synchronized long startNewSession(Communication communicationPolicy) {
        long id = 0;

        //        PolicyRule defaultPolicy = new PolicyRule();

        //if (!defaultPolicy.equals(po)) {
        try {
            boolean sessionAccepted = false;
            Long longId;
            do {
                id = new Random().nextLong() + System.currentTimeMillis();
                longId = new Long(id);
                if (this.sessions.get(longId) == null) {

                    /* sessionID doest not exist, so we can
                     * create one
                     */
                    Session newSession = new Session(id, communicationPolicy);
                    this.sessions.put(longId, newSession);
                    sessionAccepted = true;
                }
            } while (!sessionAccepted);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ProActiveLogger.getLogger(Loggers.SECURITY)
                       .debug("starting a new session : " + id);
        return id;
    }

    /**
     * Method encrypt.
     * @param sessionID the session we use to encrypt the Object
     * @param object the object to encrypt
     * @return byte[][] encrypted result
     */
    public byte[][] encrypt(long sessionID, Object object, int type)
        throws RenegotiateSessionException {
        Session session = this.sessions.get(new Long(sessionID));

        if (session != null) {
            try {
                while (!session.isSessionValidated()) {
                    // System.out.println("I wait session " +  session.getSessionID()+ "validation ");
                    Thread.sleep(50);
                }

                ProActiveLogger.getLogger(Loggers.SECURITY)
                               .debug("Ciphering object, session is " +
                    sessionID);

                byte[] byteArray = ObjectToByteConverter.MarshallStream.convert(object);

                return session.writePDU(byteArray, type);
            } catch (Exception e) {
                throw new RenegotiateSessionException(
                    "Something wrong when I tried to crypt the message");
            }

            //         return encryptionEngine.encrypt(message, ((Session) sessions.get(s)).getSessionKey(id));
        } else {
            throw new RenegotiateSessionException(
                "Requested session was not found, need to negotiate another one");
        }
    }

    /**
     * Method decrypt.
     * @param sessionID the session we use to decrypt the message
     * @param message the message to decrypt
     * @return byte[] the decrypted message returns as byte array
     */
    public byte[] decrypt(long sessionID, byte[][] message, int type)
        throws RenegotiateSessionException {
        Session session = this.sessions.get(new Long(sessionID));
        if (session != null) {
            try {
                int counterLimit = SecurityConstants.MAX_SESSION_VALIDATION_WAIT;
                while (!session.isSessionValidated() && (counterLimit > 0)) {
                    //System.out.println("(decrypt) I wait session " +  session.getSessionID()+ "validation ");
                    Thread.sleep(50);
                    counterLimit--;
                }

                if (counterLimit == 0) {
                    throw new RenegotiateSessionException(
                        "Decrypting Request, session validation delay has expired");
                }
                return session.readPDU(message[0], message[1], type);
            } catch (IOException e) {
                throw new RenegotiateSessionException(
                    "Decrypting the session was not found, need to renegotiate a new one");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            throw new RenegotiateSessionException(
                "While decrypting the session was not found, need to renegotiate a new one");
        }

        return null;
    }

    public boolean mutualAuthenticationSenderSide(UniversalBody distantBody,
        X509Certificate distantBodyCertificate) throws AuthenticationException {
        checkCertificate(distantBodyCertificate);
        unilateralAuthenticationSenderSide(distantBody);

        return true;
    }

    /**
     * Method checkCertificate. Checks the validity of an certificate
     * @param distantBodyCertificate the certificate to check
     * @return boolean. returns true if the certificate is valid, false otherwise
     */
    private boolean checkCertificate(X509Certificate distantBodyCertificate) {
        //  logger.info("Checking distant OA certificate validity");
        try {
            distantBodyCertificate.checkValidity();
        } catch (CertificateExpiredException e) {
            logger.warn(distantBodyCertificate.getSubjectDN() +
                " has expired, negociation stopped");

            return false;
        } catch (CertificateNotYetValidException e) {
            logger.warn(distantBodyCertificate.getSubjectDN() +
                " is not yet valid, negociation stopped");

            return false;
        }

        //     logger.info("Retrieving DistantOA Domain Server");
        //        String domainLocation = distantBodyCertificate.getIssuerDN().getName();
        return true;
    }

    public boolean unilateralAuthenticationSenderSide(UniversalBody distantBody)
        throws AuthenticationException {
        long rb = this.randomLongGenerator.generateLong(32);
        AuthenticationTicket authenticationTicket = new AuthenticationTicket();
        String B = this.getCertificate().getIssuerDN().getName();
        long ra = authenticationTicket.random;
        String addresse = authenticationTicket.identity;

        if (addresse.equals(B) == false) {
            throw new AuthenticationException(
                "SessionInitializer : WRONG IDENTITY");
        }

        // Emitter Certificate Checking
        X509Certificate emitterCertificate = authenticationTicket.certificate;
        //        String A = emitterCertificate.getIssuerDN().getName();

        // A is the sessionInitializer
        checkCertificate(emitterCertificate);

        AuthenticationTicketProperty properties = new AuthenticationTicketProperty();

        try {
            properties = (AuthenticationTicketProperty) ((SignedObject) authenticationTicket.signedAuthenticationTicketProperty).getObject();
        } catch (Exception e) {
            System.out.println(
                "SessionInitializer : Exception in AuthenticationTicketProperty extraction : " +
                e);
        }

        if (properties.random1 != ra) {
            throw new AuthenticationException("SessionInitializer : wrong ra");
        }

        if (properties.random2 != rb) {
            throw new AuthenticationException("SessionInitializer : wrong rb");
        }

        if (properties.identity.equals(B) == false) {
            throw new AuthenticationException("SessionInitializer : wrong B");
        }

        //    this.authentication = true;
        return true;
    }

    /**
     * Method keyNegociationSenderSide. starts the challenge to negociate a session key.
     * @param distantSecurityEntity distant active object we want to communicate to.
     * @param sessionID the id of the session we will use
     * @return boolean returns true if the negociation has succeed.
     * @throws KeyExchangeException
     */
    public boolean keyNegociationSenderSide(
        SecurityEntity distantSecurityEntity, long sessionID)
        throws KeyExchangeException {
        Session session = this.sessions.get(new Long(sessionID));

        if (session == null) {
            throw new KeyExchangeException("the session is null");
        }

        try {
            // Step 1. public key exchange for authentication
            //
            // Send a HELLO to server + my random value.
            // The server will now respond with its Hello + random.
            //  se_rand is the server response
            // Read the HELLO back from the server and collect
            // the Server Random value.
            //
            session.sec_rand.nextBytes(session.cl_rand);
            session.se_rand = distantSecurityEntity.randomValue(sessionID,
                    session.cl_rand);

            // Next send my public key from the key pair that is only
            // used for encryption/decryption purposes. Then sign the whole
            // exchange with my signing only key pair.
            //
            //
            // Set up Signature Class.
            //
            byte[] my_pub;
            byte[] my_cert;
            byte[] sig_code;

            Signature sig;

            //   synchronized  (synchro) {
            sig = Signature.getInstance("MD5withRSA", "BC");

            //
            // Init signature with the private key used for signing.
            //
            sig.initSign(this.getPrivateKey(), session.sec_rand);

            //
            // All signatures incorporate the client random and the server
            // random values.
            //
            sig.update(session.cl_rand); // Incorporated into every sig.
            sig.update(session.se_rand);

            //
            // Get my public key (for encryption) as a byte array.
            //
            my_pub = this.getPublicKey().getEncoded();

            //
            // Get my certificate (for sig validation and auth) as a byte array.
            //
            my_cert = this.getCertificateEncoded();

            sig.update(my_pub); // Incorporate public key into signature.
            sig.update(my_cert); // Incorporate certificate into signature.

            sig_code = sig.sign();
            //    }
            //            System.out.println(session);
            //
            // complete the PDU and send it to the server
            //
            byte[][] tab = distantSecurityEntity.publicKeyExchange(sessionID,
                    my_pub, my_cert, sig_code);

            //
            // Now server should respond with its public key exchange message.
            // If it does not I must break as the protocol has been broken.
            //
            //
            // Read in Server Public key.
            //
            byte[] pub_key = tab[0];

            //
            // Before we can use the public key we must convert it back
            // to a Key object by using the KeyFactory and the appropriate
            // KeySpec.. In this case the X509EncodedKeySpec is the correct one.
            //
            X509EncodedKeySpec key_spec = new X509EncodedKeySpec(pub_key);

            //synchronized (synchro) {
            KeyFactory key_fact = KeyFactory.getInstance("RSA", "BC");

            //
            // Recover Servers Public key.
            //
            session.distantOAPublicKey = key_fact.generatePublic(key_spec);

            //  }
            //
            // Read in the encoded form of the X509Certificate that the
            // server uses. For authentication of its identity.
            //
            byte[] cert = tab[1];

            //
            // Set up a Certificate Factory to process the raw certificate
            // back into an X509 Certificate
            //
            // synchronized (synchro) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            //
            // Recover Servers Certificate
            //
            session.distantOACertificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(
                        cert));
            //     }
            // NOTE:
            // At this point it should be noted that the client must employ
            // some mechanism to validate and authenticate the servers
            // certificate.
            //              PublicKey se_public = distantOA.getPublicKey();
            //             X509Certificate server_cert = distantOA.getCertificate();
            //
            // Read in Signature code.
            //
            sig_code = tab[2];

            //
            // Now we must verify the data that we received against
            // the signature code at the end of the pdu.
            //
            //
            // Using the authentication certificate sent by the server.
            //
            sig.initVerify(session.distantOACertificate);
            sig.update(session.cl_rand); // Incorporate in Client Random.
            sig.update(session.se_rand); // Incorporate in Server Random.
            sig.update(pub_key); // Incorporate in Public key (as sent in encoded form).
            sig.update(cert); // Incorporate in Certificate. (as sent in encoded form).

            if (!sig.verify(sig_code)) {
                throw new Exception(
                    "(CLIENT)Signature failed on Public key exchange data unit");
            }

            // ==== confidentiality part : secret key exchange
            //
            // Now that we have successfully exchanged public keys
            // The client now needs to being a SecretKey Exchange process.
            // First we need to generate some secrets using the appropriate
            // KeyGenerator. When using the JCE you should always use a
            // KeyGenerator instance specifically set up for your target cipher.
            // The KeyGenerator code will generate a secret that is "safe" for use
            // and filter out any keys that may be weak or broken for that cipher.
            //
            KeyGenerator key_gen = KeyGenerator.getInstance("AES", "BC"); // Get instance for AES

            key_gen.init(192, session.sec_rand); // Use a 192 bit key size.
            session.cl_aes_key = key_gen.generateKey(); // Generate the  SecretKey

            key_gen.init(160, session.sec_rand); // Set up for a 160 bit key.
            session.cl_hmac_key = key_gen.generateKey(); // Generate key for HMAC.

            //
            // For AES in CBC mode requires an IV equal in length to the block
            // size of the cipher. AES has a bock size of 128 bits so the IV is a
            // constant block size of 128 bits regardless of key size.
            //
            byte[] cl_iv = new byte[16];
            session.cl_iv = new IvParameterSpec(cl_iv);

            byte[] aes_key;
            byte[] iv;
            byte[] mac;
            byte[] lock;
            byte[] sigtab;

            //
            // I have added this extra piece of data so that an attacker
            // cannot resign or modify the secret exchange without being one of the recipients.
            //
            byte[] tmp_lock = new byte[24];
            Cipher aes_lock = null;

            //
            // Next we need it instantiate the Cipher class that will be used by
            // the client. As this is the client side the cipher needs to be set
            // up for Encryption.
            //
            session.cl_cipher.init(Cipher.ENCRYPT_MODE, session.cl_aes_key,
                session.cl_iv, session.sec_rand);

            //
            // Set up Client side MAC with key.
            //
            session.cl_mac.init(session.cl_hmac_key);

            //
            // Load byte array with random data.
            //
            session.sec_rand.nextBytes(tmp_lock);

            //
            // Set up RSA for encryption.
            //
            session.rsa_eng.init(Cipher.ENCRYPT_MODE,
                session.distantOAPublicKey, session.sec_rand);

            //
            // Set up and instace of AES for the Signature locking data.
            //
            aes_lock = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
            aes_lock.init(Cipher.ENCRYPT_MODE, session.cl_aes_key,
                session.cl_iv, session.sec_rand);

            //
            //  Secret Keys Exchange. = confidentiality
            //
            //
            // Set up Signature so that the server can validate our data.
            //
            sig.initSign(this.getPrivateKey());
            sig.update(session.cl_rand); // Incorporate Client Random
            sig.update(session.se_rand); // Incorporate Server Random.

            aes_key = session.rsa_eng.doFinal(session.cl_aes_key.getEncoded()); // Encrypt the encoded AES key.
            sig.update(aes_key); // Incorporate into signature.

            iv = session.rsa_eng.doFinal(session.cl_iv.getIV()); // Encrypt the IV.
            sig.update(iv); // Incorporate into signature.

            mac = session.rsa_eng.doFinal(session.cl_hmac_key.getEncoded()); // Encrypt and encode MAC key.
            sig.update(mac); // Incorporate into signature.

            lock = aes_lock.doFinal(tmp_lock); // Encrypt lock data.
            sig.update(tmp_lock); // Incorporate plain text of lock data into signature.

            // send to server and get the results
            sigtab = sig.sign();

            byte[][] tabresult = distantSecurityEntity.secretKeyExchange(sessionID,
                    aes_key, iv, mac, lock, sigtab);

            //
            // Read back the server secret.
            //
            // The server should respond with its secret if it does not then
            // the protocol is broken..
            //
            //
            // Set up RSA in decrypt mode so that we can decrypt their
            // server's secrets.
            //
            byte[] aes_key_enc;

            //
            // Read back the server secret.
            //
            // The server should respond with its secret if it does not then
            // the protocol is broken..
            //
            //
            // Set up RSA in decrypt mode so that we can decrypt their
            // server's secrets.
            //
            byte[] iv_enc;

            //
            // Read back the server secret.
            //
            // The server should respond with its secret if it does not then
            // the protocol is broken..
            //
            //
            // Set up RSA in decrypt mode so that we can decrypt their
            // server's secrets.
            //
            byte[] hmac_key_enc;

            //
            // Read back the server secret.
            //
            // The server should respond with its secret if it does not then
            // the protocol is broken..
            //
            //
            // Set up RSA in decrypt mode so that we can decrypt their
            // server's secrets.
            //
            byte[] tmp_loc;

            session.rsa_eng.init(Cipher.DECRYPT_MODE, this.getPrivateKey(),
                session.sec_rand);

            //
            // Read in secret key.
            //
            aes_key_enc = tabresult[0];

            //
            // Read in IV
            //
            iv_enc = tabresult[1];

            //
            // Read in HMAC key.
            //
            hmac_key_enc = tabresult[2];

            //
            // Read in lock
            //
            tmp_loc = tabresult[3];

            //
            // Now we must validate the received data.
            // NOTE: the need to decrypt the tmp_lock data.
            //
            //
            // Set up AES lock so we can decrypt lock data.
            //
            SecretKey sk = new SecretKeySpec(session.rsa_eng.doFinal(
                        aes_key_enc), "AES");
            IvParameterSpec ivspec = new IvParameterSpec(session.rsa_eng.doFinal(
                        iv_enc));
            aes_lock.init(Cipher.DECRYPT_MODE, sk, ivspec);
            sig.initVerify(session.distantOACertificate); // Set up using server's certificate.
            sig.update(session.cl_rand);
            sig.update(session.se_rand);
            sig.update(aes_key_enc);
            sig.update(iv_enc);
            sig.update(hmac_key_enc);
            sig.update(aes_lock.doFinal(tmp_loc));

            if (!sig.verify(tabresult[4])) {
                throw new Exception(
                    "Signature failed on Public key exchange data unit");
            } else {
                //   System.out.println("Client: Server PDU for secret key exchange signature passed");
            }

            //
            // At this point we have successfully exchanged secrets..
            // So now we need to set up a Cipher class to allow us to
            // Decrypt data sent from the server to the client.
            //
            //
            session.se_aes_key = new SecretKeySpec(session.rsa_eng.doFinal(
                        aes_key_enc), "AES");
            session.se_iv = new IvParameterSpec(session.rsa_eng.doFinal(iv_enc));
            session.se_cipher.init(Cipher.DECRYPT_MODE, session.se_aes_key,
                session.se_iv);

            //
            // We also need to set up the MAC so that we can validate
            // data sent from the server.
            //
            session.se_hmac_key = new SecretKeySpec(session.rsa_eng.doFinal(
                        hmac_key_enc), "AES");
            session.se_mac.init(session.se_hmac_key);

            //  System.out.println("session key end");
            //
            // Set up session to generate appropriate PDUs.
            // To see this shape of the PDUs that are used to exchange
            // encrypted data.
            //
            // setup for sending and receiving are automaticly done if we succeed
            // the key echange
        } catch (Exception e) {
            e.printStackTrace();
            throw new KeyExchangeException(
                "something wrong with the key exchange, see the stack trace");
        }

        return true;
    }

    protected PrivateKey getPrivateKey() {
        try {
            return (PrivateKey) this.keyStore.getKey(SecurityConstants.KEYSTORE_ENTITY_PATH,
                null);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        }

        return null;
    }

    public AuthenticationTicket mutualAuthenticationReceiverSide(
        AuthenticationTicket authenticationTicket, long randomID)
        throws org.objectweb.proactive.core.security.crypto.AuthenticationException {
        return null;
    }

    /**
     * Method generateSessionKey. generates a session key using Rijndael algorithms.
     * @return Key a symetric key used to encrypt/decrypt communications between active objects
     */
    private Key generateSessionKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("Rijndael", "BC");
            keyGen.init(128, new SecureRandom());

            return keyGen.generateKey();
        } catch (java.security.NoSuchProviderException e) {
            e.printStackTrace();
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public AuthenticationTicket unilateralAuthenticationReceiverSide(
        long randomID, long rb, String emittor) throws AuthenticationException {
        return null;
    }

    public ConfidentialityTicket keyNegociationReceiverSide(
        ConfidentialityTicket confidentialityTicket, long randomID)
        throws KeyExchangeException {
        return null;
    }

    public byte[] randomValue(long sessionID, byte[] clientRandomValue)
        throws SecurityNotAvailableException, RenegotiateSessionException {
        // server side
        // Step one..
        // Upon receipt of client Hello the server process reads
        // in a byte array of 32 random bytes. The server process
        // then responds with a byte array of random data.
        //
        // 	System.out.println("RAndomValue sessionID : " + new Long(sessionID));
        //        	System.out.println("++++++++++++++++++ List opened sessions : ++++++++++++++++++++++++");
        //         System.out.println("Certificat : " + certificate.getSubjectDN());
        //        for (Enumeration e = sessions.elements() ; e.hasMoreElements() ;) {
        //          System.out.println(e.nextElement());
        //      }
        //        System.out.println("++++++++++++++++++ End List opened sessions : ++++++++++++++++++++++++");
        Session session = this.sessions.get(new Long(sessionID));

        //	System.out.println("fsdfsda session : " + session);
        if (session == null) {
            throw new RenegotiateSessionException(
                "Session not started,session is null");
        }

        try {
            //         System.out.println("Server: got HELLO from client");
            session.cl_rand = clientRandomValue;

            //
            // Generate Server's Random;
            //
            session.sec_rand.nextBytes(session.se_rand); // Fill with random data.

            //   System.out.println("Server: Sending my HELLO to client");
        } catch (Exception e) {
            System.out.println("Server: Hello failed");
            e.printStackTrace();
        }

        return session.se_rand;
    }

    public byte[][] publicKeyExchange(long sessionID, byte[] pub_key,
        byte[] cert, byte[] signature)
        throws SecurityNotAvailableException, RenegotiateSessionException,
            KeyExchangeException {
        // server side
        // Step two..
        // 1. The server reads in the clients public key.
        // 2. The clients signing certificate.
        // 3. A signature.
        //
        // Note that the signature is composed of:
        //    Client public key;
        //    Concatenation of Clients random and Server random;
        //    Clients signing certificate.
        //
        // By including the shared randoms we effectively make
        // this exchange unique each time a session is started.
        // This would stop an attacker from replaying a previously
        // recorded exchange.
        //
        Session session = this.sessions.get(new Long(sessionID));

        if (session == null) {
            throw new KeyExchangeException("Session not started");
        }

        try {
            // System.out.println("Server: Reading public key + cert + sig from client" + sessionID);
            // setting distant OA proxy
            // session.distantSecureEntity = distantSecurityEntity;
            // Read public key
            //
            // Now we must take in the encoded public key and
            // use a KeyFactory to turn it back into a Key Object.
            //
            X509EncodedKeySpec key_spec = new X509EncodedKeySpec(pub_key);
            KeyFactory key_fact = KeyFactory.getInstance("RSA", "BC");
            session.distantOAPublicKey = key_fact.generatePublic(key_spec); // Generate the public key.

            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            // System.out.println("certif :" + cert);
            session.distantOACertificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(
                        cert)); // Convert.

            Signature sig = null;

            sig = Signature.getInstance("MD5withRSA", "BC"); // Set up Signer.
            sig.initVerify(session.distantOAPublicKey); // Initialize with clients signing certificate.
            sig.update(session.cl_rand); // Incorporate client random.
            sig.update(session.se_rand); // Incorporate server random.
            sig.update(pub_key); // Incorporate encoded public key.
            sig.update(cert); // Incorporate encoded certificate.

            if (!sig.verify(signature)) {
                System.out.println(session);
                logger.warn("Signature failed on Public key exchange data unit");
                throw new Exception(
                    "Signature failed on Public key exchange data unit");
            } else {
                //    System.out.println("Server: Client PDU signature passed");
            }

            //   System.out.println("Server: Sending my public key + cert + sig to client.");
            //
            // Send server public key to client.
            //
            //
            // Set up signer.
            //
            sig.initSign(this.getPrivateKey());
            sig.update(session.cl_rand);
            sig.update(session.se_rand);

            //
            // Get my public key (for encryption) as a byte array.
            //
            byte[] my_pub = this.getPublicKey().getEncoded();

            //
            // Get my certificate (for sig validation and auth) as
            // a byte array.
            //
            byte[] my_cert = this.getCertificateEncoded();
            sig.update(my_pub);
            sig.update(my_cert);

            byte[][] result = new byte[4][];
            result[0] = this.getPublicKey().getEncoded();
            result[1] = this.getCertificateEncoded();
            signature = sig.sign();
            result[2] = signature;

            // return the results to the client
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new KeyExchangeException(e.toString());
        }
    }

    public static String displayByte(byte[] tab) {
        String s = "";

        for (int i = 0; i < tab.length; i++) {
            s += tab[i];
        }

        return s;
    }

    /**
     * Method secretKeyExchange. exchange secret between objects
     * @param sessionID the session
     * @param aesKey the private key
     * @param iv
     * @param macKey the MAC key
     * @param lockData
     * @param signatur signature of aesKey,iv, macKey and lockData
     * @return byte[][]
     */
    public byte[][] secretKeyExchange(long sessionID, byte[] aesKey, byte[] iv,
        byte[] macKey, byte[] lockData, byte[] signatur) {
        byte[][] result = new byte[5][];

        try {
            Session session = this.sessions.get(new Long(sessionID));

            if (session == null) {
                return result;
            }

            // Part 3: The Secret Exchange..
            //
            // 1. Read SecretKey encrypted with RSA.
            // 2. Read IV encrypted with RSA.
            // 3. Read HMAC key encrypted with RSA.
            // 4. Read in signature.
            //
            // NOTE: No need to send cert again. Certificate
            // sent in previous stage.
            //
            // Cipher aes_lock = Cipher.getInstance("AES/CBC/PKCS7Padding");
            //
            // Read in secret key .
            //
            byte[] clientAESKeyEncoded = aesKey;

            //
            // Read in IV
            //
            byte[] clientIVEncoded = iv;

            //
            // Read in HMAC key.
            //
            byte[] clientHMACKeyEncoded = macKey;

            //
            // Read in lock
            //
            byte[] clientLockData = lockData;

            //
            // Validate message against client's signing certificate
            // exchanged in the previous stage.
            //
            // But first we need to decrypt the lock data to incorperate into the exchange.
            //
            session.rsa_eng.init(Cipher.DECRYPT_MODE, this.getPrivateKey(),
                session.sec_rand);

            SecretKey sk = new SecretKeySpec(session.rsa_eng.doFinal(
                        clientAESKeyEncoded), "AES");

            IvParameterSpec ivspec = new IvParameterSpec(session.rsa_eng.doFinal(
                        clientIVEncoded));

            session.se_cipher.init(Cipher.DECRYPT_MODE, sk, ivspec,
                session.sec_rand);

            Signature sig = Signature.getInstance("MD5withRSA", "BC");

            sig.initVerify(session.distantOACertificate);

            sig.update(session.cl_rand);
            sig.update(session.se_rand);
            sig.update(clientAESKeyEncoded);
            sig.update(clientIVEncoded);
            sig.update(clientHMACKeyEncoded);
            byte[] lock = session.se_cipher.doFinal(lockData);
            sig.update(lock);

            if (!sig.verify(signatur)) {
                throw new KeyExchangeException(
                    "(Server) :Signature failed on Public key exchange data unit");
            }

            //
            // Now we can set up a Cipher instance that will decrypt
            // data sent from the client.
            //
            session.cl_aes_key = new SecretKeySpec(session.rsa_eng.doFinal(
                        clientAESKeyEncoded), "AES");
            session.cl_iv = new IvParameterSpec(session.rsa_eng.doFinal(
                        clientIVEncoded));
            session.cl_cipher.init(Cipher.DECRYPT_MODE, session.cl_aes_key,
                session.cl_iv);

            //
            // Set up the MAC to validate data sent from the client
            // side.
            //
            session.cl_mac_enc = session.rsa_eng.doFinal(clientHMACKeyEncoded);
            session.cl_hmac_key = new SecretKeySpec(session.cl_mac_enc, "AES");
            session.cl_mac.init(session.cl_hmac_key);

            //
            // Now send my secrets back to client encrypted with
            // public key exchanged by client.
            //
            //
            // Generate my secrets.
            //
            KeyGenerator key_gen = KeyGenerator.getInstance("AES", "BC");
            key_gen.init(192, session.sec_rand);
            session.se_aes_key = key_gen.generateKey();

            key_gen.init(160, session.sec_rand);
            session.se_hmac_key = key_gen.generateKey();

            // initialization of IV
            session.se_iv = new IvParameterSpec(new byte[16]);

            session.se_cipher.init(Cipher.ENCRYPT_MODE, session.se_aes_key,
                session.se_iv, session.sec_rand);

            byte[] my_iv = session.se_cipher.getIV();
            session.se_iv = new IvParameterSpec(my_iv);

            session.se_mac.init(session.se_hmac_key);
            clientLockData = new byte[24];
            session.sec_rand.nextBytes(clientLockData);

            //
            // Set up RSA for encryption.
            //
            sig.initSign(this.getPrivateKey());
            sig.update(session.cl_rand);
            sig.update(session.se_rand);
            session.rsa_eng.init(Cipher.ENCRYPT_MODE,
                session.distantOAPublicKey, session.sec_rand);

            //
            // Encrypt and send AES key.
            //
            result[0] = session.rsa_eng.doFinal(session.se_aes_key.getEncoded());
            sig.update(result[0]);

            //
            // Encrypt and send IV for cipher.
            //
            result[1] = session.rsa_eng.doFinal(my_iv);
            sig.update(result[1]);

            //
            // Encrypt and send MAC key..
            //
            result[2] = session.rsa_eng.doFinal(session.se_hmac_key.getEncoded());
            sig.update(result[2]);

            //
            // Encrypt and send LOCK data..
            //
            session.se_cipher.init(Cipher.ENCRYPT_MODE, session.se_aes_key,
                new IvParameterSpec(my_iv), session.sec_rand);
            result[3] = session.se_cipher.doFinal(clientLockData);
            sig.update(clientLockData); // Incorporate plain text into signature.

            result[4] = sig.sign();

            //
            // Now we have finished exchanging symmetric keys we
            // can set up our PDU generators to send encrypted
            // messages.
            //
            session.setSessionValidated(true);
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (KeyExchangeException e) {
            e.printStackTrace();
        }

        return result;
    }

    // implements Serializable
    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException {
        //	privateKeyEncoded = privateKey.getEncoded();
        try {
            if (this.keyStore != null) {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                this.keyStore.store(bout, "ha".toCharArray());

                this.encodedKeyStore = bout.toByteArray();
                this.keyStore = null;
                bout.close();
            }
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        logger = ProActiveLogger.getLogger(Loggers.SECURITY);

        this.randomLongGenerator = new RandomLongGenerator();

        if (this.encodedKeyStore != null) {
            try {
                this.keyStore = KeyStore.getInstance("PKCS12", "BC");
                this.keyStore.load(new ByteArrayInputStream(
                        this.encodedKeyStore), "ha".toCharArray());
                //   certificate = (X509Certificate) keyStore.getCertificate(SecurityConstants.KEYSTORE_ENTITY_PATH);
                this.encodedKeyStore = null;
            } catch (KeyStoreException e) {
                // TODOSECURITYSECURITY Auto-generated catch block
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                // TODOSECURITYSECURITY Auto-generated catch block
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                // TODOSECURITYSECURITY Auto-generated catch block
                e.printStackTrace();
            } catch (CertificateException e) {
                // TODOSECURITYSECURITY Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODOSECURITYSECURITY Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public long getSessionIDTo(X509Certificate cert) {

        /*
           Object o;
           Object o1;
           o = o1 = null;
           if (myBody instanceof BodyImpl) {
               o1 = ((BodyImpl) myBody).getReifiedObject();
               if (o1 instanceof Flower) {
                   o = ((Flower) o1).getName();
               } else {
                   o = o1;
               }
           }
         */

        //	System.out.println(o + "----------------------");
        //		System.out.println(o + "Source :" + certificate.getSubjectDN());
        //  	System.out.println(o + "Target :" + cert.getSubjectDN());
        Session session = null;
        if (this.sessions == null) {
            ProActiveLogger.getLogger(Loggers.SECURITY_CRYPTO)
                           .debug("sessions field is null");
            return 0;
        }

        for (Enumeration e = this.sessions.elements(); e.hasMoreElements();) {
            session = (Session) e.nextElement();

            /*   System.out.println("-----------------\nsession " + session);
               System.out.println("session distantBody " + session.distantOACertificate.getSubjectDN());
               System.out.println("distantBodyCertificate " + this.certificate.getSubjectDN());
               System.out.println("-----------------\n");
             */
            if (session != null) {
                //			System.out.println(o + "tested :" + session.distantOACertificate.getSubjectDN());
                if ((cert != null) && (session.distantOACertificate != null) &&
                        cert.equals(session.distantOACertificate)) {
                    //     logger.info("found an already initialized session" + session);
                    //certificate.equals(session.distantOACertificate);
                    //				System.out.println(o+"=====yes========");
                    return session.sessionID;
                }
            }
        }

        //	System.out.println("=======no======");

        /* We didn't find a session */
        return 0;
    }

    /**
     * Method getPublicKey.
     * @return PublicKey the public key of the active object
     */
    public PublicKey getPublicKey() {
        return getCertificate().getPublicKey();
    }

    //    /**
    //     *
    //     */
    //    public void setParentCertificate(X509Certificate certificate) {
    //        parentCertificate = certificate;
    //    }
    public Hashtable<Long, String> getOpenedConnexion() {
        Hashtable<Long, String> table = null;
        if (this.sessions == null) {
            return table;
        }

        table = new Hashtable<Long, String>();

        for (Enumeration e = this.sessions.keys(); e.hasMoreElements();) {
            Long l = (Long) e.nextElement();
            table.put(l, l.toString());
        }

        return table;
    }

    /**
     * allows to set the name of the current virtual node
     * @param string the name of the current Virtual Node if any
     */
    public void setVNName(String string) {
        //  System.out.println("setting vn node name " + string);
        this.VNName = string;

        //policyServer.setVNName(string);
    }

    /**
     * @return virtual node name where object has been created
     */
    public String getVNName() {
        return this.VNName;
    }

    /**
     * @return policy server
     */
    public PolicyServer getPolicyServer() {
        return this.policyServer;
    }

    /**
     * This method returns the entity certificate as byte array. It should be used
     * when the certificate will be serialized. Indeed, certificates like others java.security.* objects
     * are not serializabled object
     * @return certificate as byte array
     */
    public byte[] getCertificateEncoded() {
        try {
            X509Certificate cert = this.getCertificate();
            if (cert != null) {
                return cert.getEncoded();
            }
            return null;
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Set entity policy server
     * @param policyServer
     */
    public void setPolicyServer(PolicyServer policyServer) {
        this.policyServer = policyServer;
        //this.keyStore = policyServer.getKeyStore();
    }

    /**
     * @return entities that inforces security policy on the object
     */
    public ArrayList<Entity> getEntities() {
        Entity entity = null;
        ArrayList<Entity> a = new ArrayList<Entity>();
        switch (this.type) {
        case SecurityConstants.ENTITY_TYPE_OBJECT:
            //	Entity entity = new
            break;
        case SecurityConstants.ENTITY_TYPE_NODE:
            entity = new EntityVirtualNode(this.getCertificate().getSubjectDN()
                                               .toString().substring(3),
                    this.policyServer.getApplicationCertificate(),
                    this.getCertificate());
            break;
        default:
            break;
        }

        if (entity != null) {
            a.add(entity);
        }

        if (this.parent != null) {
            try {
                ArrayList<Entity> parentEntities = this.parent.getEntities();
                if (parentEntities == null) {
                    return null;
                }
                int parentSize = parentEntities.size();
                for (int i = 0; i < parentSize; i++) {
                    a.add(parentEntities.get(i));
                }
            } catch (SecurityNotAvailableException e) {
                // forget it
                return null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return a;
    }

    public Session getSession(long id) {
        return this.sessions.get(new Long(id));
    }

    public X509Certificate[] getMyCertificateChain() {
        try {
            return (X509Certificate[]) this.policyServer.getKeyStore()
                                                        .getCertificateChain(SecurityConstants.KEYSTORE_ENTITY_PATH);
        } catch (KeyStoreException e) {
            return null;
        }
    }

    public SecurityEntity getParent() {
        return this.parent;
    }

    public void setParent(SecurityEntity parent) {
        this.parent = parent;
    }

    public ProActiveSecurityManager generateSiblingCertificate(
        String siblingName) {
        ProActiveSecurityManager siblingPSM = new ProActiveSecurityManager();

        try {
            siblingPSM.setPolicyServer((PolicyServer) this.policyServer.clone());
            siblingPSM.generateEntityCertificate(siblingName);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return siblingPSM;
    }

    protected void generateEntityCertificate(String siblingName) {
        siblingName = "CN=" + siblingName;
        try {
            KeyPair siblingKeyPair = KeyTools.genKeys(1024);
            X509Certificate cert;

            ProActiveLogger.getLogger(Loggers.SECURITY_MANAGER)
                           .debug("generate sibling security manager for " +
                siblingName);

            cert = CertTools.genCert(siblingName, 65 * 24 * 360L, null,
                    siblingKeyPair.getPrivate(), siblingKeyPair.getPublic(),
                    true,
                    ((X509Certificate) this.policyServer.getKeyStore()
                     .getCertificate(SecurityConstants.KEYSTORE_APPLICATION_PATH)).getSubjectDN()
                     .toString(),
                    (PrivateKey) this.policyServer.getKeyStore()
                                                  .getKey(SecurityConstants.KEYSTORE_APPLICATION_PATH,
                        null),
                    this.policyServer.getKeyStore()
                                     .getCertificate(SecurityConstants.KEYSTORE_APPLICATION_PATH)
                                     .getPublicKey());

            this.keyStore = KeyTools.createP12(SecurityConstants.KEYSTORE_ENTITY_PATH,
                    siblingKeyPair.getPrivate(), cert,
                    this.policyServer.getKeyStore()
                                     .getCertificateChain(SecurityConstants.KEYSTORE_APPLICATION_PATH));
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
            //        } catch (UnrecoverableEntryException e) {
            //            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ProActiveSecurityManager getProActiveSecurityManager() {
        return this;
    }
}
