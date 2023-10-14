/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 20??/??/??
 */
package pt.lsts.neptus.types.map;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JLabel;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.colormap.InterpolationColorMap;
import pt.lsts.neptus.gui.ParametersSheetPanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.messages.TupleList;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.mp.MapChangeListener;
import pt.lsts.neptus.renderer2d.MissionRenderer;
import pt.lsts.neptus.renderer2d.Renderer;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * This class represents a remote object whose position can change over time and may be forwarding data (from its sensors)
 * Its similar to a vehicle in the way that it holds a position that varies throughout time but its position is not controlled from within a Neptus console.
 * @author ZP
 * @author RJPG
 */
public class DynamicElement extends MarkElement implements PropertiesProvider {

	
	protected String objectClass = "Drifter";
	protected double heading = 0.0;
	protected TupleList data = new TupleList("");
	protected Color innerColor = Color.gray;
	protected int idleTimeSecs = -1;
	protected int connectionTimeoutSecs = 120;
	protected double radius = 5;
	protected Ellipse2D circle = new Ellipse2D.Double(-radius,-radius,2*radius,2*radius);
    protected boolean showDetails = false;
    protected JLabel details = new JLabel(" ");
    private InterpolationColorMap colorMap = new InterpolationColorMap(new double[]{0, 0.5, 1.0}, new Color[] {Color.green, Color.yellow, Color.red});
	private long lastUpdateTime = -1;	
    
	
	public String getType() {
		return "DynamicObject";
	}
	
	public DynamicElement() {
	    super();
	}

	public int getLayerPriority() {
		return 6;
	}

	public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {
		
		//NeptusLog.pub().info("<###>Dynamic Element: paint");
		
		double zoom = renderer.getZoom();
        
        double offsets[] = (getCenterLocation().getOffsetFrom(renderer.getCenter()));
        AffineTransform oldTransform = g.getTransform();
        g.translate((offsets[1]) * zoom, (offsets[0]) * zoom);
        
        g.setColor(Color.black);
        float rad = (float)radius;
        
        if (idleTimeSecs == -1)
        	innerColor = Color.gray;
        else if (idleTimeSecs == 0)
        	innerColor = Color.white;
        else
        {
        	innerColor = colorMap.getColor((double)idleTimeSecs/(double)connectionTimeoutSecs);        	
        }
        g.setPaint(new GradientPaint(-rad, rad, innerColor, rad, -rad, innerColor.darker()));
        
        g.fill(circle);
        
        if (!isSelected()) 
            g.setColor(Color.black);
        else
            g.setColor(Color.red);
        
        g.draw(circle);
        
        String id = this.getId();        
        Rectangle2D rect = g.getFontMetrics().getStringBounds(id, g);
        g.setColor(Color.YELLOW);
        g.scale(1,-1);
        g.drawString(id, (int) -rect.getWidth()/2, (int)radius*2+5);
        String txt = "(idle for "+idleTimeSecs+" s)";
        rect = g.getFontMetrics().getStringBounds(txt, g);
        
        g.drawString(txt, (int) -rect.getWidth()/2, (int)radius*2+15);
        g.scale(1,-1);
        
        if (isShowDetails()) {
            g.rotate(rotation);
            g.scale(1,-1);
            g.translate(radius + 3, -radius);
            details.setSize(details.getPreferredSize());
            details.paint(g);
        }
        g.setTransform(oldTransform);
	}

	public ParametersPanel getParametersPanel(boolean editable, MapType map) {
		return new ParametersSheetPanel(getProperties());
	}
	
	public void initialize(ParametersPanel paramsPanel) {
		if (paramsPanel instanceof ParametersSheetPanel) {
            setProperties(((ParametersSheetPanel)paramsPanel).getProperties());
        }
	}

	public DefaultProperty[] getProperties() {		
		DefaultProperty sensorClass = PropertiesEditor.getPropertyInstance("Class", String.class, getObjectClass(), false);
		DefaultProperty centerLoc = PropertiesEditor.getPropertyInstance("Location", LocationType.class, getCenterLocation(), false);
		DefaultProperty heading = PropertiesEditor.getPropertyInstance("Heading", Double.class, getHeading(), false);
		DefaultProperty data = PropertiesEditor.getPropertyInstance("Data", String.class, getData().toString(), false);
		DefaultProperty idleTime = PropertiesEditor.getPropertyInstance("Idle Time", Integer.class, getIdleTimeSecs(), false);
		DefaultProperty timeout = PropertiesEditor.getPropertyInstance("Connection Timeout (secs)", Integer.class, getConnectionTimeoutSecs(), true);
		DefaultProperty showDetails = PropertiesEditor.getPropertyInstance("Show Data", Boolean.class, isShowDetails(), true);
		return new DefaultProperty[] {sensorClass, centerLoc, heading, data, idleTime, timeout, showDetails};
	}
	
