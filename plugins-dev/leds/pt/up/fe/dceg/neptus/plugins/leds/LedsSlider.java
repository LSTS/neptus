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

import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.NeptusLog;

/**
 * This component has a label, a slider, a text representing the desired value for brightness, and a text with the value
 * queried from imc message of the leds brightness (three,each led?). Each slider is responsible to set the brightness
 * of a group of 3 leds.
 * 
 * @author hfq
 * 
 */
public class LedsSlider extends JPanel implements ChangeListener, ActionListener {
    private static final long serialVersionUID = 1L;

    private LedsControlPanel parent;

    private JSlider slider;
    // private String sliderName;
    private int sliderNumber = 0;;
    private int sliderValue = 0;
    // private JLabel sliderLabel;
    private JTextField sliderTextField;
    private Border loweredetched;
    private TitledBorder titled;

    private JPanel collectionLedsPanel;
    private JLabel led1Label;
    private JTextField led1;
    private JLabel led2Label;
    private JTextField led2;
    private JLabel led3Label;
    private JTextField led3;

    /**
     * @param name
     * @param parent
     */
    public LedsSlider(int sliderNumber, LedsControlPanel parent) {
        // super();
        this.setLayout(new MigLayout());
        this.parent = parent;
        // this.sliderName = name;
        this.sliderNumber = sliderNumber;
        // this.setBackground(Color.BLACK);
        this.setOpaque(false);
        this.setSize(LedsControlPanel.WIDTH, LedsControlPanel.HEIGHT / 4);
        createBorder();
        createSlider();
        createSliderTextField();
        createTextFieldsForGroup();
    }

    /**
     * Create border for this component
     */
    private void createBorder() {
        // loweredetched = BorderFactory.createLoweredSoftBevelBorder();
        loweredetched = BorderFactory.createLoweredBevelBorder();

        titled = BorderFactory.createTitledBorder(loweredetched, "Leds Group " + String.valueOf(sliderNumber));
        titled.setTitleJustification(TitledBorder.LEFT);
        titled.setTitlePosition(TitledBorder.DEFAULT_POSITION);

        this.setBorder(titled);
    }

