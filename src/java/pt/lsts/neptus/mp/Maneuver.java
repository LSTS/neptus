/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 14/Set/2004
 */
package pt.lsts.neptus.mp;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.table.TableCellRenderer;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.gui.editor.CoordinatesPropertyEditor;
import pt.lsts.neptus.gui.editor.ZUnitsEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.actions.PlanActions;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.maneuvers.ManeuversUtil;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.XmlOutputMethods;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.NameNormalizer;

/**
 * This is the superclass of every Maneuver To create a new maneuver, all the abstract classes must be implemented
 * 
 * @author Ze Carlos (LSTS-FEUP)
 */

public abstract class Maneuver implements XmlOutputMethods, PropertiesProvider, Comparable<Maneuver> {
    protected static final Color COLOR_HELP = new Color(255, 125, 255);
    protected static final int X = 0, Y = 1, Z = 2, T = 3;
    protected ArrayList<VehicleType> vehicles = new ArrayList<>();
    
    public static final String CT_STRING = "String";
    public static final String CT_NUMBER = "Number";
    public static final String CT_BOOLEAN = "Boolean";
    public static final double circleDiam = 10;

    private static Random rnd = new Random(System.currentTimeMillis());

    public String id = NameNormalizer.getRandomID();

    private int maxTime = 10000, minTime = 0; // The maxinum number of seconds that any maneuver can take
    protected LinkedHashMap<String, String> customSettings = new LinkedHashMap<String, String>();
    protected LinkedHashMap<String, String> customSettingsTypeHint = new LinkedHashMap<String, String>();
    private int xPosition = rnd.nextInt(500), yPosition = rnd.nextInt(300);
    private boolean ManeuverEnded = false, initialManeuver = false;
    private Hashtable<String, String> transitions = new Hashtable<String, String>();
    private MissionType missionType = null;
    
    // JDialog dialog;
    protected static final String DEFAULT_ROOT_ELEMENT = "node";

    protected PlanActions startActions = new PlanActions();
    protected PlanActions endActions = new PlanActions();

    /**
     * Class constructor
     */
    public Maneuver() {
        super();
    }

    GradientPaint paint1 = new GradientPaint((float) -circleDiam, (float) -circleDiam, Color.gray, (float) circleDiam,
            (float) circleDiam, Color.white);
    GradientPaint paint2 = new GradientPaint((float) -circleDiam, (float) -circleDiam, Color.blue, (float) circleDiam,
            (float) circleDiam, Color.white);
    GradientPaint paint3 = new GradientPaint((float) -circleDiam - 2, (float) -circleDiam - 2, Color.yellow,
            (float) circleDiam + 4, (float) circleDiam + 4, Color.green);
    GradientPaint paint4 = new GradientPaint((float) -circleDiam - 2, (float) -circleDiam - 2, Color.white,
            (float) circleDiam + 4, (float) circleDiam + 4, Color.red);

    Ellipse2D.Double ellis = new Ellipse2D.Double(-circleDiam / 2, -circleDiam / 2, circleDiam, circleDiam);
    Ellipse2D.Double biggerEllis = new Ellipse2D.Double(-circleDiam / 2 - 2, -circleDiam / 2 - 2, circleDiam + 4,
            circleDiam + 4);
    Rectangle2D.Double activeRect = new Rectangle2D.Double(-circleDiam / 2 - 4, -circleDiam / 2 - 4, circleDiam + 8,
            circleDiam + 8);

    /**
     * Looks for other maneuvers that are located in the same position as this maneuver (only considers the horizontal
     * distance)
     * 
     * @param plan The plan to look for maneuvers
     * @param includeThisManeuver If this maneuvers is to be included in the returned array
     * @return An arrays of Maneuvers with all maneuvers that have the same exact position as this maneuver (the current
     *         maneuver is not returned). If no maneuver was found to be in this position, returns an empty array of
     *         maneuvers.
     */
    public Maneuver[] getSamePositionManeuvers(PlanType plan, boolean includeThisManeuver) {

        if (!(this instanceof LocatedManeuver)) {
            return new Maneuver[0];
        }

        Vector<Maneuver> samePosMans = new Vector<Maneuver>();
        Maneuver[] allMans = plan.getGraph().getAllManeuvers();

        LocationType myPos = ((LocatedManeuver) this).getManeuverLocation();

        for (Maneuver m : allMans) {
            if (m instanceof LocatedManeuver) {
                if (!includeThisManeuver && m.getId().equals(getId()))
                    continue;

                double dist = ((LocatedManeuver) m).getManeuverLocation().getHorizontalDistanceInMeters(myPos);
                if ((float) dist < 0.01)
                    samePosMans.add(m);
            }
        }

        return samePosMans.toArray(new Maneuver[0]);
    }

    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        if (this instanceof LocatedManeuver) {
            if (planElement.isBeingEdited()) {
                g2d.setPaint(paint2);
                g2d.fill(ellis);
            }
            else {
                g2d.setPaint(paint1);
                g2d.fill(ellis);
            }
        }
        else {
            g2d.setPaint(paint1);
            g2d.fill(ellis);
        }
        
