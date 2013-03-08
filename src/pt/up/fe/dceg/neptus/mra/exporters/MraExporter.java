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
 * Feb 19, 2013
 * $Id:: MraExporter.java 9956 2013-02-20 03:32:28Z robot                       $:
 */
package pt.up.fe.dceg.neptus.mra.exporters;

import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;

/**
 * @author jqcorreia
 *
 */
public interface MraExporter {
    public boolean canBeApplied(IMraLogGroup source);
    public void process();
    
    /**
     * Return the name that will be shown on Exporters menu 
     * @return the name string
     */
    public String getName();
}
