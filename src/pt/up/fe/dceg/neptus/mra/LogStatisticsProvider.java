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
 * Aug 27, 2011
 */
package pt.up.fe.dceg.neptus.mra;

import java.util.Vector;

import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;

/**
 * @author zp
 *
 */
public interface LogStatisticsProvider {

    public String getTitle();
    public void parseLog(IMraLogGroup source) throws Exception;        
    public Vector<LogStatisticsItem> getStatistics();
}
