/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * Jun 17, 2005
 * $Id:: FetcherWMS.java 9616 2012-12-30 23:23:22Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.mme.wms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.util.LinkedList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

import com.vividsolutions.wms.BoundingBox;
import com.vividsolutions.wms.MapRequest;
import com.vividsolutions.wms.WMService;

/**
 * @author zp
 */
public class FetcherWMS {

	private LocationType topLeft = new LocationType();
	private LocationType bottomRight = new LocationType();
	private int imageWidth = 500, imageHeight = 500;
	//private String wmsServerURL = "http://onearth.jpl.nasa.gov/wms.cgi?";
	
	
	private String wmsServerURL = "http://www2.demis.nl/wms/wms.asp?Service=WMS&WMS=BlueMarble&";
	private String[] requestedLayers = new String[] {"Earth Image","Borders"};
	
	
	public Image fetchImage() {
		Image resultingImage = null;
		
		WMService service = new WMService(wmsServerURL);
		try {
			
			service.initialize();
		
			MapRequest request = service.createMapRequest();
			//service.
			double latLon2[] = getTopLeft().getAbsoluteLatLonDepth();
			double latLon1[] = getBottomRight().getAbsoluteLatLonDepth();
			
			//BoundingBox bbox = new BoundingBox("EPSG:4326", (float)topLeft.getLongitudeAsDoubleValue(),(float)topLeft.getLatitudeAsDoubleValue(),(float)bottomRight.getLongitudeAsDoubleValue(),(float)bottomRight.getLatitudeAsDoubleValue());
			BoundingBox bbox = new BoundingBox("EPSG:4326", (float) latLon2[1], (float) latLon1[0], (float) latLon1[1], (float) latLon2[0]);
			System.out.println("minX:"+bbox.getMinX()+", minY:"+bbox.getMinY()+", maxX:"+bbox.getMaxX()+", maxY:"+bbox.getMaxY());
			request.setBoundingBox(bbox);

			LinkedList<String> layers = new LinkedList<String>();
			for (int i = 0; i < getRequestedLayers().length; i++) 
				layers.add(getRequestedLayers()[i]);
			
			request.setImageHeight(getImageHeight());
			request.setImageWidth(getImageWidth());
			
			request.setLayers(layers);
			request.setFormat("PNG");
			
			System.out.println(request.getURL());
			resultingImage = request.getImage();
		}
		catch (Exception e) {
			GuiUtils.errorMessage(new JFrame(), "Error ocurred while fetching image", "Error ocurred while fetching image: "+e.getMessage());
			e.printStackTrace(System.err);
		}
		
		return resultingImage;
	}
	
	
	public LocationType getBottomRight() {
		return bottomRight;
	}
	public void setBottomRight(LocationType bottomRight) {
		this.bottomRight = bottomRight;
	}
	public int getImageHeight() {
		return imageHeight;
	}
	public void setImageHeight(int imageHeight) {
		this.imageHeight = imageHeight;
	}
	public int getImageWidth() {
		return imageWidth;
	}
	public void setImageWidth(int imageWidth) {
		this.imageWidth = imageWidth;
	}
	public String[] getRequestedLayers() {
		return requestedLayers;
	}
	public void setRequestedLayers(String[] requestedLayers) {
		this.requestedLayers = requestedLayers;
	}
	public LocationType getTopLeft() {
		return topLeft;
	}
	public void setTopLeft(LocationType topLeft) {
		this.topLeft = topLeft;
	}
	public String getWmsServerURL() {
		return wmsServerURL;
	}
	public void setWmsServerURL(String wmsServerURL) {
		this.wmsServerURL = wmsServerURL;
	}
	
	public static void main(String[] args) {
		JFrame frame = new JFrame("WMS Test");
		FetcherWMS fetcher = new FetcherWMS();
		
		LocationType topLeft = new LocationType(), bottomRight = new LocationType();
		topLeft.setLatitude(45);
		topLeft.setLongitude(-10);
		bottomRight.setLatitude(35);
		bottomRight.setLongitude(-5);
		fetcher.setBottomRight(bottomRight);
		fetcher.setTopLeft(topLeft);
		JPanel main = new JPanel();
		main.setLayout(new BorderLayout());
		JLabel jlabel = new JLabel(new ImageIcon(fetcher.fetchImage()));
		jlabel.setBackground(Color.RED);
		jlabel.setSize(400,400);
		main.add(jlabel, BorderLayout.CENTER);
		//frame.add(new JLabel(new ImageIcon(img)));
		frame.setContentPane(main);
		frame.setSize(200,300);
		//frame.add(jlabel);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	/*TODO retornar o bitmap referente é altitude do terreno 
	 *  se for null como está o mapa ficará plano 
	 * 
	 */ 
	public Image fetchTerrainImage() {
		Image returned=null;
		return returned;
	}
}
