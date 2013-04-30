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
 * Author: José Pinto
 * Jun 28, 2012
 */
package pt.up.fe.dceg.neptus.plugins.trex;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.gui.editor.UnixTimeEditor;
import pt.up.fe.dceg.neptus.imc.TrexAttribute;
import pt.up.fe.dceg.neptus.imc.TrexAttribute.ATTR_TYPE;
import pt.up.fe.dceg.neptus.imc.TrexOperation;
import pt.up.fe.dceg.neptus.imc.TrexOperation.OP;
import pt.up.fe.dceg.neptus.imc.TrexToken;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;


/**
 * @author zp
 *
 */
public class TrexGoal implements PropertiesProvider {
    protected enum Timelines{
        ESTIMATOR("Estimator"), NAVIGATIOR("Navigatior");
        
        public final String name;
        private Timelines(String name) {
            this.name = name;
        }
    }

    protected enum Predicates {
        AT("At");

        public final String name;

        private Predicates(String name) {
            this.name = name;
        }
    }

    private enum Properties {
        TIMELINE("Timeline", null),
        PREDICATE("Predicate", null),
        GOAL_ID("Goal ID", null),
        USE_START_DATE("Specify start date", null),
        USE_END_DATE("Specify end date", null),
        START_DATE("Start date", "start"),
        END_DATE("End date", "end"),
        SECONDS(null, "secs");

        public final String label;
        public final String varName;

        private Properties(String label, String varName) {
            this.label = label;
            this.varName = varName;
        }
    }

    protected Timelines timeline;
    protected Predicates predicate;
    protected String goalId;
    protected long start, end, secs;
    protected boolean setStartDate, setEndDate;
    protected String xml;
    protected ArrayList<TrexAttribute> attributes;


    public TrexGoal() {
        goalId = "N_" + System.currentTimeMillis();// FIXME add counter
        timeline = Timelines.ESTIMATOR;
        predicate = Predicates.AT;
        start = new Date().getTime() / 1000;
        end = new Date().getTime() / 1000;
        secs = 0;
        setStartDate = setEndDate = false;
        xml = "";
        attributes = new ArrayList<TrexAttribute>();
    }

    public ArrayList<TrexAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public DefaultProperty[] getProperties() {
        
        DefaultProperty startProp = PropertiesEditor.getPropertyInstance(Properties.START_DATE.label, Long.class,
                start,
                true);
        DefaultProperty endProp = PropertiesEditor
                .getPropertyInstance(Properties.END_DATE.label, Long.class, end, true);
        
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(startProp, UnixTimeEditor.class);
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(endProp, UnixTimeEditor.class);
        DefaultProperty[] properties = new DefaultProperty[] {
                PropertiesEditor.getPropertyInstance(Properties.TIMELINE.label, String.class, timeline, false),
                PropertiesEditor.getPropertyInstance(Properties.PREDICATE.label, String.class, predicate, false),
                PropertiesEditor.getPropertyInstance(Properties.GOAL_ID.label, String.class, goalId, true),
                startProp, 
                PropertiesEditor
                        .getPropertyInstance(Properties.USE_START_DATE.label, Boolean.class, setStartDate, true),
                endProp,
                PropertiesEditor.getPropertyInstance(Properties.USE_END_DATE.label, Boolean.class, setEndDate, true)
        };
        return properties;
    }

    @Override
    public void setProperties(Property[] properties) {
        for (Property p : properties) {
            if (p.getName().equals(Properties.GOAL_ID.label)) {
                goalId = p.getValue().toString();
            }
            else if (p.getName().equals(Properties.START_DATE.label)) {
                start = (Long)p.getValue();
            }
            else if (p.getName().equals(Properties.END_DATE.label)) {
                end = (Long)p.getValue();
            }
            else if (p.getName().equals(Properties.USE_START_DATE.label)) {
                setStartDate = ((Boolean)p.getValue()).booleanValue();
            }
            else if (p.getName().equals(Properties.USE_END_DATE.label)) {
                setEndDate = ((Boolean)p.getValue()).booleanValue();
            }
        }
    }

