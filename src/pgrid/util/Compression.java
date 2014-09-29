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

import java.io.*;
import java.util.zip.*;

import pgrid.Constants;

/**
 * This class compresses and decompresses data using the Java {@link java.util.zip.Deflater} and
 * {@link java.util.zip.Inflater}.
 *
 * @author @author <a href="mailto:Roman Schmidt <Roman.Schmidt@epfl.ch>">Roman Schmidt</a>
 * @version 1.0 2003/04/15
 * @see java.util.zip.Deflater
 * @see java.util.zip.Inflater
 */
public class Compression {

	/**
	 * Creates a new <code>Compression</code>.
	 */
	protected Compression() {
		// do nothing
	}

	/**
	 * Compresses the delivered data with the default level, and returns it.
	 *
	 * @param data   the byte array to compress.
	 * @param offset the first byte to compress.
	 * @param len    the amount of bytes to compress.
	 * @return the compressed bytes.
	 */
	public static byte[] compress(byte[] data, int offset, int len) {
		return compress(data, offset, len, Deflater.DEFAULT_COMPRESSION);
	}

	/**
	 * Compresses the delivered data, and returns it.
	 *
	 * @param data        the byte array to compress.
	 * @param offset      the first byte to compress.
	 * @param len         the amount of bytes to compress.
	 * @param compression the compression level (levels of {@link java.util.zip.Deflater}).
	 * @return the compressed bytes.
	 */
	public static byte[] compress(byte[] data, int offset, int len, int compression) {
		if (data == null)
			return null;
		if ((offset + len) > data.length)
			return null;

		Deflater compresser = new Deflater(compression);
		compresser.setInput(data, offset, len);
		compresser.finish();
		byte[] buffer = new byte[1024];
		int bufferPos = 0;
		while (!compresser.finished()) {
			if (bufferPos >= buffer.length) {
				final byte[] tmp = buffer;
				buffer = new byte[buffer.length + len];
				System.arraycopy(tmp, 0, buffer, 0, tmp.length);
			}
			final int tmpLen = compresser.deflate(buffer, bufferPos, buffer.length - bufferPos);
			bufferPos += tmpLen;
		}
		byte[] ret = new byte[bufferPos];
		System.arraycopy(buffer, 0, ret, 0, bufferPos);
		return ret;
	}

	/**
	 * Decompresses the delivered data, and returns it.
	 *
	 * @param data   the byte array to decompress.
	 * @param offset the first byte to decompress.
	 * @param len    the amount of bytes to decompress.
	 * @return the decompressed bytes.
	 * @throws DataFormatException
	 */
	public static byte[] decompress(byte[] data, int offset, int len) throws DataFormatException {
		if (data == null)
			return null;
		if ((offset + len) > data.length)
			return null;
		Inflater decompresser = new Inflater();
		decompresser.setInput(data, offset, len);
		byte[] buffer = new byte[1024];
		int bufferPos = 0;
		while (decompresser.getRemaining() > 0) {
			if (bufferPos >= buffer.length) {
				byte[] tmp = buffer;
				buffer = new byte[buffer.length + len];
				System.arraycopy(tmp, 0, buffer, 0, tmp.length);
			}
			int tmpLen = decompresser.inflate(buffer, bufferPos, buffer.length - bufferPos);
			bufferPos += tmpLen;
		}

		decompresser.end();
		byte[] ret = new byte[bufferPos];
		System.arraycopy(buffer, 0, ret, 0, bufferPos);
		return ret;
	}
	
	public static void compressFile(String source, String dest){
		long time = System.currentTimeMillis();
		int BUFFER = 2048;
		byte[] buff = new byte[BUFFER];
		BufferedInputStream origin = null;
		ZipOutputStream zout = null;
		try {
			FileInputStream fis = new FileInputStream(Constants.CSV_DIR+source);
			origin = new BufferedInputStream(fis,BUFFER);
			zout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(Constants.CSV_DIR+dest)));
			ZipEntry entry = new ZipEntry(source);
			zout.putNextEntry(entry);
			int count;
			while((count = origin.read(buff, 0, BUFFER))!=-1){
				zout.write(buff,0,count);
			}
//			zout.flush();
			zout.closeEntry();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				origin.close();
				zout.finish();
				zout.close();
			} catch (IOException e) {
				e.printStackTrace ();
			}
			Constants.LOGGER.finest("Compression took : "+ (System.currentTimeMillis() -time));
			Constants.LOGGER.finest("TRANSMIT FILE SIZE : "+ new File(Constants.CSV_DIR+dest).length());
		}
	}
	public static void decompressFile(String source, String destn){
		long time = System.currentTimeMillis();
		
		int BUFFER = 2048;
		try {

			BufferedOutputStream dest = null;
			FileInputStream fis = new FileInputStream(source);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
			ZipEntry entry = zis.getNextEntry();

			int count;
			byte data[] = new byte[BUFFER];

			// write the files to the disk
			FileOutputStream fos = new FileOutputStream(Constants.CSV_DIR+entry.getName());
			dest = new BufferedOutputStream(fos, BUFFER);
			while ((count = zis.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, count);
			}
			dest.flush();
			dest.close();
			zis.close();
		} catch(Exception e) {
			e.printStackTrace();
			Constants.LOGGER.finest("Failed to decompress :"+source);
		}		
		Constants.LOGGER.finest("Decompression took : "+ (System.currentTimeMillis() -time));
		Constants.LOGGER.finest("RECEIVED FILE SIZE : "+ new File(source).length());
	}

}