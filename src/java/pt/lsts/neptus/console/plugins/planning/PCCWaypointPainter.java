/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 2009/10/27
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedHashMap;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.console.plugins.planning.UavPiccoloControl.PiccoloControlConfiguration;
import pt.lsts.neptus.console.plugins.planning.UavPiccoloControl.WaypointColors;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LatLonFormatEnum;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 * @author Paulo Dias
 */
@LayerPriority(priority=40)
public class PCCWaypointPainter implements Renderer2DPainter {
	
    protected boolean paintEnable = true;
    protected boolean paintHandoverWP = true;
    protected boolean paintExternalWP = false;
    
	//protected LinkedHashMap<Integer, PCCWaypoint> waypoints = new LinkedHashMap<Integer, PCCWaypoint>(); 
	protected LinkedHashMap<String, LinkedHashMap<Integer, PCCWaypoint>> waypoints = new LinkedHashMap<String, LinkedHashMap<Integer,PCCWaypoint>>();
    protected LinkedHashMap<String, Boolean> showWaypoints = new LinkedHashMap<String, Boolean>();
	
	//protected int destinationWaypoint = -1;
	//protected int sourceWaypoint = -1;
	//protected LocationType vehiclePosition = null;
	protected LinkedHashMap<String, LocationType> vehiclePositionList = new LinkedHashMap<String, LocationType>();
	protected float thickness = 10;
	protected GeneralPath endTriangle = new GeneralPath();
	
	protected LinkedHashMap<String, Integer> toWaypoints = new LinkedHashMap<String, Integer>();
	protected LinkedHashMap<String, Integer> fromWaypoints = new LinkedHashMap<String, Integer>();
    protected LinkedHashMap<String, Integer> serviceWaypoints = new LinkedHashMap<String, Integer>();
    protected LinkedHashMap<String, WaypointColors> colorsWaypoints = new LinkedHashMap<String, WaypointColors>();
    protected LinkedHashMap<String, PiccoloControlConfiguration> configsWaypoints = new LinkedHashMap<String, PiccoloControlConfiguration>();
	
    protected String mainVehicle = ""; // main vehicle
    
	public WaypointColors colors;
	
	{
		endTriangle.moveTo(0, 0);
		endTriangle.lineTo(-4*thickness, 2*thickness);
		endTriangle.lineTo(-4*thickness, -2*thickness);
		endTriangle.closePath();
	}
	
	/**
     * @return the mainVehicle
     */
    public String getMainVehicle() {
        return mainVehicle;
    }
    
    /**
     * @param mainVehicle the mainVehicle to set
     */
    public void setMainVehicle(String mainVehicle) {
        if (mainVehicle == null)
            this.mainVehicle = "";
        else
            this.mainVehicle = mainVehicle;
    }
	
    /**
     * @return the configsWaypoints
     */
    public LinkedHashMap<String, PiccoloControlConfiguration> getConfigsWaypoints() {
        return configsWaypoints;
    }
    
    /**
     * @param configsWaypoints the configsWaypoints to set
     */
    public void setConfigsWaypoints(
            LinkedHashMap<String, PiccoloControlConfiguration> configsWaypoints) {
        this.configsWaypoints = configsWaypoints;
    }
    
	/**
     * @return the paintEnable
     */
    public boolean isPaintEnable() {
        return paintEnable;
    }
    
    /**
     * @param paintEnable the paintEnable to set
     */
    public void setPaintEnable(boolean paintEnable) {
        this.paintEnable = paintEnable;
    }

    /**
     * @return the paintHandoverWP
     */
    public boolean isPaintHandoverWP() {
        return paintHandoverWP;
    }
    
    /**
     * @param paintHandoverWP the paintHandoverWP to set
     */
    public void setPaintHandoverWP(boolean paintHandoverWP) {
        this.paintHandoverWP = paintHandoverWP;
    }
    
    /**
     * @return the paintExternalWP
     */
    public boolean isPaintExternalWP() {
        return paintExternalWP;
    }
    
