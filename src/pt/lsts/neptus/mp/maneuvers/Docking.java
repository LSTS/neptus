/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Miguel Rosa
 * 19/05/2016
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Docking.VEHICLEFUNCTION;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.gui.editor.ComboEditor;
import pt.lsts.neptus.gui.editor.renderer.I18nCellRenderer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.Maneuver.SPEED_UNITS;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.util.XMLUtil;

/**
 * @author mrosa
 */
public class Docking extends Goto implements StateRendererInteraction,
IMCSerialization,  PathProvider , ManeuverWithSpeed{
 
    
    private enum vehicleFunction { Station , Target}
    @NeptusProperty(name = "Docking Target", description = "System name to perform docking maneuver.")
    private String target = "";
    @NeptusProperty(name = "Vehicle Function", description = "Vehicle Fuction. Station to perform docking, Target to receive docking.")
    private vehicleFunction vfunction = vehicleFunction.Station;
    
    private double max_speed = 30;

    protected InteractionAdapter adapter = new InteractionAdapter(null);
    protected Point2D lastDragPoint = null;

    protected boolean editing = false;

    protected Vector<double[]> points = new Vector<double[]>();
    
    protected static final LinkedHashMap<Long, String> wpDockingFunctionMap = new LinkedHashMap<Long, String>();

    
    

    static {
        
        wpDockingFunctionMap.put(1l, I18n.textmark("Station"));
      
        wpDockingFunctionMap.put(2l, I18n.textmark("Target"));
  
    }
        
    public Docking() {
      //  super();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.Maneuver#getType()
     */
    @Override
    public String getType() {
        return "Docking";
    }
    
    
    // parse XML
    @Override
    public void loadFromXML(String xml) {
        super.loadFromXML(xml);
        try {
            Document doc = DocumentHelper.parseText(xml);
            
            //Get target name
            Node node = doc.selectSingleNode("//target");
            if (node != null)
                target = node.getText();
            
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            return;
        }
        
        System.out.println("After loading XML, my ID is "+getId());

    }

    @Override
    public Object clone() {
        Docking clone = new Docking();
        this.clone(clone);
        clone.loadFromXML(getManeuverXml());
        clone.target = this.target;
        clone.vfunction = this.vfunction;
      
        return clone;
    }

    // retrieve as XML
    @Override
    public Document getManeuverAsDocument(String rootElementName) {
        Document doc = super.getManeuverAsDocument(rootElementName);
        
        Element root = doc.getRootElement();
        
        root.addElement("target").setText(target);

        return doc;
    }

    @Override
    public String getName() {
        return getType();
    }

    @Override
    public Image getIconImage() {
        return adapter.getIconImage();
    }

    @Override
    public Cursor getMouseCursor() {
        return adapter.getMouseCursor();
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        adapter.mouseClicked(event, source);        
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        adapter.mousePressed(event, source);
        lastDragPoint = event.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (lastDragPoint == null) {
            adapter.mouseDragged(event, source);
            lastDragPoint = event.getPoint();
            return;
        }
  
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        adapter.mouseMoved(event, source);
    }

    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        adapter.mouseReleased(event, source);
        lastDragPoint = null;
    }

    @Override
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
        adapter.wheelMoved(event, source);
    }

    @Override
    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        adapter.keyPressed(event, source);
    }

    @Override
    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        adapter.keyReleased(event, source);
    }

    @Override
    public void keyTyped(KeyEvent event, StateRenderer2D source) {
        adapter.keyTyped(event, source);
    }
    
    @Override
    public void mouseExited(MouseEvent event, StateRenderer2D source) {
        adapter.mouseExited(event, source);
    }
    
    @Override
    public void focusGained(FocusEvent event, StateRenderer2D source) {
        adapter.focusGained(event, source);        
    }

    @Override
    public void focusLost(FocusEvent event, StateRenderer2D source) {
        adapter.focusLost(event, source);
    }
    
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        editing = mode;
        adapter.setActive(mode, source);
    }



    @Override
    public List<double[]> getPathPoints() {
        return Collections.unmodifiableList(points);
    }

    @Override
    public List<LocationType> getPathLocations() {
        Vector<LocationType> locs = new Vector<>();
//   
        return locs;
    }
    

    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);

    }


   
    @Override
    public IMCMessage serializeToIMC() {
        
        pt.lsts.imc.Docking man = new pt.lsts.imc.Docking();
        man.setLat(destination.getLatitudeRads());
        man.setLon(destination.getLongitudeRads());
        man.setTarget(target);
        man.setMaxSpeed(max_speed);
        
        String vehicleFunction = this.getVehicleFunction();
        try {
            if ("Station".equalsIgnoreCase(vehicleFunction))
                man.setVehiclefunction(VEHICLEFUNCTION.STATION);
            else if ("Target".equalsIgnoreCase(vehicleFunction))
                man.setVehiclefunction(VEHICLEFUNCTION.TARGET);
     
          
        } catch (Exception ex) {
            NeptusLog.pub().error(this, ex);
        }
        
       return man;
    }

    @Override
    public void parseIMCMessage(IMCMessage message) {
        pt.lsts.imc.Docking man = null;
        try {
            man = pt.lsts.imc.Docking.clone(message);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        target = man.getTarget();
        max_speed = man.getMaxSpeed();
        destination.setLatitudeRads(man.getLat());
        destination.setLongitudeRads(man.getLon());
      

        
        String vfunction = message.getString("vehicle_function");
        if (vfunction.equals("STATION"))
            setVehicleFunction(vehicleFunction.Station);
        else
            setVehicleFunction(vehicleFunction.Target);

    }

    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        
        Vector<DefaultProperty> props = new Vector<DefaultProperty>();
        
        DefaultProperty type = PropertiesEditor.getPropertyInstance("Vehicle Function", String.class, this.vfunction, true);
        type.setShortDescription("Vehicle Fuction. Station to perform docking, Target to receive docking.");
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(type, new ComboEditor<String>(wpDockingFunctionMap.values().toArray(new String[]{})));
        PropertiesEditor.getPropertyRendererRegistry().registerRenderer(type, new I18nCellRenderer());
        props.add(type);
        
        
        DefaultProperty dt = PropertiesEditor.getPropertyInstance("Docking System Target", String.class, this.target, true);
        dt.setShortDescription("System name to perform or receive docking maneuver.");
        props.add(dt);
        
     // return props;
        return ManeuversUtil.getPropertiesFromManeuver(this);
      
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        ManeuversUtil.setPropertiesToManeuver(this, properties);

    }

    public String getTooltipText() {
        return super.getTooltipText()+"<hr>"+
        I18n.text("vehicle function") + ": <b>"+vfunction+"<br>" + I18n.text("target") + ": <b>"+I18n.text(target)+"</b>" ;
    }
    
    public void setVehicleFunction( vehicleFunction vfunction) {
        this.vfunction = vfunction;
    }
    
    public String getVehicleFunction() {
        return vfunction.toString();
    }
    
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
    
    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
    }
    
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
    }
    


    public static void main(String[] args) {
        
        Docking rc = new Docking();
 
        System.out.println(XMLUtil.getAsPrettyPrintFormatedXMLString(rc.asXML().substring(39)));

 
    }
}
