/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * Jun 15, 2015
 */
package pt.lsts.neptus.plugins.urready4os.vtk.window;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.vtk.events.AEventsHandler;

/**
 * @author pdias
 * 
 */
public class EventsHandlerRhod3D extends AEventsHandler {

    /**
     * @param interactorStyle
     */
    public EventsHandlerRhod3D(InteractorStyleRhod3D interactorStyle, IMraLogGroup source) {
        super(interactorStyle, source);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.vtk.events.AEventsHandler#init()
     */
    @Override
    protected void init() {

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.vtk.events.AEventsHandler#setHelpMsg()
     */
    @Override
    protected void setHelpMsg() {
        msgHelp = "<html><font size='2'><br><div align='center'><table border='1' align='center'>"
                + "<tr><th>Keys</th><th>"
                + I18n.text("Description")
                + "</th></tr>"
                + "<tr><td>r, R</td><td>"
                + I18n.text("Reset camera view along the current view direction")
                + "</td>"
                + "<tr><td>f, F</td><td>"
                + I18n.text("Fly Mode - point with mouse cursor the direction and press 'f' to fly")
                + "</td>"
                + "<tr><th>Mouse</th><th>"
                + I18n.text("Description")
                + "</th></tr>"
                + "<tr><td>"
                + I18n.text("Left mouse button")
                + "</td><td>"
                + I18n.text("Rotate camera around its focal point")
                + "</td>"
                + "<tr><td>"
                + I18n.text("Middle mouse button")
                + "</td><td>"
                + I18n.text("Pan camera")
                + "</td>"
                + "<tr><td>"
                + I18n.text("Right mouse button")
                + "</td><td>"
                + I18n.text("Zoom (In/Out) the camera")
                + "</td>"
                + "<tr><td>"
                + I18n.text("Mouse wheel")
                + "</td><td>"
                + I18n.text("Zoom (In/Out) the camera - Static focal point") + "</td>";
    }
}