    /**
     * @param paintExternalWP the paintExternalWP to set
     */
    public void setPaintExternalWP(boolean paintExternalWP) {
        this.paintExternalWP = paintExternalWP;
    }
    
    /**
     * @param vehicle
     * @return If not known return true.
     */
    public boolean isVehicleWaypointsVisible(String vehicle) {
        boolean ret = true;
        synchronized (showWaypoints) {
            if (showWaypoints.containsKey(vehicle)) {
                ret = showWaypoints.get(vehicle);
            }
        }
        return ret;
    }
    
    /**
     * @param vehicle
     * @param visible
     */
    public void setVehicleWaypointsVisible(String vehicle, boolean visible) {
        synchronized (showWaypoints) {
            showWaypoints.put(vehicle, visible);
        }
    }
    
	public String[] getVehiclesList() {
		String[] ret;
		synchronized (waypoints) {
			ret = waypoints.keySet().toArray(new String[waypoints.keySet().size()]);
		}
		return ret;
	}

	public boolean hasVehicle(String id) {
	    boolean ret = false;
	    synchronized (waypoints) {
	        ret = waypoints.containsKey(id);
	    }
	    return ret;
	}

	
	public void paintPlan(Graphics2D g, StateRenderer2D renderer, String vehicle) {
	    if (!isPaintEnable())
            return;
	    
	    if (!isVehicleWaypointsVisible(vehicle))
	        return;
	    
		boolean drawnActive = false;
		
		//VehicleType v = VehiclesHolder.getVehicleById(vehicle);
		//NeptusLog.pub().info("<###> "+vehicle+" -> "+v);
		
		Color c1 = VehiclesHolder.getVehicleById(vehicle).getIconColor();
		Color c2 = c1.brighter();
		LinkedHashMap<Integer, PCCWaypoint> wpts = new LinkedHashMap<Integer, PCCWaypoint>();
		synchronized (waypoints) {
			wpts.putAll(waypoints.get(vehicle));
		}
		
        PiccoloControlConfiguration config = configsWaypoints.get(vehicle);

		Integer dst = toWaypoints.get(vehicle);
		Integer src = fromWaypoints.get(vehicle);
		
		WaypointColors wpcTmp = this.colorsWaypoints.get(vehicle);
		if (wpcTmp != null) {
        }
		
		int destinationWaypoint = dst != null ? dst : -1;
		int sourceWaypoint = src != null ? src : -1;
		
		for (PCCWaypoint wpt : wpts.values()) {
			boolean active = false;
			if (wpt.radius > 0 && destinationWaypoint == wpt.number)
				active = true;
			else if (sourceWaypoint == wpt.number
					&& wpt.next == destinationWaypoint)
				active = true;

			if (active)
				drawnActive = true;

			Color fillColor = (active) ? c2 : c1;
			if (active && getMainVehicle().equalsIgnoreCase(vehicle))
			    fillColor = Color.MAGENTA;

			if (!active && destinationWaypoint != wpt.number && config != null) {
			    if (!paintHandoverWP && config.isHandhoverWP((short) wpt.number))
			        continue;

			    if (!paintExternalWP && config.isExternalWP((short) wpt.number))
			        continue;
			}

			
			Color lineColor = Color.black;// (active)? Color.orange.darker()
												// : Color.magenta.darker();

			Point2D start, finish = null;
			start = renderer.getScreenPosition(wpt.location);
			double radius = wpt.radius * renderer.getZoom();

			if (wpts.containsKey(wpt.next)) {
				try {
					finish = renderer
							.getScreenPosition(wpts.get(wpt.next).location);
				} catch (Exception e) {
					e.printStackTrace();
					finish = start;
				}
			}
			else {
				finish = start;
			}
			
			double dist = start.distance(finish);
			Area arrow = new Area(new Rectangle2D.Double(0, -thickness / 2,
					dist - 2 * thickness, thickness));

			Graphics2D clone = (Graphics2D) g.create();
			clone.translate(start.getX(), start.getY());

			if (!finish.equals(start))
				clone.rotate(Math.atan2(finish.getY() - start.getY(),
						finish.getX() - start.getX()));

			AffineTransform trans = new AffineTransform();
			trans.translate(dist, 0);
			if (!finish.equals(start))
				arrow.add(new Area(endTriangle)
				.createTransformedArea(trans));

			clone.setColor(fillColor);
			clone.fill(arrow);
			clone.setColor(lineColor);
			clone.draw(arrow);

			if (wpt.radius > 0) {
			    boolean paintRadius = true;
                if ((destinationWaypoint != wpt.number)
                        && (config != null && config.isServiceWP((short)wpt.number)))
			        paintRadius = false;
			    
			    if (paintRadius) {
			        Area loiterArea = new Area(new Ellipse2D.Double(
			                start.getX() - radius - thickness / 2, start.getY()
			                - radius - thickness / 2, radius * 2
			                + thickness, radius * 2 + thickness));
			        loiterArea.subtract(new Area(new Ellipse2D.Double(start
			                .getX() - radius + thickness / 2, start.getY()
			                - radius + thickness / 2, radius * 2 - thickness,
			                radius * 2 - thickness)));
			        g.setColor(fillColor);
			        g.fill(loiterArea);
			        g.setColor(lineColor);
			        g.draw(loiterArea);
			    }
			}
		}

		// To draw the active path if no from is active
		if (!drawnActive && (wpts.get(destinationWaypoint) != null)) {
			LocationType dest = wpts.get(destinationWaypoint).location;
			// NeptusLog.pub().info("<###>active wpt is virtual from "+vehiclePosition+" to "+dest);

			LocationType vehiclePosition = vehiclePositionList.get(vehicle);
			if (vehiclePosition != null) {
				Point2D start = renderer.getScreenPosition(vehiclePosition);
				Point2D finish = renderer.getScreenPosition(dest);
				double dist = start.distance(finish);
				Area arrow = new Area(new Rectangle2D.Double(0, -thickness / 2,
						dist - 2 * thickness, thickness));

				Graphics2D clone = (Graphics2D) g.create();
				clone.translate(start.getX(), start.getY());

				if (!finish.equals(start))
					clone.rotate(Math.atan2(finish.getY() - start.getY(),
							finish.getX() - start.getX()));

				AffineTransform trans = new AffineTransform();
				trans.translate(dist, 0);
				if (!finish.equals(start))
					arrow.add(new Area(endTriangle)
						.createTransformedArea(trans));

				clone.setColor(Color.magenta);
				clone.fill(arrow);
				clone.setColor(Color.red.darker());
				clone.draw(arrow);
			}
		}

		// For WP Strings Painting
		for (PCCWaypoint wpt : wpts.values()) {

			Point2D start = renderer.getScreenPosition(wpt.location);

			if (destinationWaypoint != wpt.number && config != null) {
			    if (!paintHandoverWP && config.isHandhoverWP((short) wpt.number))
			        continue;
			    
			    if (!paintExternalWP && config.isExternalWP((short) wpt.number))
                    continue;
			}
			
            String str = "" + wpt.number;
			if (config != null) {
			    if (config.isServiceWP((short) wpt.number))
			        str = "____S" + wpt.number;
			    else if (config.isPlanWP((short) wpt.number))
			        str = "P" + wpt.number;
			    else if (config.isHandhoverWP((short) wpt.number))
			        str = "H" + wpt.number;
                else
                    str = "E" + wpt.number;
			}
            
			if (start != null) {
			    if (destinationWaypoint == wpt.number) {
					g.setColor(Color.black);
					g.drawString(str, (int) start.getX() + 1,
							(int) start.getY() + 1);
					g.setColor(new Color(255, 128, 128));
					g.drawString(str, (int) start.getX(),
							(int) start.getY());
				} else {
				    Color bColor = Color.black;
				    Color fColor = Color.yellow.brighter();
				    if (!getMainVehicle().equalsIgnoreCase(vehicle)) {
				        fColor = ColorUtils.setTransparencyToColor(c1, 220);
				        bColor = ColorUtils.invertColor(c1, 220);
				    }
					g.setColor(bColor);
					g.drawString(str, (int) start.getX() + 1,
							(int) start.getY() + 1);
					g.setColor(fColor);
					g.drawString(str, (int) start.getX(),
							(int) start.getY());
				}
			}
		}
	}
	
