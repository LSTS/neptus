/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.console.plugins;

import pt.up.fe.dceg.neptus.console.SubPanel;

public class SubPanelChangeEvent {
    public enum SubPanelChangeAction {
        REMOVED,
        ADDED
    }

    private SubPanel panel;
    private SubPanelChangeAction action;

    public SubPanelChangeEvent(SubPanel p) {
        panel = p;
        action = SubPanelChangeAction.ADDED;
    }

    public SubPanelChangeEvent(SubPanel p, SubPanelChangeAction a) {
        panel = p;
        action = a;
    }

    public SubPanel getPanel() {
        return panel;
    }

    public void setPanel(SubPanel panel) {
        this.panel = panel;
    }

    public SubPanelChangeAction getAction() {
        return action;
    }

    public void setAction(SubPanelChangeAction action) {
        this.action = action;
    }

    public boolean removed() {
        return action.equals(SubPanelChangeAction.REMOVED);
    }

    public boolean added() {
        return action.equals(SubPanelChangeAction.ADDED);
    }
}
