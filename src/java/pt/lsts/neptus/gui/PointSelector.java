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
 * 28/Fev/2005
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.conf.GeneralPreferences;
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
	 * @return pt.lsts.neptus.gui.HeightDepthSelector	
	 */    
	public HeightDepthSelector getHeightDepthSelector() {
		if (heightDepthSelector == null) {
			heightDepthSelector = new HeightDepthSelector();
		}
		return heightDepthSelector;
	}
	
	public void setZSelectable(boolean zSelectable) {
	    heightDepthSelector.setVisible(zSelectable);
	}
	
	/**
	 * This method initializes latLongSelector	
	 * 	
	 * @return pt.lsts.neptus.gui.LatLongSelector	
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
		location.setLatitudeStr(getLatLongSelector().getLatitude());
		location.setLongitudeStr(getLatLongSelector().getLongitude());
		NeptusLog.pub().info("<###> "+location);
		return location;
	}
	
	public void setLocationType(LocationType location) {
		getHeightDepthSelector().setZ(location.getDepth());
		double[] lld = location.getAbsoluteLatLonDepth();
		getLatLongSelector().setLatitude(CoordinateUtil.decimalDegreesToDMS(lld[0]));
		getLatLongSelector().setLongitude(CoordinateUtil.decimalDegreesToDMS(lld[1]));
		
		switch (GeneralPreferences.latLonPrefFormat) {
            case DMS:
                getLatLongSelector().setDMSStyleIndicatorTo(LatLongSelector.DMS_DISPLAY);
                break;
            case DM:
                getLatLongSelector().setDMSStyleIndicatorTo(LatLongSelector.DM_DISPLAY);
                break;                
            default:
                getLatLongSelector().setDMSStyleIndicatorTo(LatLongSelector.DECIMAL_DEGREES_DISPLAY);
                break;
        }
		
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
