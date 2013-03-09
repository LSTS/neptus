/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Feb 14, 2013
 */
package pt.up.fe.dceg.neptus.plugins.rtplot;

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

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventMainSystemChange;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mra.plots.ScriptEnvironment;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.Popup.POSITION;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 *
 */
@PluginDescription(name="Real-Time plot")
@Popup(accelerator='U',pos=POSITION.CENTER,height=300,width=300)
public class RealTimePlot extends SimpleSubPanel implements IPeriodicUpdates, ConfigurationListener {

    private static final long serialVersionUID = 1L;
    private JFreeChart timeSeriesChart = null;
    private TimeSeriesCollection tsc = new TimeSeriesCollection();
    private JButton btnEdit, btnClear;
    private LinkedHashMap<String, Script> scripts = new LinkedHashMap<>();
    private Context context;
    private Global global;
    private ScriptEnvironment env = new ScriptEnvironment();
    private JPanel bottom;
    
    @NeptusProperty(name="Periodicity millis")
    public int periodicity = 1000;
    
    @NeptusProperty(name="Maximum Number of points")
    public int numPoints = 100;
    
    @NeptusProperty(name="Traces Script")
    public String traceScripts = "roll: ${EstimatedState.psi} * 180 / Math.PI;\npitch: ${EstimatedState.theta} * 180 / Math.PI";
    
    private String traceScriptsBefore = "";
    
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
        global = new Global(context);
    }
    
    @Override
    public long millisBetweenUpdates() {
        return periodicity;
    }
    
    @Override
    public boolean update() {
        if (!isShowing()) 
            return true;
        Context.enter();
        Collection<String> traces = scripts.keySet();
        
        for (String s : traces) {
            Object o = scripts.get(s).exec(context, global);
            if (o instanceof NativeJavaObject) {
                o = ((NativeJavaObject)o).unwrap();
            }
            TimeSeries ts = tsc.getSeries(s);
            if (ts == null) {
                ts = new TimeSeries(s);
                ts.setMaximumItemCount(numPoints);
                tsc.addSeries(ts);
            }
            ts.addOrUpdate(new Millisecond(new Date(System.currentTimeMillis())), Double.parseDouble(o.toString()));
        }
        Context.exit();
        
        return true;
    }
    
    @Override
    public void propertiesChanged() {
        if (!traceScripts.equals(traceScriptsBefore)) {
            tsc.removeAllSeries();
            scripts.clear();
            
            try {
                parseScript();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            
//            removeAll();
//            
//            timeSeriesChart = ChartFactory.createTimeSeriesChart(null, null, null, tsc, true, true, true);
//            add (new ChartPanel(timeSeriesChart), BorderLayout.CENTER);
//            add (bottom, BorderLayout.SOUTH);
//            doLayout();
//            invalidate();
//            revalidate();
                       
        }
               
        traceScriptsBefore = traceScripts;
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
    
    @Subscribe
    public void on(ConsoleEventMainSystemChange sysChange) {
        tsc.removeAllSeries();
        Context.enter();
        Object o = Context.javaToJS(ImcMsgManager.getManager().getState(getMainVehicleId()), global);
        ScriptableObject.putProperty(global, "state", o);
        Context.exit();
        repaint();        
    }

    @Override
    public void initSubPanel() {
        traceScriptsBefore = traceScripts;
        Object o = Context.javaToJS(ImcMsgManager.getManager().getState(getMainVehicleId()), global);
        ScriptableObject.putProperty(global, "state", o);
        ScriptableObject.putProperty(global, "env", env);
        
        propertiesChanged();    
    }

    @Override
    public void cleanSubPanel() {
        // nothing
    }
}
