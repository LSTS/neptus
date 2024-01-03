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
 * Nov 10, 2012
 */
package pt.lsts.neptus.mp;

import org.dom4j.Document;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 * 
 */
public class ManeuverLocation extends LocationType {

    private static final long serialVersionUID = 1L;

    public enum Z_UNITS {
        NONE(0, "None"),
        DEPTH(1, "Depth"),
        ALTITUDE(2, "Altitude"),
        HEIGHT(3, "Height");

        protected long value;
        protected String text;

        public long value() {
            return value;
        }

        public String text() {
            return text;
        }

        Z_UNITS(long value, String text) {
            this.value = value;
            this.text = text;
        }
        
        public static  Z_UNITS from(String text) {
            for (Z_UNITS elem : Z_UNITS.values()) {
                if (elem.text().equalsIgnoreCase(text))
                    return elem;
            }
            return null;
        }
    }

    protected double lradius = 0;
    protected double z = 0;
    protected Z_UNITS zUnits = Z_UNITS.NONE;

    public ManeuverLocation() {
        super();
    }
    
    public ManeuverLocation(LocationType loc) {
        super(loc);
        if (loc instanceof ManeuverLocation) {
            ManeuverLocation mloc = (ManeuverLocation) loc;
            this.setZ(mloc.getZ());
            this.setZUnits(mloc.getZUnits());
        }
    }
    
    /**
     * @return the z
     */
    public double getZ() {
        return z;
    }

    /**
     * @param z the z to set
     */
    public void setZ(double z) {
        this.z = z;
    }

    /**
     * @return the zUnits
     */
    public Z_UNITS getZUnits() {
        return zUnits;
    }

    /**
     * @param zUnits the zUnits to set
     */
    public void setZUnits(Z_UNITS zUnits) {
        this.zUnits = zUnits;
    }
    
    /**
     * @return Loiter Radius in meters (0 if not a loiter)
     */
    public double getRadius() {
        return lradius;
    }

    /**
     * @param lradius Loiter Radius in meters (0 for no loitering)
     */
    public void setRadius(double lradius) {
        this.lradius = lradius;
    }

    public ManeuverLocation clone() {
        ManeuverLocation loc = new ManeuverLocation();
        loc.setLocation(this);
        loc.setZ(getZ());
        loc.setZUnits(getZUnits());
        loc.setRadius(getRadius());
        return loc;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <L extends LocationType> L getNewAbsoluteLatLonDepth() {
        L ret = super.getNewAbsoluteLatLonDepth();
        ManeuverLocation manLoc;
        if (ret instanceof ManeuverLocation) {
            manLoc = (ManeuverLocation) ret;
        }
        else {
            manLoc = new ManeuverLocation();
            manLoc.setLocation(ret);
        }
        manLoc.setZ(getZ());
        manLoc.setZUnits(getZUnits());
        ret = (L) manLoc;
        return ret;
    }

    @Override
    public Document asDocument(String rootElementName) {        
        Document document = super.asDocument(rootElementName);        
        document.getRootElement().addElement("z").setText(""+getZ());
        document.getRootElement().addElement("zunits").setText(getZUnits().toString());
        return document;
    }
    
    @Override
    public boolean load(Document doc) {
        super.load(doc);
        Node node;

        node = doc.selectSingleNode("//depth");
        if (node != null) {
            setZ(Double.parseDouble(node.getText()));
            if (getZ() >= 0)
                setZUnits(Z_UNITS.DEPTH);
            else {
                setZUnits(Z_UNITS.ALTITUDE);
                setZ(-getZ());
            }
        }
        
        node = doc.selectSingleNode("//height");
        if (node != null) {
            setZUnits(Z_UNITS.HEIGHT);
            setZ(Double.parseDouble(node.getText()));
        }
        
        node = doc.selectSingleNode("//altitude");
        if (node != null) {
            setZUnits(Z_UNITS.ALTITUDE);
            setZ(Double.parseDouble(node.getText()));
        }
        
        node = doc.selectSingleNode("//z");
        if (node != null)
            setZ(Double.parseDouble(node.getText()));
        node = doc.selectSingleNode("//zunits");
        if (node != null)
            setZUnits(Z_UNITS.valueOf(node.getText()));
        return true;
    }
    
    public static void main(String[] args) {
        ManeuverLocation loc = new ManeuverLocation();
        
        loc.setZ(10);
        loc.setZUnits(Z_UNITS.ALTITUDE);
        
        String xml = loc.asXML();
        ManeuverLocation loc2 = new ManeuverLocation();
        loc2.load(xml);
        NeptusLog.pub().info("<###> "+loc2.asXML());
        
    }
}
