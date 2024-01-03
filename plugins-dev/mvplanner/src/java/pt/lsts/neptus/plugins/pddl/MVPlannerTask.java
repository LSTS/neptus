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
 * Author: zp
 * Nov 26, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.awt.Image;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.plugins.mvplanner.api.ConsoleEventPlanAllocation;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
public abstract class MVPlannerTask implements Renderer2DPainter, PropertiesProvider {

    private static int count = 1;
    protected String name = String.format(Locale.US, "t%02d", count++);
    protected HashSet<PayloadRequirement> requiredPayloads = new HashSet<PayloadRequirement>();
    protected boolean firstPriority = false;
    private String associatedAllocation = null;
    private String associatedVehicle = null;
    
    
    public abstract boolean containsPoint(LocationType lt, StateRenderer2D renderer);
    public abstract LocationType getCenterLocation();
    public abstract void translate(double offsetNorth, double offsetEast);
    public abstract void setYaw(double yawRads);
    public abstract void rotate(double amountRads);
    public abstract void growWidth(double amount);
    public abstract void growLength(double amount);
    public abstract String marshall() throws IOException;
    public abstract void unmarshall(String data) throws IOException;
    
    transient protected Image greenLed = null;
    transient protected Image orangeLed = null;
    
    protected synchronized void loadImages() {
        if (greenLed == null)
            greenLed = ImageUtils.getImage("pt/lsts/neptus/plugins/pddl/led.png");
        if (orangeLed == null)
            orangeLed = ImageUtils.getImage("pt/lsts/neptus/plugins/pddl/orangeled.png");       
    }
    
    /**
     * @return the name
     */
    public final String getName() {
        return name;
    }
    
    public void setRequiredPayloads(HashSet<PayloadRequirement> payloads) {
        this.requiredPayloads = payloads;
    }
    
    @Override
    public DefaultProperty[] getProperties() {
        Vector<DefaultProperty> props = new Vector<DefaultProperty>();
        props.add(PropertiesEditor.getPropertyInstance("First Priority", "Urgency", Boolean.class, firstPriority, true));
        
        for (PayloadRequirement pr : PayloadRequirement.values()) {
            props.add(PropertiesEditor.getPropertyInstance(pr.name(), "Payload Requirements", Boolean.class, requiredPayloads.contains(pr), true));
        }
        
        return props.toArray(new DefaultProperty[0]);
    }
    
    public final String getPayloadsAbbreviated() {
        String payloads = StringUtils.join(requiredPayloads.toArray(new PayloadRequirement[0]), ", ");
        payloads = payloads.replaceAll("camera", "cam");
        payloads = payloads.replaceAll("multibeam", "mb");
        payloads = payloads.replaceAll("edgetech", "et");
        payloads = payloads.replaceAll("sidescan", "sss");
        payloads = payloads.replaceAll("rhodamine", "rd");
        return payloads;
    }
    
    @Override
    public void setProperties(Property[] properties) {
        
        HashSet<PayloadRequirement> newReqs = new HashSet<PayloadRequirement>();
        
        for (Property p : properties) {
            if (p.getName().equals("First Priority")) {
                this.firstPriority = "true".equals(""+p.getValue());
                continue;
            }
            PayloadRequirement pr = PayloadRequirement.valueOf(p.getName());
            if (pr != null && "true".equals(""+p.getValue())) {
                newReqs.add(pr);
            }
        }
        
        setRequiredPayloads(newReqs);
    }
    
    @Override
    public String getPropertiesDialogTitle() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        // TODO Auto-generated method stub
        return null;
    }
    /**
     * @return the firstPriority
     */
    public boolean isFirstPriority() {
        return firstPriority;
    }
    /**
     * @param firstPriority the firstPriority to set
     */
    public void setFirstPriority(boolean firstPriority) {
        this.firstPriority = firstPriority;
    }
    /**
     * @return the requiredPayloads
     */
    public final HashSet<PayloadRequirement> getRequiredPayloads() {
        return requiredPayloads;
    }
    /**
     * @return the associatedAllocation
     */
    public String getAssociatedAllocation() {
        return associatedAllocation;
    }
    /**
     * @param associatedAllocation the associatedAllocation to set
     */
    public void setAssociatedAllocation(String associatedAllocation) {
        this.associatedAllocation = associatedAllocation;
    }
    /**
     * @return the associatedVehicle
     */
    public String getAssociatedVehicle() {
        return associatedVehicle;
    }
    /**
     * @param associatedVehicle the associatedVehicle to set
     */
    public void setAssociatedVehicle(String associatedVehicle) {
        this.associatedVehicle = associatedVehicle;
    }
    
    public void setAllocation(ConsoleEventPlanAllocation alloc) {
        if (alloc == null) {
            setAssociatedAllocation(null);
            setAssociatedVehicle(null);
        }
        else {
            setAssociatedVehicle(alloc.getVehicle());
            setAssociatedAllocation(alloc.getId());            
        }
    }
    
    public static ArrayList<MVPlannerTask> loadFile(File f) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(f));
        String line = reader.readLine();
        ArrayList<MVPlannerTask> tasks = new ArrayList<>();
        while (line != null) {
            if (line.startsWith("#")) {
                // ignore
            }                
            else if (line.startsWith("sample")) {
                SamplePointTask task = new SamplePointTask(new LocationType());
                task.unmarshall(line);
                tasks.add(task);
            }
            else if (line.startsWith("survey")) {
                SurveyAreaTask task = new SurveyAreaTask(new LocationType());
                task.unmarshall(line);
                tasks.add(task);
            }
            else {
                System.err.println("Unrecognized line: '" + line + "'");
            }
            line = reader.readLine();
        }
        reader.close();
        return tasks;
    }
    
    public double getLength() {
        return 0;
    }
    
    public Collection<MVPlannerTask> splitTask(double maxLength) {
        return new ArrayList<>();
    }

    public static void saveFile(File f, ArrayList<MVPlannerTask> tasks) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        for (MVPlannerTask task : tasks)
            writer.write(task.marshall() + "\n");
        writer.close();
    }
    
    public void mousePressed(MouseEvent e, StateRenderer2D renderer) {
        
    }
    
    public void mouseReleased(MouseEvent e, StateRenderer2D renderer) {
        
    }
        
    public void mouseDragged(MouseEvent e, StateRenderer2D renderer) {
        
    }
    
    public void mouseMoved(MouseEvent e, StateRenderer2D renderer) {
        
    }
    
    
}
