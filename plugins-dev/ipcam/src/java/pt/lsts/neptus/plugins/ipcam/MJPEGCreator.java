/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * Aug 2, 2010
 */
package pt.lsts.neptus.plugins.ipcam;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.FileUtil;

/**
 * @author monceaux, zp
 * @see http://stackoverflow.com/questions/1446621/100-java-encoder-for-avi-animation
 */
public class MJPEGCreator {

	int width = 0;
	int height = 0;
	double framerate = 0;
	int numFrames = 0;
	boolean countFrames = true;
	File aviFile = null;
	FileOutputStream aviOutput = null;
	FileChannel aviChannel = null;

	long riffOffset = 0;
	long aviMovieOffset = 0;

	AVIIndexList indexlist = null;

	//int[] numFramesPostions = new int[] {4,5,8};
	
	public MJPEGCreator(int width, int height, double framerate) throws Exception {
		this(new File("log/ipcam/video_"+System.currentTimeMillis()/1000+".avi"), width, height, framerate);
	}
	
	public MJPEGCreator(File aviFile, int width, int height, double framerate) throws Exception {
		this(aviFile, width, height, framerate, 0);
	}
		
	public MJPEGCreator(File aviFile, int width, int height, double framerate, int numFrames) throws Exception {
		if (numFrames != 0)
			countFrames = false;
		
		this.aviFile = aviFile;
		this.width = width;
		this.height = height;
		this.framerate = framerate;
		this.numFrames = numFrames;
		aviOutput = new FileOutputStream(aviFile);
		aviChannel = aviOutput.getChannel();
		RIFFHeader rh = new RIFFHeader();
		aviOutput.write(rh.toBytes());
		aviOutput.write(new AVIMainHeader().toBytes());
		aviOutput.write(new AVIStreamList().toBytes());
		aviOutput.write(new AVIStreamHeader().toBytes());
		aviOutput.write(new AVIStreamFormat().toBytes());
		aviOutput.write(new AVIJunk().toBytes());
		aviMovieOffset = aviChannel.position();
		aviOutput.write(new AVIMovieList().toBytes());
		indexlist = new AVIIndexList();
	}

	public void addImage(File image) throws Exception {
		
		byte[] imagedata = FileUtil.getFileAsByteArray(image.getCanonicalPath());
		addImage(imagedata, imagedata.length);
	}
	
	public void addImage(byte[] imagedata, int length) throws Exception {
		byte[] fcc = new byte[] { '0', '0', 'd', 'b' };
		int useLength = length;
		long position = aviChannel.position();
		int extra = (useLength + (int) position) % 4;
		if (extra > 0)
			useLength = useLength + extra;

		indexlist.addAVIIndex((int) position, useLength);

		aviOutput.write(fcc);
		aviOutput.write(intBytes(swapInt(useLength)));		
		aviOutput.write(imagedata, 0, length);
		if (countFrames)
			numFrames++;
		if (extra > 0) {
			for (int i = 0; i < extra; i++)
				aviOutput.write(0);
		}
		imagedata = null;
	}

	public void addImage(Image image) throws Exception {
		byte[] imagedata = writeImageToBytes(image);
		addImage(imagedata, imagedata.length);
	}
	
	public void finishAVI() throws Exception {
		NeptusLog.pub().info("<###>closing avi file...");
		byte[] indexlistBytes = indexlist.toBytes();
		aviOutput.write(indexlistBytes);
		aviOutput.close();
		long size = aviFile.length();
		NeptusLog.pub().info("<###>   adding file size ("+size+") to file");
		RandomAccessFile raf = new RandomAccessFile(aviFile, "rw");
		raf.seek(4);
		raf.write(intBytes(swapInt((int) size - 8)));
		raf.seek(aviMovieOffset + 4);
		raf.write(intBytes(swapInt((int) (size - 8 - aviMovieOffset - indexlistBytes.length))));
		
		if (countFrames) {
			
			NeptusLog.pub().info("<###>   adding frame count ("+numFrames+") to file");
			raf.seek(48);
			raf.write(intBytes(swapInt((int) numFrames)));
			raf.seek(140);
			raf.write(intBytes(swapInt((int) numFrames)));
		}
		raf.close();
		NeptusLog.pub().info("<###>   done.");
		
	}


