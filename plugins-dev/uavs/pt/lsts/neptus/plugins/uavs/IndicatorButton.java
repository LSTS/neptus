/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: christian
 * 31.10.2012
 */
package pt.lsts.neptus.plugins.uavs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.ToolTipManager;

import pt.lsts.neptus.plugins.uavs.panels.UavStateXPanel;

/**
 * @author Christian Fuchs
 *
 */
public class IndicatorButton extends JLabel implements MouseListener{
    
//--------------declarations-----------------------------------//    
    
    // The current state of the entity/UAV the button is representing
    private String state;
    
    // The name of the entity/UAV the button is representing
    private String entityName;
    
    // A description of the state
    private String description;
    
    // Hashtable containing all entities and their states when the button is representing a UAV
    private Hashtable<String,String> stateList = new Hashtable<String,String>();
    
    // Hashtable containing all entities a description of their states when the button is representing a UAV
    private Hashtable<String,String> stateListDescription = new Hashtable<String,String>();
    
    // Integer used to temporarily store the default tooltip dismissDelay
    private int defaultDismissDelay;
    
    // Triggers if the indicator is for a subsystem of a complete UAV
    private boolean isCompleteUAV;
    
    // Hashtable that stores abbreviations for the subsystem names
    // static so it is shared across all indicator buttons
    static Hashtable<String,String> abbreviations;

//--------------end of declarations----------------------------//    
    
    // this block initializes the abbreviations hashtable and fills it with the correct abbreviations
    // static, so it will be shared across all indicator buttons
    static{
        abbreviations = new Hashtable<String,String>();
        
        abbreviations.put("CPU", "CPU");
        abbreviations.put("Path Control", "PaCon");
        abbreviations.put("HTTP Server", "HTTP");
        abbreviations.put("Operational Limits", "OpLim");
        abbreviations.put("Autopilot Telemetry", "AuTel");
        abbreviations.put("Plan Database", "PLDB");
        abbreviations.put("Plan Generator", "PLGen");
        abbreviations.put("Plan Engine", "PLEng");
        abbreviations.put("Piccolo Communications", "PiCom");
        abbreviations.put("Piccolo Supervision", "PiSup");
        abbreviations.put("Piccolo Control Loops", "PiCL");
        abbreviations.put("Heartbeat", "Dune");
    }
    
//--------------Setters and Getters----------------------------//
    
    public void setState(String state){
        this.state = state;
        updateToolTip();
    }
    
    public void addToState(String key, String newState){
        this.stateList.put(key, newState);
        updateToolTip();
    }
    
    public void addToStateDescription(String key, String newDescription){
        this.stateListDescription.put(key, newDescription);
        updateToolTip();
    }
    
    public String getState(){
        return state;
    }
    
    public void setEntityName(String entityName){
        this.entityName = entityName;
    }
    
    public String getEntityName(){
        return entityName;
    }
    
    public void setStateList(Hashtable<String,String> stateList){
        this.stateList = stateList;
    }
    
    public Hashtable<String,String> getStateList(){
        return stateList;
    }
    
    public void setStateListDescription(Hashtable<String,String> stateListDescription){
        this.stateListDescription = stateListDescription;
    }
    
    public Hashtable<String,String> getStateListDescription(){
        return stateListDescription;
    }
    
    public void setDescription(String description){
        this.description = description;
    }
    
    public String getDescription(){
        return description;
    }
    
//--------------End of Setters and Getters---------------------//     
    
    private static final long serialVersionUID = 1L;
    
    // only constructor for the IndicatorButton
    // the entityName and label text will be set to the String that is provided
    // the default state is BOOT
    public IndicatorButton(String entityName, boolean completeUAV){
        
        super();
        setEntityName(entityName);
        setState("BOOT");
        addMouseListener(this);
        setHorizontalAlignment(CENTER);
        isCompleteUAV = completeUAV;
        
        // when the indicator represents a complete UAV, set the text to the UAV's name
        // if it is for a subsystem, set the text to the subsystem's abbreviated name
        if (isCompleteUAV){
            setText(entityName);
        }
        else{
            setText(abbreviations.get(entityName));
        }
    }
    
    @Override
    public void paintComponent(Graphics g){
        
        // anti-aliasing
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // select the color based on the state
        selectColor(g);
        
        // draw the rounded rectangle
        g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), this.getWidth() / 4, this.getWidth() / 4);

        // this call must be the last so that the text get's drawn on top of the rectangle
        super.paintComponent(g);
    }
    
    // Method that updates the tooltip text and uses either the single entity or UAV representation
    public void updateToolTip(){
        
        // if this button is for a complete UAV, use the multiUAV tooltip
        if (isCompleteUAV){
            setToolTipText("<html>" + entityName + ": " + state + stateListToString(stateList, stateListDescription));
        }
        else{
            setToolTipText("<html>" + entityName + ":<br>" + state + "<br>" + description);
        }
    }
    
    // Method that returns a string representation of the UAV's and entities' states
    // Only the UAV's name and state as well as all non-NORMAL entities are shown
    public String stateListToString(Hashtable<String,String> states, Hashtable<String,String> statesDescription){
        
        String str = "";
        
        for(String name: states.keySet()){
            if (!states.get(name).equals("NORMAL")){
                str = str + "<br>" + name + ": " + states.get(name) + ", " + statesDescription.get(name);
            }
            
        }
        
        return str;
    }
    
    // Method that sets the color based on the state
    private void selectColor(Graphics g){
        switch (state){
            case "BOOT":
                g.setColor(Color.blue);
                break;
            case "NORMAL": 
                g.setColor(Color.green.darker());
                break;
            case "FAULT": 
                g.setColor(Color.yellow.darker());
                break;
            case "ERROR": 
                g.setColor(Color.orange);
                break;
            case "FAILURE": 
                g.setColor(Color.red);
                break;
            default:
                g.setColor(Color.red);
        }
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        
        if (isCompleteUAV){
            ((UavStateXPanel)this.getParent()).iAmClicked(this);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        defaultDismissDelay = ToolTipManager.sharedInstance().getDismissDelay();
        ToolTipManager.sharedInstance().setDismissDelay(60000);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        ToolTipManager.sharedInstance().setDismissDelay(defaultDismissDelay);
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }
    
    public void setEnabled(boolean enabled){
        
        
        
        if(enabled){
            setFont(getFont().deriveFont(Font.BOLD));
        }
        else{
            setFont(getFont().deriveFont(Font.PLAIN));
        }
        
    }
}
