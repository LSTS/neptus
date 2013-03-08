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
 * Oct 30, 2011
 * $Id:: MraCsvExporter.java 9795 2013-01-29 15:57:08Z jqcorreia                $:
 */
package pt.up.fe.dceg.neptus.plugins.odss;

import java.text.DecimalFormat;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.importers.ImcLogUtils;
import pt.up.fe.dceg.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name="CSV Data", author="ZP", icon="pt/up/fe/dceg/neptus/plugins/odss/fileshare.png")
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
        return (source.getLog("EstimatedState") != null && ( 
                source.getLog("Conductivity") != null ||
                source.getLog("Temperature") != null ||
                source.getLog("Salinity") != null)
                //FIXME 
//                &&
//                ImcLogUtils.getEntityListReverse(source).get("CTD") != null
                );
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
        
        csvEditor.setText("time,latitude,longitude,conductivity,temperature,pressure,salinity\n");

        final IMraLog estimatedState = source.getLog("EstimatedState");
        final IMraLog[] logs = new IMraLog[] {
                source.getLog("Conductivity"),
                source.getLog("Temperature"),
                source.getLog("Pressure"),
                source.getLog("Salinity")
        };
        final int ctdId = ImcLogUtils.getEntityListReverse(source).get("CTD");
        final long firstTime = (estimatedState.currentTimeMillis()/1000)*1000;
        final long start = estimatedState.nextLogEntry().getHeader().getLong("timestamp");

        loadingThread = new Thread() {

            public void run() {
                long curTime = firstTime;
                DecimalFormat latFormat = new DecimalFormat("0.000000");
                DecimalFormat valFormat = new DecimalFormat("0.000");

                while(true) {
                    String line = ""+(start+(curTime/1000));
                    long nextTime = curTime + timestep;
                    IMCMessage msgEstimatedState = estimatedState.getEntryAtOrAfter(curTime);
                    if (msgEstimatedState == null)
                        break;
                    LocationType loc = IMCUtils.getLocation(msgEstimatedState);
                    loc.convertToAbsoluteLatLonDepth();

                    line +=","+latFormat.format(loc.getLatitudeAsDoubleValue());
                    line +=","+latFormat.format(loc.getLongitudeAsDoubleValue());
                    //line +=","+valFormat.format(loc.getDepth());

                    for (IMraLog log : logs) {
                        double sum = 0;
                        int count = 0;
                        
                        // In case some log object doesnt exist continue to next log
                        if(log == null)
                            continue;
                        
                        IMCMessage m = log.getEntryAtOrAfter(curTime);
                        
                        while (log.currentTimeMillis() < nextTime) {
                            if (m.getHeader().getInteger("src_ent") == ctdId) {
                                count ++;
                                sum += m.getDouble("value");                                
                            }
                            m = log.nextLogEntry();

                            if(m == null) // End of log break condition
                                break;
                        }
                        
                        if (sum > 0)
                            line += ","+valFormat.format(sum/count);
                        else
                            line += ",-1";
                    }
                    csvEditor.setText(csvEditor.getText()+line+"\n");
                    curTime = nextTime;
                }               
            };
        };

        loadingThread.start();
        return loadingThread;
    }
    
    public Type getType() {
        return Type.VISUALIZATION;
    }
}
