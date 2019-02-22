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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesDataItem;

import groovy.lang.GroovyShell;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.util.GuiUtils;

/**
 * Changes to use Groovy on Feb 2019
 * @author keila
 * 
 */
public class ScriptedPlot extends MRATimeSeriesPlot {

    protected LinkedHashMap<String, String> traces = new LinkedHashMap<>();
    protected LinkedHashMap<String, String> hiddenTraces = new LinkedHashMap<>();

    protected ScriptableIndex scIndex = null;
    protected LsfIndex index;
    
    private GroovyShell shell;
    private final String scriptPath;
    private MRAPanel mra;
    private String title = null;

    @Override
    public String getName() {
        if(title == null)
            return  I18n.text(Arrays.toString(traces.keySet().toArray()));
        else 
            return I18n.text(title);
    }

    @Override
    public String getTitle() {
        
        if(title == null)
            return  I18n.text(Arrays.toString(traces.keySet().toArray()));
        else 
            return I18n.text(title);
    }
    
    public ScriptedPlot(MRAPanel panel, String path) {
        super(panel);
        scriptPath = path;
        index = panel.getSource().getLsfIndex();
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

    @Override
    public boolean canBeApplied(LsfIndex index) {
        this.index = index;
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
    
    public void addQuery(String id, String field) {
        traces.put(id, field);
    }
    public void title(String t) {
        title = t;
    }


    @Override
    public void process(LsfIndex source) {
        this.scIndex = new ScriptableIndex(source, 0);
        this.index = source;

        double step = Math.max(timestep, 0.05);
        for (int i = index.advanceToTime(0, step); i != -1; 
                i = index.advanceToTime(i, index.timeOf(i) + step)) {

            scIndex.lsfPos = i;
            for (Entry<String, String> entry : traces.entrySet()) {
                String seriesName = index.getMessage(i).getSourceName()+"."+entry.getKey();
                if (traces.containsKey(entry.getKey())) {
                    double value = scIndex.val(entry.getValue());
                    if (value != Double.NaN) {
                        addValue((long)(index.timeOf(i)*1000), seriesName, value);
                    }
                }
//                else {
//                    scripts.get(trace).exec(context, global);
//                }
            }            
        }

    }

    /**
     * This internal class allows plot scripts to access the current log time and message fields in the log
     * 
     * @author zp
     */
    public class ScriptableIndex {

        protected LsfIndex lsfIndex;
        protected int lsfPos;
        protected int prevPos = 0;

        /**
         * Class constructor is passed the LsfIndex and an initial index (usually 0)
         * 
         * @param source The index to be used by scripts
         * @param curIndex The position in the index
         */
        public ScriptableIndex(LsfIndex source, int curIndex) {
            this.lsfIndex = source;
            this.lsfPos = curIndex;            
        }

        public void mark(double time, String label) {
            mraPanel.addMarker(new LogMarker(label, time * 1000,0,0));
        }

        public void mark(String label) {
            mark(lsfIndex.timeOf(lsfPos), label);
        }

        /**
         * This method returns the current log time
         * 
         * @return current log time
         */
        public double time() {
            return lsfIndex.timeOf(lsfPos);
        }

        /**
         * This method evaluates a field expression (like "EstimatedState[Navigation].x") and returns its current value
         * in the log
         * 
         * @param expression The expression to be valuated
         * @return The value (double) or Double.NaN if the expression is invalid or the log does not contain the
         *         required fields at current time
         */
        public double val(String expression) {

            Pattern p = Pattern.compile("(\\w+)(\\[(\\w+)\\])?\\.(\\w+)");
            Matcher m = p.matcher(expression);

            if (!m.matches()) {
                return Double.NaN;
            }
            String message, entity, field;
            message = m.group(1);
            if (m.groupCount() > 2) {
                entity = m.group(3);
                field = m.group(4);
            }
            else {
                entity = null;
                field = m.group(2);
            }

            int msgType = index.getDefinitions().getMessageId(message);

            if (entity == null) {
                int msgIdx = index.getPreviousMessageOfType(msgType, lsfPos);
                if (msgIdx == -1)
                    return Double.NaN;
                else
                    return index.getMessage(msgIdx).getDouble(field);
            }
            else {
                int msgIdx = index.getPreviousMessageOfType(msgType, lsfPos);
                while (msgIdx >= prevPos) {

                    if (msgIdx == -1)
                        return Double.NaN;
                    else if (index.entityNameOf(msgIdx).equals(entity)) {
                        prevPos = msgIdx;
                        return index.getMessage(msgIdx).getDouble(field);
                    }
                    else {
                        msgIdx = index.getPreviousMessageOfType(msgType, msgIdx);
                    }
                }
            }
            return Double.NaN;
        }
    }
}
