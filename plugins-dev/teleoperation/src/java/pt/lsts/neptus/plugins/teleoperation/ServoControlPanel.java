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
 * 4 de Ago de 2010
 */
package pt.lsts.neptus.plugins.teleoperation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zepinto
 * 
 */
@PluginDescription(name = "Servo Control")
public class ServoControlPanel extends ConsolePanel implements ConfigurationListener, ISliderPanelListener {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Servo id")
    public int servoId = 0;

    @NeptusProperty(name = "Minimum Value (degrees)")
    public double minVal = -45;

    @NeptusProperty(name = "Mean Value (degrees)")
    public double meanVal = 0;

    @NeptusProperty(name = "Maximum Value (degrees)")
    public double maxVal = +45;

    @NeptusProperty(name = "Title")
    public String title = "title";

    protected SliderPanel slider = new SliderPanel();

    public ServoControlPanel(ConsoleLayout console) {
        super(console);
        setLayout(new BorderLayout());
        add(slider);
        ToolbarButton tbutton = new ToolbarButton(
                ImageUtils.getIcon("pt/lsts/neptus/plugins/teleoperation/settings.png"), "Settings", "settings");
        add(tbutton, BorderLayout.EAST);
        tbutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSettingPanel();
            }
        });
        ToolbarButton cbutton = new ToolbarButton(
                ImageUtils.getIcon("pt/lsts/neptus/plugins/teleoperation/start.png"), "Center", "center");
        cbutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                slider.setValue(meanVal);
                slider.repaint();
            }
        });
        add(cbutton, BorderLayout.WEST);

        propertiesChanged();
        slider.setValue(meanVal);
        slider.addSliderListener(this);
    }

    protected void showSettingPanel() {
        final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this));
        final SliderPanel min = new SliderPanel(), max = new SliderPanel(), mean = new SliderPanel();
        final JTextField text = new JTextField("" + servoId);

        JPanel panel = new JPanel(new GridLayout(5, 1));
        JPanel tmp1 = new JPanel(new BorderLayout());
        tmp1.add(new JLabel("Servo ID:"), BorderLayout.WEST);
        tmp1.add(text);
        panel.add(tmp1);
        min.setMin(-180);
        max.setMin(-180);
        min.setMax(180);
        max.setMax(180);
        mean.setMin(-180);
        mean.setMax(180);

        min.setTitle("min");
        max.setTitle("max");
        mean.setTitle("zero");

        min.setValue(minVal);
        max.setValue(maxVal);
        mean.setValue(meanVal);

        min.addSliderListener(this);
        max.addSliderListener(this);
        mean.addSliderListener(this);

        panel.add(min);
        panel.add(mean);
        panel.add(max);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ok.setPreferredSize(new Dimension(80, 24));
        cancel.setPreferredSize(new Dimension(80, 24));

        controls.add(ok);
        controls.add(cancel);
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    servoId = Integer.parseInt(text.getText());
                    if (servoId < 0)
                        throw new Exception();
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(dialog, "Servo Control Settings", "Servo ID must be a positive integer");
                    ex.printStackTrace();
                    return;
                }
                minVal = min.getValue();
                maxVal = max.getValue();
                meanVal = mean.getValue();
                dialog.dispose();
                getConsole().setConsoleChanged(true);
                propertiesChanged();
            }
        });

        panel.add(controls);
        dialog.setContentPane(panel);
        dialog.setTitle("Servo control settings");
        dialog.setModal(true);
        dialog.setSize(450, 180);
        dialog.setVisible(true);
    }

    @Override
    public void propertiesChanged() {
        slider.setMin(minVal);
        slider.setMax(maxVal);
        slider.setMean(meanVal);
        slider.setTitle(title);
        slider.repaint();
    }

    IMCMessage msg = null;

    @Override
    public void SliderChanged(SliderPanel source) {
        if (msg == null)
            msg = IMCDefinition.getInstance().create("SetServoPosition");
        msg.setValue("id", servoId);
        msg.setValue("value", Math.toRadians(source.getValue()));
        send(msg);
    }

    public static void main(String[] args) {
        GuiUtils.testFrame(new ServoControlPanel(null));
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
}
