/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2009 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@ow2.org
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
 * $PROACTIVE_INITIAL_DEV$
 */
package org.ow2.proactive.authentication.crypto;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.Cipher;


/**
 * Asymmetric cryptography utilities for KeyPair generation, encryption and decryption
 * <p>
 * Refer to the Java Cryptography Extension Reference Guide
 * {@link http://java.sun.com/j2se/1.5.0/docs/guide/security/jce/JCERefGuide.html} to
 * determine which parameters are best for key algorithm, key size and cipher;
 * although "RSA", 1024 and "RSA/ECB/PKCS1Padding" should be good enough in most
 * cases.
 * 
 * @author The ProActive Team
 * @since ProActive Scheduling 1.1
 * 
 */
public class KeyPairUtil {

    /**
     * Generates a pair of public and private keys
     * 
     * @param algorithm algorithm used for key generation, ie RSA
     * @param size size of the generated key, must be power of 2 and greater than 512
     * @param privPath path to file to which the generated private key will be saved
     * @param pubPath path to file to which the generated public key will be saved
     * @throws KeyException key generation or saving failed
     */
    public static void generateKeyPair(String algorithm, int size, String privPath, String pubPath)
            throws KeyException {
        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyException("Cannot initialize keypair generator", e);
        }

        SecureRandom random = new SecureRandom();
        keyGen.initialize(size, random);

        KeyPair keyPair = keyGen.generateKeyPair();

        PrivateKey privKey = keyPair.getPrivate();
        PublicKey pubKey = keyPair.getPublic();

        FileOutputStream out = null;

        try {
            out = new FileOutputStream(new File(privPath));
            out.write(privKey.getEncoded());
            out.close();
        } catch (Exception e) {
            throw new KeyException("Cannot write private key to disk", e);
        }

        try {
            out = new FileOutputStream(new File(pubPath));
            out.write((algorithm + "\n").getBytes());
            out.write((size + "\n").getBytes());
            out.write(pubKey.getEncoded());
            out.close();
        } catch (Exception e) {
            throw new KeyException("Cannot write public key to disk", e);
        }
    }

    /**
     * Encrypt a message using asymmetric keys
     * 
     * @param pubKey public key used for encryption
     * @param cipherParams cipher parameters: transformations (ie RSA/ECB/NoPadding)
     * @param message the message to encrypt
     * @return the encrypted message
     * @throws KeyException encryption failed, public key recovery failed
     */
    public static byte[] encrypt(PublicKey pubKey, int size, String cipherParams, byte[] message)
            throws KeyException {

        if (message.length * 8 > size) {
            throw new KeyException("Cannot encrypt " + message.length + "B with a " + size +
                "b key... Try with a " + npot(message.length) * 8 + "b key.");
        }

        Cipher ciph = null;
        try {
            ciph = Cipher.getInstance(cipherParams);
            ciph.init(Cipher.ENCRYPT_MODE, pubKey);
        } catch (Exception e) {
            throw new KeyException("Could not initialize cipher", e);
        }

        byte[] res = null;
        try {
            res = ciph.doFinal(message);
        } catch (Exception e) {
            throw new KeyException("Could not encrypt message.", e);
        }

        return res;
    }

    /**
     * Decrypt a message using asymmetric keys
     * 
     * @param algorithm algorithm used for key generation
     * @param privKey Private key used for decryption
     * @param cipherParams cipher parameters: transformations (ie RSA/ECB/NoPadding)
     * @param message the encrypted message
     * @return the decrypted message
     * @throws KeyException private key recovery failed, decryption failed
     */
    public static byte[] decrypt(String algorithm, PrivateKey privKey, String cipherParams, byte[] message)
            throws KeyException {

        Cipher ciph = null;
        try {
            ciph = Cipher.getInstance(cipherParams);
            ciph.init(Cipher.DECRYPT_MODE, privKey);
        } catch (Exception e) {
            throw new KeyException("Could not initialize cipher", e);
        }

        byte[] res = null;
        try {
            res = ciph.doFinal(message);
        } catch (Exception e) {
            throw new KeyException("Could not descrypt message.", e);
        }

        return res;
    }

    /**
     * Next Power Of Two
     * 
     * @param n some integer
     * @return the lowest power of two greater than <code>n</code>
     */
    private static int npot(int n) {
        if (n <= 0)
            return 1;
        int pow = 2;
        while (n > pow) {
            pow *= 2;
        }

        return pow;
    }
}
