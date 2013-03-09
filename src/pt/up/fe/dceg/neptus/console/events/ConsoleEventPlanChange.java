/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Hugo
 * Nov 21, 2012
 */
package pt.up.fe.dceg.neptus.console.events;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

/**
 * Console Event for changes to the active plan <br>
 * This triggers when {@link ConsoleLayout#setPlan(PlanType)} is called
 * 
 * @author Hugo
 * 
 */
public class ConsoleEventPlanChange {
    private final PlanType old;
    private final PlanType current;

    /**
     * Console Event for changes to the active plan <br>
     * This triggers when {@link ConsoleLayout#setPlan(PlanType)} is called
     * 
     */
    public ConsoleEventPlanChange(PlanType old, PlanType current) {
        this.old = old;
        this.current = current;
    }

    /**
     * @return the old
     */
    public PlanType getOld() {
        return old;
    }

    /**
     * @return the current
     */
    public PlanType getCurrent() {
        return current;
    }
}
