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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.Popup.POSITION;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

/**
 * This panel is reponsible for controlling the Leds brightness on Adamastor
 * @author hfq
 *
 * FIX ME - Change icon
 * Use:
 * IMC::SetLedBrightness
 * IMC::QueryLedBrightness
 * 
 * SetLedBrightness extends IMCMessage
 * QueryLedBrightness extends IMCMessage
 * 
 */
@Popup(pos = POSITION.TOP_LEFT, width = 200, height = 300, accelerator = 'D')
@PluginDescription(author = "hfq", description = "Panel that enables setting up leds brightness", name = "Leds Control Panel", version = "0.1", icon = "images/menus/tip.png")
public class LedsControlPanel extends SimpleSubPanel implements ActionListener, WindowListener {
    private static final long serialVersionUID = 1L;
    
    private ConsoleLayout console;
    
    //private JSlider slider;
    
    private LedsSlider slider1, slider2, slider3, slider4;
    
    static final int LED_MIN_BRIGHTNESS = 0;
    static final int LED_MAX_BRIGHTNESS = 100;
    static final int LED_INIT_BRIGHTNESS = 0;
    
    // Can have a timer to turn on the 4 groups of leds (3 leds per group) in a clockwise matter
    //Timer time;

    /**
     * @param console
     */
    public LedsControlPanel(ConsoleLayout console) {
        super(console);
        this.console = console;
        this.setLayout(new MigLayout("insets 0"));
        setSize(500, 600);
        this.removeAll();
        
        //console.addMainVehicleListener(this);
        ImcMsgManager.getManager().addListener(this);
        
        slider1 = new LedsSlider("Leds 1 ");
        slider2 = new LedsSlider("Leds 2 ");
        slider3 = new LedsSlider("Leds 3 ");
        slider4 = new LedsSlider("Leds 4 ");
        this.add(slider1, "wrap");
        this.add(slider2, "wrap");
        this.add(slider3, "wrap");
        this.add(slider4, "wrap");
        
        //SetLedBrightness bright = new SetLedBrightness();
        //QueryLedBrightness queryBright = new QueryLedBrightness();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        GuiUtils.testFrame(new LedsControlPanel(null));

    }

    @Override
    public void initSubPanel() {}

    @Override
    public void cleanSubPanel() {}

    @Override
    public void windowOpened(WindowEvent e) {}

    @Override
    public void windowClosing(WindowEvent e) {}

    @Override
    public void windowClosed(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {}

    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowActivated(WindowEvent e) {}

    @Override
    public void windowDeactivated(WindowEvent e) {}

    @Override
    public void actionPerformed(ActionEvent e) {}

}
