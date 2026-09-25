/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.gui.editor.ComboEditor;

public class Sampling extends Maneuver implements LocatedManeuver, ManeuverWithSpeed, IMCSerialization, StatisticsProvider {

    private static final String XML_ROOT = "Sampling";

    public enum SamplerType {
        REDX("RedX"),
        WHITEX("WhiteX"),
        DORIS("Doris");

        private final String displayName;

        SamplerType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static SamplerType fromString(String text) {
            if (text == null) return null;
            for (SamplerType type : values()) {
                if (type.name().equalsIgnoreCase(text) || type.displayName.equalsIgnoreCase(text)) {
                    return type;
                }
            }
            return null;
        }
    }

    public enum DorisType {
        DRIFT("Drift"),
        MOVE("Move");

        private final String displayName;

        DorisType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static DorisType fromString(String text) {
            if (text == null) return null;
            for (DorisType type : values()) {
                if (type.name().equalsIgnoreCase(text) || type.displayName.equalsIgnoreCase(text)) {
                    return type;
                }
            }
            return null;
        }
    }

    private String samplingType = "";
    private String samplingArgs = "";
    private Double radius = null;
    private SpeedType speed = new SpeedType(1000, Units.RPM);
    private ManeuverLocation location = new ManeuverLocation();

    private Double samplerRadius = 10.0;
    private Double samplerSpeed = 0.0;
    private DorisType dorisType = DorisType.DRIFT;
    private Double dorisBearing = 0.0;

