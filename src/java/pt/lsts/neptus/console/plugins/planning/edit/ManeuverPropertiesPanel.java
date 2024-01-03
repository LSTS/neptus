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
 * Nov 30, 2011
 */
package pt.lsts.neptus.console.plugins.planning.edit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;
import javax.swing.undo.UndoManager;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.params.ManeuverPayloadConfig;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
public class ManeuverPropertiesPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    protected PropertySheetPanel propsPanel = new PropertySheetPanel();
    protected JPanel controls = new JPanel();
    protected JToggleButton editBtn = new JToggleButton(I18n.text("Edit"),  ImageUtils.getScaledIcon("images/planning/man_edit.png", 16, 16));
    protected JButton deleteBtn = new JButton(I18n.text("Delete"), ImageUtils.getScaledIcon("images/planning/man_remove.png", 16, 16));
    protected Maneuver maneuver;
    protected String beforeXml = null;
    protected boolean changed = false;
    protected UndoManager manager = new UndoManager();
    protected PlanType plan;
    protected ManeuverPayloadConfig payloadConfig = null;
    protected List<Property> combinedProps = new ArrayList<>();  
    
    public ManeuverPropertiesPanel() {
        setLayout(new BorderLayout());     
        setBorder(new TitledBorder(I18n.text("No maneuver selected")));
        add(propsPanel, BorderLayout.CENTER);
        controls.setLayout(new GridLayout(1, 0));        
        controls.add(deleteBtn);
        controls.add(editBtn);
        editBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
        
        add(controls, BorderLayout.SOUTH);
        
        propsPanel.setDescriptionVisible(true);
        propsPanel.setMode(PropertySheetPanel.VIEW_AS_CATEGORIES);
        propsPanel.addPropertySheetChangeListener(new PropertyChangeListener() {            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Property prop = combinedProps.stream()
                        .filter((p) -> p.getName().equalsIgnoreCase(((Property) evt.getSource()).getName())).findFirst()
                        .orElse(null);
                if (prop != null) {
                    String[] res = PluginUtils.validatePluginProperties(maneuver, new Property[] {prop});
                    if (res != null && res.length > 0) {
                        propsPanel.removePropertySheetChangeListener(this);
                        prop.setValue(evt.getOldValue());
                        propsPanel.addPropertySheetChangeListener(this);
                        propsPanel.repaint();
                        return;
                    }
                }

                setProps();
            }
        });
       
        propsPanel.setEditorFactory(PropertiesEditor.getPropertyEditorRegistry());    
        propsPanel.setRendererFactory(PropertiesEditor.getPropertyRendererRegistry());    
        propsPanel.setToolBarVisible(false);        
    }


    /**
     * @return the maneuver
     */
    public Maneuver getManeuver() {
        return maneuver;
    }

    public void setProps() {
        String before = maneuver.asXML();        
        payloadConfig.setProperties(propsPanel.getProperties());
        boolean wasInitialManeuver = maneuver.isInitialManeuver();
        try {
            maneuver.setProperties(propsPanel.getProperties());
        }
        catch (Exception e) {
            NeptusLog.pub().error(e, e);
        }
        if (maneuver.isInitialManeuver())
            plan.getGraph().setInitialManeuver(maneuver.getId());
        else {
            if (wasInitialManeuver) {
                maneuver.setInitialManeuver(true);
                setManeuver(maneuver);
                return;
            }
        }
        propsPanel.removePropertyChangeListener(payloadConfig);
        
        if (manager != null)
            manager.addEdit(new ManeuverWithSettingsChanged(maneuver, plan, before));
        changed = true;
    }

    public void setManeuver(Maneuver man) {
        boolean sameMan = this.maneuver == man;
        
        if (this.maneuver != null) {
            
            if (propsPanel.getTable().getEditorComponent() != null) {
                propsPanel.getTable().commitEditing();
                setProps();
            }
        }
        String vehicle = "unknown";
        if (plan != null)
            vehicle = plan.getVehicle();
                
        payloadConfig = new ManeuverPayloadConfig(vehicle, man, propsPanel);
        this.maneuver = man;
        if (!sameMan)
            editBtn.setSelected(false);
        changed = false;
        if (man == null) {
            setBorder(new TitledBorder(I18n.text("No maneuver selected")));
            try {
                synchronized (propsPanel) {
                    propsPanel.setProperties(new Property[0]);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            editBtn.setEnabled(false);
            deleteBtn.setEnabled(false);
            return;
        }
        
        this.beforeXml = man.getManeuverXml();
        
        // also add props 
        DefaultProperty[] manProps = man.getProperties();
        
        DefaultProperty[] payloadProps = new ManeuverPayloadConfig(vehicle, man, propsPanel).getProperties();
        
        combinedProps.clear();
        for (DefaultProperty p : manProps)
            combinedProps.add(p);
        for (DefaultProperty p : payloadProps)
            combinedProps.add(p);
        
        // To avoid ConcurrentModificationException on the PropertySheetTableModel
        synchronized (propsPanel) {
            propsPanel.setProperties(combinedProps.stream().toArray(Property[]::new));
        }
        
        setBorder(new TitledBorder(man.getId()));
        
        deleteBtn.setEnabled(true);
        if (!sameMan)
            editBtn.setSelected(false);
        
        if (maneuver instanceof StateRendererInteraction)               
            editBtn.setEnabled(true);
        else
            editBtn.setEnabled(false);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 200);
    }
    
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    
    /**
     * @return the changed
     */
    public boolean isChanged() {
        return changed;
    }
    
    /**
     * @return the beforeXml
     */
    public String getBeforeXml() {
        return beforeXml;
    }
    
    /**
     * @return the editBtn
     */
    public JToggleButton getEditBtn() {
        return editBtn;
    }

    /**
     * @return the deleteBtn
     */
    public JButton getDeleteBtn() {
        return deleteBtn;
    }

    /**
     * @return the manager
     */
    public UndoManager getManager() {
        return manager;
    }

    /**
     * @param manager the manager to set
     */
    public void setManager(UndoManager manager) {
        this.manager = manager;
    }

    /**
     * @return the plan
     */
    public PlanType getPlan() {
        return plan;
    }

    /**
     * @param plan the plan to set
     */
    public void setPlan(PlanType plan) {
        this.plan = plan;
    }

    public static void main(String[] args) {
        Goto gotoM = new Goto();
        ManeuverPropertiesPanel props = new ManeuverPropertiesPanel();
        GuiUtils.testFrame(props);
        props.setManeuver(gotoM);
    }
}
