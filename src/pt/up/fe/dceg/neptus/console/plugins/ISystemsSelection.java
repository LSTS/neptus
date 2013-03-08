/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 4/12/2011
 * $Id:: ISystemsSelection.java 9615 2012-12-30 23:08:28Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.util.Collection;

/**
 * @author pdias
 *
 */
public interface ISystemsSelection {

    /**
     * Returns a list of selected system ids
     * @return The ids of the selected systems
     */
    public abstract Collection<String> getSelectedSystems(boolean clearSelection);

    /**
     * Returns a list of selected vehicles. 
     * Even if different types of systems are selected only the vehicles will be returned
     * @return The list of selected vehicles (possibly empty)
     */
    public abstract Collection<String> getSelectedVehicles(boolean clearSelection);

    public abstract Collection<String> getAvailableSelectedSystems();

    public abstract Collection<String> getAvailableVehicles();
}