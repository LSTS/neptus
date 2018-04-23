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
 * Feb 14, 2013
 */
package pt.lsts.neptus.plugins.rtplot;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.plots.ScriptEnvironment;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;

/**
 * @author zp
 *
 */
@PluginDescription(name="Real-Time plot", icon="pt/lsts/neptus/plugins/rtplot/rtplot.png")
@Popup(accelerator='U',pos=POSITION.CENTER,height=300,width=300)
public class RealTimePlot extends ConsolePanel implements IPeriodicUpdates, ConfigurationListener {

    private static final long serialVersionUID = 1L;
    private JFreeChart timeSeriesChart = null;
    private TimeSeriesCollection tsc = new TimeSeriesCollection();
    private JButton btnEdit, btnClear;
    private LinkedHashMap<String, Script> scripts = new LinkedHashMap<>();
    private Context context;

    private LinkedHashMap<String, Global> vehicleContexts = new LinkedHashMap<String, Global>();
    //private Global global;
    private ScriptEnvironment env = new ScriptEnvironment();
    private JPanel bottom;

    @NeptusProperty(name="Periodicity (milliseconds)")
    public int periodicity = 1000;

    @NeptusProperty(name="Maximum Number of points")
    public int numPoints = 100;

    @NeptusProperty(name="Traces Script")
    public String traceScripts = "roll: ${EstimatedState.phi} * 180 / Math.PI;\npitch: ${EstimatedState.theta} * 180 / Math.PI;\nyaw: ${EstimatedState.psi} * 180 / Math.PI";

    private String traceScriptsBefore = "";
    private int numPointsBefore = numPoints;

    public RealTimePlot(ConsoleLayout c) {
        super(c);

        setLayout(new BorderLayout());
        bottom = new JPanel(new GridLayout(1,0));

        btnEdit = new JButton(I18n.text("Settings"));
        bottom.add(btnEdit);
        btnEdit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                RealTimePlotSettings.editSettings(RealTimePlot.this);
            }
        });
        btnClear = new JButton(I18n.text("Clear"));
        bottom.add(btnClear);
        btnClear.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tsc.removeAllSeries();
            }
        });

        add(bottom, BorderLayout.SOUTH);

        timeSeriesChart = ChartFactory.createTimeSeriesChart(null, null, null, tsc, true, true, true);
        add (new ChartPanel(timeSeriesChart), BorderLayout.CENTER);

        context = Context.enter();
        context.initStandardObjects();
        //global = new Global(context);
    }

    @Override
    public long millisBetweenUpdates() {
        return periodicity;
    }

    @Override
    public boolean update() {
        if (!isShowing()) 
            return true;
        Collection<String> traces = scripts.keySet();

        for (ImcSystem sys : ImcSystemsHolder.lookupActiveSystemVehicles()) {
            context = Context.enter();
            context.initStandardObjects();
            if (!vehicleContexts.containsKey(sys.getName())) {
                Global global = new Global(context);
                Object o = Context.javaToJS(ImcMsgManager.getManager().getState(sys.getName()), global);
                ScriptableObject.putProperty(global, "state", o);
                ScriptableObject.putProperty(global, "env", env);
                vehicleContexts.put(sys.getName(), global);
            }

            Global global = vehicleContexts.get(sys.getName());
            for (String s : traces) {
                                try {
                    Object o = scripts.get(s).exec(context, global);
                    if (o instanceof NativeJavaObject) {
                        o = ((NativeJavaObject)o).unwrap();
                    }
                    String seriesName = sys.getName()+"."+s;
                    TimeSeries ts = tsc.getSeries(seriesName);
                    if (ts == null) {
                        ts = new TimeSeries(seriesName);
                        ts.setMaximumItemCount(numPoints);
                        tsc.addSeries(ts);
                    }
                    ts.addOrUpdate(new Millisecond(new Date(System.currentTimeMillis())), Double.parseDouble(o.toString()));
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }                
            }
            Context.exit();
        }
        return true;
    }

    @Override
    public void propertiesChanged() {
        if (!traceScripts.equals(traceScriptsBefore) || numPoints != numPointsBefore) {
            tsc.removeAllSeries();
            scripts.clear();

            try {
                parseScript();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        traceScriptsBefore = traceScripts;
        numPointsBefore = numPoints;
    }

    protected void parseScript() throws Exception {
        Pattern p = Pattern.compile("([\\w ]+):(.*)");

        for (String line : traceScripts.split("\n")) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                String ss = m.group(2);
                ss = ss.replaceAll("\\$\\{([^\\}]*)\\}", "state.expr(\"$1\")");
                String name = m.group(1);
                Context.enter();
                Script sc = context.compileString(ss, name, 1, null);
                Context.exit();
                scripts.put(name, sc);
            }
        }
        tsc.removeAllSeries();
    }

    @Override
    public void initSubPanel() {
        traceScriptsBefore = traceScripts;
        propertiesChanged();    
    }

    @Override
    public void cleanSubPanel() {
        // nothing
    }
}
