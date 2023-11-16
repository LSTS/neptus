/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Paulo Dias
 * 8/11/2023
 */
package pt.lsts.neptus.plugins.remoteactionsextra;

import pt.lsts.imc.EntityState;
import pt.lsts.imc.VehicleState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;

import javax.swing.JButton;
import java.awt.Color;

class TakeControlMonitor {
    private static final Color COLOR_IN_CONTROL = new Color(144, 255, 0, 128);
    private static final Color COLOR_ASKED_CONTROL = new Color(255, 221, 0, 128);
    private static final Color COLOR_ERR_CONTROL = new Color(255, 0, 0, 128);
    private static final Color COLOR_NO_INFO_CONTROL = null;

    private final RemoteActionsExtra parent;
    private String entityName = "OBS Broker";
    private JButton button;
    private  EntityState.STATE entState = null;
    private VehicleState.OP_MODE opMode = null;

    public TakeControlMonitor(RemoteActionsExtra parent, JButton button) {
        this.parent = parent;
        this.button = button;
    }

    public TakeControlMonitor(RemoteActionsExtra parent) {
        this.parent = parent;
        this.button = null;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        if (!entityName.equalsIgnoreCase(this.entityName)) {
            reset();
        }

        this.entityName = entityName;
    }

    private void reset() {
        entState = null;
        opMode = null;
        resetControl();
    }

    public JButton getButton() {
        return button;
    }

    public void setButton(JButton button) {
        this.button = button;
    }

    public void on(ConsoleEventMainSystemChange evt) {
        reset();
    }

    public void on(EntityState msg) {
        try {
            if (!msg.getSourceName().equals(parent.getMainVehicleId())
                    || !msg.getEntityName().equals(entityName)) {
                return;
            }
        } catch (Exception e) {
            NeptusLog.pub().warn(e.getMessage());
            return;
        }

        entState = msg.getState();
        updateState();
    }

    public void on(VehicleState msg) {
        if (!msg.getSourceName().equals(parent.getMainVehicleId())) {
            return;
        }

        opMode = msg.getOpMode();
        updateState();
    }

    private void updateState() {
        if (button == null) return;
        if (entState == null) return;
        if (opMode == null) return;

        boolean isExternal = false;
        switch (opMode) {
            case EXTERNAL:
                isExternal = true;
                break;
            case BOOT:
            case ERROR:
            case MANEUVER:
            case CALIBRATION:
            case SERVICE:
            default:
                break;
        }

        switch (entState) {
            case NORMAL:
                if (isExternal) {
                    resetControl();
                } else {
                    inControl();
                }
                break;
            case ERROR:
            case BOOT:
            case FAULT:
            case FAILURE:
            default:
                noControl();
                break;
        }
    }

    void askedControl() {
        changeColor(COLOR_ASKED_CONTROL);
    }

    private void inControl() {
        changeColor(COLOR_IN_CONTROL);
    }

    private void noControl() {
        changeColor(COLOR_ERR_CONTROL);
    }

    private void resetControl() {
        changeColor(COLOR_NO_INFO_CONTROL);
    }

    private void changeColor(Color color) {
        if (button == null) return;

        button.setBackground(color);
        button.revalidate();
        button.repaint(100);
    }
}
