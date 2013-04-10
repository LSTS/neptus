package pt.up.fe.dceg.neptus.plugins.spot;

import java.text.SimpleDateFormat;

public class SpotMessage implements Comparable<SpotMessage>{

    public final double latitude, longitude;
    public final long timestamp;
	String id;
	
	public SpotMessage(double lat, double lon, long timestamp, String id) {
		this.latitude = lat;
		this.longitude = lon;
		this.timestamp = timestamp;
		this.id = id;
	}
	
	@Override
	public int compareTo(SpotMessage o) {
		return (int)(timestamp - o.timestamp);
	}

	@Override
	public int hashCode() {
		return new String(""+latitude+longitude+id+timestamp).hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return hashCode() == obj.hashCode();
	}
	
	public static String  latString(double latitude, String posneg) {
		String ns = posneg.substring(0, 1);
		if (latitude < 0) {
			ns = posneg.substring(1);
			latitude = - latitude;
		}
		String ret = ((int)latitude) + ns;
		latitude -= (int)latitude;
		
		latitude *= 60;
		ret += (int)latitude+"'";
		latitude -= (int)latitude;
		ret += (float)(latitude * 60);
		
		return ret;
		
	}
	
	public String asVehicleState() {
		SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss.SSS");
		SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
		
		String xml = "<VehicleState id='"+id+"' time='"+time.format(timestamp)+"' date='"+date.format(timestamp)+"'>\n";
		xml += "  <coordinate>\n    <id>id</id>\n    <name>name</name>\n    <coordinate>\n";
	    xml += "      <latitude>"+latString(latitude, "NS")+"</latitude>\n";
	    xml += "      <longitude>"+latString(longitude, "EW")+"</longitude>\n";
	    xml += "      <depth>0</depth>\n    </coordinate>\n  </coordinate>\n";
	    xml += "  <attitude><phi>0</phi><theta>0</theta><psi>0</psi></attitude>\n  <imc/>\n";
	    xml += "</VehicleState>\n";
		return xml;
	}

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return id + " (" + latitude + ", " + longitude + ") " + timestamp;
    }
}
