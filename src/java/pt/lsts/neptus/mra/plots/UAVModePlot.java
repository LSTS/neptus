/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Braga
 * Oct 20, 2014
 */
package pt.lsts.neptus.mra.plots;

import javax.swing.ImageIcon;

import pt.lsts.imc.AutopilotMode;
import pt.lsts.imc.AutopilotMode.AUTONOMY;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.imc.lsf.LsfIterator;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author Braga
 * 
 */
@PluginDescription(name="UAV Mode", author = "José Braga")
public class UAVModePlot extends PiePlot {

    public UAVModePlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("AutopilotMode");
    }
    
    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getIcon("pt/lsts/neptus/mra/plots/chart-pie.png");
    }

    @Override
    public void process(LsfIndex source) {
        LsfIterator<AutopilotMode> it = source.getIterator(AutopilotMode.class);

        while(it.hasNext()) {
            AutopilotMode u = it.next();
            AUTONOMY autonomyLevel = u.getAutonomy();

            addValue(translate(autonomyLevel), 1);
        };

        cleanupSeries(0.01);
    }

    private String translate(AUTONOMY type) {
        switch (type) {
            case MANUAL:
                return I18n.text("MANUAL");
            case ASSISTED:
                return I18n.text("ASSISTED");
            case AUTO:
                return I18n.text("AUTO");
        }

        return "";
    }
}
