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
 * Author: Margarida Faria
 * Apr 29, 2013
 */
package pt.lsts.neptus.plugins.trex.goals;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.TrexAttribute;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author meg, zp
 *
 */
public class AUVDrifterSurvey extends TrexGoal implements Renderer2DPainter {
    private static final String predicate = "Survey";
    private static final String timeline = "drifter";
    private Shape survey;
    private LocationType center;
    private Point2D firstPoint;
    private double rotationRads;
    private static final double pitch = Math.toRadians(15); 
    private static final double hspeed = 1.3 * Math.cos(pitch); 


    public enum Attributes {
        LATITUDE("center_lat", TrexAttribute.ATTR_TYPE.FLOAT),
        LONGITUDE("center_lon", TrexAttribute.ATTR_TYPE.FLOAT),
        PATH("path", TrexAttribute.ATTR_TYPE.ENUM),
        SIZE("size", TrexAttribute.ATTR_TYPE.FLOAT),
        LAGRANGIAN("lagrangian", TrexAttribute.ATTR_TYPE.BOOL),
        HEADING("heading", TrexAttribute.ATTR_TYPE.FLOAT),
        SPEED_EAST("speed_east", TrexAttribute.ATTR_TYPE.FLOAT),
        SPEED_NORTH("speed_north", TrexAttribute.ATTR_TYPE.FLOAT);

        public String name;
        public TrexAttribute.ATTR_TYPE type;

        private Attributes(String name, TrexAttribute.ATTR_TYPE type) {
            this.name = name;
            this.type = type;
        }
    }

    public enum PathType {
        SQUARE("square"),
        BACK_FORTH("forth_and_back"),
        GO_TO("go_to"),
        UPWARD("upward_transect"),
        SQUARE_TWICE("square_twice");

        public String name;

        private PathType(String name) {
            this.name = name;
        }
    }

    private final HashMap<Attributes, Object> attributes;

    /**
     * @param latrad
     * @param lonrad
     */
    public AUVDrifterSurvey(double latrad, double lonrad, float size, float speed,
            boolean lagrangian, PathType path, float heading) {
        super(timeline, predicate);
        attributes = new HashMap<AUVDrifterSurvey.Attributes, Object>();
        attributes.put(Attributes.LATITUDE, latrad);
        attributes.put(Attributes.LONGITUDE, lonrad);
        attributes.put(Attributes.SIZE, size);
        attributes.put(Attributes.LAGRANGIAN, lagrangian);
        attributes.put(Attributes.PATH, path);
        attributes.put(Attributes.HEADING, heading);

        if (speed != 0 && lagrangian) {
            double speed_e = Math.sin(heading) * speed;
            double speed_n = Math.cos(heading) * speed;
            attributes.put(Attributes.SPEED_EAST, speed_e);
            attributes.put(Attributes.SPEED_NORTH, speed_n);
        }

        buildShape(path, new LocationType(Math.toDegrees(latrad),Math.toDegrees(lonrad)), size, heading, speed);
    }

