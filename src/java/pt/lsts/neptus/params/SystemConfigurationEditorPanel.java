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
 * Author: Paulo Dias
 * Mar 2, 2013
 */
package pt.lsts.neptus.params;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertyEditorRegistry;
import com.l2fprod.common.propertysheet.PropertyRendererRegistry;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.propertysheet.PropertySheetTableModel.Item;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.EntityParameters;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.QueryEntityParameters;
import pt.lsts.imc.SaveEntityParameters;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.params.SystemProperty.Scope;
import pt.lsts.neptus.params.SystemProperty.Visibility;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class SystemConfigurationEditorPanel extends JPanel implements PropertyChangeListener {

    protected final LinkedHashMap<String, SystemProperty> params = new LinkedHashMap<>();

    protected PropertySheetPanel psp;
    private JButton sendButton;
    private JButton saveButton;
    private JButton refreshButton;
    private JButton resetButton;
    private JButton collapseButton;
    
    private JLabel titleLabel;
    private JCheckBox checkAdvance;
    private JComboBox<Scope> scopeComboBox;
    
    protected boolean refreshing = false;
    private PropertyEditorRegistry per;
    private PropertyRendererRegistry prr;

    private Scope scopeToUse = Scope.GLOBAL;
    private Visibility visibility = Visibility.USER;

    protected String systemId;
    protected ImcSystem sid = null;
    
    protected ImcMsgManager imcMsgManager;
    
    public SystemConfigurationEditorPanel(String systemId, Scope scopeToUse, Visibility visibility,
            boolean showSendButton, boolean showScopeCombo, boolean showResetButton, ImcMsgManager imcMsgManager) {
        this.systemId = systemId;
        this.imcMsgManager = imcMsgManager;
        
        this.scopeToUse = scopeToUse;
        this.visibility = visibility;
        
        initialize(showSendButton, showScopeCombo, showResetButton);
    }
    
    private void initialize(boolean showSendButton, boolean showScopeCombo, boolean showResetButton) {
        setLayout(new MigLayout());

        scopeComboBox = new JComboBox<Scope>(Scope.values()) {
            public void setSelectedItem(Object anObject) {
                super.setSelectedItem(anObject);
            }
        };
        scopeComboBox.setRenderer(new ListCellRenderer<Scope>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends Scope> list, Scope value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = new JLabel(I18n.text(value.getText()));
                label.setOpaque(true);
                if (isSelected) {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                }
                else {
                    label.setBackground(list.getBackground());
                    label.setForeground(list.getForeground());
                }
    
                return label;
            }
        });
        scopeComboBox.setSelectedItem(scopeToUse);
        scopeComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                scopeToUse = (Scope) e.getItem();
                new Thread() {
                    @Override
                    public void run() {
                        if (refreshButton != null)
                            refreshButton.doClick(50);
                    }
                }.start();
            }
        });
        
        titleLabel = new JLabel("<html><b>" + createTitle() + "</b></html>");
        add(titleLabel, "w 100%, wrap"); 

        // Configure Property sheet
        psp = new PropertySheetPanel();
        psp.setSortingCategories(true);
        psp.setSortingProperties(false);
        psp.setDescriptionVisible(true);
        psp.setMode(PropertySheet.VIEW_AS_CATEGORIES);
        psp.setToolBarVisible(false);
        
        resetPropertiesEditorAndRendererFactories();
        
        add(psp, "w 100%, h 100%, wrap");
        
        sendButton = new JButton(new AbstractAction(I18n.text("Send")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendPropertiesToSystem();
            }
        });
        sendButton.setToolTipText(I18n.text("Send the modified properties."));
        if (showSendButton) {
            add(sendButton, "sg buttons, split");
        }

        refreshButton = new JButton(new AbstractAction(I18n.text("Refresh")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshPropertiesOnPanel();
            }
        });
        refreshButton.setToolTipText(I18n.text("Requests the entities sections parameters from the vehicle."));
        add(refreshButton, "sg buttons, split");

        saveButton = new JButton(new AbstractAction(I18n.text("Save")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                savePropertiesToSystem();
            }
        });
        saveButton.setToolTipText(I18n.text("Saves the visible entities sections in the vehicle."));
        if (showSendButton) {
            add(saveButton, "sg buttons, split");
        }

        collapseButton = new JButton(new AbstractAction(I18n.text("Collapse All")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < psp.getTable().getSheetModel().getRowCount(); i++) {
                    Item o = (Item) psp.getTable().getSheetModel().getObject(i);
                    if (o.isVisible() && !o.hasToggle()) { 
                        o.getParent().toggle();
                    }
                }
            }
        });
        collapseButton.setToolTipText(I18n.text("Collapse all sections."));
        add(collapseButton, "sg buttons, split");

        
        resetButton = new JButton(new AbstractAction(I18n.text("Reset")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetPropertiesOnPanel();
            }
        });
        resetButton.setToolTipText(I18n.text("Local reset. Needs to be sent to system."));
        if (showResetButton)
            add(resetButton, "sg buttons, gapbefore 30, split, wrap");
        resetButton.setToolTipText(I18n.text("Local reset. Needs to be sent to system."));
                    
        if (showScopeCombo)
            add(scopeComboBox, "split, w :160:");

        checkAdvance = new JCheckBox(I18n.text("Access Developer Parameters"));
        checkAdvance.setToolTipText("<html>" + I18n.textc("Be careful changing these values.<br>They may make the vehicle inoperable.",
                "This will be a tooltip, and use <br> to change line."));
