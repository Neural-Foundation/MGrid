/**
 *
 * Copyright (c) 2002-2008 The P-Grid Team, All Rights Reserved.
 *
 * This file is part of the P-Grid package.
 * P-Grid homepage: http://www.p-grid.org/
 *  
 * The P-Grid package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The P-Grid package is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the P-Grid package.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package pgrid.util.guid;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * This class creates unique IDs which are used to uniquely designate
 * Gridella objects.
 * <B>Remark:</B> java.security.SecureRandom was not used since it is only
 * available with JDK 1.2.
 * This class implements the <code>Singleton</code> pattern as defined by
 * Gamma et.al. As there could only exist one instance of this class, other
 * clients must use the <code>sharedInstance</code> function to use this class.
 *
 * @author <a href="mailto:Renault John <Renault.John@epfl.ch>">Renault John </a>
 * @author <a href=mailto:"manfred.hauswirth@epfl.ch">Manfred Hauswirth</a>
 * @version 1.0.0
 */
class GUIDGenerator {

	/**
	 * The reference to the only instance of this class (Singleton
	 * pattern). This differs from the C++ standard implementation by Gamma
	 * et.al. since Java ensures the order of static initialization at runtime.
	 *
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	private static GUIDGenerator mSharedInstance = new GUIDGenerator();

	/**
	 * The digester used to generate the unique ID.
	 */
	private MessageDigest md = null;

	/**
	 * Lock object
	 */
	private final Object mLock = new Object();

	/**
	 * the digest algorithm for generating the unique ID.
	 */
	private String algorithm = null;

	/**
	 * the random number generator.
	 */
	private SecureRandom rnd = new SecureRandom();

	/**
	 * prefix
	 */
	private byte[] host = null;

	/**
	 * Counters
	 */
	private WeakHashMap mCounters = new WeakHashMap();

	/**
	 * The constructor must be protected to ensure that only subclasses can
	 * call it and that only one instance can ever get created. A client that
	 * tries to instantiate GUIDGenerator directly will get an error at compile-time.
	 */
	private GUIDGenerator() {
		algorithm = "SHA";
		rnd.setSeed(System.currentTimeMillis());
	}

	/**
	 * Called by the public generation methods; generates the seed and
	 * digests it (using the algorithm defined in this.algorithm);
	 * the digest is the unique ID.
	 * The seed is a random string of the form
	 * <host>/<ip adress><date><random number> and generated anew with
	 * every call.
	 *
	 * @param seed to be used.
	 * @return the newly created byte array.
	 * @throws java.security.NoSuchAlgorithmException
	 *          if the algorithm is not available
	 *          in the caller's environment.
	 */
	private byte[] _generate(byte[] seed) throws NoSuchAlgorithmException {
		md = MessageDigest.getInstance(algorithm);
		byte[] result = null;
		synchronized(mLock) {
			md.update(seed);
			result = md.digest();
		}

		return result;
	}

	/**
	 * Called by the public generation methods; generates a unique ID.
	 *
	 * @return the newly created byte array.
	 */
	private byte[] _generate() {
		if (host == null) {
			host = new byte[16];
			rnd.nextBytes(host);
		}
		int counter = 0;
		int th = Thread.currentThread().hashCode();

		// increase counter
		Object c = mCounters.get(Thread.currentThread());
		if (c != null)
			counter = (((Integer)c).intValue())+1;
		else {
			// use a random number as a basis for our per thread counter. This prevent the risk of
			// having two concecutive thread with the same hashcode which would generation the
			// same GUID sequences.                            
			counter = rnd.nextInt(Short.MAX_VALUE);
		}
		
		mCounters.put(Thread.currentThread(), new Integer(counter));

		// create GUID
		byte[] hash = new byte[24];
		int l = host.length;
		System.arraycopy(host, 0, hash, 0, l);

		hash[l] = (byte)(th);
		hash[l+1] = (byte)(th >> 8);
		hash[l+2] = (byte)(th >> 16);
		hash[l+3] = (byte)(th >> 24);
		hash[l+4] = (byte)(counter);
		hash[l+5] = (byte)(counter >> 8);
		hash[l+6] = (byte)(counter >> 16);
		hash[l+7] = (byte)(counter >> 24);

		return hash;
	}

	/**
	 * Generates a new unique ID using the default algorithm.
	 *
	 * @return the newly created uniqueID bytes.
	 */
	byte[] generate() {
		return _generate();
	}

	/**
	 * Generates a new unique ID using the specified algorithm.
	 *
	 * @param seed the seed to be used to generate a GUID
	 * @return the newly created uniqueID bytes.
	 * @throws java.security.NoSuchAlgorithmException
	 *          if the algorithm is not available
	 *          in the caller's environment.
	 */
	byte[] generate(byte[] seed) {
		byte[] uid = null;
		try {
			uid = _generate(seed);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return uid;
	}

	/**
	 * Returns the current digest algorithm that is used to generate
	 * unique IDs.
	 *
	 * @return the name of the current digest algorithm. See Appendix A in
	 *         the <a href="http://java.sun.com/products/jdk/1.2/docs/guide/security/CryptoSpec.html#AppA">
	 *         Java Cryptography Architecture API Specification &
	 *         Reference</a> for information about standard algorithm
	 *         names.
	 */
	String getAlgorithm() {
		return algorithm;
	}

	/**
	 * Sets the digest algorithm to be used for further calls of generate().
	 *
	 * @param algorithm the name of the digest algorithm. See Appendix A in
	 *                  the <a href="http://java.sun.com/products/jdk/1.2/docs/guide/security/CryptoSpec.html#AppA">
	 *                  Java Cryptography Architecture API Specification &
	 *                  Reference</a> for information about standard algorithm
	 *                  names.
	 */
	void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	/**
	 * This creates the only instance of this class. This differs from the
	 * C++ standard implementation by Gamma et.al. since Java ensures the
	 * order of static initialization at runtime.
	 *
	 * @return the shared instance of Config.
	 * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip67.html">
	 *      Lazy instantiation - Balancing performance and resource usage</a>
	 */
	public static GUIDGenerator sharedInstance() {
		return mSharedInstance;
	}

}