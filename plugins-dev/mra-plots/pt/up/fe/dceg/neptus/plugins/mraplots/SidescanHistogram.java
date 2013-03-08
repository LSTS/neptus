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
 * Dec 16, 2012
 * $Id:: SidescanHistogram.java 9615 2012-12-30 23:08:28Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.plugins.mraplots;

import java.awt.BorderLayout;

import javax.swing.JComponent;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;


/**
 * @author zp
 *
 */
@PluginDescription(name="Sidescan Histogram")
public class SidescanHistogram extends SimpleMRAVisualization {

    private static final long serialVersionUID = 1L;
    protected Histogram histogram = new Histogram();
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLog("SonarData") != null || source.getLog("SidescanPing") != null;
    }
    
    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }
    
    @Override
    public JComponent getVisualization(final IMraLogGroup source, double timestep) {
        setLayout(new BorderLayout());
        add(histogram);
        
        Thread t = new Thread( new Runnable() {
            
            @Override
            public void run() {
                double sums[] = null;
                int count = 0;
                String msg = "SonarData";
                if (source.getLog("SonarData") == null)
                    msg = "SidescanPing";
                for (IMCMessage ping : source.getLsfIndex().getIterator(msg)) {
                    byte[] data = ping.getRawData("data");
                    if (sums == null)
                        sums = new double[data.length];
                    
                    for (int i = 0; i < sums.length; i++)
                        sums[i] += data[i] & 0xFF;
                    
                    count++;
                    
                    if (count % 100 == 0) {
                        double[] tmp = new double[sums.length];
                        for (int i = 0; i < sums.length; i++)
                            tmp[i] = sums[i] / count;
                        histogram.setData(tmp, 0, 255);
                    }
                }
                
                for (int i = 0; i < sums.length; i++)
                    sums[i] = sums[i] / count; 
                
                histogram.setData(sums, 0, 255);
            }
        });
        t.setDaemon(true);
        t.start();
        return this;
    }
    
    public SidescanHistogram(MRAPanel panel) {
        super(panel);
    }
    
}
