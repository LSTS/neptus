/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Manuel Ribeiro
 * May 8, 2015
 */
package pt.lsts.neptus.mp.maneuvers;

import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.def.Boolean;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author Manuel Ribeiro
 *
 */
public class Sample extends Goto {

    protected static final String DEFAULT_ROOT_ELEMENT = "Sample";
    
    @NeptusProperty(name="Syringe 0", description="Take sample using syringe 0")
    private boolean useSyringe0 = false;
    @NeptusProperty(name="Syringe 1", description="Take sample using syringe 1")
    private boolean useSyringe1 = false;
    @NeptusProperty(name="Syringe 2", description="Take sample using syringe 2")
    private boolean useSyringe2 = false;
    
    @Override
    public String getType() {
        return "Sample";
    }

    @Override
    public Object clone() {  
        Sample clone = new Sample();
        super.clone(clone);
        clone.setManeuverLocation(getManeuverLocation());
        clone.setSpeed(getSpeed());
        clone.setSpeedTolerance(getSpeedTolerance());
        clone.setUseSyringe0(useSyringe0);
        clone.setStateSyringe1(useSyringe1);
        clone.setStateSyringe2(useSyringe2);
        
        return clone;
    }
    
    @Override
    public void parseIMCMessage(IMCMessage message) {
        try {
            pt.lsts.imc.Sample msg = pt.lsts.imc.Sample.clone(message);
            
            setMaxTime(msg.getTimeout());
            setSpeed(SpeedType.parseImcSpeed(message));
            ManeuverLocation pos = new ManeuverLocation();
            pos.setLatitudeRads(msg.getLat());
            pos.setLongitudeRads(msg.getLon());
            pos.setZ(msg.getZ());
            pos.setZUnits(ManeuverLocation.Z_UNITS.valueOf(msg.getZUnits().toString()));
            
            switch (msg.getSyringe0()) {
                case TRUE:
                    setUseSyringe0(true);
                    break;
                case FALSE:
                    setUseSyringe0(false);
                    break;
            }
    
            switch (msg.getSyringe1()) {
                case TRUE:
                    setStateSyringe1(true);
                    break;
                case FALSE:
                    setStateSyringe1(false);
                    break;
            }
            
            switch (msg.getSyringe2()) {
                case TRUE:
                    setStateSyringe2(true);
                    break;
                case FALSE:
                    setStateSyringe2(false);
                    break;
            }
            setManeuverLocation(pos);
            setCustomSettings(msg.getCustom());
            
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public IMCMessage serializeToIMC() {
        pt.lsts.imc.Sample sampleManeuver = new pt.lsts.imc.Sample();
        sampleManeuver.setTimeout(this.getMaxTime());
        LocationType l = getManeuverLocation();
        l.convertToAbsoluteLatLonDepth();
        
        sampleManeuver.setLat(l.getLatitudeRads());
        sampleManeuver.setLon(l.getLongitudeRads());
        sampleManeuver.setZ(getManeuverLocation().getZ());
        sampleManeuver.setZUnits(ZUnits.valueOf(getManeuverLocation().getZUnits().name()));

        getSpeed().setSpeedToMessage(sampleManeuver);
       
        sampleManeuver.setSyringe0(getStateSyringe0() ? Boolean.TRUE : Boolean.FALSE);
        sampleManeuver.setSyringe1(getStateSyringe1() ? Boolean.TRUE : Boolean.FALSE);
        sampleManeuver.setSyringe2(getStateSyringe2() ? Boolean.TRUE : Boolean.FALSE);

        sampleManeuver.setCustom(getCustomSettings());

        return sampleManeuver;
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        return ManeuversUtil.getPropertiesFromManeuver(this);     
    }
    
    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        ManeuversUtil.setPropertiesToManeuver(this, properties);
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.Goto#loadFromXML(java.lang.String)
     */
    @Override
    public void loadManeuverFromXML(String xml) {
        super.loadManeuverFromXML(xml);
        
        try {
            Document doc = DocumentHelper.parseText(xml);
            
            Node node1 = doc.selectSingleNode("useSyringe0");
            if (node1 != null) {
                String val = node1.getText();
                boolean bol = java.lang.Boolean.getBoolean(val);
                useSyringe0 = bol;
            }

            node1 = doc.selectSingleNode("useSyringe1");
            if (node1 != null) {
                String val = node1.getText();
                boolean bol = java.lang.Boolean.getBoolean(val);
                useSyringe1 = bol;
            }

            node1 = doc.selectSingleNode("useSyringe2");
            if (node1 != null) {
                String val = node1.getText();
                boolean bol = java.lang.Boolean.getBoolean(val);
                useSyringe2 = bol;
            }

        }
        catch (Exception e) {
            NeptusLog.pub().info(I18n.text("Error while loading the XML:")+"{" + xml + "}");
            NeptusLog.pub().error(this, e);
        }
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.Goto#getManeuverAsDocument(java.lang.String)
     */
    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document doc = super.getManeuverAsDocument(rootElementName);
        Element root = doc.getRootElement();
        
        root.addElement("useSyringe0").setText("" + useSyringe0);;
        root.addElement("useSyringe1").setText("" + useSyringe1);;
        root.addElement("useSyringe2").setText("" + useSyringe2);;
        
        return doc;
    }
    
    /**
     * @return syringe0 state
     */
    public boolean getStateSyringe0() {
        return useSyringe0;
    }
    
    /**
     * @param syringe0
     */
    public void setUseSyringe0(boolean syringe0) {
        this.useSyringe0 = syringe0;
    }

    /**
     * @return syringe1 state
     */
    public boolean getStateSyringe1() {
        return useSyringe1;
    }

    /**
     * @param syringe1
     */
    public void setStateSyringe1(boolean syringe1) {
        this.useSyringe1 = syringe1;
    }

    /**
     * @return syringe2 state
     */
    public boolean getStateSyringe2() {
        return useSyringe2;
    }

    /**
     * @param syringe2
     */
    public void setStateSyringe2(boolean syringe2) {
        this.useSyringe2 = syringe2;
    }
}