        if (planElement.getActiveManeuver() != null && planElement.getActiveManeuver().equals(getId())) {
            g2d.setColor(Color.yellow);
            g2d.fill(activeRect);
            g2d.setColor(new Color(150, 0, 0));
            g2d.draw(biggerEllis);
        }

        if (isInitialManeuver()) {
            if (planElement.isBeingEdited()) {
                g2d.setPaint(paint3);
                g2d.fill(biggerEllis);
                g2d.setColor(new Color(0, 150, 0));
                g2d.draw(biggerEllis);
            }
            else {
                g2d.setColor(Color.green.darker().darker());
                g2d.fill(biggerEllis);
            }
        }
        
        if (planElement.getSelectedManeuver() != null && planElement.getSelectedManeuver().equals(getId())) {
            g2d.setPaint(paint4);
            g2d.fill(biggerEllis);
            g2d.setColor(new Color(150, 0, 0));
            g2d.draw(biggerEllis);
        }
        else {
            g2d.setColor(Color.black);
            g2d.draw(ellis);
        }

        if (this instanceof LocatedManeuver) {
            ManeuverLocation loc = ((LocatedManeuver) this).getManeuverLocation();
            // g2d.setPaint(paint1);
            g2d.setColor(Color.black);
            double zVal = loc.getZ();
            switch (loc.getZUnits()) {
                case ALTITUDE:
                    g2d.fill(new Rectangle2D.Double(-8, 5, 16, 3));
                    g2d.setColor(Color.orange);
                    g2d.draw(new Line2D.Double(-6, 6, 6, 6));
                    if (zVal <= 0) {
                        Stroke st = g2d.getStroke();
                        g2d.setStroke(new BasicStroke(2));
                        g2d.setColor(Color.orange.brighter());
                        g2d.draw(new Line2D.Double(-6, -6, 6, 6));
                        g2d.draw(new Line2D.Double(-6, 6, 6, -6));
                        g2d.setStroke(st);
                    }
                    break;
                case DEPTH:
                    if (loc.getZ() == 0) {
                        g2d.fill(new Rectangle2D.Double(-10, -1, 20, 2));
                        g2d.setColor(Color.white);
                        g2d.draw(new Line2D.Double(-8, 0, 8, 0));
                    }
                    else {
                        g2d.fill(new Rectangle2D.Double(-8, -8, 16, 3));
                        g2d.setColor(Color.cyan.brighter());
                        g2d.draw(new Line2D.Double(-6, -6, 6, -6));
                        if (zVal <= 0) {
                            Stroke st = g2d.getStroke();
                            g2d.setStroke(new BasicStroke(2));
                            g2d.setColor(Color.orange.brighter());
                            g2d.draw(new Line2D.Double(-6, -6, 6, 6));
                            g2d.draw(new Line2D.Double(-6, 6, 6, -6));
                            g2d.setStroke(st);
                        }
                    }
                    break;
                case HEIGHT:
                    g2d.setColor(Color.white);
                    g2d.draw(new Line2D.Double(-6, 0, 6, 0));
                    g2d.draw(new Line2D.Double(0, -6, 0, 6));
                    break;
                default:
                    break;
            }
        }

        Font oldFont = g2d.getFont();
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        String text = getId();
        String depthStr = "";