//        if (ConfigFetch.getDistributionType() == DistributionEnum.DEVELOPER)
            add(checkAdvance);
//        else
//            visibility = Visibility.USER;
        if (visibility == Visibility.DEVELOPER)
            checkAdvance.setSelected(true);
        else
            checkAdvance.setSelected(false);
        checkAdvance.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (checkAdvance.isSelected())
                    visibility = Visibility.DEVELOPER;
                else
                    visibility = Visibility.USER;
                
                refreshPropertiesOnPanel();
            }
        });
        checkAdvance.setFocusable(false);

        refreshPropertiesOnPanel();
        
        revalidate();
        repaint();
    }

    private void resetPropertiesEditorAndRendererFactories() {
        per = new PropertyEditorRegistry();
        // per.registerDefaults();
        psp.setEditorFactory(per);
        prr = new PropertyRendererRegistry();
        // prr.registerDefaults();
        psp.setRendererFactory(prr);
    }
    
    /**
     * @return the params
     */
    public LinkedHashMap<String, SystemProperty> getParams() {
        return params;
    }
    
    /**
     * @return the systemId
     */
    public String getSystemId() {
        return systemId;
    }
    
    /**
     * @param systemId the systemId to set
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
        sid = ImcSystemsHolder.getSystemWithName(this.systemId);
        refreshPropertiesOnPanel();
    }

    /**
     * @return the refreshing
     */
    public boolean isRefreshing() {
        return refreshing;
    }
    
    /**
     * @param refreshing the refreshing to set
     */
    public void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
    }
    
    private synchronized void refreshPropertiesOnPanel() {
        titleLabel.setText("<html><b>" + createTitle() + "</b></html>");
        removeAllPropertiesFromPanel();
        
        resetPropertiesEditorAndRendererFactories();
        
        ArrayList<SystemProperty> pr = ConfigurationManager.getInstance().getProperties(systemId, visibility, scopeToUse);
        ArrayList<String> secNames = new ArrayList<>();
        for (SystemProperty sp : pr) {
            String sectionName = sp.getCategoryId();
            String name = sp.getName();
            if (!secNames.contains(sectionName))
                secNames.add(sectionName);
            params.put(sectionName + "." + name, sp);
            sp.addPropertyChangeListener(this);
            psp.addProperty(sp);
            if (sp.getEditor() != null) {
                per.registerEditor(sp, sp.getEditor());
            }
            if (sp.getRenderer() != null) {
                prr.registerRenderer(sp, sp.getRenderer());
            }
        }
        // Let us make sure all dependencies between properties are ok
        for (SystemProperty spCh : params.values()) {
            for (SystemProperty sp : params.values()) {
                PropertyChangeEvent evt = new PropertyChangeEvent(spCh, spCh.getName(), null, spCh.getValue());
                sp.propertyChange(evt);
            }
        }
        for (String sectionName : secNames) {
            queryValues(sectionName, scopeToUse.getText(), visibility.getText());
        }
        
        revalidate();
        repaint();
    }

    private synchronized void resetPropertiesOnPanel() {
        for (SystemProperty sp : params.values()) {
            sp.resetToDefault();
        }
        psp.repaint();
    }

    private void removeAllPropertiesFromPanel() {
        params.clear();
        for (Property p : psp.getProperties()) {
            psp.removeProperty(p);
        }
    }

    private String createTitle() {
        return I18n.textf("%systemName Parameters", getSystemId() == null ? "" : getSystemId());
    }

    /* (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
//        NeptusLog.pub().info("<###>--------------- " + evt);
        if(!refreshing && evt.getSource() instanceof SystemProperty) {
            SystemProperty sp = (SystemProperty) evt.getSource();
            sp.setValue(evt.getNewValue());
            
            for (SystemProperty sprop : params.values()) {
                sprop.propertyChange(evt);
            }
            sp.propertyChange(evt);
        }
    }
    
    private void queryValues(String entityName, String scope, String visibility) {
        QueryEntityParameters qep = new QueryEntityParameters();
        qep.setScope(scope);
        qep.setVisibility(visibility);
        qep.setName(entityName);
        send(qep);
    }

    private void saveRequest(String entityName) {
        SaveEntityParameters qep = new SaveEntityParameters();
        qep.setName(entityName);
        send(qep);
    }

    private void sendProperty(SystemProperty... propsList) {
        Map<String, ArrayList<EntityParameter>> mapCategoryParameterList = new LinkedHashMap<String, ArrayList<EntityParameter>>(); 
        for (SystemProperty prop : propsList) {
            if (prop.getValue() == null)
                continue;

            String category = prop.getCategoryId();
            if (category == null)
                continue;
            
            EntityParameter ep = new EntityParameter();
            ep.setName(prop.getName());
            boolean isList = false;
            if (ArrayList.class.equals(prop.getType()))
                isList = true;
            String str = (String) prop.getValue().toString();
            if (isList)
                str = ConfigurationManager.convertArrayListToStringToPropValueString(str);
            ep.setValue(str);

            ArrayList<EntityParameter> entParamList = mapCategoryParameterList.get(category);
            if (entParamList == null) {
                entParamList = new ArrayList<>();
                mapCategoryParameterList.put(category, entParamList);
            }
            entParamList.add(ep);
        }

        ArrayList<SetEntityParameters> msgs = new ArrayList<>(mapCategoryParameterList.size());
        for (String cat : mapCategoryParameterList.keySet()) {
            ArrayList<EntityParameter> propList = mapCategoryParameterList.get(cat);
            SetEntityParameters setParams = new SetEntityParameters();
            setParams.setName(cat);
            setParams.setParams(propList);
            msgs.add(setParams);
        }

        for (SetEntityParameters setEntityParameters : msgs) {
            send(setEntityParameters);
        }
    }

    /**
     * This will send to the system the necessary SetEntityParameters messages with SystemProperty message(s)
     * that are needed. It will only send the SystemProperty messages that are locally dirty.
     */
    private void sendPropertiesToSystem() {
        Set<SystemProperty> sentProps = new LinkedHashSet<SystemProperty>();
        ArrayList<SystemProperty> sysPropToSend = new ArrayList<>();
        for (SystemProperty sp : params.values()) {
            if (sp.getTimeDirty() > sp.getTimeSync()) {
                // sendProperty(sp);
                sysPropToSend.add(sp);
                sentProps.add(sp);
            }
        }
        if (sysPropToSend.size() > 0) {
            sendProperty(sysPropToSend.toArray(new SystemProperty[sysPropToSend.size()]));
            
            ArrayList<String> secNames = new ArrayList<>();
            for (SystemProperty sp : sentProps) {
                String sectionName = sp.getCategoryId();
                if (!secNames.contains(sectionName))
                    secNames.add(sectionName);
            }        
            for (String sec : secNames) {
                queryValues(sec, scopeToUse.getText(), visibility.getText());
            }
        }
    }

    private void savePropertiesToSystem() {
        Collection<SystemProperty> propsInPanel = params.values();
        if (propsInPanel.size() > 0) {
            ArrayList<String> secNames = new ArrayList<>();
            for (SystemProperty sp : propsInPanel) {
                String sectionName = sp.getCategoryId();
                if (!secNames.contains(sectionName))
                    secNames.add(sectionName);
            }        
            for (String sec : secNames) {
                queryValues(sec, scopeToUse.getText(), visibility.getText());
            }
            
            for (String sec : secNames) {
                saveRequest(sec);
            }
        }
    }

    private void send(IMCMessage msg) {
        MessageDeliveryListener mdl = new MessageDeliveryListener() {
            @Override
            public void deliveryUnreacheable(IMCMessage message) {
            }
            
            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
            }
            
            @Override
            public void deliveryTimeOut(IMCMessage message) {
            }
            
            @Override
            public void deliverySuccess(IMCMessage message) {
            }
            
            @Override
            public void deliveryError(IMCMessage message, Object error) {
            }
        };
        if (sid == null)
            sid = ImcSystemsHolder.getSystemWithName(getSystemId());
        if (sid != null) {
            imcMsgManager.sendReliablyNonBlocking(msg, sid.getId(), mdl);
        }
        else {
            imcMsgManager.sendMessageToSystem(msg, getSystemId());
        }
    }
    
    public static void updatePropertyWithMessageArrived(SystemConfigurationEditorPanel systemConfEditor, IMCMessage message) {
        if (systemConfEditor == null || message == null || !(message instanceof EntityParameters))
            return;
        
        try {
            systemConfEditor.setRefreshing(true);
            EntityParameters eps = EntityParameters.clone(message);
            String section = eps.getName();
            for(EntityParameter ep : eps.getParams()) {
                SystemProperty p = systemConfEditor.getParams().get(section + "." + ep.getName());
                if(p == null) {
                    NeptusLog.pub().warn("Property not in config: " + section + " - " + ep.getName() + " from system with ID " + message.getSrc());
                }
                else {
                    boolean isList = false;
//                    NeptusLog.pub().info("<###>Prop type and if is list:: " + p.getType() + " " + (ArrayList.class.equals(p.getType())));
                    if (ArrayList.class.equals(p.getType()))
                        isList = true;
                    //Object value = ConfigurationManager.getValueTypedFromString(ep.getValue(), p.getValueType());
                    Object value = !isList ? ConfigurationManager.getValueTypedFromString(ep.getValue(),
                            p.getValueType()) : ConfigurationManager.getListValueTypedFromString(ep.getValue(),
                            p.getValueType());
                    p.setValue(value);
                    p.setTimeSync(System.currentTimeMillis());
                }
            }
            systemConfEditor.revalidate();
            systemConfEditor.repaint();
            systemConfEditor.setRefreshing(false);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
//        ImcMsgManager.getManager().start();
//        MonitorIMCComms icmm = new MonitorIMCComms(ImcMsgManager.getManager());
//        GuiUtils.testFrame(icmm);
        GuiUtils.setLookAndFeel();
        String vehicle = "lauv-noptilus-1";
        
        GeneralPreferences.language = "en_US";
        
        final SystemConfigurationEditorPanel sc1 = new SystemConfigurationEditorPanel(vehicle, Scope.MANEUVER,
                Visibility.USER, true, true, true, ImcMsgManager.getManager());
        final SystemConfigurationEditorPanel sc2 = new SystemConfigurationEditorPanel(vehicle, Scope.MANEUVER,
                Visibility.USER, true, true, true, ImcMsgManager.getManager());
        
//        ImcMsgManager.getManager().addListener(new MessageListener<MessageInfo, IMCMessage>() {
//            @Override
//            public void onMessage(MessageInfo info, IMCMessage msg) {
//                NeptusLog.pub().info("<###>---------");
//                SystemConfigurationEditorPanel.updatePropertyWithMessageArrived(sc1, msg);
//                SystemConfigurationEditorPanel.updatePropertyWithMessageArrived(sc2, msg);
//            }
//        }, vehicle, new MessageFilter<MessageInfo, IMCMessage>() {
//            @Override
//            public boolean isMessageToListen(MessageInfo info, IMCMessage msg) {
//                boolean ret = EntityParameter.ID_STATIC == msg.getMgid();
//                return ret;
//            }
//        });
        
        GuiUtils.testFrame(sc1);
        GuiUtils.testFrame(sc2);
    }
}
