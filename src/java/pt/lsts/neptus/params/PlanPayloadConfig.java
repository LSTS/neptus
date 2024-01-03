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
 * Author: zp/pdias
 * Apr 6, 2017
 */
package pt.lsts.neptus.params;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.gui.editor.renderer.I18nCellRenderer;
import pt.lsts.neptus.params.SystemProperty.Scope;
import pt.lsts.neptus.params.SystemProperty.Visibility;
import pt.lsts.neptus.params.renderer.I18nSystemPropertyRenderer;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * TODO Merge this with {@link ManeuverPayloadConfig}
 * 
 * @author zp
 * @author pdias
 * 
 */
public class PlanPayloadConfig implements PropertiesProvider, PropertyChangeListener {

    protected String vehicle;
    protected PlanType plan;
    protected PropertySheetPanel psp;
    protected ArrayList<SystemProperty> props = null;
    protected final LinkedHashMap<String, SystemProperty> params = new LinkedHashMap<>();

    public PlanPayloadConfig(String vehicle, PlanType plan, PropertySheetPanel psp) {
        this.plan = plan;        
        this.vehicle = vehicle;
        this.psp = psp;
    }
    
   protected void updateFromActions(PlanType plan) {
       for (IMCMessage m : plan.getStartActions().getAllMessages()) {
           if (m.getMgid() == SetEntityParameters.ID_STATIC) {
               try {
                   setParameters(SetEntityParameters.clone(m));
               }
               catch (Exception e) {
                   e.printStackTrace();
               }
           }
       }
   }
   
   protected void setParameters(SetEntityParameters param) {
       String section = param.getName();
       for(EntityParameter ep : param.getParams()) {
           SystemProperty p = params.get(section + "." + ep.getName());
           if(p == null) {
               NeptusLog.pub().warn("Property not in config: " + section + "." + ep.getName());
           }
           else {
               boolean isList = false;
               if (ArrayList.class.equals(p.getType()))
                   isList = true;
               Object value = !isList ? ConfigurationManager.getValueTypedFromString(ep.getValue(),
                       p.getValueType()) : ConfigurationManager.getListValueTypedFromString(ep.getValue(),
                       p.getValueType());
               p.setValue(value);
               p.setTimeSync(System.currentTimeMillis());               
           }
       }
   }
   

    @SuppressWarnings("deprecation")
    @Override
    public SystemProperty[] getProperties() {
        if (props == null) {
            props = ConfigurationManager.getInstance().getClonedProperties(vehicle, Visibility.USER,
                    Scope.PLAN);

            for (SystemProperty sp : props) {
                sp.resetToDefault();
                
                String name = sp.getName();
                params.put(sp.getCategoryId() + "." + name, sp);

                sp.addPropertyChangeListener(this);

                if (psp != null) {
                    if (sp.getEditor() != null) {
                        psp.getEditorRegistry().registerEditor(sp, sp.getEditor());
                    }
                    if (sp.getRenderer() != null) {
                        DefaultCellRenderer rend;                    
                        if(sp.getRenderer() instanceof I18nSystemPropertyRenderer) {
                            I18nSystemPropertyRenderer rendSProp = (I18nSystemPropertyRenderer) sp.getRenderer();
                            I18nCellRenderer newRend = new I18nCellRenderer(rendSProp.getUnitsStr());
                            newRend.setI18nMapper(rendSProp.getI18nMapper());
                            rend = newRend;
                            psp.getRendererRegistry().registerRenderer(sp, rend);
                        }
                    }
                }                
            }
            
            updateFromActions(plan);
            
            // Let us make sure all dependencies between properties are ok
            for (SystemProperty spCh : params.values()) {
                for (SystemProperty sp : params.values()) {
                    PropertyChangeEvent evt = new PropertyChangeEvent(spCh, spCh.getName(), null, spCh.getValue());
                    sp.propertyChange(evt);
                }
            }
            
            // To force initialization of startActions     
            setProperties(props.toArray(new SystemProperty[0]));
        }
        return props.toArray(new SystemProperty[0]);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() instanceof SystemProperty) {
            SystemProperty sp = (SystemProperty) evt.getSource();
            sp.setValue(evt.getNewValue());

            for (SystemProperty sprop : params.values()) {
                sprop.propertyChange(evt);
            }
            sp.propertyChange(evt);
        }
        if (psp != null)
            psp.repaint();
    }

    @Override
    public void setProperties(Property[] properties) {
        Map<String, ArrayList<EntityParameter>> mapCategoryParameterList = new LinkedHashMap<String, ArrayList<EntityParameter>>();

        for (Property p : properties) {
            if (p instanceof SystemProperty) {
                SystemProperty sp = (SystemProperty) p;

                if (p.getValue() == null)
                    continue;

                String category = sp.getCategoryId();
                if (category == null)
                    continue;

                EntityParameter ep = new EntityParameter();
                ep.setName(sp.getName());

                boolean isList = false;
                if (ArrayList.class.equals(sp.getType()))
                    isList = true;
                String str = (String) sp.getValue().toString();
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
        }
        
        Vector<IMCMessage> ps = new Vector<>();
        for (String entity : mapCategoryParameterList.keySet()) {
            SetEntityParameters tmp = new SetEntityParameters();
            tmp.setName(entity);
            tmp.setParams(mapCategoryParameterList.get(entity));
            ps.add(tmp);
        }
        
        plan.getStartActions().parseMessages(ps);
    }

    @Override
    public String getPropertiesDialogTitle() {
        return null;
    }

    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }
}