        if (planElement.getSelectedManeuver() != null && planElement.getSelectedManeuver().equals(getId())
                && this instanceof LocatedManeuver) {
            ManeuverLocation loc = ((LocatedManeuver) this).getManeuverLocation();
            double depth = loc.getZ();
            switch (loc.getZUnits()) {
                case ALTITUDE:
                    depthStr = "[" + I18n.text("altitude") + ": " + depth + I18n.textc("m", "meters") + "]";
                    break;
                case DEPTH:
                    depthStr = "[" + I18n.text("depth") + ": " + depth + I18n.textc("m", "meters") + "]";
                    break;
                case HEIGHT:
                    depthStr = "[" + I18n.text("height") + ": " + depth + I18n.textc("m", "meters") + "]";
                    break;
                default:
                    depthStr = "[" + I18n.textc("z", "Maneuver's z value") + ": " + depth + I18n.textc("m", "meters") + "]";
                    break;
            }
            text += depthStr;
        }

        Maneuver[] posMans = getSamePositionManeuvers(planElement.getPlan(), true);

        if (posMans.length > 1) {
            text = posMans[0].getId();
            if (getId().equals(posMans[0].getId()))
                text += depthStr;
            for (int i = 1; i < posMans.length && i < 3; i++) {
                text += ", " + posMans[i].getId();
                if (getId().equals(posMans[i].getId()))
                    text += depthStr;
            }
        }
        if (posMans.length > 3) {
            text += "..." + posMans[posMans.length - 1].getId();
            if (getId().equals(posMans[posMans.length - 1].getId()))
                text += depthStr;
        }
        Rectangle2D stringBounds = g2d.getFontMetrics().getStringBounds(text, g2d);

