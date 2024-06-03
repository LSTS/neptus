/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Apr 9, 2019
 */
package pt.lsts.neptus.mra.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.MagneticField;
import pt.lsts.imc.TotalMagIntensity;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author zp
 *
 */
@PluginDescription(name="Export Magnetometer data to CSV")
public class MagnetometryExporter implements MRAExporter {

    public MagnetometryExporter(IMraLogGroup source) {
        
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        LsfIndex index = source.getLsfIndex();
        pmonitor.setMaximum(index.getNumberOfMessages());
        pmonitor.setMinimum(index.getNumberOfMessages());
        pmonitor.setNote(I18n.text("Creating output folder..."));
        
        File dir = new File(source.getFile("mra"), "magnetometer");
        dir.mkdirs();
        File file1 = new File(dir, "TotalMagneticIntensity.csv");
        File file2 = new File(dir, "MagneticField.csv");
        
        CorrectedPosition pos = CorrectedPosition.getInstance(source);
        
        try {
            BufferedWriter tmi = new BufferedWriter(new FileWriter(file1));
            tmi.write("time (unix), time (utc), type, latitude, longitude, depth, altitude, tmi\n");

            for (TotalMagIntensity msg : index.getIterator(TotalMagIntensity.class)) {
                SystemPositionAndAttitude loc = pos.getPosition(msg.getTimestamp());

                String line = String.format("%.03f, %s, %30s, %.07f, %.07f, %.02f, %.02f, %.03f\n", msg.getTimestamp(),
                        sdf.format(msg.getDate()), msg.getEntityName(), loc.getPosition().getLatitudeDegs(),
                        loc.getPosition().getLongitudeDegs(), loc.getDepth(), loc.getAltitude(), msg.getValue()*100_000);

                tmi.write(line);
            }
            tmi.close();

            BufferedWriter mf = new BufferedWriter(new FileWriter(file2));
            mf.write(
                    "time (unix), time (utc), type, latitude, longitude, depth, altitude, roll, pitch, yaw, x, y, z\n");

            for (MagneticField msg : index.getIterator(MagneticField.class)) {
                SystemPositionAndAttitude loc = pos.getPosition(msg.getTimestamp());

                String line = String.format(
                        "%.03f, %s, %30s, %.07f, %.07f, %.02f, %.02f, %.02f, %.02f, %.02f, %.03f, %.03f, %.03f\n",
                        msg.getTimestamp(), sdf.format(msg.getDate()), msg.getEntityName(),
                        loc.getPosition().getLatitudeDegs(), loc.getPosition().getLongitudeDegs(), loc.getDepth(),
                        loc.getAltitude(), Math.toDegrees(loc.getRoll()), Math.toDegrees(loc.getPitch()),
                        Math.toDegrees(loc.getYaw()), msg.getX()*100_000, msg.getY()*100_000, msg.getZ()*100_000);

                mf.write(line);
            }
            
            mf.close();
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            e.printStackTrace();
            return e.getMessage();
        }
        
        return "Completed.";
        
    }

}
