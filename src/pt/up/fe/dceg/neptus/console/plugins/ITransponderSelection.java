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
 * Jun 28, 2011
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.util.Collection;

import pt.up.fe.dceg.neptus.types.map.TransponderElement;

/**
 * This interface is provided by any components that allow the user to make a transponders selection
 * @author pdias
 *
 */
public interface ITransponderSelection {

    /**
     * Retrieve a list of transponders currently selected
     * @return list of selected transponders
     */
    public Collection<TransponderElement> getSelectedTransponders();
}
