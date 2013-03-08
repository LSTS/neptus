/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Nov 10, 2012
 * $Id:: ManeuverLocation.java 9618 2013-01-02 11:30:46Z zepinto                $:
 */
package pt.up.fe.dceg.neptus.mp;

import org.dom4j.Document;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zp
 * 
 */
public class ManeuverLocation extends LocationType {

    private static final long serialVersionUID = 1L;

    public enum Z_UNITS {
        NONE(0),
        DEPTH(1),
        ALTITUDE(2),
        HEIGHT(3);

        protected long value;

        public long value() {
            return value;
        }

        Z_UNITS(long value) {
            this.value = value;
        }
    }

    protected double z = 0;
    protected Z_UNITS zUnits = Z_UNITS.NONE;

    public ManeuverLocation() {
        super();
    }
    
    public ManeuverLocation(LocationType loc) {
        super(loc);
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
    
    public ManeuverLocation clone() {
        ManeuverLocation loc = new ManeuverLocation();
        loc.setLocation(this);
        loc.setZ(getZ());
        loc.setZUnits(getZUnits());
        return loc;
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
        System.out.println(loc2.asXML());
        
    }
}
