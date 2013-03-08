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
 * Apr 21, 2005
 * $Id:: HomeReferenceElement.java 9845 2013-02-01 19:53:46Z pdias        $:
 */
package pt.up.fe.dceg.neptus.types.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import pt.up.fe.dceg.neptus.gui.CoordinateSystemPanel;
import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zecarlos
 */
public class HomeReferenceElement extends AbstractElement {

	private CoordinateSystem coordinateSystem = new CoordinateSystem();
	private CoordinateSystemPanel params = null;
	
	public HomeReferenceElement(MapGroup mg, MapType map) {
		super(mg, map);
		
		if (mg != null && mg.getCoordinateSystem() != null)
			setCoordinateSystem(mg.getCoordinateSystem());
		
		setName("Home Reference");
		setYawDeg(getCoordinateSystem().getYaw());
		setPitchDeg(getCoordinateSystem().getPitch());
		setRollDeg(getCoordinateSystem().getRoll());
	}
	
	public HomeReferenceElement() {
	    super();
	}
	
	public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {

        Point2D tt = renderer.getScreenPosition(getCenterLocation());
        g.translate(tt.getX(), tt.getY());
        g.rotate(getYawRad()-renderer.getRotation());
        
		if (!isSelected())
			g.setColor(Color.RED);
		else
			g.setColor(Color.WHITE);
		
		g.drawOval(-5,-5,10,10);
		
		if (!isSelected())
			g.setColor(Color.WHITE);
		else
			g.setColor(Color.RED);
		
		g.drawLine(-8, 0, 8, 0);
		g.drawLine(0, -8, 0, 8);
				
	}


	public ParametersPanel getParametersPanel(boolean editable, MapType map) {
		params = new CoordinateSystemPanel(getCoordinateSystem());
		params.setEditable(false);
		return params;
	}

	public void initialize(ParametersPanel paramsPanel) {
		//Since the parameters panel is not editable... 
		// no changes could have been performed
	}

	public boolean containsPoint(LocationType lt, StateRenderer2D renderer) {
        double distance = getCenterLocation().getDistanceInMeters(lt);
        if ((distance * renderer.getZoom()) < 10) {
            return true;
        }

        return false;
	}

	public LocationType getCenterLocation() {
		LocationType lt = new LocationType();
		lt.setLocation(getCoordinateSystem());
		return lt;
	}

	public void setCenterLocation(LocationType l) {
	    centerLocation = l;
		// System.err.println("Use setCoordinateSystem() instead"); ????
	}

	public CoordinateSystem getCoordinateSystem() {
		return coordinateSystem;
	}
	
	public void setCoordinateSystem(CoordinateSystem coordinateSystem) {
		this.coordinateSystem.setCoordinateSystem(coordinateSystem);
	}
	
	@Override
	public double getYawRad() {
		return Math.toRadians(coordinateSystem.getYaw());
	}
	
	@Override
	public double getYawDeg() {
		return coordinateSystem.getYaw();
	}
	
	
	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.mme.objects.MapObject#getLayerPriority()
	 */
	public int getLayerPriority() {
		return 8;
	}
	
	@Override
	public String getType() {
		return "Home Reference";
	}
	
	@Override
	public ELEMENT_TYPE getElementType() {
	    return ELEMENT_TYPE.TYPE_HOMEREFERENCE;
	}
	
}
