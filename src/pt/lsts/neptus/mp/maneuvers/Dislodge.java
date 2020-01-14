/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * 28/10/2014
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Graphics2D;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.Dislodge.DIRECTION;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.map.PlanElement;

/**
 * @author pdias
 *
 */
public class Dislodge extends DefaultManeuver implements IMCSerialization {

    protected static final String DEFAULT_ROOT_ELEMENT = "Dislodge";

    @NeptusProperty(name="RPM", description="RPM to be used")
    private int rpm = 1200;

    @NeptusProperty(name="Direction", description="Direction to which the vehicle should attempt to unstuck.")
    private pt.lsts.imc.Dislodge.DIRECTION direction = DIRECTION.AUTO;

    public Dislodge() {
    }

    public String validateRpm(int value) {
        if (value < 0)
            return I18n.text("RPM should be positive");
        return null;
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
     * @see pt.lsts.neptus.mp.maneuvers.DefaultManeuver#getType()
     */
    @Override
    public String getType() {
        return DEFAULT_ROOT_ELEMENT;
    }

    @Override
    public Object clone() {
        Dislodge clone = new Dislodge();
        super.clone(clone);
        clone.setRpm(getRpm());
        clone.setDirection(getDirection());
        return clone;
    }

    /**
     * @return the rpm
     */
    public int getRpm() {
        return rpm;
    }
    
    /**
     * @param rpm the rpm to set
     */
    public void setRpm(int rpm) {
        this.rpm = rpm;
    }
    
    /**
     * @return the direction
     */
    public pt.lsts.imc.Dislodge.DIRECTION getDirection() {
        return direction;
    }
    
    /**
     * @param direction the direction to set
     */
    public void setDirection(pt.lsts.imc.Dislodge.DIRECTION direction) {
        this.direction = direction;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.DefaultManeuver#getManeuverAsDocument(java.lang.String)
     */
    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( rootElementName );
        root.addAttribute("kind", "automatic");

        Element rpmElem = root.addElement("rpm");
        rpmElem.setText(String.valueOf(getRpm()));

        Element directionElem = root.addElement("direction");
        directionElem.setText(getDirection().toString());

        return document;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.DefaultManeuver#loadFromXML(java.lang.String)
     */
    @Override
    public void loadManeuverFromXML(String xml) {
        super.loadManeuverFromXML(xml);
        try {
            Document doc = DocumentHelper.parseText(xml);
            
            try {
                setRpm(Integer.parseInt(doc.selectSingleNode("//rpm").getText()));
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            try {
                setDirection(DIRECTION.valueOf(doc.selectSingleNode("//direction").getText()));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            return;
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.IMCSerialization#serializeToIMC()
     */
    @Override
    public IMCMessage serializeToIMC() {
        pt.lsts.imc.Dislodge dislodge = new pt.lsts.imc.Dislodge();

        dislodge.setTimeout(getMaxTime());
        dislodge.setRpm(rpm);
        dislodge.setDirection(direction);
        dislodge.setCustom(getCustomSettings());
        
        return dislodge;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.maneuvers.IMCSerialization#parseIMCMessage(pt.lsts.imc.IMCMessage)
     */
    @Override
    public void parseIMCMessage(IMCMessage message) {
        if (!DEFAULT_ROOT_ELEMENT.equalsIgnoreCase(message.getAbbrev()))
            return;
        
        pt.lsts.imc.Dislodge dislodge = null;
        try {
            dislodge = pt.lsts.imc.Dislodge.clone(message);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        setMaxTime(dislodge.getTimeout());
        rpm = Double.valueOf(dislodge.getRpm()).intValue();
        direction = dislodge.getDirection();
        setCustomSettings(dislodge.getCustom());
    }
    
    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);
        g2d = (Graphics2D) g2d.create();

        g2d.dispose();
    }
}