    private void buildShape(PathType type, LocationType center, double size, double rotation, double speed) {

        double halfSize = size/2;
        this.center = center;
        this.rotationRads = rotation;
        switch (type) {
            case GO_TO:
                survey = new Line2D.Double(0,0, 0,0);
                firstPoint = new Point2D.Double(0,0);
                break;
            case BACK_FORTH:
                firstPoint = new Point2D.Double(0,halfSize);
                survey = new Line2D.Double(0, -halfSize, 0, halfSize);
                break;
            case UPWARD:
                firstPoint = new Point2D.Double(0,halfSize);                                
                survey = new Line2D.Double(0, -halfSize, 0, halfSize);
                break;
            case SQUARE: {
                firstPoint = new Point2D.Double(-halfSize,-halfSize);
                GeneralPath gp = new GeneralPath();
                Point2D.Double[] pts = new Point2D.Double[5];
                pts[0] = new Point2D.Double(-halfSize, -halfSize);
                pts[1] = new Point2D.Double(halfSize, -halfSize - (size / hspeed)*speed);
                pts[2] = new Point2D.Double(halfSize, halfSize - 2 *(size / hspeed)*speed);
                pts[3] = new Point2D.Double(-halfSize, halfSize - 3 * (size / hspeed)*speed);
                pts[4] = new Point2D.Double(-halfSize, -halfSize - 4 * (size / hspeed)*speed);                    

                gp.moveTo(-halfSize, -halfSize);
                for (int i = 1; i < 5; i++) {
                    gp.lineTo(pts[i].getX(), pts[i].getY());
                }
                survey = gp;
            }
            break;
            case SQUARE_TWICE: {         
                firstPoint = new Point2D.Double(-halfSize,-halfSize);
                GeneralPath gp = new GeneralPath();
                Point2D.Double[] pts = new Point2D.Double[9];
                pts[0] = new Point2D.Double(-halfSize, -halfSize);
                pts[1] = new Point2D.Double(halfSize, -halfSize - (size / hspeed)*speed);
                pts[2] = new Point2D.Double(halfSize, halfSize - 2 *(size / hspeed)*speed);
                pts[3] = new Point2D.Double(-halfSize, halfSize - 3 * (size / hspeed)*speed);
                pts[4] = new Point2D.Double(-halfSize, -halfSize - 4 * (size / hspeed)*speed);                                        
                pts[5] = new Point2D.Double(halfSize, -halfSize - 5 * (size / hspeed)*speed);
                pts[6] = new Point2D.Double(halfSize, halfSize - 6 *(size / hspeed)*speed);
                pts[7] = new Point2D.Double(-halfSize, halfSize - 7 * (size / hspeed)*speed);
                pts[8] = new Point2D.Double(-halfSize, -halfSize - 8 * (size / hspeed)*speed);                    

                gp.moveTo(-halfSize, -halfSize);
                for (int i = 1; i < 9; i++) {
                    gp.lineTo(pts[i].getX(), pts[i].getY());
                    System.out.println(pts[i]);
                }
                survey = gp;
            }
            break;
            default:
                break;
        }        
    }

    @Override
    public Collection<TrexAttribute> getAttributes() {
        Vector<TrexAttribute> attributes = new Vector<TrexAttribute>();
        TrexAttribute attrTemp = new TrexAttribute();
        attrTemp.setName(Attributes.LATITUDE.name);
        attrTemp.setMin(this.attributes.get(Attributes.LATITUDE) + "");
        attrTemp.setMax(this.attributes.get(Attributes.LATITUDE) + "");
        attrTemp.setAttrType(Attributes.LATITUDE.type);
        attributes.add(attrTemp);
        attrTemp = new TrexAttribute();
        attrTemp.setName(Attributes.LONGITUDE.name);
        attrTemp.setMin(this.attributes.get(Attributes.LONGITUDE) + "");
        attrTemp.setMax(this.attributes.get(Attributes.LONGITUDE) + "");
        attrTemp.setAttrType(Attributes.LATITUDE.type);
        attributes.add(attrTemp);
        attrTemp = new TrexAttribute();
        attrTemp.setName(Attributes.SIZE.name);
        attrTemp.setMin(this.attributes.get(Attributes.SIZE) + "");
        attrTemp.setMax(this.attributes.get(Attributes.SIZE) + "");
        attrTemp.setAttrType(Attributes.SIZE.type);
        attributes.add(attrTemp);
        attrTemp = new TrexAttribute();
        attrTemp.setName(Attributes.LAGRANGIAN.name);
        attrTemp.setMin(this.attributes.get(Attributes.LAGRANGIAN) + "");
        attrTemp.setMax(this.attributes.get(Attributes.LAGRANGIAN) + "");
        attrTemp.setAttrType(Attributes.LAGRANGIAN.type);
        attributes.add(attrTemp);
        attrTemp = new TrexAttribute();
        attrTemp.setName(Attributes.PATH.name);
        PathType pathObj = (PathType) this.attributes.get(Attributes.PATH);
        attrTemp.setMin(pathObj.name + "");
        attrTemp.setMax(pathObj.name + "");
        attrTemp.setAttrType(Attributes.PATH.type);
        attributes.add(attrTemp);
        attrTemp = new TrexAttribute();

        if (this.attributes.get(Attributes.SPEED_EAST) != null && this.attributes.get(Attributes.SPEED_NORTH) != null) {
            attrTemp = new TrexAttribute();
            attrTemp.setName(Attributes.SPEED_EAST.name);
            attrTemp.setMin(this.attributes.get(Attributes.SPEED_EAST) + "");
            attrTemp.setMax(this.attributes.get(Attributes.SPEED_EAST) + "");
            attrTemp.setAttrType(Attributes.SPEED_EAST.type);
            attributes.add(attrTemp);                

            attrTemp = new TrexAttribute();
            attrTemp.setName(Attributes.SPEED_NORTH.name);
            attrTemp.setMin(this.attributes.get(Attributes.SPEED_NORTH) + "");
            attrTemp.setMax(this.attributes.get(Attributes.SPEED_NORTH) + "");
            attrTemp.setAttrType(Attributes.SPEED_NORTH.type);
            attributes.add(attrTemp);                            
        }
        else {
            attrTemp.setName(Attributes.HEADING.name);
            attrTemp.setMin(this.attributes.get(Attributes.HEADING) + "");
            attrTemp.setMax(this.attributes.get(Attributes.HEADING) + "");
            attrTemp.setAttrType(Attributes.HEADING.type);
            attributes.add(attrTemp);
        }

        return attributes;
    }

