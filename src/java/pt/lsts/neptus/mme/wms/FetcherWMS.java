/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * Jun 17, 2005
 */
package pt.lsts.neptus.mme.wms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.util.LinkedList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.vividsolutions.wms.BoundingBox;
import com.vividsolutions.wms.MapRequest;
import com.vividsolutions.wms.WMService;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

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
			
			BoundingBox bbox = new BoundingBox("EPSG:4326", (float) latLon2[1], (float) latLon1[0], (float) latLon1[1], (float) latLon2[0]);
			NeptusLog.pub().info("<###>minX:"+bbox.getMinX()+", minY:"+bbox.getMinY()+", maxX:"+bbox.getMaxX()+", maxY:"+bbox.getMaxY());
			request.setBoundingBox(bbox);

			LinkedList<String> layers = new LinkedList<String>();
			for (int i = 0; i < getRequestedLayers().length; i++) 
				layers.add(getRequestedLayers()[i]);
			
			request.setImageHeight(getImageHeight());
			request.setImageWidth(getImageWidth());
			
			request.setLayers(layers);
			request.setFormat("PNG");
			
			NeptusLog.pub().info("<###> "+request.getURL());
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
		topLeft.setLatitudeDegs(45);
		topLeft.setLongitudeDegs(-10);
		bottomRight.setLatitudeDegs(35);
		bottomRight.setLongitudeDegs(-5);
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