	public static int swapInt(int v) {
		return (v >>> 24) | (v << 24) | ((v << 8) & 0x00FF0000)
				| ((v >> 8) & 0x0000FF00);
	}

	public static short swapShort(short v) {
		return (short) ((v >>> 8) | (v << 8));
	}

	public static byte[] intBytes(int i) {
		byte[] b = new byte[4];
		b[0] = (byte) (i >>> 24);
		b[1] = (byte) ((i >>> 16) & 0x000000FF);
		b[2] = (byte) ((i >>> 8) & 0x000000FF);
		b[3] = (byte) (i & 0x000000FF);

		return b;
	}

	public static byte[] shortBytes(short i) {
		byte[] b = new byte[2];
		b[0] = (byte) (i >>> 8);
		b[1] = (byte) (i & 0x000000FF);

		return b;
	}

	private class RIFFHeader {
		public byte[] fcc = new byte[] { 'R', 'I', 'F', 'F' };
		public int fileSize = 0;
		public byte[] fcc2 = new byte[] { 'A', 'V', 'I', ' ' };
		public byte[] fcc3 = new byte[] { 'L', 'I', 'S', 'T' };
		public int listSize = 200;
		public byte[] fcc4 = new byte[] { 'h', 'd', 'r', 'l' };

		public RIFFHeader() {

		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(intBytes(swapInt(fileSize)));
			baos.write(fcc2);
			baos.write(fcc3);
			baos.write(intBytes(swapInt(listSize)));
			baos.write(fcc4);
			baos.close();

			return baos.toByteArray();
		}
	}

	private class AVIMainHeader {
		/*
		 * 
		 * FOURCC fcc; DWORD cb; DWORD dwMicroSecPerFrame; DWORD
		 * dwMaxBytesPerSec; DWORD dwPaddingGranularity; DWORD dwFlags; DWORD
		 * dwTotalFrames; DWORD dwInitialFrames; DWORD dwStreams; DWORD
		 * dwSuggestedBufferSize; DWORD dwWidth; DWORD dwHeight; DWORD
		 * dwReserved[4];
		 */

		public byte[] fcc = new byte[] { 'a', 'v', 'i', 'h' };
		public int cb = 56;
		public int dwMicroSecPerFrame = 0; // (1 / frames per sec) * 1,000,000
		public int dwMaxBytesPerSec = 10000000;
		public int dwPaddingGranularity = 0;
		public int dwFlags = 65552;
		public int dwTotalFrames = 0; // replace with correct value
		public int dwInitialFrames = 0;
		public int dwStreams = 1;
		public int dwSuggestedBufferSize = 0;
		public int dwWidth = 0; // replace with correct value
		public int dwHeight = 0; // replace with correct value
		public int[] dwReserved = new int[4];

		public AVIMainHeader() {
			dwMicroSecPerFrame = (int) ((1.0 / framerate) * 1000000.0);
			dwWidth = width;
			dwHeight = height;
			dwTotalFrames = numFrames;
		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(intBytes(swapInt(cb)));
			baos.write(intBytes(swapInt(dwMicroSecPerFrame)));
			baos.write(intBytes(swapInt(dwMaxBytesPerSec)));
			baos.write(intBytes(swapInt(dwPaddingGranularity)));
			baos.write(intBytes(swapInt(dwFlags)));
			//NeptusLog.pub().info("<###>Avi mainHeader offset:"+baos.size());
			baos.write(intBytes(swapInt(dwTotalFrames)));
			baos.write(intBytes(swapInt(dwInitialFrames)));
			baos.write(intBytes(swapInt(dwStreams)));
			baos.write(intBytes(swapInt(dwSuggestedBufferSize)));
			baos.write(intBytes(swapInt(dwWidth)));
			baos.write(intBytes(swapInt(dwHeight)));
			baos.write(intBytes(swapInt(dwReserved[0])));
			baos.write(intBytes(swapInt(dwReserved[1])));
			baos.write(intBytes(swapInt(dwReserved[2])));
			baos.write(intBytes(swapInt(dwReserved[3])));
			baos.close();

			return baos.toByteArray();
		}
	}

