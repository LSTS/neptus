package pt.up.fe.dceg.neptus.plugins.sidescan;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;

import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;

/**
 * This class holds a sidescan ping metainformation (time, position, operation frequency, etc) 
 * @author zp
 */
public class SidescanIndexEntry implements Comparable<SidescanIndexEntry> {

    /**
     * Size of each ping information frame
     */
	public static final int FRAME_SIZE = 54;
    
	/**
	 * Offset of this ping in the binary data file
	 */
    public long offset = 0;
    
    /**
     * When this ping was received
     */
	public long timestamp = 0;
	
	/**
	 * Latitude of the vehicle while receiving this ping
	 */
	public double latitude = 0;
	
	/**
	 * Longitude of the vehicle while receiving this ping
	 */
	public double longitude = 0;
	
	/**
	 * Altitude of the vehicle while receiving this ping
	 */
	public float altitude = 0;
	
	/**
	 * The orientation of the vehicle while receiving this ping
	 */
	public float yawDegs = 0;
	
	/**
	 * The speed the vehicle was travelling at while receiving this ping
	 */
	public float speed = 0;	
	
	/**
	 * The sidescan frequency when this ping was taken
	 */
	public int opFreq = 0;
	
	/**
	 * The configured sidescan range for this ping
	 */
	public int range = 0;
	
	/**
	 * The number of data bytes belonging to this ping
	 */
	public short numBytes = 0;
	
	/**
	 * Writes this sidescan ping information to the given DataOutputStream
	 * @param dos Where to write this ping metainfo to
	 * @throws IOException If its not possible to write for some external reason
	 */
	public void write(DataOutputStream dos) throws IOException {
		dos.writeLong(offset);
		dos.writeLong(timestamp);
		dos.writeDouble(latitude);
		dos.writeDouble(longitude);
		dos.writeFloat(altitude);
		dos.writeFloat(yawDegs);
		dos.writeFloat(speed);
		dos.writeInt(opFreq);
		dos.writeInt(range);
		dos.writeShort(numBytes);		
	}
	
	/**
	 * Reads the information of this ping from the given DataInputStream
	 * @param dis Where to read the information from
	 * @throws IOException If it is not possible to read for some external reason
	 */
	public void read(DataInputStream dis) throws IOException {
		offset = dis.readLong();
		timestamp = dis.readLong();
		latitude = dis.readDouble();
		longitude = dis.readDouble();
		altitude = dis.readFloat();
		yawDegs = dis.readFloat();
		speed = dis.readFloat();
		opFreq = dis.readInt();		
		range = dis.readInt();		
		numBytes = dis.readShort();				
	}
	
	/**
	 * Comparator is based on the offset in the data file
	 */
	public int compareTo(SidescanIndexEntry o) {
		return (int) (this.offset - o.offset);
	};
	
	/**
	 * This method can be used to load all the sidescan index entries from a given log source
	 * @param source Where to load the sidescan index file (sidescan-index.mra)
	 * @return All the read sidescan entries as a Vector
	 * @throws IOException If for some reason it is not possible to load the index (File Not Found, ...)
	 * this exception is thrown
	 */
	public static Vector<SidescanIndexEntry> parseIndex(IMraLogGroup source) throws IOException {
		DataInputStream dis = new DataInputStream(new FileInputStream(source.getFile("sidescan-index.mra")));
		Vector<SidescanIndexEntry> entries = new Vector<SidescanIndexEntry>();
		while(dis.available() > 0) {
			SidescanIndexEntry entry = new SidescanIndexEntry();
			entry.read(dis);
			entries.add(entry);
		}		
		return entries;
	}
	

}
