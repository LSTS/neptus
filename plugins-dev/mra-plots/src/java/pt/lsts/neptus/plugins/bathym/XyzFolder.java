package pt.lsts.neptus.plugins.bathym;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to store XYZ data into a folder of XYZ tiles
 * The tiles are split as in Google Maps tiles (zoom level 15)
 * @author zp
 *
 */
public class XyzFolder {

	private ConcurrentHashMap<String, BufferedWriter> writers = new ConcurrentHashMap<String, BufferedWriter>();
	
	private int zoom = 16;
	private File root;
	private boolean appendToExistingFiles = false;
	
	/**
	 * Creates 
	 * @param folder
	 */
	public XyzFolder(File folder) {
		this.root = folder;
		if (!root.exists())
			root.mkdirs();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		        try {
		            close();
		        }
		        catch (Exception e) {
		            e.printStackTrace();
                }
		    };
		});
	}
	
	/**
	 * In case an XYZ file already exists, should we append or override it?
	 * @param appendToExistingFiles
	 */
	public void setAppendToExistingFiles(boolean appendToExistingFiles) {
		this.appendToExistingFiles = appendToExistingFiles;
	}

	/**
     * @return the zoom
     */
    public int getZoom() {
        return zoom;
    }

    /**
     * @param zoom the zoom to set
     */
    public void setZoom(int zoom) {
        this.zoom = zoom;
    }

    /**
	 * Add a sample to this XYZ folder
	 * @param lat WGS84 latitude
	 * @param lon WGS84 Longitude
	 * @param z Depth / bathymetry
	 * @param additionalData Other fields to store in this XYZ file
	 * @throws IOException In case the file / folder cannot be written to
	 */
	public void addSample(double lat, double lon, double z, String... additionalData) throws IOException {
		BufferedWriter writer = getFile(lat, lon);
		
		writer.write(String.format("%.7f, %.7f, %.2f", lat, lon, z));
		for (String s: additionalData)
			writer.write(", "+s);
		writer.write("\n");
	}
	
	private BufferedWriter getFile(double lat, double lon) throws IOException {
		int xtile = (int)Math.floor( (lon + 180) / 360 * (1<<zoom) ) ;
		int ytile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom) ) ;
		if (xtile < 0)
			xtile=0;
		if (xtile >= (1<<zoom))
			xtile=((1<<zoom)-1);
		if (ytile < 0)
			ytile=0;
		if (ytile >= (1<<zoom))
			ytile=((1<<zoom)-1);
		String filename = "z" + zoom + "_" + xtile + "_" + ytile+".xyz";	
	
		writers.computeIfAbsent(filename, f -> {
			try {
				return new BufferedWriter(new FileWriter(new File(root, f), appendToExistingFiles));
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		});
		return writers.get(filename);
	}
	
	public void close() throws Exception {
	    for (BufferedWriter writer : writers.values())
	        writer.close();
	}
}
