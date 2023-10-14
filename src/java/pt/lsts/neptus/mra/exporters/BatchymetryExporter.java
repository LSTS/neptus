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
 * Mar 25, 2020
 */
package pt.lsts.neptus.mra.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

import javax.swing.JFileChooser;
import javax.swing.ProgressMonitor;

import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PopUp;
import pt.lsts.imc.VehicleMedium;
import pt.lsts.imc.VehicleMedium.MEDIUM;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 *
 */
public class BatchymetryExporter implements MRAExporter {

    @NeptusProperty
    public static File outFile = new File("/home/zp/Desktop/out.csv");
    
    private CorrectedPosition positions = null;
    
    public BatchymetryExporter(IMraLogGroup source) {
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        try  {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile, true));
            positions = new CorrectedPosition(source);
            LsfIndex index = source.getLsfIndex();
            int timeIdx = 0;
            for (double time = index.getStartTime()+1; time < index.getEndTime(); time++) {
                timeIdx = index.advanceToTime(timeIdx, time);
                int vmIdx = index.getPreviousMessageOfType(VehicleMedium.ID_STATIC, timeIdx);
                int manIdx = index.getPreviousMessageOfType(PlanControlState.ID_STATIC, timeIdx);
                if (vmIdx == -1 || manIdx == -1)
                    continue;
                
                VehicleMedium medium = index.getMessage(vmIdx, VehicleMedium.class);
                PlanControlState manState = index.getMessage(manIdx, PlanControlState.class);
                if (manState.getManType() == PopUp.ID_STATIC)
                    continue;
                SystemPositionAndAttitude pos = positions.getPosition(time);
                if (pos.getAltitude() == -1)
                    continue;
                
                double tide = TidePredictionFactory.getTideLevel((long)(time*1000));
                String line = time+","+medium.getSourceName()+","+pos.getPosition().getLatitudeDegs()+","+pos.getPosition().getLongitudeDegs()+","+(pos.getAltitude()+pos.getDepth()-tide);
                if (medium.getMedium() == MEDIUM.UNDERWATER || medium.getMedium() == MEDIUM.WATER) {
                    System.out.println(line);
                    writer.write(line+"\n");                    
                }
            }
            writer.flush();
            writer.close();
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return "Error: "+e.getMessage();
        }
        
        return "done.";
    }
    
    private static File selectDestination() {
        JFileChooser fileChooser = GuiUtils.getFileChooser(new File("."));
        int op = fileChooser.showDialog(null, "Set destination");
        if (op == JFileChooser.APPROVE_OPTION)
            return fileChooser.getSelectedFile();
        else
            System.exit(0);
        return null;
    }
    public static void main(String[] args) throws Exception {
        ConfigFetch.initialize();
        GuiUtils.setLookAndFeelNimbus();
        
        BatchymetryExporter.outFile = selectDestination();
        if (outFile.exists())
            Files.delete(outFile.toPath());
        BatchMraExporter.apply(BatchymetryExporter.class);
    }

}
