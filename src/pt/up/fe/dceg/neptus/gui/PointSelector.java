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
 * 28/Fev/2005
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;

import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
/**
 * @author Zé Carlos
 */
public class PointSelector extends ParametersPanel {

	private static final long serialVersionUID = 1L;

	private HeightDepthSelector heightDepthSelector = null;
	private LatLongSelector latLongSelector = null;
    private boolean editable;

    public PointSelector() {
		super();
		initialize();
	}

	/**
	 * This method initializes heightDepthSelector	
	 * 	
	 * @return pt.up.fe.dceg.neptus.gui.HeightDepthSelector	
	 */    
	public HeightDepthSelector getHeightDepthSelector() {
		if (heightDepthSelector == null) {
			heightDepthSelector = new HeightDepthSelector();
		}
		return heightDepthSelector;
	}
	
	/**
	 * This method initializes latLongSelector	
	 * 	
	 * @return pt.up.fe.dceg.neptus.gui.LatLongSelector	
	 */    
	public LatLongSelector getLatLongSelector() {
		if (latLongSelector == null) {
			latLongSelector = new LatLongSelector();
		}
		return latLongSelector;
	}
	
	public String getErrors() {
		if (getLatLongSelector().getErrors() != null)
			return getLatLongSelector().getErrors();
		else
			return getHeightDepthSelector().getErrors();
	}
	
	public LocationType getLocationType() {
		if (getErrors() != null)
			return null;
		
		LocationType location = new LocationType();
		
		location.setDepth(getHeightDepthSelector().getDepth());
		location.setLatitude(getLatLongSelector().getLatitude());
		location.setLongitude(getLatLongSelector().getLongitude());
		System.out.println(location);
		return location;
	}
	
	public void setLocationType(LocationType location) {
		getHeightDepthSelector().setZ(location.getDepth());		
		getLatLongSelector().setLatitude(CoordinateUtil.parseLatitudeStringToDMS(location.getLatitude()));
		getLatLongSelector().setLongitude(CoordinateUtil.parseLongitudeStringToDMS(location.getLongitude()));
	}
	
	/**
	 * This method initializes this
	 * @return void
	 */
	private  void initialize() {
        this.setLayout(new BorderLayout());
        this.setBounds(0, 0, 420, 200);
        this.add(getHeightDepthSelector(), java.awt.BorderLayout.SOUTH);
        this.add(getLatLongSelector(), java.awt.BorderLayout.CENTER);
	}
	
	
	public void setEditable(boolean value) {
	    this.editable = value;
	    getHeightDepthSelector().setEditable(editable);
	    getLatLongSelector().setEditable(editable);
	}
}
