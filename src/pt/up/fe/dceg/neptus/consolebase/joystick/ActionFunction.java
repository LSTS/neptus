/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Rui Gonçalves
 * 2010/05/19
 */
package pt.up.fe.dceg.neptus.consolebase.joystick;

/**
 * @author Rui Goncalves
 * 
 */
public class ActionFunction {

    private String action = null;
    private String function = null;
    private boolean negative = false;

    public ActionFunction(String act, String func, boolean neg) {

        action = act;
        function = func;
        negative = neg;

    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

}
