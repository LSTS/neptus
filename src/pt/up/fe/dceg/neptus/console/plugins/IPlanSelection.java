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
 * $Id:: IPlanSelection.java 9615 2012-12-30 23:08:28Z pdias                    $:
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.util.Vector;

import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

/**
 * This interface is provided by any components that allow the user to make a plan selection
 * @author zp
 *
 */
public interface IPlanSelection {

    /**
     * Retrieve a list of plans currently selected
     * @return list of selected plans
     */
    public Vector<PlanType> getSelectedPlans();
    
    
}
