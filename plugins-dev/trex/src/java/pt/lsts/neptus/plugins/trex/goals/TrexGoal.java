/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 28, 2012
 */
package pt.lsts.neptus.plugins.trex.goals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.TrexAttribute;
import pt.lsts.imc.TrexAttribute.ATTR_TYPE;
import pt.lsts.imc.TrexOperation;
import pt.lsts.imc.TrexOperation.OP;
import pt.lsts.imc.TrexToken;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.gui.editor.UnixTimeEditor;

/**
 * @author zp
 * @author mfaria
 * 
 */
public abstract class TrexGoal implements PropertiesProvider {

    protected String goalId, timeline, predicate;
    protected Date minStartTime = new Date(), maxStartTime = new Date(System.currentTimeMillis() + 3600 * 1000 * 24);
    protected Date minEndTime = new Date(), maxEndTime = new Date(System.currentTimeMillis() + 3600 * 1000 * 24);
    protected boolean specifyStartDate = false, specifyEndDate = false;
    
    /**
     * This method could (and should) be override by subclasses to define additional attributes
     * @return Attributes specific to this goal
     */
    public abstract Collection<TrexAttribute> getAttributes();
    public abstract void parseAttributes(Collection<TrexAttribute> attributes);
    public abstract void setSpecificProperties(Collection<Property> properties);
    public abstract Collection<DefaultProperty> getSpecificProperties();

    public abstract String toJson();
    
    /**
     * @return the timeline
     */
    public String getTimeline() {
        return timeline;
    }

    /**
     * @param timeline the timeline to set
     */
    public void setTimeline(String timeline) {
        this.timeline = timeline;
    }

    /**
     * @return the predicate
     */
    public String getPredicate() {
        return predicate;
    }

    /**
     * @param predicate the predicate to set
     */
    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }
    
    protected final Collection<TrexAttribute> getAggregatedAttributes() {
        Vector<TrexAttribute> attrs = new Vector<>();
        if (specifyStartDate)
            attrs.add(new TrexAttribute("start", ATTR_TYPE.INT, "" + minStartTime.getTime(), "" + maxStartTime.getTime()));
        if (specifyEndDate)
            attrs.add(new TrexAttribute("end", ATTR_TYPE.INT, "" + minEndTime.getTime(), "" + maxEndTime.getTime()));
        
        attrs.addAll(getAttributes());
        return attrs;
    }
    
    protected final void parseAggregatedAttributes(Collection<TrexAttribute> attributes) {
        
        Vector<TrexAttribute> unparsed = new Vector<>();
        unparsed.addAll(attributes);
        
        for (TrexAttribute t : attributes) {
            switch (t.getName()) {
                case "start":
                    unparsed.remove(t);
                    minStartTime = new Date(Long.parseLong(t.getMin()));
                    maxStartTime = new Date(Long.parseLong(t.getMax()));
                    break;
                    
                case "end":
                    unparsed.remove(t);
                    minEndTime = new Date(Long.parseLong(t.getMin()));
                    maxEndTime = new Date(Long.parseLong(t.getMax()));
                    break;
                    
                default:
                    break;
            }
        }
        
        parseAttributes(unparsed);        
    }

    public TrexGoal(String timeline, String predicate) {
        goalId = "N_" + System.currentTimeMillis();// FIXME add counter
        this.timeline = timeline;
        this.predicate = predicate;
    }

    @Override
    public final DefaultProperty[] getProperties() {

        DefaultProperty minEndProp = PropertiesEditor
                .getPropertyInstance("Minimum End time", Date.class, minEndTime, true);
        DefaultProperty maxEndProp = PropertiesEditor
                .getPropertyInstance("Maximum End time", Date.class, maxEndTime, true);
        DefaultProperty minStartProp = PropertiesEditor
                .getPropertyInstance("Minimum Start time", Date.class, minStartTime, true);
        DefaultProperty maxStartProp = PropertiesEditor
                .getPropertyInstance("Maximum Start time", Date.class, maxStartTime, true);

        PropertiesEditor.getPropertyEditorRegistry().registerEditor(minStartProp, UnixTimeEditor.class);
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(maxStartProp, UnixTimeEditor.class);
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(minEndProp, UnixTimeEditor.class);
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(maxEndProp, UnixTimeEditor.class);

        Vector<DefaultProperty> props = new Vector<>();
        DefaultProperty[] properties = new DefaultProperty[] {
                PropertiesEditor.getPropertyInstance("Timeline", String.class, timeline, true),
                PropertiesEditor.getPropertyInstance("Predicate", String.class, predicate, true),
                minStartProp, maxStartProp, minEndProp, maxEndProp,
                PropertiesEditor.getPropertyInstance("Specify Start Date", Boolean.class, specifyStartDate, true),
                PropertiesEditor.getPropertyInstance("Specify End Date", Boolean.class, specifyEndDate, true) };
        props.addAll(Arrays.asList(properties));
        
        props.addAll(getSpecificProperties());
        return props.toArray(new DefaultProperty[0]);
    }

    @Override
    public final void setProperties(Property[] properties) {

        Vector<Property> unprocessed = new Vector<>();
        unprocessed.addAll(Arrays.asList(properties));
        
        for (Property p : properties) {
            switch (p.getName()) {
                case "Timeline":
                    timeline = p.getValue().toString();
                    unprocessed.remove(p);
                    break;
                case "Predicate":
                    predicate = p.getValue().toString();
                    unprocessed.remove(p);
                    break;
                case "Specify Start Date":
                    specifyStartDate = (Boolean)p.getValue();
                    unprocessed.remove(p);
                    break;
                case "Specify End Date":
                    specifyEndDate = (Boolean)p.getValue();
                    unprocessed.remove(p);
                    break;
                case "Minimum End time":
                    minEndTime = (Date)p.getValue();
                    unprocessed.remove(p);
                    break;
                case "Maximum End time":
                    maxEndTime = (Date)p.getValue();
                    unprocessed.remove(p);
                    break;
                case "Minimum Start time":
                    minStartTime = (Date)p.getValue();
                    unprocessed.remove(p);
                    break;
                case "Maximum Start time":
                    maxStartTime = (Date)p.getValue();
                    unprocessed.remove(p);
                    break;
                default:
                    break;
            }
        }
        
        setSpecificProperties(unprocessed);
    }

    @Override
    public String getPropertiesDialogTitle() {
        return getClass().getSimpleName()+" parameters";
    }

    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }
    
    public static final TrexGoal parseImcMsg(TrexToken token) {
        TrexGoal goal;
        
        switch (token.getTimeline()+"."+token.getPredicate()) {
            case "estimator.At":
                goal = new VisitLocationGoal();
                break;
            default:
                goal = new GoalWithoutAttributes(token.getTimeline(), token.getPredicate());
                break;
        }
        
        goal.parseAggregatedAttributes(token.getAttributes());
        return goal;
    }

    /**
     * This should be called last by the child as it uses the attributes variable to add TrexAttribute
     * 
     * @return
     */
    public final TrexOperation asIMCMsg() {
        TrexOperation trexOperation = new TrexOperation(OP.POST_GOAL, goalId, new TrexToken(timeline, predicate,
                getAggregatedAttributes()));
        return trexOperation;
    }
}
