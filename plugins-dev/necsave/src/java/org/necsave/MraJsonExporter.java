/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Oct 29, 2015
 */
package org.necsave;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import javax.swing.ProgressMonitor;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.gson.Gson;

import info.necsave.msgs.PlatformState;
import info.necsave.proto.Message;
import info.necsave.proto.ProtoDefinition;
import pt.lsts.imc.AcousticLink;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.UamRxFrame;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author zp
 *
 */
@PluginDescription(name="Export State to JSON", description="Export vehicle state to JSON format")
public class MraJsonExporter implements MRAExporter {

    boolean first = true;
    
    public MraJsonExporter(IMraLogGroup log) {
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLog("EstimatedState") != null || source.getLog("UamRxFrame") != null;
    }

    private void processEstimatedState(EstimatedState msg, BufferedWriter writer, Gson gson) throws IOException {
        SystemPositionAndAttitude state = IMCUtils.parseState(msg);
        JsonState jsonState = new JsonState();
        jsonState.latitude = state.getPosition().getLatitudeDegs();
        jsonState.longitude = state.getPosition().getLongitudeDegs();
        jsonState.depth = state.getDepth();
        jsonState.altitude = state.getAltitude();
        jsonState.timestamp = state.getTime() / 1000.0;
        jsonState.p = state.getP();
        jsonState.q = state.getQ();
        jsonState.r = state.getR();
        jsonState.u = state.getU();
        jsonState.v = state.getV();
        jsonState.w = state.getW();
        jsonState.roll = state.getRoll();
        jsonState.pitch = state.getPitch();
        jsonState.yaw = state.getYaw();
        jsonState.vehicle = msg.getSourceName();
        
        writer.write(gson.toJson(jsonState));
    }
    
    private boolean processUamFrame(UamRxFrame msg, BufferedWriter writer, Gson gson) throws IOException {
        AcousticMessage acMsg = new AcousticMessage();
        byte[] data = msg.getData();
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int sync = buf.getShort()&0xFFFF;
        if (sync != 0xDE40)
            return false;
        
        String type = ProtoDefinition.getInstance().getMessageName(buf.getShort()&0xFFFF);
        if (type == null)
            return false;
        
        Message m = ProtoDefinition.getFactory().createMessage(type, ProtoDefinition.getInstance());
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(new ByteArrayInputStream(data, 4, data.length-4));
        ProtoDefinition.getInstance().deserializeFields(m, dis);
        
        acMsg.source = msg.getSysSrc();
        acMsg.destination = msg.getSysDst();
        acMsg.size = msg.getData().length;
        acMsg.receiveTime = msg.getTimestamp();
        
        if (m instanceof PlatformState) {
            PlatformState platfState = (PlatformState)m;
            acMsg.sendTime = platfState.getOriginTimestamp();
            acMsg.depth = platfState.getZ();
            acMsg.lat = Math.toDegrees(platfState.getLatitude());
            acMsg.lon = Math.toDegrees(platfState.getLongitude());
            acMsg.state = platfState.getInteger("state");
            acMsg.plan_version = platfState.getPlanVersion();
            System.out.println(gson.toJson(acMsg));
            if (!first)
                writer.write(",\n");
            first = false;
            writer.write(gson.toJson(acMsg));
        }
        else {
            System.out.println(m);
        }                   
        return true;
    }
    
    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        first = true;
        LsfIndex index = source.getLsfIndex();
        int count = 0;
        File out = new File(source.getFile("mra"), "position.json");
        BufferedWriter writer;
        Gson gson = new Gson();
        pmonitor.setNote("Searching messages in log...");
        
        try {
            writer = new BufferedWriter(new FileWriter(out));
            writer.write("[\n");    
        }
        catch (Exception e) {
            e.printStackTrace();
            return "Error writing file";
        }
        
        ArrayList<Integer> messagesOfInterest = new ArrayList<>();
        messagesOfInterest.add(EstimatedState.ID_STATIC);
        messagesOfInterest.add(UamRxFrame.ID_STATIC);
        messagesOfInterest.add(AcousticLink.ID_STATIC);
        
        ArrayList<Integer> positions = new ArrayList<>();
        
        for (int i = 0; i < index.getNumberOfMessages(); i++) {
            if (messagesOfInterest.contains(index.typeOf(i)))
                positions.add(i);
        }
          
        for (int i = 0; i < positions.size(); i++) {
            try {
                int pos = positions.get(i);
                switch (index.typeOf(pos)) {
                    case EstimatedState.ID_STATIC:
                        processEstimatedState(index.getMessage(pos, EstimatedState.class), writer, gson);
                        count++;
                        break;
                    case UamRxFrame.ID_STATIC:
                        if (processUamFrame(index.getMessage(pos, UamRxFrame.class), writer, gson))
                            count++;                        
                        break;                    
                    default:
                        break;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        try {
            writer.write("]\n");
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return "Error writing file";
        }
              

        return "Exported "+count+" messages";
    }
    
    public String process2(IMraLogGroup source, ProgressMonitor pmonitor) {
        LsfIndex index = source.getLsfIndex();
        int count = 0;

        File out = new File(source.getFile("mra"), "position.json");
        pmonitor.setNote("Exporting...");
        BufferedWriter writer;
        Gson gson = new Gson();
        
        try {
            writer = new BufferedWriter(new FileWriter(out));
            writer.write("[\n");
            
            for (int i = index.getFirstMessageOfType("EstimatedState"); 
                    i != -1; i = index.getNextMessageOfType("EstimatedState", i)) {
                if (count > 0)
                    writer.write(",\n");
                
                pmonitor.setProgress( (int) ((100.0*i)/index.getNumberOfMessages()));
                IMCMessage msg = index.getMessage(i);
                SystemPositionAndAttitude state = IMCUtils.parseState(msg);
                JsonState jsonState = new JsonState();
                jsonState.latitude = state.getPosition().getLatitudeDegs();
                jsonState.longitude = state.getPosition().getLongitudeDegs();
                jsonState.depth = state.getDepth();
                jsonState.altitude = state.getAltitude();
                jsonState.timestamp = state.getTime() / 1000.0;
                jsonState.p = state.getP();
                jsonState.q = state.getQ();
                jsonState.r = state.getR();
                jsonState.u = state.getU();
                jsonState.v = state.getV();
                jsonState.w = state.getW();
                jsonState.roll = state.getRoll();
                jsonState.pitch = state.getPitch();
                jsonState.yaw = state.getYaw();
                jsonState.vehicle = msg.getSourceName();
                
                writer.write(gson.toJson(jsonState));
                
                
                count++;
            }
            writer.write("]\n");
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return e.getClass()+": "+e.getMessage();
        }

        return "Exported "+count+" messages";
    }

    class JsonState {
        String vehicle;
        double timestamp, latitude, longitude, depth, altitude, roll, pitch, yaw;
        double u, v, w, p, q, r;
    }
    
    class AcousticMessage {
        String source;
        String destination;
        double lat, lon, depth;
        double sendTime, receiveTime;
        int size, plan_version, state;
    }
    
    class AcousticLinkQuality {
        String sys1;
        String sys2;
        double timestamp;
        int quality, rssi;
    }
    
}
