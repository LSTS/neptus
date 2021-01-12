/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 * 
 */
public class ScriptedPlot extends MRATimeSeriesPlot {

    protected LinkedHashMap<String, String> traces = new LinkedHashMap<>();
    protected LinkedHashMap<String, String> hiddenTraces = new LinkedHashMap<>();
    protected LinkedHashMap<String, Script> scripts = new LinkedHashMap<>();

    protected String init = null, end = null;
    protected Script initScript = null, endScript = null;

    protected ScriptableIndex scIndex = null;
    protected Context context;
    protected Global global;
    protected LsfIndex index;
    protected String title = getClass().getName();
    protected ScriptEnvironment env = new ScriptEnvironment();


    @Override
    public String getName() {
        return I18n.text(title);
    }

    @Override
    public String getTitle() {
        return I18n.textf("%plotname plot", title);
    }
    public ScriptedPlot(MRAPanel panel, String scriptFile) {
        super(panel);

        try {
            BufferedReader reader = new BufferedReader(new FileReader(scriptFile));
            title = reader.readLine();
            String line;
            while((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#"))
                    continue;

                while (line.endsWith("\\")) {
                    line = line.substring(0, line.length()-1) + reader.readLine();
                }

                String parts[] = line.trim().split(":");

                String script = parts[1];

                script = script.replaceAll("\\$\\{([^\\}]*)\\}", "log.val(\"$1\")");
                script = script.replaceAll("mark\\(([^\\)]+)\\)", "log.mark($1)");
                script = script.replaceAll("\\$time", "log.time()");
                script = script.replaceAll("\\$(\\w+)", "env[\"$1\"]");

                if (parts[0].isEmpty())
                    hiddenTraces.put(parts[1], script);
                else if (parts[0].equals("init"))
                    init = script;
                else if (parts[0].equals("end"))
                    end = script;
                else
                    traces.put(parts[0], script);                
            }

            reader.close();
        }
        catch (Exception e) {
            GuiUtils.errorMessage(panel, e);
        }       
    }

    protected void init() {

        context = Context.enter();
        context.initStandardObjects();
        global = new Global(context);
        Object o = Context.javaToJS(scIndex, global);
        ScriptableObject.putProperty(global, "log", o);
        ScriptableObject.putProperty(global, "env", env);
        ScriptableObject.putProperty(global, "mraPanel", mraPanel);

        if (init != null) {
            try {
                initScript = context.compileString(init, "init", 1, null);
            }
            catch (Exception e) {
                GuiUtils.errorMessage(mraPanel, "Init script Error", e.getMessage());
                e.printStackTrace();
            }
            NeptusLog.pub().debug("init");
        }

        if (end != null) {
            try {
                endScript = context.compileString(end, "end", 1, null);
            }
            catch (Exception e) {
                GuiUtils.errorMessage(mraPanel, "End script Error", e.getMessage());
                e.printStackTrace();
            }
            NeptusLog.pub().debug("ended.");
        }

        for (Entry<String, String> t : traces.entrySet()) {
            String script = t.getValue();
            try {
                context.evaluateString(global, script, t.getKey(), 1, null);
                scripts.put(t.getKey(), context.compileString(script, t.getKey(), 1, null));
            }
            catch (Exception e) {
                GuiUtils.errorMessage(mraPanel, "Plot script Error", e.getMessage());
                e.printStackTrace();
            }
        }

        for (Entry<String, String> t : hiddenTraces.entrySet()) {
            String trace = t.getValue();
            try {
                context.evaluateString(global, trace, t.getKey(), 1, null);
                scripts.put(t.getKey(), context.compileString(trace, t.getKey(), 1, null));
            }
            catch (Exception e) {
                GuiUtils.errorMessage(mraPanel, "Script Error", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        this.index = index;
        return true;
    }

    @Override
    public void process(LsfIndex source) {
        this.scIndex = new ScriptableIndex(source, 0);
        this.env = new ScriptEnvironment();
        this.index = source;
        init();

        if (initScript != null)
            initScript.exec(context, global);        

        double step = Math.max(timestep, 0.05);
        for (int i = index.advanceToTime(0, step); i != -1; 
                i = index.advanceToTime(i, index.timeOf(i) + step)) {

            scIndex.lsfPos = i;
            for (String trace : scripts.keySet()) {
                if (traces.containsKey(trace)) {
                    Object ret = scripts.get(trace).exec(context, global);
                    if (ret != null && ret instanceof Number) {
                        double val = ((Number)ret).doubleValue();
                        addValue((long)(index.timeOf(i)*1000), trace, val);
                    }
                }
                else {
                    scripts.get(trace).exec(context, global);
                }
            }            
        }

        if (endScript != null)
            endScript.exec(context, global);

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