	private class AVIStreamList {
		public byte[] fcc = new byte[] { 'L', 'I', 'S', 'T' };
		public int size = 124;
		public byte[] fcc2 = new byte[] { 's', 't', 'r', 'l' };

		public AVIStreamList() {

		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(intBytes(swapInt(size)));
			baos.write(fcc2);
			baos.close();

			return baos.toByteArray();
		}
	}

	private class AVIStreamHeader {
		/*
		 * FOURCC fcc; DWORD cb; FOURCC fccType; FOURCC fccHandler; DWORD
		 * dwFlags; WORD wPriority; WORD wLanguage; DWORD dwInitialFrames; DWORD
		 * dwScale; DWORD dwRate; DWORD dwStart; DWORD dwLength; DWORD
		 * dwSuggestedBufferSize; DWORD dwQuality; DWORD dwSampleSize; struct {
		 * short int left; short int top; short int right; short int bottom; }
		 * rcFrame;
		 */

		public byte[] fcc = new byte[] { 's', 't', 'r', 'h' };
		public int cb = 64;
		public byte[] fccType = new byte[] { 'v', 'i', 'd', 's' };
		public byte[] fccHandler = new byte[] { 'M', 'J', 'P', 'G' };
		public int dwFlags = 0;
		public short wPriority = 0;
		public short wLanguage = 0;
		public int dwInitialFrames = 0;
		public int dwScale = 0; // microseconds per frame
		public int dwRate = 1000000; // dwRate / dwScale = frame rate
		public int dwStart = 0;
		public int dwLength = 0; // num frames
		public int dwSuggestedBufferSize = 0;
		public int dwQuality = -1;
		public int dwSampleSize = 0;
		public int left = 0;
		public int top = 0;
		public int right = 0;
		public int bottom = 0;

		public AVIStreamHeader() {
			dwScale = (int) ((1.0 / framerate) * 1000000.0);
			dwLength = numFrames;
		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(intBytes(swapInt(cb)));
			baos.write(fccType);
			baos.write(fccHandler);
			baos.write(intBytes(swapInt(dwFlags)));
			baos.write(shortBytes(swapShort(wPriority)));
			baos.write(shortBytes(swapShort(wLanguage)));
			baos.write(intBytes(swapInt(dwInitialFrames)));
			baos.write(intBytes(swapInt(dwScale)));
			baos.write(intBytes(swapInt(dwRate)));
			baos.write(intBytes(swapInt(dwStart)));
			baos.write(intBytes(swapInt(dwLength)));
			baos.write(intBytes(swapInt(dwSuggestedBufferSize)));
			baos.write(intBytes(swapInt(dwQuality)));
			baos.write(intBytes(swapInt(dwSampleSize)));
			baos.write(intBytes(swapInt(left)));
			baos.write(intBytes(swapInt(top)));
			baos.write(intBytes(swapInt(right)));
			baos.write(intBytes(swapInt(bottom)));
			baos.close();

			return baos.toByteArray();
		}
	}

	private class AVIStreamFormat {
		/*
		 * FOURCC fcc; DWORD cb; DWORD biSize; LONG biWidth; LONG biHeight; WORD
		 * biPlanes; WORD biBitCount; DWORD biCompression; DWORD biSizeImage;
		 * LONG biXPelsPerMeter; LONG biYPelsPerMeter; DWORD biClrUsed; DWORD
		 * biClrImportant;
		 */

		public byte[] fcc = new byte[] { 's', 't', 'r', 'f' };
		public int cb = 40;
		public int biSize = 40; // same as cb
		public int biWidth = 0;
		public int biHeight = 0;
		public short biPlanes = 1;
		public short biBitCount = 24;
		public byte[] biCompression = new byte[] { 'M', 'J', 'P', 'G' };
		public int biSizeImage = 0; // width x height in pixels
		public int biXPelsPerMeter = 0;
		public int biYPelsPerMeter = 0;
		public int biClrUsed = 0;
		public int biClrImportant = 0;