	@Override
	public void paint(Graphics2D g, StateRenderer2D renderer) {
		if (!isPaintEnable())
		    return;
		
		Font beforeFont = g.getFont();
		g.setFont(new Font("Helvetica", Font.BOLD, 14+(int)thickness));
		
		for (String v : waypoints.keySet()) {
			paintPlan(g, renderer, v);
		}
		
		g.setFont(beforeFont);
	}
	
	/**
     * @return the waypoints
     */
    @SuppressWarnings("unchecked")
    public LinkedHashMap<Integer, PCCWaypoint> getWaypoints(String vid) {
        return (LinkedHashMap<Integer, PCCWaypoint>) waypoints.get(vid).clone();
    }
	
	public void setWaypoints(String vid, Collection<PCCWaypoint> waypts) {
		synchronized (waypoints) {			
			LinkedHashMap<Integer, PCCWaypoint> values = waypoints.get(vid);
			if (values == null)
				values = waypoints.put(vid, new LinkedHashMap<Integer, PCCWaypoint>());
			
			for (PCCWaypoint wpt : waypts) {
				values.put(wpt.number, wpt);
			}
		}
	}

	public void setWaypoint(String vid, PCCWaypoint waypoint) {
		LinkedHashMap<Integer, PCCWaypoint> values = waypoints.get(vid);

		if (values == null) {
			synchronized (waypoints) {
				values = new LinkedHashMap<Integer, PCCWaypoint>(); 
				waypoints.put(vid, values);
			}
		}
		
		//System.err.println("------------- Waypoint "+waypoint+"  "+vid+"  "+values);
		values.put(waypoint.number, waypoint);	
	}
	
