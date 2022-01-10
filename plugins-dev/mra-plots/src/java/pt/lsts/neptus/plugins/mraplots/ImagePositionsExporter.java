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
 */
package pt.lsts.neptus.plugins.mraplots;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Locale;

import javax.swing.ProgressMonitor;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author zp
 */
@PluginDescription(name="Export image positions to CSV")
public class ImagePositionsExporter implements MRAExporter {
    private CorrectedPosition positions;
    public ImagePositionsExporter(IMraLogGroup source) {
        positions = new CorrectedPosition(source);
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getFile("Photos") == null ? false : source.getFile("Photos").exists();
    }

    public String getHeader() {
        return "timestamp, latitude, longitude, depth, altitude, sog, roll, pitch, yaw";
    }

    public String getLine(double time) {
        SystemPositionAndAttitude pose = positions.getPosition(time);

        return String.format(Locale.US, "%.4f, %.5f, %.5f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f", time,
                pose.getPosition().getLatitudeDegs(), pose.getPosition().getLongitudeDegs(), pose.getDepth(),
                pose.getAltitude(), pose.getU(), Math.toDegrees(pose.getRoll()), Math.toDegrees(pose.getPitch()),
                Math.toDegrees(pose.getYaw()));

    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        File dir = new File(source.getFile("mra"), "csv");
        dir.mkdirs();

        File out = new File(dir, "ImagePositions.csv");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(out));
            bw.write(getHeader()+"\n");
            for (File f : MraPhotosVisualization.listPhotos(source.getFile("Photos"))) {
                String fname = f.getName().substring(0, f.getName().lastIndexOf("."));
                double time = Double.parseDouble(fname);
                System.out.println(fname);
                bw.write(getLine(time)+"\n");
            }
            bw.close();

            return I18n.text("Process complete");    
        }
        catch (Exception e) {
            return I18n.text("Error during processing: "+e.getClass().getSimpleName()+" / "+e.getMessage());
        }
        
    }
}
