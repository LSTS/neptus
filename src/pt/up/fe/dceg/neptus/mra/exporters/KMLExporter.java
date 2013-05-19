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
 * Author: José Correia
 * Jan 8, 2013
 */
package pt.up.fe.dceg.neptus.mra.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.Vector;

import javax.imageio.ImageIO;

import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.lsf.LsfGenericIterator;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.NeptusMRA;
import pt.up.fe.dceg.neptus.mra.WorldImage;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;
import pt.up.fe.dceg.neptus.util.llf.LogUtils;

/**
 * @author zp
 */
public class KMLExporter implements MraExporter {
    public double minLat = 180;
    public double maxLat = -180;
    public double minLon = 360;
    public double maxLon = -360;

    public double minHeight = 1000;
    public double maxHeight = -1;

    LocationType topLeftLT;
    LocationType bottomRightLT;

    File f, output;
    IMraLogGroup source;
    MRAPanel panel;

    public KMLExporter(MRAPanel panel, IMraLogGroup source) {
        this.source = source;
        this.panel = panel;
    }
    
    @Override
    public String getName() {
        return "Export to KML";
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    public String kmlHeader(String title) {
        String ret = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<kml xmlns=\"http://earth.google.com/kml/2.1\">\n";
        ret += "\t<Document>\n";
        ret += "\t\t<name>" + title + "</name>\n";
        
        Date d = new Date((long)(1000*source.getLsfIndex().getStartTime()));
        ret += "\t\t<description>Plan executed on "+d+"</description>";

        ret += "\t\t<Style id=\"estate\">\n";
        ret += "\t\t\t<LineStyle>\n";
        ret += "\t\t\t<color>99ff0000</color>\n";
        ret += "\t\t\t<width>4</width>\n";
        ret += "\t\t\t</LineStyle>\n";
        ret += "\t\t</Style>\n";

        ret += "\t\t<Style id=\"plan\">\n";
        ret += "\t\t\t<LineStyle>\n";
        ret += "\t\t\t<color>990000ff</color>\n";
        ret += "\t\t\t<width>4</width>\n";
        ret += "\t\t\t</LineStyle>\n";
        ret += "\t\t</Style>\n";

        return ret;
    }
    
    public String overlay(File imageFile, String title, LocationType sw, LocationType ne) {
        sw.convertToAbsoluteLatLonDepth();
        ne.convertToAbsoluteLatLonDepth();
        String ret = "\t\t<GroundOverlay>\n";
        try {        
        ret += "\t\t\t<name>"+title+"</name>\n";
        ret += "\t\t\t<description></description>\n";
        ret += "\t\t\t<Icon>\n";
        
        ret += "\t\t\t\t<href>"+imageFile.toURI().toURL()+"</href>\n";
        ret += "\t\t\t</Icon>\n";
        ret += "\t\t\t<LatLonBox>\n";
        ret += "\t\t\t\t<north>"+ne.getLatitudeAsDoubleValue()+"</north>\n";
        ret += "\t\t\t\t<south>"+sw.getLatitudeAsDoubleValue()+"</south>\n";
        ret += "\t\t\t\t<east>"+ne.getLongitudeAsDoubleValue()+"</east>\n";
        ret += "\t\t\t\t<west>"+sw.getLongitudeAsDoubleValue()+"</west>\n";
        ret += "\t\t\t\t<rotation>0</rotation>\n";
        ret += "\t\t\t</LatLonBox>\n";
        ret += "\t\t</GroundOverlay>\n";
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
            
        }
        return ret;
    }

    public String path(Vector<LocationType> coords, String name, String style) {
        String ret = "\t\t<Placemark>\n";
        ret += "\t\t\t<name>" + name + "</name>\n";
        ret += "\t\t\t<styleUrl>#" + style + "</styleUrl>\n";
        ret += "\t\t\t<LineString>\n";
        ret += "\t\t\t\t<altitudeMode>relative</altitudeMode>\n";
        ret += "\t\t\t\t<coordinates> ";

        for (LocationType l : coords) {
            l.convertToAbsoluteLatLonDepth();
            ret +=l.getLongitudeAsDoubleValue() + "," +  l.getLatitudeAsDoubleValue() +",0\n";//-" + l.getDepth()+"\n";
        }
        ret += "\t\t\t\t</coordinates>\n";
        ret += "\t\t\t</LineString>\n";
        ret += "\t\t</Placemark>\n";
        return ret;
    }
    
    public String kmlFooter() {
        return "\t</Document>\n</kml>\n";
    }        

    public String process() {
        
        
        try {
            File out = new File(source.getFile("mra"),"kml");
            out.mkdirs();

            out = new File(out, "data.kml");
            BufferedWriter bw = new BufferedWriter(new FileWriter(out));
            File f = source.getFile(".");
            String name = f.getCanonicalFile().getName();
            bw.write(kmlHeader(name));
            
            Vector<LocationType> states = new Vector<>();
            
            // Path
            LsfGenericIterator it = source.getLsfIndex().getIterator("EstimatedState", 0, 3000);
            for (IMCMessage s : it) {
                states.add(IMCUtils.parseState(s).getPosition());
            }
            bw.write(path(states, "Estimated State", "estate"));
            
            // plan
            MissionType mt = LogUtils.generateMission(source);
            PlanType plan = LogUtils.generatePlan(mt, source);
            bw.write(path(plan.planPath(), "Planned waypoints", "plan"));
            
            // DVL
            
            WorldImage imgDvl = new WorldImage(1, ColorMapFactory.createJetColorMap());
            imgDvl.setMaxVal(20d);
            imgDvl.setMinVal(3d);
            it = source.getLsfIndex().getIterator("EstimatedState", 0, 100);
            for (IMCMessage s : it) {                
                LocationType loc = IMCUtils.parseState(s).getPosition();
                double alt = s.getDouble("alt");
                double depth = s.getDouble("depth");
                if (alt == -1 || depth < NeptusMRA.minDepthForBathymetry)
                    continue;
                else
                    imgDvl.addPoint(loc, s.getDouble("alt"));             
            }            
            ImageIO.write(imgDvl.processData(), "PNG", new File(out.getParent(),"dvl_bath.png"));
            bw.write(overlay(new File(out.getParent(),"dvl_bath.png"), "DVL Bathymetry", imgDvl.getSouthWest(), imgDvl.getNorthEast()));
                       
            // MULTIBEAM
//            
//            if (source.getFile("multibeam.83P") != null) {
//                WorldImage imgMb = new WorldImage(1, ColorMapFactory.createHotColorMap());        
//                DeltaTParser parser = new DeltaTParser(source);
//                parser.rewind();
//                BathymetrySwath swath;
//                long first = (long)(1000 * source.getLsfIndex().getStartTime());
//                
//                //long first = parser.getFirstTimestamp();
//                long time = (long)(1000 * source.getLsfIndex().getEndTime()) - first;
//                long lastPercent = -1;
//                
//                while((swath = parser.nextSwath(0.1)) != null) {
//                    LocationType loc = swath.getPose().getPosition();
//                    for(BathymetryPoint bp : swath.getData()) {
//                        if (Math.random() < 0.1)
//                            continue;
//                        LocationType loc2 = new LocationType(loc);
//                        if(bp == null)
//                            continue;
//                        loc2.translatePosition(bp.north, bp.east, 0);
//                        imgMb.addPoint(loc2, bp.depth);
//                        long percent = ((swath.getTimestamp() - first) * 100) / time;
//                        if (percent != lastPercent)
//                            NeptusLog.pub().info("MULTIBEAM: "+percent+"% done...");
//                        lastPercent = percent;
//                    }
//                }
//                
//                ImageIO.write(imgMb.processData(), "PNG", new File(out.getParent(),"mb_bath.png"));
//                bw.write(overlay(new File(out.getParent(),"mb_bath.png"), "Multibeam Bathymetry", imgMb.getSouthWest(), imgMb.getNorthEast()));
//            }
            bw.write(kmlFooter());            
            
            bw.close();
            
            return "Log exported to "+out.getAbsolutePath();
        }
        catch (Exception e) {
            GuiUtils.errorMessage("Error while exporting to KML", "Exception of type " + e.getClass().getSimpleName()
                    + " occurred: " + e.getMessage());
            e.printStackTrace();
            return e.getClass().getSimpleName()+" while exporting to KML: "+e.getMessage();
        }
    }

}
