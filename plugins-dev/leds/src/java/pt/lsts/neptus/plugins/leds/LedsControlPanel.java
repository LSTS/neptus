/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: hfq
 * Aug 30, 2013
 */
package pt.lsts.neptus.plugins.leds;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.Border;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.LedBrightness;
import pt.lsts.imc.QueryLedBrightness;
import pt.lsts.imc.SetLedBrightness;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.util.GuiUtils;

/**
 * This panel is responsible for controlling the Leds brightness placed on Adamastor.
 * 
 * @author hfq
 * 
 *         FIXME - Change panel icon
 * 
 *         There are 4 groups of leds, each one containing 3 leds, placed on the front of the Rov
 */
@Popup(pos = POSITION.TOP_LEFT, width = 300, height = 530, accelerator = 'D')
@PluginDescription(author = "hfq", description = "Panel that enables setting up leds brightness", name = "Leds Control Panel", version = "0.1", icon = "images/menus/tip.png")
public class LedsControlPanel extends ConsolePanel implements IPeriodicUpdates, ActionListener, ItemListener {
    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Periodicity (milliseconds)", description = "Set update periodicity in miliseconds", editable = true) 
    public int periodicity = 1000;

    private ConsoleLayout console;
    protected LedsSlider slider1, slider2, slider3, slider4;
    protected JPanel checkBoxPanel;
    protected JCheckBox checkBoxSetAllLeds;
    protected PictureComponent picComp;
    protected int sliderNumComp = 0;
    protected boolean allLedsToBeSet = false;

    protected LinkedHashMap<String, SetLedBrightness> msgsSetLeds = new LinkedHashMap<>();
    protected LinkedHashMap<String, QueryLedBrightness> msgsQueryLeds = new LinkedHashMap<>();

    // Can have a timer to turn on the 4 groups of leds (3 leds per group) in a clockwise matter
    // Timer time;

    /**
     * @param console
     */
    public LedsControlPanel(ConsoleLayout console) {
        super(console);
        this.console = console;
        this.setLayout(new MigLayout("fill"));
        this.removeAll();
        this.setOpaque(true);
        this.setResizable(true);

        initMsgMapping();
    }

    /**
     * Fill up leds mapping for the SetLedBrightness and QueryLedBrightness messages
     */
    private void initMsgMapping() {
        for (int i = 0; i < 12; ++i) {
            SetLedBrightness setMsg = new SetLedBrightness();
            setMsg.setName(LedsUtils.ledNames[i]);
            msgsSetLeds.put(LedsUtils.ledNames[i], setMsg);

            QueryLedBrightness queryMsg = new QueryLedBrightness();
            queryMsg.setName(LedsUtils.ledNames[i]);
            msgsQueryLeds.put(LedsUtils.ledNames[i], queryMsg);
        }
    }

    /**
     * create and add components to this panel
     */
    private void createPanel() {
        slider1 = new LedsSlider(1, this);
        slider2 = new LedsSlider(2, this);
        slider3 = new LedsSlider(3, this);
        slider4 = new LedsSlider(4, this);
        this.add(slider1, "w 100%, wrap");
        this.add(slider2, "w 100%, wrap");
        this.add(slider3, "w 100%, wrap");
        this.add(slider4, "w 100%, wrap");

        checkBoxPanel = new JPanel();
        setPropertiesCheckBox();
        this.add(checkBoxPanel, "w 100%, wrap");

        picComp = new PictureComponent(this);
        this.add(picComp, "w 100%, wrap");
    }

    /**
     * Set properties of Panel containing the checkbox, and properties of the components that are added to this panel
     */
    private void setPropertiesCheckBox() {
        checkBoxPanel.setLayout(new MigLayout());
        checkBoxPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(0, 1, 0, 0)));
        checkBoxPanel.setOpaque(false);

        checkBoxSetAllLeds = new JCheckBox("Set up all Leds");
        checkBoxSetAllLeds.setName("Set all Leds");
        checkBoxSetAllLeds.setMnemonic(KeyEvent.VK_A);
        checkBoxSetAllLeds.setSelected(false);
        checkBoxSetAllLeds.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
        checkBoxSetAllLeds.setOpaque(false);
        checkBoxSetAllLeds.addItemListener(this);
        checkBoxPanel.add(checkBoxSetAllLeds, "wrap");
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphic2d = (Graphics2D) g;
        Color color1 = getBackground();
        Color color2 = Color.DARK_GRAY;
        GradientPaint gradPaint = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
        graphic2d.setPaint(gradPaint);
        graphic2d.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override
    public void initSubPanel() {
        createPanel();
        Border panEdge = BorderFactory.createEmptyBorder(0, 10, 10, 10);
        this.setBorder(panEdge);

        this.console.addMainVehicleListener(this);

        ImcMsgManager.getManager().addListener(this);
    }

    @Override
    public void cleanSubPanel() {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    /**
    *
    */
    private void printMsgMapping() {
        NeptusLog.pub().info("Led brightness class id: " + SetLedBrightness.ID_STATIC);
        NeptusLog.pub().info("Query Led class id  " + QueryLedBrightness.ID_STATIC);
        for (Entry<String, SetLedBrightness> entry : msgsSetLeds.entrySet()) {
            NeptusLog.pub().info("Key: " + entry.getKey() + " Value: " + entry.getValue());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */
    @Override
    public long millisBetweenUpdates() {
        // TODO Auto-generated method stub
        return periodicity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public boolean update() {
        for (Entry<String, QueryLedBrightness> entry : msgsQueryLeds.entrySet()) {
            QueryLedBrightness msg = msgsQueryLeds.get(entry.getKey());
            send(msg);

            // NeptusLog.pub().info("Key: " + entry.getKey() + " Value: " + entry.getValue());
        }
        return true;
    }

    @Subscribe
    public void consume(LedBrightness msg) {
        try {
            // update the LedsSlider component with TextFields with the current Led Brightness
            // got from msg QueryLedBrightness
            String name = msg.getName();
            short i = msg.getValue();
            NeptusLog.pub().info("value of brightness on led " + name + ": " + i);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        LedsControlPanel lcp = new LedsControlPanel(null);
        lcp.printMsgMapping();
        Border panEdge = BorderFactory.createEmptyBorder(0, 10, 10, 10);
        lcp.setBorder(panEdge);
        lcp.createPanel();
        GuiUtils.testFrame(lcp, "Test" + lcp.getClass().getSimpleName(), LedsUtils.PANEL_WIDTH, LedsUtils.PANEL_HEIGHT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (allLedsToBeSet)
            allLedsToBeSet = false;
        else {
            allLedsToBeSet = true;
            sliderNumComp = 5;
        }
    }
}
