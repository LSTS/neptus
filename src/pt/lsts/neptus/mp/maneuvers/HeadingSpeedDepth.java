/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Paulo Dias
 * 2008/09/20
 */
package pt.lsts.neptus.mp.maneuvers;

import java.util.LinkedHashMap;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.editor.ComboEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.Bitmask;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author pdias
 * 
 */
public class HeadingSpeedDepth extends DefaultManeuver implements IMCSerialization {

    double speed = 1000, speedTolerance = 100, depth = 1.5, heading = -1;
    int duration = 10;
    String units = "RPM";
    boolean useHeading = true, useSpeed = true, useDepth = true;
    protected static final String DEFAULT_ROOT_ELEMENT = "HeadingSpeedDepth";

    private final int ANGLE_CALCULATION = -1;
    private final int FIRST_ROTATE = 0;
    private final int HORIZONTAL_MOVE = 1;

    int current_state = ANGLE_CALCULATION;

    private double targetAngle, rotateIncrement, startTime = -1;

    // private GotoParameters params = new GotoParameters();

    /**
	 * 
	 */
    public HeadingSpeedDepth() {
        // TODO Auto-generated constructor stub
    }

    public String getType() {
        return "HeadingSpeedDepth";
    }

    public Document getManeuverAsDocument(String rootElementName) {

        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);
        root.addAttribute("kind", "automatic");

        if (!isUseHeading())
            root.addAttribute("useHeading", "false");
        if (!isUseSpeed())
            root.addAttribute("useSpeed", "false");
        if (!isUseDepth())
            root.addAttribute("useDepth", "false");

        Element depth = root.addElement("depth");
        depth.setText(String.valueOf(getDepth()));

        Element duration = root.addElement("duration");
        duration.setText(String.valueOf(getDuration()));

        Element velocity = root.addElement("speed");
        velocity.addAttribute("tolerance", String.valueOf(getSpeedTolerance()));
        velocity.addAttribute("type", "float");
        velocity.addAttribute("unit", getUnits());
        velocity.setText(String.valueOf(getSpeed()));

        Element heading = root.addElement("heading");
        heading.setText(String.valueOf(getHeading()));