    @Override
    public void parseAttributes(Collection<TrexAttribute> attributes) {
        //TODO
    }

    @Override
    public Collection<DefaultProperty> getSpecificProperties() {

        Vector<DefaultProperty> props = new Vector<>();

        props.add(PropertiesEditor.getPropertyInstance("Latitude", Double.class,
                this.attributes.get(Attributes.LATITUDE), true));
        props.add(PropertiesEditor.getPropertyInstance("Longitude", Double.class,
                this.attributes.get(Attributes.LONGITUDE), true));

        return props;
    }

    @Override
    public void setSpecificProperties(Collection<Property> properties) {
        for (Property p : properties) {
            switch (p.getName()) {
                case "Latitude":
                    this.attributes.put(Attributes.LATITUDE, (Double) p.getValue());
                case "Longitude":
                    this.attributes.put(Attributes.LONGITUDE, (Double) p.getValue());
                default:
                    break;
            }
        }
    }

    public LocationType getLocation() {
        LocationType loc = new LocationType(Math.toDegrees((Double)this.attributes.get(Attributes.LATITUDE)),
                Math.toDegrees((Double)this.attributes.get(Attributes.LONGITUDE)));
        return loc;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        g.setColor(Color.green.darker().darker());
        if (survey != null) {            
            Point2D centerPt = renderer.getScreenPosition(center);
            g.translate(centerPt.getX(), centerPt.getY());
            g.rotate(rotationRads);
            g.scale(renderer.getZoom(), renderer.getZoom());
            g.fill(new Ellipse2D.Double(firstPoint.getX()-5, firstPoint.getY()-5, 10, 10));
            g.draw(survey);
        }   
        else {
            System.err.println("survey is null!");
        }
    }

    @Override
    public String toJson() {
        return "{"
                + "\"on\": \""+super.timeline+"\",\"pred\": \""+super.predicate+"\","
                + "\"Variable\":"
                + "["
                + "{\"float\":{\"value\": \""+this.attributes.get(Attributes.LATITUDE)+"\"}, \"name\": \""+Attributes.LATITUDE.name+"\"},"
                + "{\"float\":{\"value\": \""+this.attributes.get(Attributes.LONGITUDE)+"\"}, \"name\": \""+Attributes.LONGITUDE.name+"\"}"
                + "{\""+Attributes.SIZE.type.toString().toLowerCase()+"\":{\"value\": \"" + this.attributes.get(Attributes.SIZE) + "\"}, \"name\": \""+Attributes.SIZE.name+"\"}"
                + "{\""+Attributes.PATH.type.toString().toLowerCase()+"\":{\"value\": \"" + this.attributes.get(Attributes.PATH) + "\"}, \"name\": \""+Attributes.PATH.name+"\"}"
                + "{\""+Attributes.LAGRANGIAN.type.toString().toLowerCase()+"\":{\"value\": \"" + this.attributes.get(Attributes.LAGRANGIAN) + "\"}, \"name\": \""+Attributes.LAGRANGIAN.name+"\"}"
                + "]}";
    }

    /**
     * @return the survey
     */
    public Shape getShape() {
        return survey;
    }

    /**
     * @return the rotationRads
     */
    public final double getRotationRads() {
        return rotationRads;
    }

}
