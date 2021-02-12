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
 * Author: José Pinto
 * Apr 15, 2011
 */
package pt.lsts.neptus.plugins.accu;

import java.awt.Color;
import java.util.Vector;

import javax.vecmath.Point3d;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.types.mission.MissionType;

/**
 * @author zp
 *
 */
public class AccuUtils {

    public static IMCMessage getAccuMap(MissionType mt) {
        IMCMessage msg;
        try {
            msg = IMCDefinition.getInstance().create("Map", "id", mt.getId());
            Vector<IMCMessage> features = new Vector<IMCMessage>();
        for (AbstractElement elem : MapGroup.getMapGroupInstance(mt).getAllObjects()) {
                IMCMessage feature = getAccuMapFeature(elem);
            if (feature != null)
                features.add(feature);
        }
        
        msg.setValue("features", IMCUtils.asMessageList(features));
        return msg;
    }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static IMCMessage getMapPoint(LocationType loc) {
        double lld[] = loc.getAbsoluteLatLonDepth();
        lld[0] = Math.toRadians(lld[0]);
        lld[1] = Math.toRadians(lld[1]);
        lld[2] = -lld[2];
        
        try {
            return IMCDefinition.getInstance().create("MapPoint", "lat", lld[0], "lon", lld[1], "alt", lld[2]);
    }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static IMCMessage getAccuMapFeature(AbstractElement element) {
        
        try {
            IMCMessage msg = IMCDefinition.getInstance().create("MapFeature", "id", element.getId());
        Color color = Color.black;
        
        switch (element.getElementType()) {
            case TYPE_MARK:
                msg.setValue("feature_type", "POI");
                msg.setValue("feature", getMapPoint(element.getCenterLocation()));
                if (!element.getId().equalsIgnoreCase("start"))
                    color = Color.white;
                else {
                    msg.setValue("feature_type", "STARTLOC");
                    color = Color.green;
                }
                break;
                
            case TYPE_PATH:                
                PathElement path = ((PathElement)element);
                if (path.isShape())
                    msg.setValue("feature_type", "FILLEDPOLY");
                else
                    msg.setValue("feature_type", "LINE");
                color = path.getMyColor();
                    Vector<IMCMessage> mapPoints = new Vector<IMCMessage>();
                mapPoints.add(getMapPoint(element.getCenterLocation()));
                for (Point3d pt : path.getPoints()) {
                    LocationType loc = new LocationType(element.getCenterLocation());
                    loc.translatePosition(pt.x, pt.y, pt.z);
                    mapPoints.add(getMapPoint(loc));
                }
                msg.setValue("feature", IMCUtils.asMessageList(mapPoints));
                break;
                
            case TYPE_TRANSPONDER:
                msg.setValue("feature_type", "TRANSPONDER");
                msg.setValue("feature", getMapPoint(element.getCenterLocation()));
                color = Color.orange;
                break;
                
            case TYPE_HOMEREFERENCE:
                msg.setValue("id", "homeref");
                msg.setValue("feature_type", "HOMEREF");                
                msg.setValue("feature", getMapPoint(element.getCenterLocation()));
                color = Color.red;
                break;
                
            case TYPE_PARALLELEPIPED:
                // TODO
                // ParallelepipedElement pp = (ParallelepipedElement) element;
                // msg.setValue("feature_type", "FILLEDPOLY");                
                // color = pp.getMyColor();
                
            default:
                return null;                  
        }
        msg.setValue("rgb_red", color.getRed());
        msg.setValue("rgb_green", color.getGreen());
        msg.setValue("rgb_blue", color.getBlue());
        
        return msg;
    }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
//    public static void main(String[] args) throws Exception {
//        ConfigFetch.initialize();
//        MissionType mt = new MissionType("missions/APDL/missao-apdl.nmisz");
//        IMCMessage msg = getAccuMap(mt);
//        UDPTransport transp = new UDPTransport(6001, 1);
//        SerializationBufferImpl sbi = new SerializationBufferImpl();
//        msg.serialize(sbi);        
//        transp.sendMessage("127.0.0.1", 6002, sbi.getBuffer());
//        JLabel tarea = new JLabel(IMCUtils.getAsHtml(msg));
//        tarea.setBackground(Color.white);
//        GuiUtils.testFrame(new JScrollPane(tarea));        
//    }    
}
