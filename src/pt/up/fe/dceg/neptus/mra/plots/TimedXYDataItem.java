/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Dec 3, 2012
 * $Id:: TimedXYDataItem.java 9615 2012-12-30 23:08:28Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.mra.plots;

import org.jfree.data.xy.XYDataItem;

/**
 * @author jqcorreia
 *
 */
public class TimedXYDataItem extends XYDataItem {
    public long timestamp;
    public String label;
    
    public TimedXYDataItem(double x, double y, long timestamp, String label) {
        super(x, y);
        this.timestamp = timestamp;
        this.label = label;
    }

    private static final long serialVersionUID = 1L;
    
}
