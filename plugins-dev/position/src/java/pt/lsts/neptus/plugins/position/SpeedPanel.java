/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * 2009/06/03
 */
package pt.lsts.neptus.plugins.position;

import java.awt.BorderLayout;
import java.awt.Color;
import java.text.DecimalFormat;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;

/**
 * @author zp
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Vehicle Speed", author = "ZP and Paulo Dias", icon = "pt/lsts/neptus/plugins/position/position.png", description = "Shows the Vehicle's speed")
public class SpeedPanel extends ConsolePanel implements IPeriodicUpdates, ConfigurationListener,
        NeptusMessageListener {

    private DisplayPanel displaySpeed;
    private DecimalFormat formatter = new DecimalFormat("0.00");
    private long lastUpdate = 0;

    @NeptusProperty(name = "Update interval", description = "Interval between updates in milliseconds")
    public long millisBetweenUpdates = 100;

    @NeptusProperty(name = "True or Indicated Speed", description = "true for True and false for Indicated Speed")
    public boolean isTrueSpeed = true;

    @NeptusProperty(name = "SI Unit or knots", description = "true for m/s and false for knot")
    public boolean isSIUnit = true;

    @NeptusProperty(name = "Font Size", description = "The font size. Use '0' for automatic.")
    public int fontSize = DisplayPanel.DEFAULT_FONT_SIZE;

    protected final double m_sToKnotConv = 1.94384449244;

    private String partSpeedTxt = " speed ";

//    @Override
//    public String[] variablesToListen() {
//        return new String[] { "EstimatedState.vx", "EstimatedState.vy", "EstimatedState.vz", "EstimatedState.u",
//                "EstimatedState.v", "EstimatedState.w" };
//    }

    public SpeedPanel(ConsoleLayout console) {
        super(console);
        initialize();
    }

    protected void initialize() {
        displaySpeed = new DisplayPanel((isTrueSpeed ? "true" : "ind.") + partSpeedTxt
                + (isSIUnit ? "(m/s)" : "(knot)"));
        displaySpeed.setFontSize(fontSize);
        setLayout(new BorderLayout());
        add(displaySpeed, BorderLayout.CENTER);
    }

    @Override
    public void propertiesChanged() {
        displaySpeed.setFontSize(fontSize);
        invalidate();
        revalidate();
    }

    @Override
    public long millisBetweenUpdates() {
        return 500;
    }

    boolean connected = true;

    @Override
    public boolean update() {

        if (connected && System.currentTimeMillis() - lastUpdate > 3000) {
            displaySpeed.setFontColor(Color.red.darker());
            connected = false;
        }

        if (!connected && System.currentTimeMillis() - lastUpdate < 3000) {
            displaySpeed.setFontColor(Color.black);
            connected = true;
        }
        return true;
    }

    @Override
    public void cleanSubPanel() {
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "EstimatedState" };
    }

    @Override
    public void messageArrived(IMCMessage message) {
        double u, v, w;
        
        if (isTrueSpeed) {
            u = message.getDouble("vx");
            v = message.getDouble("vy");
            w = message.getDouble("vz");
        }
        else {
            u = message.getDouble("u");
            v = message.getDouble("v");
            w = message.getDouble("w");
        }

        double res = Math.sqrt(u * u + v * v + w * w);
        res = (isSIUnit ? res : res * m_sToKnotConv);
        displaySpeed.setText(formatter.format(res));
        displaySpeed.setTitle((isTrueSpeed ? "true" : "ind.") + partSpeedTxt + (isSIUnit ? "(m/s)" : "(knot)"));
        lastUpdate = System.currentTimeMillis();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
