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
 * 2018/12/05
 */
package pt.lsts.neptus.mp.maneuvers;

import java.util.LinkedHashMap;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;

/**
 * @author zp
 *
 */
public class FollowReference extends DefaultManeuver implements IMCSerialization {

    private int controlSource = -1, controlEntity = -1;
    private double timeout = 60;
    private float loiterRadius = 20;
    private float altitudeInterval = 1;

    protected static final String DEFAULT_ROOT_ELEMENT = "FollowReference";

    public FollowReference() {
    }

    public String getType() {
        return DEFAULT_ROOT_ELEMENT;
    }

    public Document getManeuverAsDocument(String rootElementName) {

        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);
        root.addAttribute("kind", "automatic");

        Element src = root.addElement("controlSource");
        src.setText(String.valueOf(getControlSource()));

        Element ent = root.addElement("controlEntity");
        ent.setText(String.valueOf(getControlEntity()));

        Element timeout = root.addElement("timeout");
        timeout.setText(String.valueOf(getTimeout()));

        Element loiterRadius = root.addElement("loiterRadius");
        loiterRadius.setText(String.valueOf(getLoiterRadius()));

        Element altitudeInterval = root.addElement("altitudeInterval");
        altitudeInterval.setText(String.valueOf(getAltitudeInterval()));

        return document;
    }

    public void loadManeuverFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            setControlSource(Integer.parseInt(doc.selectSingleNode("//controlSource").getText()));
            setControlEntity(Integer.parseInt(doc.selectSingleNode("//controlEntity").getText()));
            setTimeout(Double.parseDouble(doc.selectSingleNode("//timeout").getText()));
            setLoiterRadius(Float.parseFloat(doc.selectSingleNode("//loiterRadius").getText()));
            setAltitudeInterval(Float.parseFloat(doc.selectSingleNode("//altitudeInterval").getText()));
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            return;
        }
    }

    /**
     * @return the controlSource
     */
    public int getControlSource() {
        return controlSource;
    }

    /**
     * @param controlSource the controlSource to set
     */
    public void setControlSource(int controlSource) {
        this.controlSource = controlSource;
    }

    /**
     * @return the controlEntity
     */
    public int getControlEntity() {
        return controlEntity;
    }

    /**
     * @param controlEntity the controlEntity to set
     */
    public void setControlEntity(int controlEntity) {
        this.controlEntity = controlEntity;
    }

    /**
     * @return the timeout
     */
    public double getTimeout() {
        return timeout;
    }

    /**
     * @param timeout the timeout to set
     */
    public void setTimeout(double timeout) {
        this.timeout = timeout;
    }

    /**
     * @return the loiterRadius
     */
    public float getLoiterRadius() {
        return loiterRadius;
    }

    /**
     * @param loiterRadius the loiterRadius to set
     */
    public void setLoiterRadius(float loiterRadius) {
        this.loiterRadius = loiterRadius;
    }

    /**
     * @return the altitudeInterval
     */
    public float getAltitudeInterval() {
        return altitudeInterval;
    }

    /**
     * @param altitudeInterval the altitudeInterval to set
     */
    public void setAltitudeInterval(float altitudeInterval) {
        this.altitudeInterval = altitudeInterval;
    }

    public Object clone() {
        FollowReference clone = new FollowReference();
        super.clone(clone);

        clone.setControlSource(getControlSource());
        clone.setControlEntity(getControlEntity());
        clone.setLoiterRadius(getLoiterRadius());
        clone.setTimeout(getTimeout());
        clone.setAltitudeInterval(getAltitudeInterval());
        return clone;
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> properties = new Vector<DefaultProperty>();

        properties.add(
                PropertiesEditor.getPropertyInstance("Controlling Source", Integer.class, getControlSource(), true));
        properties.add(
                PropertiesEditor.getPropertyInstance("Controlling Entity", Integer.class, getControlEntity(), true));
        properties.add(PropertiesEditor.getPropertyInstance("Controlling Timeout", Integer.class, getTimeout(), true));
        properties.add(
                PropertiesEditor.getPropertyInstance("Altitude Interval", Float.class, getAltitudeInterval(), true));
        properties.add(PropertiesEditor.getPropertyInstance("Loiter Radius", Float.class, getLoiterRadius(), true));

        return properties;
    }

    public String getPropertiesDialogTitle() {
        return getId() + " parameters";
    }

    public void setProperties(Property[] properties) {
        super.setProperties(properties);

        for (Property p : properties) {
            if (p.getName().equals("Controlling Source")) {
                setControlSource((Integer) p.getValue());
            }
            else if (p.getName().equals("Controlling Entityd")) {
                setControlEntity((Integer) p.getValue());
            }
            else if (p.getName().equals("Controlling Timeout")) {
                setTimeout((Integer) p.getValue());
            }
            else if (p.getName().equals("Altitude Interval")) {
                setAltitudeInterval((Float) p.getValue());
            }
            else if (p.getName().equals("Loiter Radius")) {
                setLoiterRadius((Float) p.getValue());
            }
        }
    }

    public String[] getPropertiesErrors(Property[] properties) {
        return super.getPropertiesErrors(properties);
    }

    @Override
    public void parseIMCMessage(IMCMessage message) {

        try {
            pt.lsts.imc.FollowReference msg = (pt.lsts.imc.FollowReference) message;
            setControlSource(msg.getControlSrc());
            setControlEntity(msg.getControlEnt());
            setTimeout(msg.getTimeout());
            setAltitudeInterval((float) msg.getAltitudeInterval());
            setLoiterRadius((float) msg.getLoiterRadius());
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
    }

    public IMCMessage serializeToIMC() {

        pt.lsts.imc.FollowReference msg = new pt.lsts.imc.FollowReference();

        msg.setControlSrc(getControlSource());
        msg.setControlEnt((short) getControlEntity());
        msg.setLoiterRadius(getLoiterRadius());
        msg.setAltitudeInterval(getAltitudeInterval());
        msg.setTimeout(getTimeout());

        LinkedHashMap<String, Object> tl = new LinkedHashMap<String, Object>();

        for (String key : getCustomSettings().keySet())
            tl.put(key, getCustomSettings().get(key));
        msg.setValue("custom", IMCMessage.encodeTupleList(tl));

        return msg;
    }
    
    public static void main(String[] args) {
        FollowReference man = new FollowReference();
        System.out.println(man.asXML("FollowReference"));
        
    }
}
