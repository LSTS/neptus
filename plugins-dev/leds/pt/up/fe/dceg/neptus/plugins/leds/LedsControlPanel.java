/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: hfq
 * Aug 30, 2013
 */
package pt.up.fe.dceg.neptus.plugins.leds;

import javax.swing.JSlider;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.Popup.POSITION;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * This panel is reponsible for controlling the Leds brightness on Adamastor
 * @author hfq
 *
 * FIX ME - Change icon
 */
@Popup(pos = POSITION.TOP_LEFT, width = 400, height = 300, accelerator = 'D')
@PluginDescription(author = "hfq", description = "Panel that enables setting up leds brightness", name = "Leds Control Panel", icon = "images/menus/tip.png")
public class LedsControlPanel extends SimpleSubPanel {
    private static final long serialVersionUID = 1L;
    
    private ConsoleLayout console;
    
    private JSlider slider;

    /**
     * @param console
     */
    public LedsControlPanel(ConsoleLayout console) {
        super(console);
        this.console = console;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        GuiUtils.testFrame(new LedsControlPanel(null));

    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }

}