    /**
     * Create and set JSlider
     */
    public void createSlider() {
        // sliderLabel = new JLabel(sliderName, JLabel.CENTER);
        // // sliderLabel.setBackground(Color.DARK_GRAY);
        // sliderLabel.setFont(new Font(Font.SERIF, (Font.BOLD + Font.ITALIC), 12));
        // sliderLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // sliderLabel.setOpaque(false);

        slider = new JSlider(JSlider.HORIZONTAL, LedsUtils.LED_MIN_BRIGHTNESS, LedsUtils.LED_MAX_BRIGHTNESS,
                LedsUtils.LED_INIT_BRIGHTNESS);
        slider.setValue(LedsUtils.LED_INIT_BRIGHTNESS);

        slider.setMajorTickSpacing(20);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setCursor(new Cursor(Cursor.HAND_CURSOR));

        slider.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));

        slider.setToolTipText("Leds Group " + String.valueOf(sliderNumber) + " Brightness Controller");
        slider.setFont(new Font(Font.SERIF, Font.ITALIC, 10));
        slider.setOpaque(false);

        slider.addChangeListener(this);

        // this.add(sliderLabel);
        this.add(slider);
    }

    /**
     * Create and set text field showing slider values
     */
    private void createSliderTextField() {
        sliderTextField = new JTextField();
        sliderTextField.setColumns(3);
        sliderTextField.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        sliderTextField.setText(String.valueOf(sliderValue));
        sliderTextField.setEditable(false);
        sliderTextField.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 1));
        this.add(sliderTextField);
    }

    /**
     * Create and set text fields that are will hold queried brightness values for each led on this each group
     */
    private void createTextFieldsForGroup() {
        collectionLedsPanel = new JPanel();
        collectionLedsPanel.setLayout(new MigLayout());
        collectionLedsPanel.setOpaque(false);
        collectionLedsPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        collectionLedsPanel.setName(String.valueOf(sliderNumber));

        led1Label = new JLabel();
        led2Label = new JLabel();
        led3Label = new JLabel();
        setPropertiesForLedsLabel(led1Label, led2Label, led3Label);

        led1 = new JTextField();
        led2 = new JTextField();
        led3 = new JTextField();
        setPropertiesForLedsTextField(led1, led2, led3);

        collectionLedsPanel.add(led1Label);
        collectionLedsPanel.add(led1);
        collectionLedsPanel.add(led2Label);
        collectionLedsPanel.add(led2);
        collectionLedsPanel.add(led3Label);
        collectionLedsPanel.add(led3);

        this.add(collectionLedsPanel);
    }

    /**
     * set properties of Leds label component
     * 
     * @param led3Label2
     * @param led2Label2
     * @param led1Label2
     */
    private void setPropertiesForLedsLabel(JLabel label1, JLabel label2, JLabel label3) {
        label1.setFont(new Font(Font.SERIF, (Font.BOLD + Font.ITALIC), 10));
        label1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 1));
        label1.setOpaque(false);

        label2.setFont(new Font(Font.SERIF, (Font.BOLD + Font.ITALIC), 10));
        label2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 1));
        label2.setOpaque(false);

        label3.setFont(new Font(Font.SERIF, (Font.BOLD + Font.ITALIC), 10));
        label3.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 1));
        label3.setOpaque(false);

        switch (sliderNumber) {
            case 1:
                label1.setText(LedsUtils.ledNames[0]);
                label2.setText(LedsUtils.ledNames[1]);
                label3.setText(LedsUtils.ledNames[2]);
                break;
            case 2:
                label1.setText(LedsUtils.ledNames[3]);
                label2.setText(LedsUtils.ledNames[4]);
                label3.setText(LedsUtils.ledNames[5]);
                break;
            case 3:
                label1.setText(LedsUtils.ledNames[6]);
                label2.setText(LedsUtils.ledNames[7]);
                label3.setText(LedsUtils.ledNames[8]);
                break;
            case 4:
                label1.setText(LedsUtils.ledNames[9]);
                label2.setText(LedsUtils.ledNames[10]);
                label3.setText(LedsUtils.ledNames[11]);
                break;
        }
    }

    /**
     * Set properties of Leds textfield component
     * 
     * @param textField
     * @param led32
     * @param led22
     */
    private void setPropertiesForLedsTextField(JTextField text1, JTextField text2, JTextField text3) {
        text1.setColumns(3);
        text1.setEditable(false);
        text1.setText(String.valueOf(0));

        text2.setColumns(3);
        text2.setEditable(false);
        text2.setText(String.valueOf(0));

        text3.setColumns(3);
        text3.setEditable(false);
        text3.setText(String.valueOf(0));
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider) e.getSource();
        
        if(!parent.allLedsToBeSet) {           
            if (!source.getValueIsAdjusting()) {
                sliderValue = source.getValue();
                sliderTextField.setText(String.valueOf(sliderValue));
                if (sliderValue == 0) {
                    NeptusLog.pub().info(
                            "Value of slider " + String.valueOf(sliderNumber) + " value in perc: " + sliderValue
                                    + " value in brightness: " + LedsUtils.convPercToLedsBright(sliderValue));
                }
                else {
                    // send message with the value of
                    // LedsUtils.convPercToLedsBright(sliderValue)
                    NeptusLog.pub().info(
                            "Value of slider " + String.valueOf(sliderNumber) + " value in perc: " + sliderValue
                                    + " value in brightness: " + LedsUtils.convPercToLedsBright(sliderValue));
                }
            }
            else {
                sliderValue = source.getValue();
                sliderTextField.setText(String.valueOf(sliderValue));
            }
            if (sliderNumber != parent.sliderNumComp)
                parent.picComp.updatePicture(sliderNumber);
            parent.sliderNumComp = sliderNumber;
        } 
        else {
            if(!source.getValueIsAdjusting()) {
                sliderValue = source.getValue();
                sliderTextField.setText(String.valueOf(sliderValue));
                for (int i = 1; i <= 4; ++i) {
                    if(this.equals(parent.slider1)) {
                        parent.slider2.slider.setValue(sliderValue);
                        parent.slider2.sliderTextField.setText(String.valueOf(sliderValue));
                        parent.slider3.slider.setValue(sliderValue);
                        parent.slider3.sliderTextField.setText(String.valueOf(sliderValue));
                        parent.slider4.slider.setValue(sliderValue);
                        parent.slider4.sliderTextField.setText(String.valueOf(sliderValue));
                    }
                    else if (this.equals(parent.slider2)) {
                        parent.slider1.slider.setValue(sliderValue);
                        parent.slider1.sliderTextField.setText(String.valueOf(sliderValue));
                        parent.slider3.slider.setValue(sliderValue);
                        parent.slider3.sliderTextField.setText(String.valueOf(sliderValue));
                        parent.slider4.slider.setValue(sliderValue);
                        parent.slider4.sliderTextField.setText(String.valueOf(sliderValue));                       
                    }
                    else if (this.equals(parent.slider3)) {
                        parent.slider1.slider.setValue(sliderValue);
                        parent.slider1.sliderTextField.setText(String.valueOf(sliderValue));
                        parent.slider2.slider.setValue(sliderValue);
                        parent.slider2.sliderTextField.setText(String.valueOf(sliderValue));
                        parent.slider4.slider.setValue(sliderValue);
                        parent.slider4.sliderTextField.setText(String.valueOf(sliderValue));                        
                    }
                    else if (this.equals(parent.slider4)) {
                        parent.slider1.slider.setValue(sliderValue);
                        parent.slider1.sliderTextField.setText(String.valueOf(sliderValue));
                        parent.slider2.slider.setValue(sliderValue);
                        parent.slider2.sliderTextField.setText(String.valueOf(sliderValue));
                        parent.slider3.slider.setValue(sliderValue);
                        parent.slider3.sliderTextField.setText(String.valueOf(sliderValue));                        
                    }
                }
//                if (this.getName().equals()) {
//                    parent.s
//                }
            }
            else {
                sliderValue = source.getValue();
                sliderTextField.setText(String.valueOf(sliderValue));
            }
            //sliderNumber = 5;
            //parent.sliderNumComp = sliderNumber;
            parent.picComp.updatePicture(parent.sliderNumComp);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        NeptusLog.pub().info("Action performed");

    }
}
