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

package pgrid.util;

/**
 * This class converts bytes to binary strings, to hexadecimal strings, and hexadecimal strings to bytes.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0 2003/04/15
 */
public class Converter {

	/**
	 * The hexadecimal characters.
	 */
	private static final char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	/**
	 * Create a new <code>Converter</code>.
	 */
	protected Converter() {
		// do nothing
	}

	/**
	 * Returns a binary string represantation of an array of bytes.
	 *
	 * @param data the bytes to convert.
	 * @return a binary string represantation of the delivered bytes.
	 */
	public static String bytesToBinaryString(byte[] data) {
		if (data == null)
			return "null";
		if (data.length == 0)
			return "";

		StringBuffer suid = new StringBuffer();
		int range = Math.abs((new Byte(Byte.MAX_VALUE)).intValue()) + Math.abs((new Byte(Byte.MIN_VALUE)).intValue()) + 1;
		int dec;
		StringBuffer binary;
		int maxdigits = (Integer.toBinaryString(range - 1)).length();

		for (int i = 0; i < data.length; i++) {
			dec = new Byte(data[i]).intValue();
			if (dec < 0) {
				dec = range + dec;
			}
			binary = new StringBuffer(Integer.toBinaryString(dec));
			for (int j = binary.length(); j < maxdigits; j++) {
				suid.append('0');
			}
			suid.append(binary);
		}
		return suid.toString();
	}

	/**
	 * Returns a binary string represantation of an array of bytes.
	 *
	 * @param data   the bytes to convert.
	 * @param offset where to start to convert.
	 * @param len    the number of bytes to convert.
	 * @return a binary string represantation of the delivered bytes.
	 */
	public static String bytesToBinaryString(final byte[] data, final int offset, final int len) {
		final byte[] buffer = new byte[len];
		System.arraycopy(data, offset, buffer, 0, len);
		return bytesToBinaryString(buffer);
	}

	/**
	 * Returns a string represantation of an array of bytes. Each byte is
	 * representated as a hexadecimal char. The first byte is given by the
	 * <TT>offset</TT>, and the length by <TT>len</TT>.
	 *
	 * @param data   the bytes to convert.
	 * @param offset where to start to convert.
	 * @param len    the number of bytes to convert.
	 * @return a string represantation of the delivered bytes.
	 */
	public static String bytesToHexString(final byte[] data, final int offset, final int len) {
		final StringBuffer buf = new StringBuffer(len * 2);
		bytesToHexString(data, offset, len, buf);
		return buf.toString();
	}

	/**
	 * Converts an array of bytes to a string represantation with hexadecimal
	 * chars. The bytes are delivered with a certain offset, where to start,
	 * and the length, where to stop. The generated string is returned in the
	 * <TT>StringBuffer</TT>.
	 *
	 * @param data   the bytes to convert.
	 * @param offset where to start to convert.
	 * @param len    the number of bytes to convert.
	 * @param outbuf the string represantation of the delivered bytes.
	 */
	public static void bytesToHexString(final byte[] data, final int offset, final int len, final StringBuffer outbuf) {
		final int end = offset + len;
		for (int i = offset; i < end; i++) {
			outbuf.append(HEX_CHAR[(data[i] >> 4) & 0xF]);
			outbuf.append(HEX_CHAR[data[i] & 0xF]);
		}
	}

	/**
	 * Converts a hexadecimal string in an array of bytes.
	 *
	 * @param hexStr the hex string to convert.
	 * @return the produced bytes.
	 */
	public static byte[] hexStringToBytes(final String hexStr) {
		final int len = hexStr.length();
		final byte[] bytes = new byte[(len + 1) / 2];
		try {
			int j = 0;
			for (int i = 0; i < len; i += 2) {
				final String str = hexStr.substring(i, i + 2);
				bytes[j++] = (Integer.valueOf(str, 16)).byteValue();
			}
		} catch (StringIndexOutOfBoundsException e) {
			return null;
		} catch (NumberFormatException e) {
			return null;
		}
		return bytes;
	}

}