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
 * Oct 30, 2011
 */
package pt.lsts.neptus.plugins.odss;

import java.text.DecimalFormat;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import pt.lsts.imc.Conductivity;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Salinity;
import pt.lsts.imc.Temperature;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.ImcLogUtils;
import pt.lsts.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * FIXME - Can be deleted?
 * 
 * @author zp
 * 
 */
@PluginDescription(name="CSV Data", author="ZP", icon="pt/lsts/neptus/plugins/odss/fileshare.png", active=false)
public class MraCsvExporter extends SimpleMRAVisualization {

    /**
     * @param panel
     */
    public MraCsvExporter(MRAPanel panel) {
        super(panel);
    }

    private static final long serialVersionUID = 1L;
    protected JEditorPane csvEditor = new JEditorPane();
    protected Thread loadingThread = null;
    protected long timestep = 2000;

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return false;
        //        return (source.getLog("EstimatedState") != null && (source.getLog("Conductivity") != null
        //                || source.getLog("Temperature") != null || source.getLog("Salinity") != null)
        //                // FIXME
        //                // &&
        //                // ImcLogUtils.getEntityListReverse(source).get("CTD") != null
        //                );
    }

    @Override
    public JComponent getVisualization(IMraLogGroup source, double timestep) {
        csvEditor.setEditable(false);

        if (csvEditor.getText().length() == 0)
            startLoading(source);

        return new JScrollPane(csvEditor);
    }

    protected Thread startLoading(IMraLogGroup source) {
        if (loadingThread != null)
            loadingThread.interrupt();

        final LsfIndex index = source.getLsfIndex();

        csvEditor.setText("timestamp,latitude,longitude,depth,altitude,conductivity,temperature,salinity\n");

        final int ctdId = ImcLogUtils.getEntityListReverse(source).get("CTD");
        final double start = index.getStartTime();
        final double end = index.getEndTime();
        // final IMraLog estimatedState = source.getLog("EstimatedState");
        //
        // final IMraLog[] logs = new IMraLog[] {
        // source.getLog("Conductivity"),
        // source.getLog("Temperature"),
        // source.getLog("Pressure"),
        // source.getLog("Salinity")
        // };
        // final int ctdId = ImcLogUtils.getEntityListReverse(source).get("CTD");
        // final long firstTime = (estimatedState.currentTimeMillis()/1000)*1000;
        // final long start = estimatedState.nextLogEntry().getHeader().getLong("timestamp");
        //
        loadingThread = new Thread() {

            @Override
            public void run() {
                DecimalFormat latFormat = new DecimalFormat("0.000000");
                DecimalFormat valFormat = new DecimalFormat("0.000");
                int condIndex = 0, tempIndex = 0, salIndex = 0;
                for (double curTime = start + 1; curTime < end; curTime += 1.0) {
                    String line = valFormat.format(curTime);
                    IMCMessage state = index.getMessageAt("EstimatedState", curTime);
                    if (state == null)
                        continue;
                    LocationType loc = IMCUtils.getLocation(state);
                    loc.convertToAbsoluteLatLonDepth();
                    line += "," + latFormat.format(loc.getLatitudeDegs());
                    line += "," + latFormat.format(loc.getLongitudeDegs());
                    line += "," + valFormat.format(state.getDouble("depth"));
                    line += "," + latFormat.format(state.getDouble("alt"));
                    int newCondIndex = index.getMessageAtOrAfer(Conductivity.ID_STATIC, ctdId, condIndex,
                            state.getTimestamp());
                    if (newCondIndex != -1) {
                        condIndex = newCondIndex;
                        line += "," + valFormat.format(index.getMessage(condIndex).getDouble("value"));
                    }
                    else
                        line += ",-1";

                    int newTempIndex = index.getMessageAtOrAfer(Temperature.ID_STATIC, ctdId, tempIndex,
                            state.getTimestamp());
                    if (newTempIndex != -1) {
                        tempIndex = newTempIndex;
                        line += "," + valFormat.format(index.getMessage(tempIndex).getDouble("value"));
                    }
                    else
                        line += ",-1";

                    int newSalIndex = index.getMessageAtOrAfer(Salinity.ID_STATIC, ctdId, salIndex,
                            state.getTimestamp());
                    if (newSalIndex != -1) {
                        salIndex = newSalIndex;
                        line += "," + valFormat.format(index.getMessage(salIndex).getDouble("value"));
                    }
                    else
                        line += ",-1";

                    csvEditor.setText(csvEditor.getText() + line + "\n");
                }
            };
        };

        loadingThread.start();
        return loadingThread;
    }

    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }
}
