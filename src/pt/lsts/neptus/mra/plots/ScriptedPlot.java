/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 28, 2012
 */
package pt.lsts.neptus.mra.plots;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesDataItem;

import groovy.lang.GroovyShell;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.util.GuiUtils;

/**
 * Changes to use Groovy on Feb 2019
 * 
 * @author keila
 * 
 */
public class ScriptedPlot extends MRATimeSeriesPlot {

    protected LinkedHashMap<String, String> traces = new LinkedHashMap<>();

    protected LsfIndex index;

    private GroovyShell shell;
    private final String scriptPath;
    private MRAPanel mra;
    private String title = null;
    private boolean processed = false;

    @Override
    public String getName() {
        if (title == null)
            return I18n.text(Arrays.toString(traces.keySet().toArray()));
        else
            return I18n.text(title);
    }

    @Override
    public String getTitle() {

        if (title == null)
            return I18n.text(Arrays.toString(traces.keySet().toArray()));
        else
            return I18n.text(title);
    }

    public ScriptedPlot(MRAPanel panel, String path) {
        super(panel);
        scriptPath = path;
        index = panel.getSource().getLsfIndex();

        // init shell
        CompilerConfiguration cnfg = new CompilerConfiguration();
        ImportCustomizer imports = new ImportCustomizer();
        imports.addStarImports("pt.lsts.imc", "java.lang.Math", "pt.lsts.neptus.mra.plots");
        imports.addStaticStars("pt.lsts.neptus.mra.plots.ScriptedPlotGroovy");
        cnfg.addCompilationCustomizers(imports);
        shell = new GroovyShell(this.getClass().getClassLoader(), cnfg);
        runScript(scriptPath);
    }

    /**
     * Runs the Groovy script after verifying its validity by parsing it.
     * 
     * @param script Text script
     */
    public void runScript(String path) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
            }
            shell.setVariable("plot_", ScriptedPlot.this);
            String defplot = "configPlot plot_";
            shell.evaluate(defplot);
            // shell.invokeMethod("configPlot", this);
            String script = sb.toString();
            shell.parse(script);
            shell.evaluate(script);
            reader.close();
        }
        catch (Exception e) {
            GuiUtils.errorMessage(mra, "Error Parsing Script", e.getLocalizedMessage());
        }
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        for(String s: traces.values()) {
            String messageType = s.split("\\.")[0];
            if(!index.containsMessagesOfType(messageType))
                return false;
        }
        return true;
    }

    /**
     * Adds a new time series to the existing plot. If the series already exists, it updates it.
     * 
     * @param ts the TimeSeries to be added
     */
    public void addTimeSeries(TimeSeries ts) {
        String trace = ts.getKey().toString();

        if (forbiddenSeries.contains(trace))
            return;

        if (!super.series.containsKey(trace)) {
            addTrace(trace);
        }
        for (int i = 0; i < ts.getItemCount(); i++) {
            TimeSeriesDataItem value = ts.getDataItem(i);
            super.series.get(trace).addOrUpdate(value);
        }
    }

    public void addTimeSeries(String id, String query) {
        traces.put(id, query);
    }

    public TimeSeries getTimeSeriesFor(String id) {
        if (!processed) {
            process(index);
            processed = true;
        }
        System.err.println("Getting TimeSeries for "+id);
        return series.get(id);
    }
    
    public void addQuery(String id,String query) {
        traces.put(id,query);
        super.forbiddenSeries.addElement(id);
    }

    public void title(String t) {
        title = t;
    }

    @Override
    public void process(LsfIndex source) {
        if (!processed) {
            for (Entry<String, String> entry : traces.entrySet()) {
                String messageName, entity = null, variable = null;
                messageName = entry.getValue().split("\\.")[0];
                if (entry.getValue().split("\\.").length == 2)
                    variable = entry.getValue().split("\\.")[1];
                else if (entry.getValue().split("\\.").length == 3) {
                    entity = entry.getValue().split("\\.")[1];
                    variable = entry.getValue().split("\\.")[2];
                }
                for (IMCMessage m : source.getIterator(messageName, 0, (long) (timestep * 1000))) {
                    String seriesName = m.getSourceName() + "." + entry.getKey();
                    if (entity != null) {
                        if (m.getEntityName().equals(entity)) {
                            double val = m.getDouble(variable);
                            addValue(m.getTimestampMillis(), seriesName, val);
                        }
                    }
                    else {
                        double val = m.getDouble(variable);
                        addValue(m.getTimestampMillis(), seriesName, val);
                    }
                }
            }
        }
        processed = true;
    }
    
    public void mark(double time, String label) {
        mraPanel.addMarker(new LogMarker(label, time * 1000, 0, 0));
    }
}
