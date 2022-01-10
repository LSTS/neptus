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
 * May 28, 2014
 */
package pt.lsts.neptus.plugins.sunfish.awareness;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.JFileChooser;

import pt.lsts.neptus.plugins.sunfish.awareness.SunfishAssetProperties.AssetDesc;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;


/**
 * @author zp
 *
 */
public class PositionsToKml {
    
    public static void main(String[] args) throws Exception {
        
        Collection<AssetDesc> props =  new SunfishAssetProperties().fetchAssets();
        Collection<AssetPosition> history = PositionHistory.getHistory();
        
        LinkedHashMap<String, Vector<AssetPosition>> tracks = new LinkedHashMap<>();
        LinkedHashMap<String, AssetDesc> assetProperties = new LinkedHashMap<>();
        
        for (AssetDesc desc : props)
            assetProperties.put(desc.name, desc);
        
        for (AssetPosition p : history) {
            String name = p.getAssetName();
            if (!tracks.containsKey(name))
                tracks.put(name, new Vector<AssetPosition>());
            tracks.get(name).add(p);
        }
        
        for (Vector<AssetPosition> v : tracks.values())
            Collections.sort(v);

        StringBuilder sb = new StringBuilder();
        sb.append(prelude());
        
        for (String name : tracks.keySet()) {
            sb.append(track(name, assetProperties.get(name), tracks.get(name)));
        }
        
        sb.append(epilogue());
        
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Destination to save KML");
        chooser.setFileFilter(GuiUtils.getCustomFileFilter("KML files", "kml"));
        int op = chooser.showSaveDialog(null);
        if (op == JFileChooser.APPROVE_OPTION)
            FileUtil.saveToFile(chooser.getSelectedFile().getAbsolutePath(), sb.toString());
    }
    
    private static String getColor(Color c) {
        return String.format("%02x", c.getAlpha()) + 
                String.format("%02x", c.getBlue()) +
                String.format("%02x", c.getGreen()) +
                String.format("%02x", c.getRed());
    }
    
    private static String track(String name, AssetDesc desc, Vector<AssetPosition> positions) {
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'hh:mm:ss'Z'");
        
        StringBuilder b = new StringBuilder(); 
        String description = "";
        b.append("\t<Folder>\n");
        if (desc != null) {
            name = desc.friendly;
            description = desc.description;
            b.append("\t<name>"+desc.friendly+"</name>\n");
            b.append("\t<description>"+desc.description+"</description>\n");            
        }
        else
            b.append("\t<name>"+name+"</name>\n");
        
        AssetPosition last = positions.lastElement();

        b.append("\t<Placemark>\n"+
                "\t\t<name>"+name+"</name>\n"+
                "\t\t<description>"+description+"</description>\n"+
                //"\t\t<color>ffcccccc</color>\n"+
                "\t\t<Point>\n"+
                "\t\t<TimeStamp>"+sdf.format(new Date(last.getTimestamp()))+"</TimeStamp>\n"+
                "\t\t\t<coordinates>"+last.getLoc().getLongitudeDegs()+","+last.getLoc().getLatitudeDegs()+"</coordinates>\n"+
                "\t\t</Point>\n"+
                "\t\t<Style>\n"+
                "\t\t\t<LineStyle>\n"+
                (desc == null ? "\t\t\t<color>ff33cc33</color>\n" : "\t\t\t<color>"+getColor(desc.color)+"</color>\n")+
                "\t\t\t<width>3</width>\n"+
                "\t\t\t</LineStyle>\n"+
                "\t\t</Style>\n");
        
        b.append("\t<LineString>\n"+
                "\t<extrude>1</extrude>\n"+
                "\t<tessellate>1</tessellate>\n"+
                "\t<altitudeMode>absolute</altitudeMode>\n"+
                "\t<coordinates>");
        
        boolean first = true;
        
        for (AssetPosition p : positions) {
            if (first) {
                b.append(p.getLoc().getLongitudeDegs()+","+p.getLoc().getLatitudeDegs()+",0");
                first = false;
            }
            else
                b.append(","+p.getLoc().getLongitudeDegs()+","+p.getLoc().getLatitudeDegs()+",0");
        }
        
        b.append("</coordinates>\n"+
                "\t</LineString>\n"+
                "\t</Placemark>\n");
        
        long lastTime = 0;
        
        for (AssetPosition p : positions) {
            
            if (p.getTimestamp() - lastTime > 3600000) {
                b.append("\t\t\t<Placemark>"+
                        "\t\t\t<name>"+sdf.format(new Date(p.getTimestamp()))+"</name>\n"+
                        "\t\t\t<description>Position @ "+sdf.format(new Date(p.getTimestamp()))+"</description>\n"+
                        "\t\t\t<TimeStamp>"+sdf.format(new Date(p.getTimestamp()))+"</TimeStamp>\n"+
                        "\t\t\t<Point>"+                        
                        "\t\t\t<coordinates>"+p.getLoc().getLongitudeDegs()+","+p.getLoc().getLatitudeDegs()+
                        "</coordinates>\n"+
                        "\t\t\t</Point>\n"+
                        "\t\t\t</Placemark>");
                lastTime = p.getTimestamp();
            }
            
        }

        b.append("\t</Folder>\n");
        return b.toString();
        
    }
    
    private static String prelude() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\"> <Document>\n"+
                "\t<name>Sunfish Tag positions</name>\n";
    }
    
    private static String epilogue() {
        return "</Document> </kml>\n";
    }
}
