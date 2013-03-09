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
 * Author: zp
 * Jun 28, 2012
 */
package pt.up.fe.dceg.neptus.plugins.trex;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.gui.editor.UnixTimeEditor;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author zp
 *
 */
public class TrexGoal implements PropertiesProvider {

    protected String timeline = "estimator", predicate = "At";
    protected String goalId = "Neptus_"+System.currentTimeMillis();
    protected double speed = 1.0, depth = 2, lat_deg = 41, lon_deg = -8, tolerance = 15;
    protected long start = new Date().getTime()/1000, end = new Date().getTime()/1000, secs = 0;
    protected boolean setStartDate = false;
    protected boolean setEndDate = false;
    @Override
    public DefaultProperty[] getProperties() {
        
        DefaultProperty startProp = PropertiesEditor.getPropertyInstance("Start date", Long.class, start, true);
        DefaultProperty endProp = PropertiesEditor.getPropertyInstance("End date", Long.class, end, true);
        
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(startProp, UnixTimeEditor.class);
        PropertiesEditor.getPropertyEditorRegistry().registerEditor(endProp, UnixTimeEditor.class);
        return new DefaultProperty[] {
                PropertiesEditor.getPropertyInstance("Timeline", String.class, timeline, false),
                PropertiesEditor.getPropertyInstance("Predicate", String.class, predicate, false),
                PropertiesEditor.getPropertyInstance("Goal ID", String.class, goalId, true),
                PropertiesEditor.getPropertyInstance("Depth", Double.class, depth, true),
                PropertiesEditor.getPropertyInstance("Speed", Double.class, speed, true),
                PropertiesEditor.getPropertyInstance("Tolerance", Double.class, tolerance, true),
                PropertiesEditor.getPropertyInstance("Latitude (degrees)", Double.class, lat_deg, true),
                PropertiesEditor.getPropertyInstance("Longitude (degrees)", Double.class, lon_deg, true),
                startProp, 
                PropertiesEditor.getPropertyInstance("Specify start date", Boolean.class, setStartDate, true),
                endProp,
                PropertiesEditor.getPropertyInstance("Specify end date", Boolean.class, setEndDate, true)
        };
    }

    @Override
    public void setProperties(Property[] properties) {
        for (Property p : properties) {
            if (p.getName().equals("Goal ID")) {
                goalId = p.getValue().toString();
            }
            else if (p.getName().equals("Tolerance")) {
                tolerance = (Double)p.getValue();
            }
            else if (p.getName().equals("Depth")) {
                depth = (Double)p.getValue();
            }
            else if (p.getName().equals("Speed")) {
                speed = (Double)p.getValue();
            }
            else if (p.getName().equals("Latitude (degrees)")) {
                lat_deg = (Double)p.getValue();
            }
            else if (p.getName().equals("Longitude (degrees)")) {
                lon_deg = (Double)p.getValue();
            }
            else if (p.getName().equals("Start date")) {
                start = (Long)p.getValue();
            }
            else if (p.getName().equals("End date")) {
                end = (Long)p.getValue();
            }
            else if (p.getName().equals("Specify start date")) {
                setStartDate = ((Boolean)p.getValue()).booleanValue();
            }
            else if (p.getName().equals("Specify end date")) {
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
    
    public LocationType getLocation() {
        LocationType loc = new LocationType(lat_deg, lon_deg);
        loc.setAbsoluteDepth(depth);
        return loc;        
    }
    
    public void parseXml(String xml) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd hh:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        Document doc = DocumentHelper.parseText(xml);
        timeline = doc.getRootElement().selectSingleNode("@on").getText();
        predicate = doc.getRootElement().selectSingleNode("@pred").getText();
        goalId = doc.getRootElement().selectSingleNode("@id").getText();
        
        List<?> l = doc.selectNodes("Goal/Variable");
        for (Object o : l) {
            Node n = (Node)o;
            String varName = n.selectSingleNode("@name").getText();
            
            if (varName.equals("lat_deg"))
                lat_deg = Double.parseDouble(n.selectSingleNode("float/@min").getText());
            else if (varName.equals("lon_deg"))
                lon_deg = Double.parseDouble(n.selectSingleNode("float/@min").getText());
            else if (varName.equals("depth"))
                depth = Double.parseDouble(n.selectSingleNode("float/@min").getText());
            else if (varName.equals("speed"))
                speed = Double.parseDouble(n.selectSingleNode("float/@min").getText());
            else if (varName.equals("tolerance"))
                tolerance = Double.parseDouble(n.selectSingleNode("float/@min").getText());
            else if (varName.equals("start"))
                start = sdf.parse(n.selectSingleNode("date/@min").getText()).getTime()/1000;
            else if (varName.equals("end"))
                end = sdf.parse(n.selectSingleNode("date/@min").getText()).getTime()/1000;
            else if (varName.equals("secs"))
                secs = Integer.parseInt(n.selectSingleNode("int/@min").getText());            
        }
    }
    
    public String asXml() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd hh:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String xml = "<Goal on='estimator' pred='At' id='"+goalId+"'>\n";
        xml += "<Variable name='depth'>\n\t<float min='"+depth+"' max='"+depth+"'/>\n</Variable>\n";
        
        xml += "<Variable name='speed'>\n\t<float min='"+speed+"' max='"+speed+"'/>\n</Variable>\n";
        xml += "<Variable name='lat_deg'>\n\t<float min='" + lat_deg
                + "' max='" + lat_deg + "'/>\n</Variable>\n";
        xml += "<Variable name='lon_deg'>\n\t<float min='" + lon_deg
                + "' max='" + lon_deg + "'/>\n</Variable>\n";
        xml += "<Variable name='secs'>\n\t<int min='"+secs+"' max='"+secs+"'/>\n</Variable>\n";
        xml += "<Variable name='tolerance'>\n\t<float min='"+tolerance+"' max='"+tolerance+"'/>\n</Variable>\n";
        
        if (setStartDate) {
            Date min = new Date(start*1000);
            xml += "<Variable name='start'>\n\t<date min='"+sdf.format(min)+"'/>\n</Variable>\n";
        }
        if (setEndDate) {
            Date min = new Date(end*1000);
            xml += "<Variable name='end'>\n\t<date min='"+sdf.format(min)+"'/>\n</Variable>\n";            
        }
        xml +="</Goal>";
        
        return xml;
    }
    
    public static void main(String[] args) throws Exception {
        TrexGoal goal = new TrexGoal();
        
        
        PropertiesEditor.editProperties(goal, true);
        String xml = goal.asXml();
        System.out.println(xml);
        PropertiesEditor.editProperties(goal, true);
        goal.parseXml(xml);
        
        System.out.println(goal.asXml());
    }
    
    
}
