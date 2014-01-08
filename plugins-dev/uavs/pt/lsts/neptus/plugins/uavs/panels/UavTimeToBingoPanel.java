/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: christian
 * 06.11.2012
 */
package pt.lsts.neptus.plugins.uavs.panels;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.Timer;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleSubPanel;

/**
 * @author Christian Fuchs
 * @version 0.2
 * @category UavPanel
 * Neptus panel that displays a countdown until the time to BINGO is reached
 */

@PluginDescription(name="Uav TimeToBingo Panel", icon="pt/lsts/neptus/plugins/uavs/planning.png", author="Christian Fuchs")
public class UavTimeToBingoPanel extends SimpleSubPanel{

//--------------declarations-----------------------------------//
    
    private static final long serialVersionUID = 1L;
    
    private long endTime;
    
    private long startTime;
    
    private Timer timer;
    
    private JTextField textF;
    
    private JProgressBar bar;
    
    private JButton startButton, stopButton;
    
    private JLabel label;
    
    private int oldColor;
    
//--------------end of declarations----------------------------//
    
    public UavTimeToBingoPanel(ConsoleLayout console){
        super(console);
        
        //clears all the unused initializations of the standard SimpleSubPanel
        removeAll();
    }
    
//--------------Setters and Getters----------------------------//
    
    public void setEndTime(long endTime){
        this.endTime = endTime;
    }
    
    public long getEndTime(){
        return endTime;
    }
    
    public void setStartTime(long startTime){
        this.startTime = startTime;
    }
    
    public long getStartTime(){
        return startTime;
    }
    
    public void setTimer(Timer timer){
        this.timer = timer;
    }
    
    public Timer getTimer(){
        return timer;
    }
    
    public void setTextField(JTextField textF){
        this.textF = textF;
        add(textF);
    }
    
    public void setProgressBar(JProgressBar bar){
        this.bar = bar;
    }
    
    public JProgressBar getProgressBar(){
        return bar;
    }
    
//--------------End of Setters and Getters---------------------//
    
    @Override
    public void initSubPanel(){
        
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
                
        // create JLabel that contains the title
        label = new JLabel("Time To BINGO", JLabel.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 20));
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;
        c.weightx = 0;
        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy = 0;
        add(label, c);
        
        
        // create progressbar and set its minimum and maximum
        setProgressBar(new JProgressBar());
        oldColor = bar.getForeground().getRGB();
        bar.setMinimum(0);
        bar.setString(timeToString(0));
        bar.setStringPainted(true);
        bar.addMouseListener(new MouseListener() {
            
            @Override
            public void mouseReleased(MouseEvent e) {
                
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                
                if (textF.isVisible()){
                    textF.setVisible(false);
                    startButton.setVisible(false);
                    stopButton.setVisible(false); 
                }
                else{
                    textF.setVisible(true);
                    startButton.setVisible(true);
                    stopButton.setVisible(true);
                }
            }
        });
        
        // layout stuff for the bar
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 40;      
        c.weightx = 0.0;
        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy = 1;
        
        // create and configure an action listener
        // this listener will be connected to the timer and "fire" at every timer increment
        // the action listener then sets the bar's string and value accordingly
        ActionListener listener = new ActionListener(){
            
            @Override
            public void actionPerformed(ActionEvent e) {
//                NeptusLog.pub().info("<###>ActionTimer");
                bar.setValue((int) (System.currentTimeMillis() - startTime));
                
                if (System.currentTimeMillis() > (startTime + endTime)){
                    bar.setForeground(Color.red);
                    bar.setString(timeToString(-(startTime + endTime - System.currentTimeMillis())));
                }
                else{
                    bar.setString(timeToString(startTime + endTime - System.currentTimeMillis()));
                }
            }
        };
        add(bar, c);
        
        // text field
        textF = new JTextField("hh:mm:ss");
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 2;
        
        //creating a panel to hold the text field keeps the gridbaglayout from resizing when the text field visibility is turned off
        JPanel containerPanel1 = new JPanel();
        containerPanel1.setLayout(new GridLayout(1,1));
        containerPanel1.add(textF);
        add(containerPanel1, c);
        
        // start button
        startButton = new JButton("START");
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setStartTime(System.currentTimeMillis());
                setEndTime(stringToTime(textF.getText()));
                
                // endTime will only be 0 if the string is not of the form hh:mm:ss
                if (!( endTime == 0)){
                    bar.setForeground(new Color(oldColor));
                    bar.setValue(0);
                    bar.setMaximum((int) endTime);
                    bar.setString(timeToString(endTime));
                    timer.start();
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true); 
                    
                    textF.setVisible(false);
                    startButton.setVisible(false);
                    stopButton.setVisible(false);
                }
                else{
                    textF.setText("hh:mm:ss");
                }
            }
        });
        startButton.setToolTipText("Starts the timer with the specified time");
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 1;
        c.gridy = 2;
        
        //creating a panel to hold the button keeps the gridbaglayout from resizing when the button visibility is turned off
        JPanel containerPanel2 = new JPanel();
        containerPanel2.setLayout(new GridLayout(1,1));
        containerPanel2.add(startButton);
        add(containerPanel2, c);
        
        // stop button
        stopButton = new JButton("STOP");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timer.stop();
                stopButton.setEnabled(false);
                startButton.setEnabled(true);
            }
        });
        stopButton.setToolTipText("Starts the timer again with the specified time");
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 2;
        c.gridy = 2;
        
        //creating a panel to hold the button keeps the gridbaglayout from resizing when the button visibility is turned off
        JPanel containerPanel3 = new JPanel();
        containerPanel3.setLayout(new GridLayout(1,1));
        containerPanel3.add(stopButton);
        add(containerPanel3, c);
        
        setTimer(new Timer(1000, listener));
//        timer.start();
        
    }
    
    // convert a time in milliseconds to a string of the form hh:mm:ss
    public String timeToString(long remainingTime){
        
        String format = String.format("%%0%dd", 2);  
        remainingTime = remainingTime / 1000;  
        String seconds = String.format(format, remainingTime % 60);  
        String minutes = String.format(format, (remainingTime % 3600) / 60);  
        String hours = String.format(format, remainingTime / 3600);  
        String time =  hours + ":" + minutes + ":" + seconds;  
        
        return time;  
        
    }
    
    // convert a string of form hh:mm:ss to a time in milliseconds
    public long stringToTime(String str){
        long time = 0;
        
        // check if the string is really of the form hh:mm:ss
        if (str.matches("\\d{2}:[012345]\\d:[012345]\\d")){
            int hours = Integer.parseInt(str.split(":")[0]);
            int minutes = Integer.parseInt(str.split(":")[1]);
            int seconds = Integer.parseInt(str.split(":")[2]);
            
            time = 1000 * ((hours * 3600) + (minutes * 60) + seconds);
        }
        
        return time;
    }
    
    @Override
    public void cleanSubPanel(){
    }

}