        return document;
    }

    public void loadFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            Node rootMnv = doc.selectSingleNode("HeadingSpeedDepth");
            boolean speedFound = true;
            if (rootMnv == null) {
                doc.selectSingleNode("HeadingVelocityDepth");
                speedFound = false;
            }
            else
                speedFound = true;
            setDepth(Double.parseDouble(rootMnv.selectSingleNode("depth").getText()));
            setHeading(Double.parseDouble(rootMnv.selectSingleNode("heading").getText()));
            Node speedNode = speedFound ? rootMnv.selectSingleNode("speed") : rootMnv.selectSingleNode("velocity");
            Node durNode = rootMnv.selectSingleNode("duration");
            if (durNode != null)
                setDuration(Integer.parseInt(durNode.getText()));
            setSpeed(Double.parseDouble(speedNode.getText()));
            setUnits(speedNode.valueOf("@unit"));
            setSpeedTolerance(Double.parseDouble(speedNode.valueOf("@tolerance")));

            Node flagNode = rootMnv.selectSingleNode("@useHeading");
            if (flagNode != null)
                setUseHeading(Boolean.parseBoolean(flagNode.getText()));
            else
                setUseHeading(true);
            flagNode = speedFound ? rootMnv.selectSingleNode("@useSpeed") : rootMnv.selectSingleNode("@useVelocity");
            if (flagNode != null)
                setUseSpeed(Boolean.parseBoolean(flagNode.getText()));
            else
                setUseSpeed(true);
            flagNode = rootMnv.selectSingleNode("@useDepth");
            if (flagNode != null)
                setUseDepth(Boolean.parseBoolean(flagNode.getText()));
            else
                setUseDepth(true);

        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            return;
        }
    }

    private int count = 0;

    public SystemPositionAndAttitude ManeuverFunction(SystemPositionAndAttitude lastVehicleState) {

        SystemPositionAndAttitude nextVehicleState = (SystemPositionAndAttitude) lastVehicleState.clone();

        switch (current_state) {

            case (ANGLE_CALCULATION):
                if (getHeading() < 0) {
                    count = 0;
                    this.current_state = HORIZONTAL_MOVE;
                    nextVehicleState = ManeuverFunction(lastVehicleState);
                    break;
                }
                targetAngle = Math.toRadians(getHeading());

                double angleDiff = (targetAngle - lastVehicleState.getYaw());

                while (angleDiff < 0)
                    angleDiff += Math.PI * 2; // 360º

                while (angleDiff > Math.PI * 2)
                    angleDiff -= Math.PI * 2;

                if (angleDiff > Math.PI)
                    angleDiff = angleDiff - Math.PI * 2;

                rotateIncrement = angleDiff / 3;// (-25.0f / 180.0f) * (float)
                                                // Math.PI;
                count = 0;
                this.current_state = FIRST_ROTATE;
                startTime = System.currentTimeMillis() / 1000d;
                nextVehicleState = ManeuverFunction(lastVehicleState);
                break;

            // Initial rotation towards the target point
            case FIRST_ROTATE:
                if (count++ < 3)
                    nextVehicleState.rotateXY(rotateIncrement);
                else {
                    nextVehicleState.setYaw(targetAngle);
                    current_state = HORIZONTAL_MOVE;
                }
                break;

            // The movement between the initial and final point, in the plane xy
            // (horizontal)
            case HORIZONTAL_MOVE:
                double calculatedVelocity = 1;

                if (units.equals("m/s"))
                    calculatedVelocity = speed;
                else if (units.equals("RPM"))
                    calculatedVelocity = speed / 500.0;
                double deltaTime = System.currentTimeMillis() / 1000d - startTime;
                if (deltaTime >= getDuration()) {
                    nextVehicleState.setPosition(lastVehicleState.getPosition());
                    endManeuver();
                }
                else {
                    nextVehicleState.moveForward(calculatedVelocity);
                    double depthDiff = getDepth() - nextVehicleState.getPosition().getDepth();

                    double depthIncr = Math.min(depthDiff, calculatedVelocity);
                    double curDepth = nextVehicleState.getPosition().getDepth();
                    nextVehicleState.getPosition().setDepth(curDepth + depthIncr);
                }
                break;

            default:
                endManeuver();
        }

        return nextVehicleState;
    }

    /**
     * @return the useHeading
     */
    public boolean isUseHeading() {
        return useHeading;
    }

    /**
     * @param useHeading the useHeading to set
     */
    public void setUseHeading(boolean useHeading) {
        this.useHeading = useHeading;
    }

    /**
     * @return the useSpeed
     */
    public boolean isUseSpeed() {
        return useSpeed;
    }

    /**
     * @param useSpeed the useSpeed to set
     */
    public void setUseSpeed(boolean useSpeed) {
        this.useSpeed = useSpeed;
    }

    /**
     * @return the useDepth
     */
    public boolean isUseDepth() {
        return useDepth;
    }

    /**
     * @param useDepth the useDepth to set
     */
    public void setUseDepth(boolean useDepth) {
        this.useDepth = useDepth;
    }

    public double getDepth() {
        return depth;
    }

    public void setDepth(double depth) {
        this.depth = depth;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getSpeedTolerance() {
        return speedTolerance;
    }

    public void setSpeedTolerance(double speedTolerance) {
        this.speedTolerance = speedTolerance;
    }

    public double getHeading() {
        return heading;
    }

    public void setHeading(double heading) {
        this.heading = heading;
    }

    /**
     * @return the duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * @param duration the duration to set
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Object clone() {
        HeadingSpeedDepth clone = new HeadingSpeedDepth();
        super.clone(clone);
        // clone.params = params;
        clone.setDepth(getDepth());
        clone.setDuration(getDuration());
        clone.setHeading(getHeading());
        clone.setUnits(getUnits());
        clone.setSpeed(getSpeed());
        clone.setSpeedTolerance(getSpeedTolerance());

        clone.setUseHeading(isUseHeading());
        clone.setUseSpeed(isUseSpeed());
        clone.setUseDepth(isUseDepth());

        return clone;
    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        Vector<DefaultProperty> properties = new Vector<DefaultProperty>();

        DefaultProperty units = PropertiesEditor.getPropertyInstance("Speed units", String.class, getUnits(), true);
        units.setShortDescription(I18n.text("The speed units"));
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(units,
                new ComboEditor<String>(new String[] { "RPM", "m/s", "%" }));

        properties.add(PropertiesEditor.getPropertyInstance("Use Speed", Boolean.class, isUseHeading(), true));
        properties.add(PropertiesEditor.getPropertyInstance("Speed", Double.class, getSpeed(), true));
        properties.add(units);

        properties
                .add(PropertiesEditor.getPropertyInstance("Speed tolerance", Double.class, getSpeedTolerance(), true));

        properties.add(PropertiesEditor.getPropertyInstance("Use Depth", Boolean.class, isUseDepth(), true));
        properties.add(PropertiesEditor.getPropertyInstance("Depth", Double.class, getDepth(), true));

        DefaultProperty heading = PropertiesEditor.getPropertyInstance("Heading (degrees)", Double.class, getHeading(),
                true);

        properties.add(PropertiesEditor.getPropertyInstance("Use Heading", Boolean.class, isUseHeading(), true));
        // PropertiesEditor.getPropertyEditorRegitry().registerEditor(units, new ComboEditor(new String[] {"RPM", "m/s",
        // "%"}));
        properties.add(heading);

        properties.add(PropertiesEditor.getPropertyInstance("Duration", "", Integer.class, getDuration(), true,
                I18n.text("Use 0 for infinity")));

        return properties;
    }

    public String getPropertiesDialogTitle() {
        return getId() + " parameters";
    }

    public void setProperties(Property[] properties) {

        super.setProperties(properties);

        for (Property p : properties) {
            if (p.getName().equals("Use Speed")) {
                setUseSpeed((Boolean) p.getValue());
            }
            if (p.getName().equals("Speed units")) {
                setUnits((String) p.getValue());
            }
            if (p.getName().equals("Speed tolerance")) {
                setSpeedTolerance((Double) p.getValue());
            }
            if (p.getName().equals("Speed")) {
                setSpeed((Double) p.getValue());
            }
            if (p.getName().equals("Use Depth")) {
                setUseDepth((Boolean) p.getValue());
            }
            if (p.getName().equals("Depth")) {
                setDepth((Double) p.getValue());
            }
            if (p.getName().equals("Use Heading")) {
                setUseHeading((Boolean) p.getValue());
            }
            if (p.getName().equals("Heading (degrees)")) {
                setHeading((Double) p.getValue());
            }
            if (p.getName().equals("Duration")) {
                setDuration((Integer) p.getValue());
            }
        }
    }

    public String[] getPropertiesErrors(Property[] properties) {
        return super.getPropertiesErrors(properties);
    }

    @Override
    public String getTooltipText() {

        return super.getTooltipText() + "<hr>" + "heading: <b>" + (int) getHeading() + " \u00B0</b>" + "<br>speed: <b>"
                + getSpeed() + " " + getUnits() + "</b>" + "<br>depth: <b>" + (int) getDepth() + " m</b>"
                + "<br>duration: <b>" + getDuration() + " s</b>" + "<br>using: <b>"
                + (isUseHeading() ? " heading" : "") + (isUseSpeed() ? " speed" : "") + (isUseDepth() ? " depth" : "")
                + "</b>";
    }

    @Override
    public void parseIMCMessage(IMCMessage message) {
        setMaxTime((int) message.getDouble("timeout"));
        setDepth(message.getDouble("z"));
        setSpeed(message.getDouble("speed"));
        setHeading(message.getDouble("heading"));
        setDuration((int) message.getDouble("duration"));

        String speed_units = message.getString("speed_units");
        if (speed_units.equals("METERS_PS"))
            setUnits("m/s");
        else if (speed_units.equals("RPM"))
            setUnits("RPM");
        else
            setUnits("%");

        LinkedHashMap<String, Boolean> indValue = message.getBitmask("ind");
        if (indValue.get("HEADING"))
            setUseHeading(true);
        else
            setUseHeading(false);
        if (indValue.get("Z"))
            setUseDepth(true);
        else
            setUseDepth(false);
        if (indValue.get("SPEED"))
            setUseSpeed(true);
        else
            setUseSpeed(false);
    }

    public IMCMessage serializeToIMC() {

        // FIXME this has nothing to do with the IMC specification!!

        // IMCMessage msgManeuver = IMCMessage.getParser().createMessage("Dive");
        IMCMessage msgManeuver = IMCDefinition.getInstance().create("LowLevelControl");
        // msgManeuver.setValue("timeout", new NativeUINT16(this.getMaxTime()));

        msgManeuver.setValue("duration", getDuration());

        msgManeuver.setValue("z", this.getDepth());

        msgManeuver.setValue("velocity", this.getSpeed());
        msgManeuver.setValue("speed", this.getSpeed());
        String enumerated = "";
        String speedU = this.getUnits();
        try {
            if ("m/s".equalsIgnoreCase(speedU))
                enumerated = "METERS_PS";
            else if ("RPM".equalsIgnoreCase(speedU))
                enumerated = "RPM";
            else if ("%".equalsIgnoreCase(speedU))
                enumerated = "PERCENTAGE";
            else if ("percentage".equalsIgnoreCase(speedU))
                enumerated = "PERCENTAGE";
        }
        catch (Exception ex) {
            NeptusLog.pub().error(this, ex);
        }
        msgManeuver.setValue("speed_units", enumerated);

        // msgManeuver.setValue("velocity", new NativeFLOAT(this.getSpeed()));
        // Enumerated enumerated = (Enumerated) msgManeuver.getValueAsObject("velocity_units");
        // String velU = this.getUnits();
        // try {
        // if ("m/s".equalsIgnoreCase(velU))
        // enumerated.setCurrentValue("METERS_PS");
        // else if ("RPM".equalsIgnoreCase(velU))
        // enumerated.setCurrentValue("RPM");
        // else if ("%".equalsIgnoreCase(velU))
        // enumerated.setCurrentValue("PERCENTAGE");
        // else if ("percentage".equalsIgnoreCase(velU))
        // enumerated.setCurrentValue("PERCENTAGE");
        // }
        // catch (Exception ex) {
        // NeptusLog.pub().error(this, ex);
        // }
        // msgManeuver.setValue("velocity_units", enumerated);

        msgManeuver.setValue("heading", Math.toRadians(this.getHeading()));

        // NativeTupleList ntl = new NativeTupleList();
        // TupleList tl = new TupleList();
        // FIXME commented line above for translation to new Message TupleList format
        LinkedHashMap<String, Object> tl = new LinkedHashMap<String, Object>();

        for (String key : getCustomSettings().keySet())
            tl.put(key, getCustomSettings().get(key));
        msgManeuver.setValue("custom", IMCMessage.encodeTupleList(tl));

        long currentValue = 0x7L;
        LinkedHashMap<Long, String> possibleValues = new LinkedHashMap<Long, String>();
        possibleValues.put(0x1L, "HEADING");
        possibleValues.put(0x2L, "Z");
        possibleValues.put(0x4L, "SPEED");
        Bitmask bm = new Bitmask(possibleValues, currentValue);
        bm.setBit("HEADING", isUseHeading());
        bm.setBit("Z", isUseDepth());
        bm.setBit("SPEED", isUseSpeed());
        msgManeuver.setValue("ind", bm);

        return msgManeuver;
    }

    // public static void main(String[] args) {
    // ConfigFetch.initialize();
    // SerializationBufferImpl sb = new SerializationBufferImpl();
    // try {
    // IMCMessage msgManeuver = IMCDefinition.getInstance().create("ElementalManeuver");
    // msgManeuver.setValue("z", 2);
    // msgManeuver.setValue("velocity", 100.0);
    // msgManeuver.serialize(sb);
    // msgManeuver.dump(System.err);
    // ByteUtil.dumpAsHex(msgManeuver.getType().getShortName(), sb.getBuffer(), System.err);
    // IMCMessage ret = new IMCMessage();
    // ret.unserialize(sb, sb.getLength());
    // ret.dump(System.err);
    // }
    // catch (MiddlewareSerializationException e) {
    // e.printStackTrace();
    // }
    // }

}
