/*
 * Copyright (c) 2004-2019 Universidade do Porto - Faculdade de Engenharia
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
 * Author: keila
 * Feb 8, 2019
 */
package pt.lsts.neptus.mra.plots;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesDataItem;

import groovy.lang.GroovyShell;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author keila
 *
 */
public class ScriptedPlot extends MRATimeSeriesPlot {

    private GroovyShell shell;
    private LsfIndex index;
    private final String scriptPath;
    private Map<String, TimeSeries> values = new LinkedHashMap<>();
    private Map<String, String> seriesIndex = new HashMap<>();
    private MRAPanel mra;
    private String title = null;

    /**
     * @param panel
     */
    public ScriptedPlot(MRAPanel panel, String path) {
        super(panel);
        mra = panel;
        scriptPath = path;
        index = mra.getSource().getLsfIndex();
        String[] fields = new String[1];
        fields[0] = "EstimatedState.depth";

        // init shell
        CompilerConfiguration cnfg = new CompilerConfiguration();
        ImportCustomizer imports = new ImportCustomizer();
        imports.addStarImports("pt.lsts.imc", "java.lang.Math", "pt.lsts.neptus.mra.plots");
        imports.addStaticStars("pt.lsts.neptus.mra.plots.ScriptedPlotGroovy");
        cnfg.addCompilationCustomizers(imports);
        shell = new GroovyShell(this.getClass().getClassLoader(), cnfg);
        runScript(scriptPath);
    }

    public String[] msgField(String expr) {
        String msg, field, entity;
        String[] fields = expr.split(".");
        if (fields.length == 2) {
            msg = fields[0];
            field = fields[1];
            entity = null;
        }
        else if (fields.length == 3) {
            msg = fields[0];
            entity = fields[1];
            field = fields[2];
        }
        return fields;
    }

    public void title(String t) {
        title = t;
    }

    public List<TimeSeries> getDataFromExpr(String expr) {
        List<TimeSeries> result = new ArrayList<>();

        if (tsc.getSeries(expr) == null) {
            GenericPlot p = new GenericPlot(msgField(expr), mra);// TODO Optimize
            p.process(index);
            for (TimeSeries t : p.series.values()) {
                result.add(t);
                tsc.addSeries(t);
            }
        }

        return result;
    }
    
    @Override
    public String getName() {
        if(title == null)
            return  Arrays.toString(seriesIndex.keySet().toArray());
        else 
            return title;
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
            shell.setVariable("plot", ScriptedPlot.this);
            String defplot = "configPlot plot";
            boolean b = shell.getVariable("plot") == null;
            System.err.println("plot var equals null? "+b);
            shell.evaluate(defplot); 
            //shell.invokeMethod("configPlot", this);
            String script = sb.toString();
            shell.parse(script);
            shell.evaluate(script);
            reader.close();
        }
        catch (Exception e) {
            GuiUtils.errorMessage(mra, "Error Parsing Script", e.getLocalizedMessage());
        }
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
        seriesIndex.put(id, query);
    }

    public void mark(double time, String label) {
        mra.addMarker(new LogMarker(label, time * 1000, 0, 0));
    }

    public void addQuery(String id, String field) {
        seriesIndex.put(id, field);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.plots.MRATimeSeriesPlot#canBeApplied(pt.lsts.imc.lsf.LsfIndex)
     */
    @Override
    public boolean canBeApplied(LsfIndex index) {
        for (Entry<String, String> entry : seriesIndex.entrySet()) {
            String field = entry.getValue();
            String messageName = field.split("\\.")[0];
            if (!index.containsMessagesOfType(messageName))
                return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.plots.MRATimeSeriesPlot#process(pt.lsts.imc.lsf.LsfIndex) adapted from @GenericPlot
     */
    @Override
    public void process(LsfIndex source) {
        for (Entry<String, String> entry : seriesIndex.entrySet()) {
            String field, entity;
            String id = entry.getKey();
            field = entry.getValue();
            String messageName = field.split("\\.")[0];
            String variable = field.split("\\.")[1];
            System.err.println("Variable " + variable);
            for (IMCMessage m : source.getIterator(messageName, 0, (long) (timestep * 1000))) {

                String seriesName = "";
                if (id.equals(field)) {
                    if (m.getValue("id") != null) {
                        seriesName = m.getSourceName() + "." + source.getEntityName(m.getSrc(), m.getSrcEnt()) + "."
                                + field + "." + m.getValue("id");
                    }
                    else {
                        seriesName = m.getSourceName() + "." + source.getEntityName(m.getSrc(), m.getSrcEnt()) + "."
                                + field;
                    }
                }
                else
                    seriesName = m.getSourceName() + "." + id;
                if (m.getMessageType().getFieldUnits(variable) != null
                        && m.getMessageType().getFieldUnits(variable).startsWith("rad")) {
                    // Special case for angles in radians
                    addValue(m.getTimestampMillis(), seriesName, Math.toDegrees(m.getDouble(variable)));
                }
                else
                    addValue(m.getTimestampMillis(), seriesName, m.getDouble(variable));
            }
        }
    }
}
