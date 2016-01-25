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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Nov 26, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public abstract class MVPlannerTask implements Renderer2DPainter, PropertiesProvider {

    protected static int count = 1;
    protected String name = String.format(Locale.US, "t%02d", count++);
    protected HashSet<PayloadRequirement> requiredPayloads = new HashSet<PayloadRequirement>();
    
    public abstract boolean containsPoint(LocationType lt, StateRenderer2D renderer);
    public abstract LocationType getCenterLocation();
    public abstract void translate(double offsetNorth, double offsetEast);
    public abstract void setYaw(double yawRads);
    public abstract void rotate(double amountRads);
    public abstract void growWidth(double amount);
    public abstract void growLength(double amount);
    
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
        payloads = payloads.replaceAll("rhodmanine", "rd");
        return payloads;
    }
    
    @Override
    public void setProperties(Property[] properties) {
        
        HashSet<PayloadRequirement> newReqs = new HashSet<PayloadRequirement>();
        
        for (Property p : properties) {
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
     * @return the requiredPayloads
     */
    public final HashSet<PayloadRequirement> getRequiredPayloads() {
        return requiredPayloads;
    }
}
