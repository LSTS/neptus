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
 * Dec 16, 2012
 */
package pt.lsts.neptus.plugins.mraplots;

import java.awt.BorderLayout;

import javax.swing.JComponent;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;


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
