/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 2012/0?/??
 * $Id:: ImcToSds.java 9763 2013-01-24 17:17:06Z zepinto                  $:
 */
package pt.up.fe.dceg.neptus.mra.exporters;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCInputStream;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;

/**
 * Class responsible for converting IMC lsf files containing sidescan data to Sonar Data Stream (SDS) format used
 * by Marine Sonic software 
 * @author jqcorreia
 *
 */
public class ImcToSds {

	public static int SDS_SYNC = 0x53594E43;
	private File inputFile;
	private File outputFile;
	private IMCInputStream is;
	private DataOutputStream os;

	public ImcToSds(File input, IMCDefinition defs) {
		setInputFile(input);
		try {
			is = new IMCInputStream(new FileInputStream(input), defs);
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setInputFile(File f) {
		inputFile = f;
	}

	public void setOutputFile(File f) {
		outputFile = f;
	}

	public void writeHeader(int time, int tag, int size, byte misc)
			throws IOException {
		int dwSize = size;
		int dwTimeStamp = time;
		int dwTag = tag;
		byte byMisc[] = new byte[3];
		byte byChecksum;

		byMisc[0] = misc;
		byMisc[1] = misc;
		byMisc[2] = misc;

		byChecksum = (byte) ((dwSize >>> 24) + (dwSize >>> 16) + (dwSize >>> 8)
				+ (dwSize) + (dwTimeStamp >>> 24) + (dwTimeStamp >>> 16)
				+ (dwTimeStamp >>> 8) + (dwTimeStamp) + (dwTag >>> 24)
				+ (dwTag >>> 16) + (dwTag >>> 8) + (dwTag) + byMisc[0]
				+ byMisc[1] + byMisc[2]); // Hardcoded way of calculating the
											// sum of all 15 bytes of the header

		os.writeInt(Integer.reverseBytes(dwSize));
		os.writeInt(Integer.reverseBytes(dwTimeStamp));
		os.writeInt(Integer.reverseBytes(dwTag));
		os.write(byMisc);
		os.write(byChecksum);
	}

	public void writeSyncPacket(int reference, short interval, int time)
			throws IOException {
		writeHeader(time, SDS_SYNC, 4 + 2 /* reference + interval */, (byte) 0xAA);

		os.writeInt(Integer.reverseBytes(reference));
		os.writeShort(Short.reverseBytes(interval));
	}

	public void writeSonarPacket(IMCMessage ping, int elapsedtime, int pingId) {
		int dataSize = ping.getRawData("data").length;

		// 16 bytes sonar ping struct
		// 2 x 22 bytes Channels description (left/right)
		// 2 x 1000 bytes of data for each channel = dataSize
		int psize = 16 + 2 * 22 + dataSize; 
		
		byte[] reserved = { 0, 0, 0, 0, 0, 0, 0 };
		
		byte[] data = ping.getRawData("data");
		byte[] leftChannel = new byte[dataSize/2];
		byte[] rightChannel = new byte[dataSize/2];
		
		
		// Process the message
		float freq = 0;

        switch (ping.getInteger("frequency")) {
            case 0:
                freq = 770;
                break;
            case 1:
                freq = 330;
                break;
            case 2:
                freq = 260;
                break;
        }
        for (int i = 0; i < dataSize / 2; i++) {
            leftChannel[dataSize / 2 - 1 - i] = data[i];
        }
        for (int i = dataSize / 2; i < dataSize; i++) {
            rightChannel[i - dataSize / 2] = data[i];
        }

        // Write the packet
		try {
			writeHeader(elapsedtime, 0x32524E52, psize, (byte) 0x00);

			// Write SONAR_DATA_PING packet
			wByte(2); // Channel count
			wInt(pingId); // Ping count (id)
			wFloat(1500); // Speed of sound;
			os.write(reserved);

			// WRITE CHANNEL_1_INFO
			wShort((short) 1); // Port SideScan
			wShort((short) 1); // Channel 1
			wFloat(freq*1000); // Operating frequency ( in Hz not in kHz)
			wFloat(ping.getInteger("range") / 1.5f); // Range in milisseconds
			wFloat(0); // Range delay set to 0
			wShort((short) 1); // Data flags set to 0 = NOTHING SPECIAL
			wShort((short) 1); // Data types set to unsigned 8 bit
			wShort((short) 1000); // Number of sample in this channel = 1000

			// WRITE CHANNEL_2_INFO
			wShort((short) 2); // Starboard SideScan
			wShort((short) 2); // Channel 2
			wFloat(freq*1000); // Operating frequency ( in Hz not in kHz)
			wFloat(ping.getInteger("range") / 1.5f); // Range in milisseconds
			wFloat(0); // Range delay set to 0
			wShort((short) 1); // Data flags set to 0 = NOTHING SPECIAL
			wShort((short) 1); // Data types set to unsigned 8 bit
			wShort((short) 1000); // Number of sample in this channel = 1000

			// Write both channels raw data
			os.write(leftChannel);
			os.write(rightChannel);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeNavigationPacket(IMCMessage msg, int elapsedtime) throws IOException {
		int psize = 30; // The sum of all the packet fields (byte size)
		double res[];
		
        res = CoordinateUtil.latLonAddNE2(Math.toDegrees(msg.getDouble("lat")), Math.toDegrees(msg.getDouble("lon")),
                msg.getDouble("x"), msg.getDouble("y"));
		
		writeHeader(elapsedtime, 0x4E415600, psize, (byte)0x00);
		
		wShort((short)2); // Source field. 2 = AUV
		wDouble(res[0]); // Latitude
		wDouble(res[1]); // Longitude
		wFloat((float)Math.atan2(msg.getDouble("x"), msg.getDouble("y")));
		wFloat((float)Math.toDegrees(msg.getDouble("psi")));
		wFloat((float)Math.sqrt(Math.pow(msg.getDouble("vx"), 2)+Math.pow(msg.getDouble("vy"), 2)));
	}
	
	public void writeFathometerPacket(IMCMessage msg, double depth, int elapsedtime) throws IOException {
	    int psize = 10; // 2 Source + 4 Depth + 4 Altitude
	    
	    writeHeader(elapsedtime, 0x46415400, psize, (byte)0x00);
	    
	    wShort((short)2); // Source 2 = AUV
	    wFloat((float)depth);
	    wFloat((float)msg.getDouble("value"));
	}
	
	public void wByte(int b) throws IOException {
		os.write(b);
	}

	public void wFloat(float f) throws IOException {
		os.writeInt(Integer.reverseBytes(Float.floatToIntBits(f)));
	}

	public void wInt(int i) throws IOException {
		os.writeInt(Integer.reverseBytes(i));
	}

	public void wShort(short s) throws IOException {
		os.writeShort(Short.reverseBytes(s));
	}
	
	public void wDouble(double d) throws IOException {
	    os.writeLong(Long.reverseBytes(Double.doubleToLongBits(d)));
	}
	
	public void convertToSDSFile(File file) {
		double firstTime = 0;
		IMCMessage msg = null;
		int c = 0;
		double prevTimeMillis = 0;
		int n = 0;
		setOutputFile(file);
		try {
			os = new DataOutputStream(new FileOutputStream(outputFile));
			// Loop through all the messages in the file (main conversion loop)
			double lastDepth = 0.0;
			while (msg != null) {
				// Write SDS first Sync package
				msg = is.readMessage();
				if (c == 0) { // First message
					writeSyncPacket((int) msg.getTimestamp(), (short) 1000, 0);
					firstTime = msg.getTimestampMillis();
					prevTimeMillis = msg.getTimestampMillis();
				}
				else {
				    // Wait for the interval to write another syncPacket
				    if(msg.getTimestampMillis() - prevTimeMillis > 1000) { // 1 second
				        writeSyncPacket((int) msg.getTimestamp(), (short) 1000, ++n * 1000);
				        prevTimeMillis = msg.getTimestampMillis();
				    }
				}
				// If Sidescanping write sonar data packet
				if (msg.getAbbrev().equals("SidescanPing")) {
					writeSonarPacket(msg, (int) (msg.getTimestampMillis() - firstTime), c);
				}
				
				// if EstimatedState write navigation packet
				if(msg.getAbbrev().equals("EstimatedState")) {
				    lastDepth = msg.getDouble("z");
					writeNavigationPacket(msg, (int) (msg.getTimestampMillis() - firstTime));
				}
				
				// If BottomDistance write fathometer packet
				if(msg.getAbbrev().equals("BottomDistance")) {
                    writeFathometerPacket(msg, lastDepth,(int) (msg.getTimestampMillis() - firstTime));
                }
				c++;
			}
		}
		catch (IOException e) {
			// Do nothing and let the loop end
		    // e.printStackTrace();
		}
		System.out.println("Number of messages processed to SDS: " + c);

		// Close data streams
		try {
			is.close();
			os.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {
		ImcToSds converter = new ImcToSds(new File(args[0]), IMCDefinition.getInstance());
		System.out.println(converter.inputFile.getAbsolutePath());
		converter.convertToSDSFile(new File(args[1]));
		System.out.println(converter.outputFile.getAbsolutePath());
	}
}
