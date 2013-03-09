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
 * Feb 5, 2013
 */
package pt.up.fe.dceg.neptus.plugins.sidescan;

import java.util.ArrayList;

/**
 * @author jqcorreia
 *
 */
public interface SidescanParser {
    public long firstPingTimestamp();
    public long lastPingTimestamp();
    public SidescanLine nextSidescanLine(double freq, int lineWidth);
    public SidescanLine getSidescanLineAt(long timestamp, double freq, int lineWidth);
    public ArrayList<SidescanLine> getLinesBetween(long timestamp1, long timestamp2, int lineWidth, int subsystem);
    
    public ArrayList<Integer> getSubsystemList();
}