	public void setProperties(Property[] properties) {
		for (Property p : properties) {
			if (p.getName().equals("Class"))
				setObjectClass(p.getValue().toString());
			else if (p.getName().equals("Location"))
				setCenterLocation((LocationType)p.getValue());
			else if (p.getName().equals("Heading"))
				setHeading((Double)p.getValue());
			else if (p.getName().equals("Data"))
				setData(new TupleList(p.getValue().toString()));
			else if (p.getName().equals("Idle Time"))
				setIdleTimeSecs((Integer)p.getValue());				
			else if (p.getName().equals("Connection Timeout (secs)"))
				setConnectionTimeoutSecs((Integer)p.getValue());
			else if (p.getName().equals("Show Data"))
				setShowDetails((Boolean)p.getValue());
		}
		
		colorMap = new InterpolationColorMap(new double[]{0, connectionTimeoutSecs/2, connectionTimeoutSecs}, new Color[] {Color.green, Color.yellow, Color.red});
	}
	
	public String getPropertiesDialogTitle() {
		return "Dynamic Object Properties";
	}
	
	public String[] getPropertiesErrors(Property[] properties) {
		return null;
	}
	
	

	public String getObjectClass() {
		return objectClass;
	}

	public void setObjectClass(String objectClass) {
		this.objectClass = objectClass;
	}

	public double getHeading() {
		return heading;
	}

	public void setHeading(double heading) {
		this.heading = heading;
	}

	public TupleList getData() {
		return data;
	}

	public void setData(TupleList data) {
		this.data = data;
	}

	public int getIdleTimeSecs() {
		return idleTimeSecs;
	}

	public void setIdleTimeSecs(int idleTimeSecs) {
		this.idleTimeSecs = idleTimeSecs;
	}

	public boolean isShowDetails() {
		return showDetails;
	}

	public void setShowDetails(boolean showDetails) {
		this.showDetails = showDetails;
	}

	public int getConnectionTimeoutSecs() {
		return connectionTimeoutSecs;
	}

	public void setConnectionTimeoutSecs(int connectionTimeoutSecs) {
		this.connectionTimeoutSecs = connectionTimeoutSecs;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(long lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}
	
	public InterpolationColorMap getColorMap() {
		return colorMap;
	}

	public void setColorMap(InterpolationColorMap colorMap) {
		this.colorMap = colorMap;
	}

	public Color getInnerColor() {
		return innerColor;
	}

	public void setInnerColor(Color innerColor) {
		this.innerColor = innerColor;
	}
	
    public static void main(String[] args) {
        ConfigFetch.initialize();
        MissionType mission = new MissionType("missions/Montemor/mission-20070919-tarde14h.nmisz");
        MapGroup mg = MapGroup.getMapGroupInstance(mission);
        MapType mt = new MapType();
        mg.addMap(mt);
        
        LocationType lt = new LocationType(/*mission.getHomeRef()*/);
        //lt.setOffsetNorth(15);
        lt.setLatitudeStr("41N12.4827");
        lt.setLongitudeStr("8W32.0861");
        final DynamicElement dynElem = new DynamicElement();
        dynElem.setCenterLocation(lt);
        dynElem.setIdleTimeSecs(0);
        dynElem.setLastUpdateTime(System.currentTimeMillis());
        
        mt.addObject(dynElem);
        
        final MissionRenderer renderer = new MissionRenderer(null, mg, MissionRenderer.R2D_AND_R3D1CAM);
        GuiUtils.testFrame(renderer);       
        
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                dynElem.setLastUpdateTime(System.currentTimeMillis());
                
                MapChangeEvent mce;
                
                mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
                
                mce.setMapGroup(dynElem.getMapGroup());
                mce.setSourceMap(dynElem.getParentMap());
                
                mce.setChangeType(MapChangeEvent.UNKNOWN_CHANGE);
            
                for (Renderer r : renderer.getRenderers()) 
                    if (r instanceof MapChangeListener)
                        ((MapChangeListener)r).mapChanged(mce);
            }           
            
        };
        Timer t = new Timer("Dynamic Element");
        t.schedule(task, 10000, 10000);
    }

}