	public void deleteWaypoint(String vid, int number) {
		synchronized (waypoints) {
			waypoints.get(vid).remove(number);
		}
	}
	
	/**
	 * @return the destinationWaypoint
	 */
	public int getDestinationWaypoint(String vid) {
        Integer ret = toWaypoints.get(vid);
        return ret == null ? -1 : ret;
	}

	/**
	 * @param destinationWaypoint the destinationWaypoint to set
	 */
	public void setDestinationWaypoint(String vid, int destinationWaypoint) {
		//this.destinationWaypoint = destinationWaypoint;
		toWaypoints.put(vid, destinationWaypoint);
		synchronized (waypoints) { // To add the vehicle to the list
			if (waypoints.get(vid) == null) {
				waypoints.put(vid, new LinkedHashMap<Integer, PCCWaypoint>());
			}
		}
	}

	/**
	 * @return the sourceWaypoint
	 */
	public int getSourceWaypoint(String vid) {
        Integer ret = fromWaypoints.get(vid);
        return ret == null ? -1 : ret;
	}

	/**
	 * @param sourceWaypoint the sourceWaypoint to set
	 */
	public void setSourceWaypoint(String vid, int sourceWaypoint) {
		fromWaypoints.put(vid, sourceWaypoint);
		synchronized (waypoints) { // To add the vehicle to the list
			if (waypoints.get(vid) == null) {
				waypoints.put(vid, new LinkedHashMap<Integer, PCCWaypoint>());
			}
		}
	}
    
	/**
	 * @param vehiclePosition the vehiclePosition to set
	 */
	public void setVehiclePosition(String vid, LocationType vehiclePosition) {
		//this.vehiclePosition = vehiclePosition;
		vehiclePositionList.put(vid, vehiclePosition);
	}
	

	/**
	 * @return the thickness
	 */
	public float getThickness() {
		return thickness;
	}

	/**
	 * @param thickness the thickness to set
	 */
	public void setThickness(float thickness) {
		this.thickness = thickness;		
		
		GeneralPath gp = new GeneralPath();
		gp.moveTo(0, 0);
		gp.lineTo(-4*thickness, 2*thickness);
		gp.lineTo(-4*thickness, -2*thickness);
		gp.closePath();
		
		endTriangle = gp;
	}
	
