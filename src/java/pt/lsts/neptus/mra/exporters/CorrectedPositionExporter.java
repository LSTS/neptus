/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Sep 16, 2018
 */
package pt.lsts.neptus.mra.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.swing.ProgressMonitor;

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
@PluginDescription(name = "Export corrected positions as CSV")
public class CorrectedPositionExporter implements MRAExporter {

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLsfIndex().containsMessagesOfType("EstimatedState");
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        CorrectedPosition positions = new CorrectedPosition(source);
        File dir = new File(source.getFile("mra"), "csv");
        dir.mkdirs();

        File out = new File(dir, "CorrectedPositions.csv");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(out))) {
            writer.write("timestamp (s), latitude (deg), longitude (deg), depth (m), altitude (m), roll (deg), pitch (deg), yaw (deg), speed (m/s)\n");
            for (SystemPositionAndAttitude pos : positions.getPositions()) {
                try {
                    writer.write(pos.getTime() / 1000.0 + ", " + pos.getPosition().getLatitudeDegs() + ", "
                            + pos.getPosition().getLongitudeDegs() + ", " + pos.getDepth() + ", " + pos.getAltitude()
                            + ", " + Math.toDegrees(pos.getRoll()) + ", " + Math.toDegrees(pos.getPitch()) + ", "
                            + Math.toDegrees(pos.getYaw()) + ", " + pos.getU() + "\n");
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                    return I18n.textf("Error writing to file: %error", e.getMessage());
                }
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return I18n.textf("Error creating output file: %error", e.getMessage());
        }
        return I18n.text("Finished");
    }
}