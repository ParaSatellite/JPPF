/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.example.dataencryption.helper;

import java.io.*;
import java.security.KeyStore;

import javax.crypto.*;

import org.jppf.utils.base64.*;
import org.jppf.utils.streams.StreamUtils;

/**
 * This class provides helper methods to provide a cipher and its parameters,
 * and create and perform operations on a keystore.
 * @author Laurent Cohen
 */
public final class Helper {
  /**
   * The keystore password.
   * This variable will be assigned the password value in clear,
   * after it has been read from a file and decoded from Base64 encoding.
   */
  private static char[] some_chars = null;
  /**
   * Cipher algorithm name. See <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher">possible values</a>.
   */
  private static final String ALGO_NAME = "AES";
  /**
   * Cipher algorithm mode. See <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher">possible values</a>.
   */
  private static final String ALGO_MODE = "CBC";
  /**
   * Cipher algorithm padding. See <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher">possible values</a>.
   */
  private static final String ALGO_PADDING = "PKCS5Padding";
  /**
   * Full trnasformation specification used when instantiating a {@link Cipher}.
   */
  private static final String TRANSFORMATION = ALGO_NAME + "/" + ALGO_MODE + "/" + ALGO_PADDING;

  /**
   * Instantiation of this class is not permitted.
   */
  private Helper() {
  }

  /**
   * Main entry point, creates the keystore.
   * The keystore is then included in the jar file generated by the script.<br/>
   * The keystore password, passed as argument, is encoded in Base64 form, then stored
   * into a file that is also included in the jar file. This ensures that no password
   * in clear is ever deployed.
   * @param args the first argument must be the keystore password in clear.
   */
  public static void main(final String... args) {
    try {
      generateKeyStore(args[0]);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Generate a keystore with a default password.
   * @param pwd default keystore password
   * @throws Exception if any error occurs.
   */
  private static void generateKeyStore(final String pwd) throws Exception {
    final byte[] passwordBytes = pwd.getBytes();
    // encode the password in Base64
    final byte[] encodedBytes = Base64Encoding.encodeBytesToBytes(passwordBytes);
    // store the encoded password to a file
    FileOutputStream fos = new FileOutputStream(getPasswordFilename());
    try {
      fos.write(encodedBytes);
      fos.flush();
    } finally {
      fos.close();
    }
    final char[] password = pwd.toCharArray();
    final KeyStore ks = KeyStore.getInstance(getProvider());
    // create an empty keystore
    ks.load(null, password);
    // generate the initial secret key
    final KeyGenerator gen = KeyGenerator.getInstance(getAlgorithm());
    final SecretKey key = gen.generateKey();
    // save the key in the keystore
    ks.setKeyEntry(getKeyAlias(), key, password, null);
    // save the keystore to a file
    fos = new FileOutputStream(getKeystoreFilename());
    try {
      ks.store(fos, password);
    } finally {
      fos.close();
    }
  }

  /**
   * Get the secret key used for encryption/decryption.
   * In this method, the secret key is read from a location in the classpath.
   * This is definitely unsecure, and for demonstration purposes only.
   * The secret key should be stored in a secure location such as a key store.
   * @return a SecretKey instance.
   * @throws Exception if any error occurs.
   */
  public static synchronized SecretKey retrieveSecretKey()  throws Exception {
    // get the keystore password
    final char[] password = Helper.getPassword();
    final ClassLoader cl = Helper.class.getClassLoader();
    final InputStream is = cl.getResourceAsStream(Helper.getKeystoreFolder() + Helper.getKeystoreFilename());
    final KeyStore ks = KeyStore.getInstance(Helper.getProvider());
    // load the keystore
    ks.load(is, password);
    // get the secret key from the keystore
    return (SecretKey) ks.getKey(Helper.getKeyAlias(), password);
  }

  /**
   * Get the keystore password.
   * @return the password as a char[].
   */
  public static char[] getPassword() {
    if (some_chars == null) {
      try {
        final String path = getKeystoreFolder() + getPasswordFilename();
        final InputStream is = Helper.class.getClassLoader().getResourceAsStream(path);
        // read the encoded password
        final byte[] encodedBytes = StreamUtils.getInputStreamAsByte(is);
        // decode the password from Base64
        final byte[] passwordBytes = Base64Decoding.decode(encodedBytes);
        some_chars = new String(passwordBytes).toCharArray();
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
    return some_chars;
  }

  /**
   * Get the password file name.
   * @return the password file name.
   */
  public static String getPasswordFilename() {
    return "password.pwd";
  }

  /**
   * Get the keystore file name.
   * @return the keystore file name.
   */
  public static String getKeystoreFilename() {
    return "keystore.ks";
  }

  /**
   * The folder in which the keystore and password file will be in the jar file.
   * @return the folder name as a string.
   */
  public static String getKeystoreFolder() {
    return "org/jppf/example/dataencryption/helper/";
  }

  /**
   * Get the key alias.
   * @return the key alias.
   */
  public static String getKeyAlias() {
    return "secretKeyAlias";
  }

  /**
   * Get the cryptographic provider, or keystore type.
   * @return the provider name.
   */
  public static String getProvider() {
    // pkcs12 and jceks are the only ootb providers that allow storing a secret key
    //return "PKCS12";
    return "JCEKS";
  }

  /**
   * Get the name of the cryptographic algorithm used to generate secret keys.
   * @return the algorithm name as a string.
   */
  public static String getAlgorithm() {
    return ALGO_NAME;
  }

  /**
   * Get the name of the cryptographic transformation used when encrypting or decrypting data.
   * @return the transformation as a string.
   */
  public static String getTransformation() {
    return TRANSFORMATION;
  }
}
