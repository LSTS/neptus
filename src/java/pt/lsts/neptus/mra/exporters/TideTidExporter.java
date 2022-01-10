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
 * Author: pdias
 * 04/12/2015
 */
package pt.lsts.neptus.mra.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;
import pt.lsts.neptus.util.bathymetry.TidePredictionFinder;
import pt.lsts.neptus.util.tid.TidWriter;

/**
 * @author pdias
 *
 */
@PluginDescription(name = "Tide TID Exporter", description = "This will export the tide correction file to TID file.")
public class TideTidExporter implements MRAExporter {

    /** The TID writer */
    private TidWriter tidWriter;
    /** The TID prediction finder */
    private TidePredictionFinder finder = null;
    
    public TideTidExporter(IMraLogGroup source) {
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        boolean ret = source.getLsfIndex().containsMessagesOfType("EstimatedState");        
        finder = TidePredictionFactory.create(source);
        ret = ret && (finder != null ? true : false); 
        return ret;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        int decimalHouses = 2;
        File outputDir = new File(source.getDir(), "mra");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(new File(outputDir, "Tide.tid")));
            tidWriter = new TidWriter(writer, decimalHouses);
            tidWriter.writeHeader("Tide Data", finder.getName());
            
            pmonitor.setMaximum(100);
            double firstTime = source.getLsfIndex().getStartTime();
            double lastTime = source.getLsfIndex().getEndTime();
            double logTime = lastTime - firstTime;
            int progress = 0;
            float lastTideVal = 0;
            long counterTides = 0;
            for (EstimatedState es : source.getLsfIndex().getIterator(EstimatedState.class)) {
                if (pmonitor.isCanceled())
                    break;
                
                progress = (int) (100 * ((es.getTimestamp() - firstTime) / logTime));
                pmonitor.setProgress(progress);
                
                Date date = es.getDate();
                float tideVal;
                try {
                    tideVal = finder.getTidePrediction(date, false);
                    tideVal = MathMiscUtils.round(tideVal, decimalHouses);
                }
                catch (Exception e) {
                    continue;
                }
                if (counterTides == 0 || Float.compare(lastTideVal, tideVal) != 0) {
                    tidWriter.writeData(date.getTime(), tideVal);
                    lastTideVal = tideVal;
                    counterTides++;
                }
            }
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
        finally {
            if (writer != null) {
                try {
                    writer.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return I18n.text("Successfully exported tides TID file.");
    }
}