		public AVIStreamFormat() {
			biWidth = width;
			biHeight = height;
			biSizeImage = width * height;
		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(intBytes(swapInt(cb)));
			baos.write(intBytes(swapInt(biSize)));
			baos.write(intBytes(swapInt(biWidth)));
			baos.write(intBytes(swapInt(biHeight)));
			baos.write(shortBytes(swapShort(biPlanes)));
			baos.write(shortBytes(swapShort(biBitCount)));
			baos.write(biCompression);
			baos.write(intBytes(swapInt(biSizeImage)));
			baos.write(intBytes(swapInt(biXPelsPerMeter)));
			baos.write(intBytes(swapInt(biYPelsPerMeter)));
			baos.write(intBytes(swapInt(biClrUsed)));
			baos.write(intBytes(swapInt(biClrImportant)));
			baos.close();

			return baos.toByteArray();
		}
	}

	private class AVIMovieList {
		public byte[] fcc = new byte[] { 'L', 'I', 'S', 'T' };
		public int listSize = 0;
		public byte[] fcc2 = new byte[] { 'm', 'o', 'v', 'i' };

		// 00db size jpg image data ...

		public AVIMovieList() {

		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(intBytes(swapInt(listSize)));
			baos.write(fcc2);
			baos.close();

			return baos.toByteArray();
		}
	}

	private class AVIIndexList {
		public byte[] fcc = new byte[] { 'i', 'd', 'x', '1' };
		public int cb = 0;
		public ArrayList<AVIIndex> ind = new ArrayList<AVIIndex>();

		public AVIIndexList() {

		}

		public void addAVIIndex(int dwOffset, int dwSize) {
			ind.add(new AVIIndex(dwOffset, dwSize));
		}

		public byte[] toBytes() throws Exception {
			cb = 16 * ind.size();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(intBytes(swapInt(cb)));
			for (int i = 0; i < ind.size(); i++) {
				AVIIndex in = (AVIIndex) ind.get(i);
				baos.write(in.toBytes());
			}

			baos.close();

			return baos.toByteArray();
		}
	}

	private class AVIIndex {
		public byte[] fcc = new byte[] { '0', '0', 'd', 'b' };
		public int dwFlags = 16;
		public int dwOffset = 0;
		public int dwSize = 0;

		public AVIIndex(int dwOffset, int dwSize) {
			this.dwOffset = dwOffset;
			this.dwSize = dwSize;
		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(intBytes(swapInt(dwFlags)));
			baos.write(intBytes(swapInt(dwOffset)));
			baos.write(intBytes(swapInt(dwSize)));
			baos.close();

			return baos.toByteArray();
		}
	}

	private class AVIJunk {
		public byte[] fcc = new byte[] { 'J', 'U', 'N', 'K' };
		public int size = 1808;
		public byte[] data = new byte[size];

		public AVIJunk() {
			Arrays.fill(data, (byte) 0);
		}

		public byte[] toBytes() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(fcc);
			baos.write(intBytes(swapInt(size)));
			baos.write(data);
			baos.close();

			return baos.toByteArray();
		}
	}

	private byte[] writeImageToBytes(Image image) throws Exception {
		BufferedImage bi = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Graphics2D g = bi.createGraphics();
		g.drawImage(image, 0, 0, width, height, null);
		ImageIO.write(bi, "jpg", baos);
		baos.close();
		bi = null;
		g = null;

		return baos.toByteArray();
	}

	public static void main(String[] args) throws Exception {
		double framerate = 10.0;
		File photoDir = new File("./log/ipcam/");
		File[] files = photoDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if (name.toLowerCase().endsWith("jpg"))
					return true;
				return false;
			}
		});
		
		Arrays.sort(files);
		int numFrames = (int) (files.length);
		
		MJPEGCreator m = new MJPEGCreator(new File("movie.avi"), 640, 480,
				framerate, numFrames);
		for (int i = 0; i < files.length; i++) {
			NeptusLog.pub().info("<###>processing file " +(i+1)+" / "+files.length);
			m.addImage(files[i]);
		}
		m.finishAVI();
	}
}
