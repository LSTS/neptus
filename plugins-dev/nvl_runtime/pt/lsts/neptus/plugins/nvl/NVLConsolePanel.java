///*
// * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
// * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
// * All rights reserved.
// * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
// *
// * This file is part of Neptus, Command and Control Framework.
// *
// * Commercial Licence Usage
// * Licencees holding valid commercial Neptus licences may use this file
// * in accordance with the commercial licence agreement provided with the
// * Software or, alternatively, in accordance with the terms contained in a
// * written agreement between you and Universidade do Porto. For licensing
// * terms, conditions, and further information contact lsts@fe.up.pt.
// *
// * Modified European Union Public Licence - EUPL v.1.1 Usage
// * Alternatively, this file may be used under the terms of the Modified EUPL,
// * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
// * included in the packaging of this file. You may not use this work
// * except in compliance with the Licence. Unless required by applicable
// * law or agreed to in writing, software distributed under the Licence is
// * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
// * ANY KIND, either express or implied. See the Licence for the specific
// * language governing permissions and limitations at
// * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
// * and http://ec.europa.eu/idabc/eupl.html.
// *
// * For more information please see <http://lsts.fe.up.pt/neptus>.
// *
// * Author: edrdo
// * May 14, 2017
// */
package pt.lsts.neptus.plugins.nvl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;


@PluginDescription(name = "NVL Runtime Feature", author = "Keila Lima")
@Popup(pos = Popup.POSITION.BOTTOM_RIGHT, width=400, height=200)
@SuppressWarnings("serial")
public class NVLConsolePanel extends ConsolePanel {
    
    private Border border;
    private JScrollPane outputPanel;
    private JTextArea output;
    
    public NVLConsolePanel(ConsoleLayout layout) {
        super(layout);
    }

   
    @Override
    public void initSubPanel() {
        NeptusPlatform.getInstance().associateTo(this);
        output = new JTextArea();
        border = BorderFactory.createTitledBorder("Script Output");
        output.setBorder(border);
        output.setEditable(false);
        output.setVisible(true);
//        output.setBackground(Color.BLACK);
//        output.setCaretColor(Color.WHITE);
        output.append("NLV Runtime Console\n");
        outputPanel = new JScrollPane(output);//RSyntaxTextArea("Script Output")
        output.setVisible(true);
        test();



    }

    @Override
    public void cleanSubPanel() {
        NeptusPlatform.getInstance().detach();
    }
    
    private void test() {
        

        JButton testButton = new JButton(
                new AbstractAction(I18n.text("Test!")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {   
                        new Thread(() -> {
                            NeptusPlatform.getInstance().run(new File("conf/nvl/imcplan.nvl"));
                        }).start();
                    }
                });
        JButton clear = new JButton(new AbstractAction(I18n.text("Clear Console!")) {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                output.setText("");
                            
            }
        });
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(testButton,BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setPreferredSize(new Dimension(300, 150));
        bottom.add(clear,BorderLayout.SOUTH);
        bottom.add(outputPanel,BorderLayout.CENTER);
        
        add(testButton,BorderLayout.CENTER);
        add(bottom,BorderLayout.SOUTH);
//        JProgressBar progressBar = new JProgressBar(SwingConstants.HORIZONTAL);
//        Border border = BorderFactory.createTitledBorder("Testing...");
//        progressBar.setValue(0); //TODO during task exec?
//        progressBar.setStringPainted(true);
//        progressBar.setBorder(border);
//        holder.add(progressBar,BorderLayout.SOUTH);
       

        
       
        
        

    }

    @Subscribe
    public void on(ConsoleEventPlanChange changedPlan) {
        NeptusPlatform.getInstance().onPlanChanged(changedPlan);
      
    }


    public void displayMessage(String fmt, Object[] args) {
        
        if(output!=null){
            output.append(String.format(fmt, args));
            output.append("\n");
            output.setCaretPosition(output.getDocument().getLength());
        }
        
    }

}