    @Override
    public Object clone() {
        Sampling clone = new Sampling();
        super.clone(clone);
        clone.setManeuverLocation(getManeuverLocation().clone());
        clone.setSpeed(getSpeed());
        clone.setSamplingType(getSamplingType());
        clone.setSamplingArgs(getSamplingArgs());
        clone.setSamplerRadius(getSamplerRadius());
        clone.setSamplerSpeed(getSamplerSpeed());
        clone.setDorisType(getDorisType());
        clone.setDorisBearing(getDorisBearing());
        return clone;
    }

    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);
        root.addAttribute("kind", "automatic");

        Element basePoint = root.addElement("basePoint");
        Element point = getManeuverLocation().asElement("point");
        basePoint.add(point);
        Element radTolerance = basePoint.addElement("radiusTolerance");
        radTolerance.setText("0");
        basePoint.addAttribute("type", "pointType");

        
        // Element trajectory = root.addElement("trajectory");
        // Element trajRadius = trajectory.addElement("radius");
        // trajRadius.setText(String.valueOf(getRadius()));
        // trajRadius.addAttribute("type", "float");

        root.addElement("samplingType").setText(getSamplingType());
        root.addElement("samplingArgs").setText(getSamplingArgs());

        SpeedType.addSpeedElement(root, this);

        return document;
    }

    @Override
    public String getType() {
        return XML_ROOT;
    }

    @Override
    public void loadManeuverFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);

            Node node = doc.selectSingleNode(XML_ROOT + "/basePoint/point");
            ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());
            setManeuverLocation(loc);

            SpeedType.parseManeuverSpeed(doc.getRootElement(), this);

            setSamplingType(doc.selectSingleNode(XML_ROOT + "/samplingType").getText());
            setSamplingArgs(doc.selectSingleNode(XML_ROOT + "/samplingArgs").getText());
            parseSamplingArgs();
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
        }
    }

    private void parseSamplingArgs() {
        if (samplingArgs == null || samplingArgs.isEmpty())
            return;

        for (String arg : samplingArgs.split("[,;]")) {
            String[] keyValue = arg.split("=", 2);
            if (keyValue.length != 2)
                continue;

            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            try {
                switch (key) {
                    case "Radius":
                        samplerRadius = Double.parseDouble(value);
                        break;
                    case "Speed":
                        samplerSpeed = Double.parseDouble(value);
                        break;
                    case "Type":
                        dorisType = DorisType.valueOf(value);
                        break;
                    case "Bearing":
                        dorisBearing = Double.parseDouble(value);
                        break;
                }
            }
            catch (Exception e) {
                NeptusLog.pub().warn("Failed to parse sampling arg: " + key + "=" + value);
            }
        }
    }

    @Override
    public ManeuverLocation getManeuverLocation() {
        return location.clone();
    }

    @Override
    public ManeuverLocation getStartLocation() {
        return location.clone();
    }

    @Override
    public ManeuverLocation getEndLocation() {
        return location.clone();
    }

    public void setManeuverLocation(ManeuverLocation location) {
        this.location = location.clone();
    }

    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        location.translatePosition(offsetNorth, offsetEast, offsetDown);
    }

    @Override
    public boolean needsPropertyReload(String propertyName) {
        return "Sampling Type".equals(propertyName) || "Doris Type".equals(propertyName);
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> props = new Vector<>();

        DefaultProperty speed = PropertiesEditor.getPropertyInstance("Speed", SpeedType.class, this.speed, true);
        speed.setShortDescription(I18n.text("The vehicle's desired speed while approaching the sampling point"));
        props.add(speed);

        String[] validSamplers = null;
        if (!vehicles.isEmpty()) {
            validSamplers = PlanUtil.getValidSamplersForVehicle(vehicles.get(0));
        }
        if (validSamplers == null || validSamplers.length == 0) {
            validSamplers = new String[] { "RedX", "WhiteX", "Doris" };
        }

        DefaultProperty samplingType = PropertiesEditor.getPropertyInstance("Sampling Type", String.class,
                this.samplingType, true);
        samplingType.setShortDescription(I18n.text("Type of sampler to use in maneuver."));
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(samplingType, new ComboEditor<String>(validSamplers));
        props.add(samplingType);

        SamplerType currentSampler = SamplerType.fromString(this.samplingType);
        if (currentSampler != null) {
            switch (currentSampler) {
                case REDX:
                case WHITEX:
                    DefaultProperty radius = PropertiesEditor.getPropertyInstance("Radius", Double.class,
                            samplerRadius, true);
                    radius.setShortDescription(I18n.text("Sampling radius in meters."));
                    props.add(radius);

                    DefaultProperty samplerSpeed = PropertiesEditor.getPropertyInstance("Sampler Speed", Double.class,
                            this.samplerSpeed, true);
                    samplerSpeed.setShortDescription(I18n.text("Sampler speed in m/s."));
                    props.add(samplerSpeed);
                    break;
                case DORIS:
                    DefaultProperty dorisTypeProp = PropertiesEditor.getPropertyInstance("Doris Type", DorisType.class,
                            this.dorisType, true);
                    dorisTypeProp.setShortDescription(I18n.text("Doris operation type (Drift or Move)."));
                    PropertiesEditor.getPropertyEditorRegistry().registerEditor(dorisTypeProp, new ComboEditor<DorisType>(DorisType.values()));
                    props.add(dorisTypeProp);

                    DefaultProperty dorisRadius = PropertiesEditor.getPropertyInstance("Radius", Double.class,
                            samplerRadius, true);
                    dorisRadius.setShortDescription(I18n.text("Sampling radius in meters."));
                    props.add(dorisRadius);

                    DefaultProperty dorisSpeed = PropertiesEditor.getPropertyInstance("Sampler Speed", Double.class,
                            this.samplerSpeed, true);
                    dorisSpeed.setShortDescription(I18n.text("Sampler speed in m/s."));
                    props.add(dorisSpeed);

                    if (this.dorisType == DorisType.MOVE) {
                        DefaultProperty bearing = PropertiesEditor.getPropertyInstance("Bearing", Double.class,
                                dorisBearing, true);
                        bearing.setShortDescription(I18n.text("Bearing in degrees."));
                        props.add(bearing);
                    }
                    break;
            }
        }

        return props;
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);

        for (Property p : properties) {

            if (p.getName().equals("Speed")) {
                setSpeed((SpeedType) p.getValue());
                continue;
            }

            if (p.getName().equals("Sampling Type")) {
                setSamplingType((String) p.getValue());
                continue;
            }

            if (p.getName().equals("Sampling Args")) {
                setSamplingArgs((String) p.getValue());
                continue;
            }

            if (p.getName().equals("Radius")) {
                setSamplerRadius((Double) p.getValue());
                continue;
            }

            if (p.getName().equals("Sampler Speed")) {
                setSamplerSpeed((Double) p.getValue());
                continue;
            }

            if (p.getName().equals("Doris Type")) {
                DorisType oldType = this.dorisType;
                DorisType newType = (DorisType) p.getValue();
                setDorisType(newType);

                if (oldType == DorisType.MOVE && newType == DorisType.DRIFT) {
                    this.dorisBearing = 0.0;
                }
                continue;
            }

            if (p.getName().equals("Bearing")) {
                setDorisBearing((Double) p.getValue());
                continue;
            }
        }

        updateSamplingArgsFromFields();
    }

    private void updateSamplingArgsFromFields() {
        SamplerType currentSampler = SamplerType.fromString(samplingType);
        if (currentSampler == null) {
            return;
        }

        StringBuilder args = new StringBuilder();

        switch (currentSampler) {
            case REDX:
            case WHITEX:
                if (samplerRadius != null) {
                    args.append("Radius=").append(samplerRadius);
                }
                if (samplerSpeed != null) {
                    if (args.length() > 0) args.append(", ");
                    args.append("Speed=").append(samplerSpeed);
                }
                break;
            case DORIS:
                if (dorisType != null) {
                    args.append("Type=").append(dorisType.name());
                }
                if (samplerRadius != null) {
                    if (args.length() > 0) args.append(", ");
                    args.append("Radius=").append(samplerRadius);
                }
                if (dorisType == DorisType.DRIFT) {
                    if (samplerSpeed != null) {
                        if (args.length() > 0) args.append(", ");
                        args.append("Speed=").append(samplerSpeed);
                    }
                }
                else if (dorisType == DorisType.MOVE) {
                    if (samplerSpeed != null) {
                        if (args.length() > 0) args.append(", ");
                        args.append("Speed=").append(samplerSpeed);
                    }
                    if (dorisBearing != null) {
                        if (args.length() > 0) args.append(", ");
                        args.append("Bearing=").append(dorisBearing);
                    }
                }
                break;
        }

        this.samplingArgs = args.toString();
        this.radius = parseRadius(this.samplingArgs);
    }

    @Override
    public String getTooltipText() {
        return super.getTooltipText() + "<hr>"
                + "<br>" + I18n.text("speed") + ": <b>" + speed + "</b>"
                + "<br>" + I18n.text("sampling type") + ": <b>" + samplingType + "</b>"
                + "<br>" + I18n.text("sampling args") + ": <b>" + samplingArgs + "</b><br>";
    }

    public LocationType getLocation() {
        return location;
    }

    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);
        AffineTransform at = g2d.getTransform();
        g2d.drawLine(-4, -4, 4, 4);
        g2d.drawLine(-4, 4, 4, -4);
        
        if (!hasRadius())
            return;
        double radius = getRadius() * renderer.getZoom();
        SamplerType currentSampler = SamplerType.fromString(samplingType);
        boolean isDorisMove = currentSampler == SamplerType.DORIS && dorisType == DorisType.MOVE;
        if (!isDorisMove) {
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.fill(new Ellipse2D.Double(-radius, -radius, radius * 2, radius * 2));
            g2d.setColor(Color.blue.darker());
            g2d.draw(new Ellipse2D.Double(-radius, -radius, radius * 2, radius * 2));
            g2d.setTransform(at);
        } else {
            double length = radius * 2;
            double bearingRad = Math.toRadians(dorisBearing);
            double x1 = -length / 2 * Math.sin(bearingRad);
            double y1 = length / 2 * Math.cos(bearingRad);
            double x2 = length / 2 * Math.sin(bearingRad);
            double y2 = -length / 2 * Math.cos(bearingRad);
            g2d.setColor(Color.blue.darker());
            g2d.draw(new Line2D.Double(x1, y1, x2, y2));
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.fill(new Ellipse2D.Double(x1 - 3, y1 - 3, 6, 6));
            g2d.fill(new Ellipse2D.Double(x2 - 3, y2 - 3, 6, 6));
            g2d.setColor(Color.blue.darker());
            g2d.draw(new Ellipse2D.Double(x1 - 3, y1 - 3, 6, 6));
            g2d.draw(new Ellipse2D.Double(x2 - 3, y2 - 3, 6, 6));
            g2d.setTransform(at);
        }
    }

    @Override
    public void parseIMCMessage(IMCMessage message) {

        ManeuverLocation pos = new ManeuverLocation();
        pos.setLatitudeRads(message.getDouble("lat"));
        pos.setLongitudeRads(message.getDouble("lon"));
        pos.setZ(message.getDouble("z"));
        String zunits = message.getString("z_units");
        if (zunits != null) {
            pos.setZUnits(ManeuverLocation.Z_UNITS.valueOf(zunits));
        }
        setManeuverLocation(pos);

        speed = SpeedType.parseImcSpeed(message);
        setSamplingType(message.getString("sampling_type"));
        setSamplingArgs(message.getString("sampling_args"));
        setCustomSettings(message.getTupleList("custom"));
    }

    @Override
    public IMCMessage serializeToIMC() {
        pt.lsts.imc.Sampling message = new pt.lsts.imc.Sampling();
        LocationType loc = getManeuverLocation();
        loc.convertToAbsoluteLatLonDepth();
        message.setLat(loc.getLatitudeRads());
        message.setLon(loc.getLongitudeRads());
        message.setZ(getManeuverLocation().getZ());
        message.setZUnits(ZUnits.valueOf(getManeuverLocation().getZUnits().name()));

        speed.setSpeedToMessage(message);

        message.setSamplingType(getSamplingType());
        message.setSamplingArgs(getSamplingArgs());
        message.setCustom(getCustomSettings());
        return message;
    }

    @Override
    public double getCompletionTime(LocationType initialPosition) {
        return getDistanceTravelled(initialPosition) / speed.getMPS();
    }

    @Override
    public double getDistanceTravelled(LocationType initialPosition) {
        return getStartLocation().getDistanceInMeters(initialPosition);
    }

    @Override
    public double getMaxDepth() {
        return getManeuverLocation().getAllZ();
    }

    @Override
    public double getMinDepth() {
        return getManeuverLocation().getAllZ();
    }

    @Override
    public Collection<ManeuverLocation> getWaypoints() {
        return Collections.singleton(getStartLocation());
    }

    @Override
    public SpeedType getSpeed() {
        return new SpeedType(speed);
    }

    @Override
    public void setSpeed(SpeedType speed) {
        this.speed = new SpeedType(speed);
    }

    public String getSamplingType() {
        return samplingType;
    }

    public void setSamplingType(String samplingType) {
        this.samplingType = samplingType == null ? "" : samplingType;
    }

    public String getSamplingArgs() {
        return samplingArgs;
    }

    public void setSamplingArgs(String samplingArgs) {
        this.samplingArgs = samplingArgs == null ? "" : samplingArgs;
        this.radius = parseRadius(this.samplingArgs);
    }

    public Double getRadius() {
        return radius;
    }

    public boolean hasRadius() {
        return radius != null;
    }

    public Double getSamplerRadius() {
        return samplerRadius;
    }

    public void setSamplerRadius(Double samplerRadius) {
        this.samplerRadius = samplerRadius;
    }

    public Double getSamplerSpeed() {
        return samplerSpeed;
    }

    public void setSamplerSpeed(Double samplerSpeed) {
        this.samplerSpeed = samplerSpeed;
    }

    public DorisType getDorisType() {
        return dorisType;
    }

    public void setDorisType(DorisType dorisType) {
        this.dorisType = dorisType;
    }

    public Double getDorisBearing() {
        return dorisBearing;
    }

    public void setDorisBearing(Double dorisBearing) {
        this.dorisBearing = dorisBearing;
    }

    public String validateSamplerRadius(double value) {
        if (value <= 0)
            return I18n.text("Radius must be greater than 0");
        return null;
    }

    public String validateSamplerSpeed(double value) {
        if (value <= 0)
            return I18n.text("Speed must be greater than 0");
        return null;
    }

    private Double parseRadius(String samplingArgs) {
        for (String arg : samplingArgs.split("[,;]")) {
            String[] keyValue = arg.split("=", 2);
            if (keyValue.length != 2 || !"Radius".equals(keyValue[0].trim())) {
                continue;
            }

            try {
                return Double.valueOf(keyValue[1].trim());
            }
            catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }
}
