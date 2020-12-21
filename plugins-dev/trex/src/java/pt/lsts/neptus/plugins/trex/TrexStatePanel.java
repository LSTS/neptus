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
 * Author: zp
 * May 31, 2015
 */
package pt.lsts.neptus.plugins.trex;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.TrexAttribute;
import pt.lsts.imc.TrexToken;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;

/**
 * @author zp
 *
 */
@PluginDescription
@Popup(pos=POSITION.CENTER, width=600, height=400)
public class TrexStatePanel extends ConsolePanel {

    private static final long serialVersionUID = -8148637113464677133L;
    private ArrayList<Long> timesteps = new ArrayList<Long>();
    private LinkedHashMap<Long, LinkedHashMap<String, TrexToken>> states = new LinkedHashMap<Long, LinkedHashMap<String,TrexToken>>();
    
    private JSlider slider = new JSlider();
    private JToggleButton rt = new JToggleButton("real-time");
    private JButton clear = new JButton("clear"); 
    private JLabel mainPane = new JLabel("<html> </html>");
    private int curTime = -1;
    
    public TrexStatePanel(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void cleanSubPanel() {
                
    }

    @Override
    public void initSubPanel() {
        setLayout(new BorderLayout());
        mainPane.setOpaque(true);
        mainPane.setBackground(Color.WHITE);
        add(new JScrollPane(mainPane), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(slider, BorderLayout.CENTER);
        bottom.add(rt, BorderLayout.EAST);
        bottom.add(clear, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);
        slider.setMaximum(0);
        slider.addChangeListener(new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                setTime(slider.getValue());                
            }
        });
        clear.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (timesteps) {
                    timesteps.clear();
                    states.clear();    
                }                
            }
        });
    }
    
    @Periodic
    public void update() {
        if (!isVisible() || !rt.isSelected())
            return;
        //setTime(timesteps.size()-2);        
    }
    
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private void setTime(int index) {
        
        if (curTime == index)
            return;
        
        long time = timesteps.get(index);
        String text = "<html><h1>"+sdf.format(new Date(time))+"</h1>\n";
        
        for (TrexToken state : states.get(time).values()) {
            long duration = (time-state.getTimestampMillis())/1000;
            text += asHtml(duration, state);
        }
        
        mainPane.setText(text+"</html>");
        curTime = index;
    }
    
    
    private String asHtml(long duration, TrexToken tok) {
        
        String txt = "<h2>";
        if (duration == 0)
            txt += "<font color=red>"+tok.getTimeline()+"."+tok.getPredicate()+"</font></h2><blockquote><table>\n";
        else
            txt += tok.getTimeline()+"."+tok.getPredicate()+"</h2><blockquote><table>\n";
        
        for (TrexAttribute attr : tok.getAttributes()) {
            if (attr.getMax().equals(attr.getMin()))
                txt += "<tr><td>"+attr.getName()+":</td><td>"+attr.getMin()+"</td></tr>\n";
            else
                txt += "<tr><td>"+attr.getName()+":</td><td>"+"["+attr.getMin()+", "+attr.getMax()+"]</td></tr>\n";
        }
        txt += "<tr><td>duration:</td><td>"+(duration+1)+"</td></tr>\n";
        txt += "</table></blockquote>\n";
        return txt;
    }
    
    @Subscribe
    public void on(TrexToken token) {
        try {
            long time = (token.getTimestampMillis()/1000)*1000;
            boolean timeChanged = false;
            synchronized (timesteps) {
                if (!timesteps.contains(time)) {
                    timeChanged = true;
                    timesteps.add(time);
                    if (timesteps.size()>1) {
                        LinkedHashMap<String, TrexToken> tokens = new LinkedHashMap<String, TrexToken>();
                        
                        tokens.putAll(states.get(timesteps.get(timesteps.size()-2)));
                        states.put(time, tokens);                    
                    }
                    else
                        states.put(time, new LinkedHashMap<String, TrexToken>());                                
                    slider.setMaximum(states.size()-1);                
                }
                if (rt.isSelected() && timeChanged) {
                    slider.setValue(states.size()-2);
                }
                
                states.get(time).put(token.getTimeline(), token);
            }        
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
