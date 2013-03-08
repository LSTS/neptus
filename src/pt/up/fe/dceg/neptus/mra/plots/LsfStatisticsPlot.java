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
 * Jan 29, 2013
 * $Id:: LsfStatisticsPlot.java 9803 2013-01-30 03:32:17Z robot                 $:
 */
package pt.up.fe.dceg.neptus.mra.plots;

import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mra.MRAPanel;

/**
 * @author zp
 *
 */
public class LsfStatisticsPlot extends PiePlot {

    public LsfStatisticsPlot(MRAPanel panel) {
        super(panel);
    }
    
    @Override
    public boolean canBeApplied(LsfIndex index) {
        return true;
    }

    @Override
    public void process(LsfIndex source) {
        for (int i = 0; i < source.getNumberOfMessages(); i++) {
            addValue(source.entityNameOf(i), source.sizeOf(i));
        }
    }

}