    @Override
    public String getPropertiesDialogTitle() {
        return "Trex goal parameters";
    }

    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }    
    
    
    //
    // public void parseXml(String xml) throws Exception {
    // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd hh:mm:ss.SSS");
    // sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    //
    // Document doc = DocumentHelper.parseText(xml);
    // timeline = doc.getRootElement().selectSingleNode("@on").getText();
    // predicate = doc.getRootElement().selectSingleNode("@pred").getText();
    // goalId = doc.getRootElement().selectSingleNode("@id").getText();
    //
    // List<?> l = doc.selectNodes("Goal/Variable");
    // for (Object o : l) {
    // Node n = (Node)o;
    // String varName = n.selectSingleNode("@name").getText();
    //
    // if (varName.equals("latitude"))
    // lat_deg = Math.toDegrees(Double.parseDouble(n.selectSingleNode("float/@min").getText()));
    // else if (varName.equals("longitude"))
    // lon_deg = Math.toDegrees(Double.parseDouble(n.selectSingleNode("float/@min").getText()));
    // else if (varName.equals("z"))
    // depth = Double.parseDouble(n.selectSingleNode("float/@min").getText());
    // else if (varName.equals("start"))
    // start = sdf.parse(n.selectSingleNode("date/@min").getText()).getTime()/1000;
    // else if (varName.equals("end"))
    // end = sdf.parse(n.selectSingleNode("date/@min").getText()).getTime()/1000;
    // else if (varName.equals("secs"))
    // secs = Integer.parseInt(n.selectSingleNode("int/@min").getText());
    // }
    // }
    
    /**
     * This should be called last by the child as it uses the attributes variable to add TrexAttribute
     * 
     * @return
     */
    public TrexOperation asIMCMsg() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd hh:mm:ss.SSS");
        TrexAttribute attribute;
        // "<Variable name='secs'>\n\t<int min='"+secs+"' max='"+secs+"'/>\n</Variable>\n"
        attribute = new TrexAttribute();
        attribute.setName(Properties.SECONDS.varName);
        attribute.setAttrType(ATTR_TYPE.INT);
        attribute.setMin(secs + "");
        attribute.setMax(secs + "");
        attributes.add(attribute);
        if (setStartDate) {
//            xml += "<Variable name='start'>\n\t<date min='"+sdf.format(min)+"'/>\n</Variable>\n";
            attribute = new TrexAttribute();
            attribute.setName(Properties.START_DATE.varName);
            attribute.setAttrType(ATTR_TYPE.INT);
            attribute.setMin(sdf.format(new Date(start * 1000)));
            attributes.add(attribute);
        }
        if (setEndDate) {
            // xml += "<Variable name='end'>\n\t<date min='"+sdf.format(min)+"'/>\n</Variable>\n";
            attribute = new TrexAttribute();
            attribute.setName(Properties.START_DATE.varName);
            attribute.setAttrType(ATTR_TYPE.INT);
            attribute.setMin(sdf.format(new Date(end * 1000)));
            attributes.add(attribute);
        }
        TrexOperation trexOperation = new TrexOperation(OP.POST_GOAL, goalId, new TrexToken(timeline.name,
                predicate.name, attributes));
        return trexOperation;
    }

    // public String asXml() {
    // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd hh:mm:ss.SSS");
    // sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    // // String xml = "<Goal on='navigator' pred='At' id='"+goalId+"'>\n";
    // // xml += "<Variable name='z'>\n\t<float min='"+depth+"' max='"+depth+"'/>\n</Variable>\n";
    // //
    // // xml += "<Variable name='latitude'>\n\t<float min='" + Math.toRadians(lat_deg)
    // // + "' max='" + Math.toRadians(lat_deg) + "'/>\n</Variable>\n";
    // // xml += "<Variable name='longitude'>\n\t<float min='" + Math.toRadians(lon_deg)
    // // + "' max='" + Math.toRadians(lon_deg) + "'/>\n</Variable>\n";
    // // xml += "<Variable name='secs'>\n\t<int min='"+secs+"' max='"+secs+"'/>\n</Variable>\n";
    //
    // // if (setStartDate) {
    // // Date min = new Date(start*1000);
    // // xml += "<Variable name='start'>\n\t<date min='"+sdf.format(min)+"'/>\n</Variable>\n";
    // // }
    // // if (setEndDate) {
    // // Date min = new Date(end*1000);
    // // xml += "<Variable name='end'>\n\t<date min='"+sdf.format(min)+"'/>\n</Variable>\n";
    // // }
    // // xml +="</Goal>";
    //
    // return xml;
    // }
    
    public static void main(String[] args) throws Exception {
        TrexGoal goal = new TrexGoal();
        
        
        PropertiesEditor.editProperties(goal, true);
        String xml = goal.asXml();
        NeptusLog.pub().info("<###> "+xml);
        PropertiesEditor.editProperties(goal, true);
        goal.parseXml(xml);
        
        NeptusLog.pub().info("<###> "+goal.asXml());
    }
    
    
}