	public static void main(String[] args) {
		ConfigFetch.initialize();
		VehiclesHolder.loadVehicles();
		IMCMessage wpt = IMCDefinition.getInstance().create("PiccoloWaypoint", "index", 0, "lat", 0, "lon", 0.00001, "depth", 0, "next", 2);
		wpt.getHeader().setValue("src", 0xbb22);
		IMCMessage wpt2 = IMCDefinition.getInstance().create("PiccoloWaypoint", "index", 2, "lat", 0.003, "lon", 0.001, "depth", 0, "next", 1);
		wpt2.getHeader().setValue("src", 0xbb22);
		IMCMessage wpt3 = IMCDefinition.getInstance().create("PiccoloWaypoint", "index", 1, "lat", 0, "lon", 0, "depth", 0, "lradius", 300, "ltime", 100, "next", 4);
		wpt3.getHeader().setValue("src", 0xbb22);
		IMCMessage track = IMCDefinition.getInstance().create("PiccoloTrackingState", "to", 2, "from", 3);
		track.getHeader().setValue("src", 0xbb22);

		IMCMessage wpt1_1 = IMCDefinition.getInstance().create("PiccoloWaypoint", "index", 0, "lat", 0, "lon", 0.00003, "depth", 0, "next", 1);
		wpt1_1.getHeader().setValue("src", 0xbb23);
		IMCMessage wpt1_2 = IMCDefinition.getInstance().create("PiccoloWaypoint", "index", 1, "lat", 0.0024, "lon", 0.0014, "depth", 0, "next", 0);
		wpt1_2.getHeader().setValue("src", 0xbb23);
		IMCMessage track_1 = IMCDefinition.getInstance().create("PiccoloTrackingState", "to", 0, "from", 1);
		track_1.getHeader().setValue("src", 0xbb23);

		
		MapPanel planPanel = new MapPanel(null);

		LocationType loc = new LocationType();
		loc.setLatitudeDegs(41);
		loc.setLongitudeDegs(-8);
		loc.translatePosition(50, 35, 0);
		LocationType loc1 = new LocationType();
        loc1.setLatitudeDegs(41);
        loc1.setLongitudeDegs(-8);
		loc1.translatePosition(150, 85, 0);

		planPanel.getRenderer().setCenter(loc);

		UavPiccoloControl pp = new UavPiccoloControl(null);
//		pp.showPCC = true;
		pp.propertiesChanged();

		pp.wptPainter.setVehiclePosition("alfa02", loc);
		pp.wptPainter.setVehiclePosition("alfa03", loc1);
		

		pp.onMessage(null, wpt);
		pp.onMessage(null, wpt2);
		pp.onMessage(null, wpt3);
		pp.onMessage(null, track);
		pp.onMessage(null, wpt1_1);
		pp.onMessage(null, wpt1_2);
		pp.onMessage(null, track_1);

		ConsoleParse.dummyConsole(planPanel, pp);
	}

}

class PCCWaypoint {
	
	int number, next;
	LocationType location;	
	float radius;
	
	public PCCWaypoint(int number, int next, LocationType loc) {
		this.number = number;
		this.next = next;
		this.location = loc;
		this.radius = 0;
	}
	
	public PCCWaypoint(int number, int next, LocationType loc, float radius) {
		this.number = number;
		this.next = next;
		this.location = loc;
		this.radius = radius;
	}
	
	public String getId() {
		return ""+number;
	}
	
	public boolean isLoiter() {
		return radius > 0;
	}
	
	@Override
	public String toString() {
        return "WPT[" + number + "]:\n\tlat: " + location.getLatitudeAsPrettyString(LatLonFormatEnum.DMS) + ", lon: "
                + location.getLongitudeAsPrettyString(LatLonFormatEnum.DMS) + ", depth: " + location.getAllZ()
                + "\n\tlradius: " + radius + ", next: " + next;
	}
}
