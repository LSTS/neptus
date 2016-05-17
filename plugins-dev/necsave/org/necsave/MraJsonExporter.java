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
 * Oct 29, 2015
 */
package org.necsave;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.swing.ProgressMonitor;

import com.google.gson.Gson;

import pt.lsts.imc.IMCMessage;
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

    public MraJsonExporter(IMraLogGroup log) {
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLog("EstimatedState") != null;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
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
}
