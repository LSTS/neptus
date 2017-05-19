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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;


@PluginDescription(name = "NVL Runtime Feature", author = "Keila Lima")
@Popup(pos = Popup.POSITION.BOTTOM_RIGHT, width=300, height=300)
@SuppressWarnings("serial")
public class NVLConsolePanel extends ConsolePanel {
    
    public NVLConsolePanel(ConsoleLayout layout) {
        super(layout);
    }

   
    @Override
    public void initSubPanel() {
        NeptusNVLPlatform.getInstance().associateTo(this);
        test();
    }

    @Override
    public void cleanSubPanel() {
        NeptusNVLPlatform.getInstance().detach();
    }
    
    private void test() {
        
        JProgressBar progressBar = new JProgressBar(SwingConstants.HORIZONTAL);
        Border border = BorderFactory.createTitledBorder("Testing...");
        JButton testButton = new JButton(
                new AbstractAction(I18n.text("Test!")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {   
                        new Thread(() -> {
                            NeptusNVLPlatform.getInstance().run(new File("conf/nvl/imcplan.nvl"));
                        }).start();
                    }
                });
        
        
        JPanel holder = new JPanel(new BorderLayout(1, 1));
//        JPanel grid = new JPanel(new GridLayout(2, 0));
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setBorder(border);
        holder.add(progressBar,BorderLayout.SOUTH);
        holder.add(testButton,BorderLayout.CENTER);
//        grid.add(progressBar,BorderLayout.SOUTH);
//        grid.add(testButton,BorderLayout.CENTER);
        add(holder,BorderLayout.CENTER);
        
        
       
        
        

    }

    @Subscribe
    public void on(ConsoleEventPlanChange changedPlan) {
        NeptusNVLPlatform.getInstance().onPlanChanged(changedPlan);
      
    }


    public void displayMessage(String fmt, Object[] args) {
        // TODO Auto-generated method stub
        
    }

}