        int x = (int) (-stringBounds.getWidth() / 2), y = (int) (stringBounds.getHeight() + 5);
        GuiUtils.drawText(text, x, y, Color.WHITE, Color.BLACK, g2d);
        g2d.setFont(oldFont);
    }

    public static Maneuver createFromXML(String xml) {
        Maneuver man = null;
        try {
            Document doc = DocumentHelper.parseText(xml);

            Element maneuver = doc.getRootElement().element("maneuver");
            Iterator<?> elementIterator = maneuver.elementIterator();

            while (elementIterator.hasNext()) {
                Element element = (Element) elementIterator.next();
                if (element.getName().equals("custom-settings") ||
                        element.getName().equals("minTime") ||
                        element.getName().equals("maxTime"))
                    continue;

                String manType = element.getName();

                try {
                    Class<Maneuver> manClass = ManeuversUtil.getManeuverFromType(manType);
                    man = manClass.newInstance();
                }
                catch (Exception e) {
                    NeptusLog.pub().error("Maneuver not found: " + manType + " (" + e.getMessage() + ")");
                }
                if (man == null)
                    return null;

                man.loadXMLWithSettings(doc);
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(System.err, e);
        }

        return man;
    }

    /**
     * Load XML from {@link #asXML()}
     * 
     * @param xml
     * @return
     */
    public boolean loadXMLWithSettings(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            return loadXMLWithSettings(doc.getRootElement());
        }
        catch (DocumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load XML from {@link #asDocument()}
     * 
     * @param doc
     * @return
     */
    public boolean loadXMLWithSettings(Document doc) {
        return loadXMLWithSettings(doc.getRootElement());
    }

    /**
     * Load XML from {@link #asElement()}
     * 
     * @param rootElm
     * @return
     */
    public boolean loadXMLWithSettings(Element rootElm) {
        Random rnd = new Random(GregorianCalendar.getInstance().getTimeInMillis());
        int xPos = rnd.nextInt(300), yPos = rnd.nextInt(250);
        Maneuver man = this;
        try {
            Document doc = rootElm.getDocument();

            String id = doc.selectSingleNode("./node()/id").getText();

            Node nd = doc.selectSingleNode("./node()/@xPos");
            if (nd != null)
                xPos = Integer.parseInt(nd.getText());

            nd = doc.selectSingleNode("./node()/@yPos");
            if (nd != null)
                yPos = Integer.parseInt(nd.getText());

            boolean isInitial = false;
            nd = doc.selectSingleNode("./node()/@start");

            if (nd != null && nd.getText().equals("true"))
                isInitial = true;

            Element maneuver = doc.getRootElement().element("maneuver");
            Iterator<?> elementIterator = maneuver.elementIterator();

            while (elementIterator.hasNext()) {
                Element element = (Element) elementIterator.next();
                if (element.getName().equals("custom-settings") ||
                        element.getName().equals("minTime") ||
                        element.getName().equals("maxTime"))
                    continue;

                // String manType = element.getName();

                man.setId(id);
                man.setInitialManeuver(isInitial);
                man.setXPosition(xPos);
                man.setYPosition(yPos);
                man.loadManeuverFromXML(element.asXML());
                man.loadFromXMLExtraParameters(element.getParent());

                nd = doc.selectSingleNode("./node()/actions/start-actions");
                if (nd != null) {
                    man.startActions.load((Element) nd);
                }
                nd = doc.selectSingleNode("./node()/actions/end-actions");
                if (nd != null) {
                    man.endActions.load((Element) nd);
                }
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(System.err, e);
            return false;
        }

        return true;
    }

    /**
     * @param xml
     */
    public void loadFromXMLExtraParameters(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            loadFromXMLExtraParameters(doc.getRootElement());
        }
        catch (DocumentException e) {
            NeptusLog.pub().error("Could not load extra parameters of maneuver");
        }
    }

    /**
     * Loads data excepts maneuver specific data (this is the parent note from maneuver
     * specific data from {@link #loadManeuverFromXML(String)})
     * 
     * @param maneuver
     */
    public void loadFromXMLExtraParameters(Element maneuver) {
        LinkedHashMap<String, String> custSettings = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> custSettingsTypeHint = new LinkedHashMap<String, String>();
        Iterator<?> elementIterator = maneuver.elementIterator();
        while (elementIterator.hasNext()) {

            Element element = (Element) elementIterator.next();
            if (element.getName().equals("minTime")) {
                minTime = Integer.parseInt(element.getText());
                continue;
            }
            if (element.getName().equals("maxTime")) {
                maxTime = Integer.parseInt(element.getText());
                continue;
            }
            if (element.getName().equals("custom-settings")) {
                List<?> lst = element.selectNodes("setting");
                custSettings.clear();
                for (Object node : lst) {
                    Element elem = (Element) node;
                    elem.getText();
                    Node n1 = elem.selectSingleNode("@name");
                    if (n1 == null)
                        continue;
                    custSettings.put(n1.getText(), elem.getText());
                    Node n2 = elem.selectSingleNode("@type-hint");
                    if (n2 == null)
                        continue;
                    String typeHint = n2.getText();
                    if (CT_NUMBER.equalsIgnoreCase(typeHint))
                        typeHint = CT_NUMBER;
                    else if (CT_BOOLEAN.equalsIgnoreCase(typeHint))
                        typeHint = CT_BOOLEAN;
                    else
                        typeHint = CT_STRING;
                    custSettingsTypeHint.put(n1.getText(), typeHint);
                }
                continue;
            }
        }
        setCustomSettings(custSettings);
        setCustomSettingsTypeHint(custSettingsTypeHint);
    }

    /**
     * @return the startActions
     */
    public PlanActions getStartActions() {
        return startActions;
    }

    /**
     * @return the endActions
     */
    public PlanActions getEndActions() {
        return endActions;
    }

    /**
     * The extending classes should provide a type name that will be shown to the user related to that maneuver.
     * This should match the IMC message maneuver abbreviation.
     */
    public String getType() {
        return getClass().getSimpleName();
    }

    /**
     * Clone this maneuver
     */
    public Object clone() {
        Maneuver m;
        try {
            m = getClass().newInstance();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        clone(m);
        m.loadManeuverFromXML(getManeuverXml());
        return m;
    }

    public Object clone(Maneuver clone) {
        clone.setMaxTime(getMaxTime());
        clone.setMinTime(getMinTime());
        clone.setXPosition(getXPosition());
        clone.setYPosition(getYPosition());
        clone.setMissionType(getMissionType());
        clone.vehicles.clear();
        clone.vehicles.addAll(vehicles);
        clone.setId(getId());
        clone.setInitialManeuver(isInitialManeuver());
        clone.setCustomSettings(getCustomSettings());
        clone.setCustomSettingsTypeHint(getCustomSettingsTypeHint());

        clone.startActions = new PlanActions();
        clone.startActions.load(startActions.asElement("start-actions"));
        clone.endActions = new PlanActions();
        clone.endActions.load(endActions.asElement("end-actions"));
        return clone;
    }

    /**
     * When the maneuver has ended, this function must be called to end the iteration
     */
    public void endManeuver() {
        ManeuverEnded = true;
    }

     /**
     * Verifies if the execution of this maneuver has ended...
     * 
     * @return <b>true</b> if the maneuver has already ended or <b>false</b> otherwise
     */
    public boolean hasEnded() {
        return this.ManeuverEnded;
    }

    public void addTransition(String target, String condition) {
        transitions.put(target, condition);
    }

    public void removeTransition(String target) {
        transitions.remove(target);
    }

    public String[] getReacheableManeuvers() {
        Object[] trans = transitions.keySet().toArray();
        String[] mans = new String[trans.length];
        for (int i = 0; i < trans.length; i++)
            mans[i] = (String) trans[i];
        return mans;
    }

    public String getTransitionCondition(String targetManeuver) {
        return (String) transitions.get(targetManeuver);
    }

    /**
     * @return Only the maneuver specific part the XML (no extra data output).
     */
    public String getManeuverXml() {
        return getManeuverAsDocument(getType()).asXML();
    }

    /**
     * @see #loadManeuverFromXML(String)
     * 
     * @param manXml
     */
    public void loadManeuverXml(String manXml) {
        loadManeuverFromXML(manXml);
    }

    public String asXML() {
        String rootElementName = getType();
        return asXML(rootElementName);
    }

    public String asXML(String rootElementName) {
        String result = "";
        Document document = asDocument(rootElementName);
        result = document.asXML();
        return result;
    }

    public Element asElement() {
        String rootElementName = getType();
        return asElement(rootElementName);
    }

    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    public Document asDocument() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    /**
     * Get all maneuver data as XML document. Calls {@link #getManeuverAsDocument(String)}
     * and integrates it in the document.
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);

        if (isInitialManeuver()) {
            root.addAttribute("start", "true");
        }
        else {
            root.addAttribute("start", "false");
        }

        Element id = root.addElement("id");
        id.setText(getId());

        root.addAttribute("xPos", String.valueOf(getXPosition()));
        root.addAttribute("yPos", String.valueOf(getYPosition()));

        Element manElement = root.addElement("maneuver");

        manElement.addElement("minTime").setText(String.valueOf(getMinTime()));

        manElement.addElement("maxTime").setText(String.valueOf(getMaxTime()));

        manElement.add(getManeuverAsDocument(getType()).getRootElement());
        if (!customSettings.isEmpty()) {
            Element customSettingsElem = manElement.addElement("custom-settings");

            for (String key : customSettings.keySet()) {
                Element setElem = customSettingsElem.addElement("setting");
                setElem.setText(customSettings.get(key));
                setElem.addAttribute("name", key);
                if (CT_NUMBER.equalsIgnoreCase(customSettingsTypeHint.get(key)))
                    setElem.addAttribute("type-hint", CT_NUMBER);
                else if (CT_BOOLEAN.equalsIgnoreCase(customSettingsTypeHint.get(key)))
                    setElem.addAttribute("type-hint", CT_BOOLEAN);
                else
                    setElem.addAttribute("type-hint", CT_STRING);
            }
        }

        Element sActionsElm = startActions.asElement("start-actions");
        Element eActionsElm = endActions.asElement("end-actions");
        if (sActionsElm.hasContent() || eActionsElm.hasContent()) {
            Element acElm = root.addElement("actions");
            if (sActionsElm.hasContent())
                acElm.add(sActionsElm);
            if (eActionsElm.hasContent())
                acElm.add(eActionsElm);
        }
        return document;
    }

    /**
     * Get each maneuver specific data. 
     * 
     * @param rootElementName
     * @return
     */
    public abstract Document getManeuverAsDocument(String rootElementName);
    
    /**
     * Expects only maneuver specific data.
     * 
     * @param xml
     */
    public abstract void loadManeuverFromXML(String xml);

    /**
     * By default the class name is returned, redefine it if that is not the desired thing.
     */
    public String toString() {
        return getType();
    }

    public ImageIcon getIcon() {
        return GuiUtils.getLetterIcon(getClass().getSimpleName().charAt(0), Color.white, Color.blue.darker(), 16);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isInitialManeuver() {
        return initialManeuver;
    }

    public void setInitialManeuver(boolean initialManeuver) {
        this.initialManeuver = initialManeuver;
    }

    public int getXPosition() {
        return xPosition;
    }

    public void setXPosition(int position) {
        xPosition = position;
    }

    public int getYPosition() {
        return yPosition;
    }

    public void setYPosition(int position) {
        yPosition = position;
    }

    public int getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(int maxTime) {
        this.maxTime = maxTime;
    }

    public int getMinTime() {
        return minTime;
    }

    public void setMinTime(int minTime) {
        this.minTime = minTime;
    }

    /**
     * @return the customSettings
     */
    public LinkedHashMap<String, String> getCustomSettings() {
        return customSettings;
    }

    /**
     * @param customSettings the customSettings to set
     */
    public void setCustomSettings(LinkedHashMap<String, String> customSettings) {
        this.customSettings = customSettings;
    }

    /**
     * @return the customSettingsTypeHint
     */
    public LinkedHashMap<String, String> getCustomSettingsTypeHint() {
        return customSettingsTypeHint;
    }

    /**
     * @param customSettingsTypeHint the customSettingsTypeHint to set
     */
    public void setCustomSettingsTypeHint(LinkedHashMap<String, String> customSettingsTypeHint) {
        this.customSettingsTypeHint = customSettingsTypeHint;
    }

    public double interpolate(double currentValue, double desiredValue, double maxStep) {
        if (currentValue == desiredValue)
            return desiredValue;

        double diff = desiredValue - currentValue;

        if (Math.abs(diff) < maxStep)
            return desiredValue;

        if (diff > 0)
            return currentValue + maxStep;

        return currentValue - maxStep;
    }

    public MissionType getMissionType() {
        return missionType;
    }

    public void setMissionType(MissionType missionType) {
        this.missionType = missionType;
    }

    /**
     * @param vehicle the vehicle to set
     */
    public final void setVehicles(List<VehicleType> vehicles) {
        this.vehicles.clear();
        
        if (vehicles != null)
            this.vehicles.addAll(vehicles);            
    }
    
    public final void setVehicle(VehicleType vehicle) {
        if (vehicle == null)
            this.vehicles.clear();
        else
            setVehicles(Arrays.asList(vehicle));
    }

    public DefaultProperty[] getProperties() {

        // System.out.println("Props for maneuver "+getId()+" from vehicles "+vehicles);
        
        Vector<DefaultProperty> props = new Vector<DefaultProperty>();

        DefaultProperty id = PropertiesEditor.getPropertyInstance("ID", I18n.text("Generic properties"),
                String.class, getId(), false);
        id.setDisplayName(I18n.text("ID"));
        id.setShortDescription(I18n.text("The identifier for this object"));
        props.add(id);
        DefaultProperty initialMan = PropertiesEditor.getPropertyInstance("Initial Maneuver",
                I18n.text("Generic properties"), Boolean.class, isInitialManeuver(), true);
        initialMan.setDisplayName(I18n.text("Initial Maneuver"));
        initialMan.setShortDescription(I18n.text("Whether this will be the first maneuver to be executed"));
        props.add(initialMan);
        
        if (this instanceof LocatedManeuver) {
            ManeuverLocation loc = (((LocatedManeuver)this).getManeuverLocation()).clone();
            loc.convertToAbsoluteLatLonDepth();
            DefaultProperty propertyLocation = PropertiesEditor.getPropertyInstance("Location", I18n.text("Location"), LocationType.class, loc, true, I18n.text("Maneuver's location"));
            propertyLocation.setDisplayName(I18n.text("Location"));
            props.add(propertyLocation);
            DefaultProperty propertyZ = PropertiesEditor.getPropertyInstance("Z", I18n.text("Location"), Double.class, loc.getZ(), true, I18n.text("Maneuver's z value"));
            propertyZ.setDisplayName(I18n.textc("Z", "Maneuver's z value"));
            props.add(propertyZ);
            DefaultProperty pz = PropertiesEditor.getPropertyInstance("Z-Units", I18n.text("Location"), ManeuverLocation.Z_UNITS.class, loc.getZUnits(), true, I18n.text("Maneuver's z units"));
            pz.setDisplayName(I18n.textc("Z-Units", "Maneuver's z units"));
            PropertiesEditor.getPropertyEditorRegistry().registerEditor(pz, vehicles.isEmpty() ? new ZUnitsEditor()
                    : new ZUnitsEditor(PlanUtil.getValidZUnitsForVehicle(vehicles.get(0))));
            PropertiesEditor.getPropertyEditorRegistry().registerEditor(propertyLocation, CoordinatesPropertyEditor.class);
            props.add(pz);
        }

        Vector<DefaultProperty> additionalProperties = additionalProperties();
        Vector<DefaultProperty> addProps = new Vector<DefaultProperty>();
        HashMap<String, PropertyEditor> peLList = new HashMap<>();
        HashMap<String, TableCellRenderer> peRendererList = new HashMap<>();
        for (DefaultProperty p : additionalProperties) {
            PropertyEditor pe = PropertiesEditor.getPropertyEditorRegistry().getEditor(p);
            peLList.put(p.getName(), pe);
            
            TableCellRenderer tcr = PropertiesEditor.getPropertyRendererRegistry().getRenderer(p);
            peRendererList.put(p.getName(), tcr);
        }
        PropertiesEditor.localizeProperties(additionalProperties, addProps);
        for (DefaultProperty p : addProps) {
            // PropertyEditor pe = PropertiesEditor.getPropertyEditorRegistry().getEditor(p);
            p.setCategory(I18n.textf("%s specific properties", getType()));
            // PropertiesEditor.getPropertyEditorRegistry().registerEditor(p, pe);
            PropertyEditor pe = peLList.get(p.getName());
            if (pe != null)
                PropertiesEditor.getPropertyEditorRegistry().registerEditor(p, pe);

            TableCellRenderer tcr = peRendererList.get(p.getName());
            if (tcr != null)
                PropertiesEditor.getPropertyRendererRegistry().registerRenderer(p, tcr);
        }

        props.addAll(addProps);

        for (String key : customSettings.keySet()) {
            String value = customSettings.get(key);
            String typeHint = customSettingsTypeHint.get(key);
            Class<?> clazz;
            if (CT_NUMBER.equalsIgnoreCase(typeHint))
                clazz = Double.class;
            else if (CT_BOOLEAN.equalsIgnoreCase(typeHint))
                clazz = Boolean.class;
            else
                clazz = String.class;
            Object valObj = null;
            try {
                if (CT_NUMBER.equalsIgnoreCase(typeHint))
                    valObj = Double.valueOf(value);
                else if (CT_BOOLEAN.equalsIgnoreCase(typeHint))
                    valObj = Boolean.valueOf(value);
                else
                    valObj = new String(value);
            }
            catch (NumberFormatException e) {
                NeptusLog.pub().error(
                        this.getType() + ": Error parsing custom setting '" + value + "' to '" + clazz.getSimpleName()
                                + "'!");
                clazz = String.class;
            }

            DefaultProperty custSet = PropertiesEditor.getPropertyInstance(key,
                    I18n.textf("%s custom settings", getType()), clazz, valObj, true);
            props.add(custSet);
        }
//
//        DefaultProperty sAcions = PropertiesEditor.getPropertyInstance("start-actions", getType() + " start actions",
//                PlanActions.class, startActions, true);
//        sAcions.setShortDescription(I18n.text("Start actions to be trigger at start of the maneuver."));
//        props.add(sAcions);
//        DefaultProperty eAcions = PropertiesEditor.getPropertyInstance("end-actions", getType() + " end actions",
//                PlanActions.class, endActions, true);
//        eAcions.setShortDescription(I18n.text("End actions to be trigger at end of the maneuver."));
//        props.add(eAcions);

        return props.toArray(new DefaultProperty[] {});
    }

    protected Vector<DefaultProperty> additionalProperties() {
        return new Vector<DefaultProperty>();
    }

    public void setProperties(Property[] properties) {
        for (Property p : properties) {
            try {
                if (p.getName().equalsIgnoreCase("Initial Maneuver")) {
                    setInitialManeuver((Boolean) p.getValue());
                }
                else  if (p.getName().equalsIgnoreCase(I18n.text("Initial Maneuver"))) {
                    setInitialManeuver((Boolean) p.getValue());
                }
                else if (p.getName().equalsIgnoreCase("Z") && this instanceof LocatedManeuver) {
                    ManeuverLocation manLoc = ((LocatedManeuver)this).getManeuverLocation();
                    manLoc.setZ((Double)p.getValue());
                    ((LocatedManeuver)this).setManeuverLocation(manLoc);
                }
                else if (p.getName().equalsIgnoreCase("Z-Units") && this instanceof LocatedManeuver) {
                    ManeuverLocation manLoc = ((LocatedManeuver)this).getManeuverLocation();
                    manLoc.setZUnits((ManeuverLocation.Z_UNITS)p.getValue());                                        
                    ((LocatedManeuver)this).setManeuverLocation(manLoc);
                }
                else if (p.getName().equalsIgnoreCase("Location") && this instanceof LocatedManeuver) {
                    ManeuverLocation loc = ((LocatedManeuver)this).getManeuverLocation();
                    loc.setLocation((LocationType)p.getValue());
                    ((LocatedManeuver)this).setManeuverLocation(loc);
                }
                else if (p.getCategory() != null
                        && p.getCategory().equalsIgnoreCase(I18n.textf("%s custom settings", getType()))) {
                    String pName = p.getName();
                    for (String key : customSettings.keySet()) {
                        if (!pName.equalsIgnoreCase(key))
                            continue;

                        // Found prop.
                        String value = p.getValue().toString();
                        customSettings.put(key, value);
                    }
                }
                else if (p.getCategory() != null
                        && p.getCategory().equalsIgnoreCase(I18n.textf("%s start actions", getType()))) {
                    String pName = p.getName();
                    if (!pName.equalsIgnoreCase("start-actions"))
                        continue;
                    // Found prop.
                    PlanActions value = (PlanActions) p.getValue();
                    startActions = value;
                }
                else if (p.getCategory() != null
                        && p.getCategory().equalsIgnoreCase(I18n.textf("%s end actions", getType()))) {
                    String pName = p.getName();
                    if (!pName.equalsIgnoreCase("end-actions"))
                        continue;
                    // Found prop.
                    PlanActions value = (PlanActions) p.getValue();
                    endActions = value;
                }
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
    }

    public String[] getPropertiesErrors(Property[] properties) {

        Vector<String> errors = new Vector<String>();

        for (Property p : properties) {
            if (p.getName().equals("Minimum time")) {
                int val = (Integer) p.getValue();
                if (val < 0)
                    errors.add(I18n.text("The minimum time value must be greater or equal than 0"));
            }
            if (p.getName().equals(I18n.text("Maximum time"))) {
                int val = (Integer) p.getValue();
                if (val <= 0)
                    errors.add(I18n.text("The maximum time value must be greater than 0"));
            }
            if (p.getCategory().equalsIgnoreCase(I18n.textf("%s custom settings", getType()))) {
                String pName = p.getName();
                for (String key : customSettings.keySet()) {
                    if (!pName.equalsIgnoreCase(key))
                        continue;

                    // Found prop.
                    Object valObj = p.getValue();
                    String typeHint = customSettingsTypeHint.get(key);
                    Class<?> clazz;
                    if (CT_NUMBER.equalsIgnoreCase(typeHint))
                        clazz = Double.class;
                    else if (CT_BOOLEAN.equalsIgnoreCase(typeHint))
                        clazz = Boolean.class;
                    else
                        clazz = String.class;
                    try {
                        if (CT_NUMBER.equalsIgnoreCase(typeHint))
                            valObj = (Double) valObj;
                        else if (CT_BOOLEAN.equalsIgnoreCase(typeHint))
                            valObj = (Boolean) valObj;
                        else
                            valObj = (String) valObj;
                    }
                    catch (NumberFormatException e) {
                        errors.add(I18n.textf("Error parsing custom setting '%s' to '%s'!", valObj.toString(),
                                clazz.getSimpleName()));
                    }
                }
            }
        }

        String[] errorsExtra = PluginUtils.validatePluginProperties(this, properties);

        errors.addAll(Arrays.asList(errorsExtra));
        return errors.toArray(new String[] {});
    }

    public String getPropertiesDialogTitle() {
        return I18n.textf("%s properties", getId());
    }
    
    public void cloneActions(Maneuver otherMan) {
        startActions = new PlanActions();
        startActions.load(otherMan.getStartActions().asElement("start-actions"));
        endActions = new PlanActions();
        endActions.load(otherMan.getEndActions().asElement("end-actions"));
    }

    public String getTooltipText() {
        String tt = "";
        if (!isInitialManeuver())
            tt = "<html><b><font color='#0000CC'>" + getId() + "</font></b> " + I18n.text(getType());
        else
            tt = "<html><b><font color='#00CC00'>" + getId() + "</font></b> " + I18n.text(getType());
        if (!getStartActions().isEmpty() || !getEndActions().isEmpty()) {
            tt += "<hr>"
                    + (getStartActions().isEmpty() ? "" : I18n.text("payload actions"))
                    + (getEndActions().isEmpty() ? "" : (getStartActions().isEmpty() ? "" : " | ")
                            + I18n.text("end actions"));
        }
        return tt;
    }

    public int compareTo(Maneuver o) {
        return getId().compareTo(o.getId());
    }
}
