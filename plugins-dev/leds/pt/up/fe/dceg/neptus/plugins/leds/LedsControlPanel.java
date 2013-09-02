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
import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.LedBrightness;
import pt.up.fe.dceg.neptus.imc.QueryLedBrightness;
import pt.up.fe.dceg.neptus.imc.SetLedBrightness;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.Popup.POSITION;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

/**
 * This panel is responsible for controlling the Leds brightness placed on Adamastor.
 * 
 * @author hfq
 * 
 *         FIX ME - Change panel icon
 * 
 *         There are 4 groups of leds, each one containing 3 leds, placed on the front of the Rov
 * 
 *         Use: IMC::SetLedBrightness IMC::QueryLedBrightness
 * 
 *         IMC::LedBrightness
 * 
 *         SetLedBrightness extends IMCMessage
 * 
 *         QueryLedBrightness extends IMCMessage will reply with LedBrightness
 * 
 *         DUNE LED4R - device that allows controlling up to 12 high-brightness LEDs [Actuators.LED4R] Enabled =
 *         Hardware Entity Label = LED Driver Serial Port - Device = /dev/ttyUSB3 LED - Names = LED0, LED1, LED2, LED3,
 *         LED4, LED5, LED6, LED7, LED8, LED9, LED10, LED11
 * 
 *         adamastor_en_US.xml Neptus conf parameters <param name="LED - Names"> <name-i18n>LED - Names</name-i18n>
 *         <type>list:string</type> <visibility>developer</visibility> <scope>global</scope> <default>01, 02, 03, 04,
 *         05, 06, 07, 08, 09, 10, 11, 12</default> <units/> <desc>List of LED names</desc> <size>12</size> </param>
 */
// @Popup(pos = POSITION.TOP_LEFT, accelerator = 'D')
@Popup(pos = POSITION.TOP_LEFT, width = 550, height = 550, accelerator = 'D')
@PluginDescription(author = "hfq", description = "Panel that enables setting up leds brightness", name = "Leds Control Panel", version = "0.1", icon = "images/menus/tip.png")
public class LedsControlPanel extends SimpleSubPanel implements IPeriodicUpdates, ActionListener, ItemListener {
    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Periodicity millis", description = "Set update periodicity in miliseconds", editable = true)
    public int periodicity = 1000;

    private ConsoleLayout console;
    public LinkedHashMap<String, Integer> msgLeds = new LinkedHashMap<>();
    protected LedsSlider slider1, slider2, slider3, slider4;
    protected JPanel checkBoxPanel;
    protected JCheckBox checkBoxSetAllLeds;
    protected PictureComponent picComp;
    protected int sliderNumComp = 0;
    protected boolean allLedsToBeSet = false;

    // Can have a timer to turn on the 4 groups of leds (3 leds per group) in a clockwise matter
    // Timer time;

    /**
     * @param console
     */
    public LedsControlPanel(ConsoleLayout console) {
        super(console);
        this.console = console;
        //this.setLayout(new MigLayout("insets 0"));
        this.setLayout(new MigLayout("fill"));
        this.removeAll();
        // this.setBackground(Color.DARK_GRAY);
        this.setOpaque(true);
        this.setResizable(true);
        // this.add

        initMsgMapping();
    }

    /**
     * Fill up leds mapping
     */
    private void initMsgMapping() {
        for (int i = 0; i < 12; ++i) {
            // msgLeds.put("LED" + (i), 0);
            msgLeds.put(LedsUtils.ledNames[0], 0);
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
        //this.add(checkBoxPanel, "wrap");
        this.add(checkBoxPanel, "w 100%, wrap");
        
        picComp = new PictureComponent(this);
        this.add(picComp, "w 100%, wrap");
        //this.add(picComp, "grow, push, span");

        SetLedBrightness msgLed1 = new SetLedBrightness();

        // QueryLedBrightness qled = new QueryLedBrightness(ledNames[0]);
        // int value1 = (Integer)qled.getValue(ledNames[0]);
    }

    /**
     * Set properties of Panel containg the checkbox, and properties of the components that are added to this panel
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
        //Color color2 = color1.darker();
        Color color3 = Color.DARK_GRAY;
        GradientPaint gradPaint = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color3);
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
        for (Entry<String, Integer> entry : msgLeds.entrySet()) {
            NeptusLog.pub().info("Key: " + entry.getKey() + " Value: " + entry.getValue());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */
    @Override
    public long millisBetweenUpdates() {
        // TODO Auto-generated method stub
        return periodicity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public boolean update() {
        QueryLedBrightness query1 = new QueryLedBrightness();
        query1.setName(LedsUtils.ledNames[0]);
        
        send(query1);
        
        return false;
    }
    
    @Subscribe
    public void consume(LedBrightness msg) {
        
        try {
            short i = msg.getValue();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    


    /**
     * @param args
     */
    public static void main(String[] args) {
        LedsControlPanel lcp = new LedsControlPanel(null);
        // lcp.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
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
