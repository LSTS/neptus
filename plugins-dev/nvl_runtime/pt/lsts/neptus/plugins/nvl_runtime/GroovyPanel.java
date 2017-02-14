/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Author: lsts
 * 10/02/2017
 */
package pt.lsts.neptus.plugins.nvl_runtime;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.codehaus.groovy.control.CompilationFailedException;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author lsts
 *
 */
@PluginDescription(name = "Groovy Panel", author = "Keila Lima")
@Popup(pos = POSITION.RIGHT, width=200, height=200, accelerator='y')
@SuppressWarnings("serial")
public class GroovyPanel extends ConsolePanel{
    
    
    private JButton openButton,stopScript;
    private GroovyListeners groovy = null; 
    private volatile boolean flag;
    private Thread thread;
    
    public GroovyPanel(ConsoleLayout console){
        super(console);
        
       
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        removeAll();
        this.flag = false;
        if (this.groovy.equals(null))
            this.groovy = new GroovyListeners(getConsole());
        
        Action selectAction = new AbstractAction(I18n.text("Select Groovy Script")) {
            
            @Override
            public void actionPerformed(ActionEvent e) {    
                
              //Handle open button action.
                if (e.getSource() == openButton) {
                    
                    
                  //Create a file chooser
                    final JFileChooser fc = new JFileChooser();
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    FileNameExtensionFilter filter = new FileNameExtensionFilter("Groovy files","groovy");
                    fc.setFileFilter(filter);
                    int returnVal = fc.showOpenDialog(GroovyPanel.this);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File groovy_script = fc.getSelectedFile();
                        
                        System.out.println("Opening: " + groovy_script.getName() + "." + "\n");
                        
                        
                                
                                stopScript.setEnabled(false); 
                                flag = true;
                                thread = new Thread() {
                                    
                                   
                                    public void run() {
                                        while(flag){    
                                        
                                        try {
                                            String[] args = null;
                                            Object app = groovy.getShell().run(groovy_script,args); //shell.getContext().getVariable();
                                            
                                        }
                                        catch (CompilationFailedException | IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    thread.interrupt();
                                }
                                };
                               
                                thread.start();
                               
                                try {
                                    
                                    thread.join(); //thread.join(millis);
                                    stopScript.setEnabled(false);
                                    System.out.println("Exiting script execution. . .\n");
                                }
                                catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                } 

                                
                                

                  
                      
                      System.out.println("Exiting filechooser block execution. . .\n");
                      stopScript.setEnabled(false);
                          
                    } 
                    
                    else {
                        System.out.println("Open command cancelled by user." + "\n");
                        
                    }
               }
                
                 
                    }
                
                };
                
                Action stopAction = new AbstractAction(I18n.text("Stop Script"), ImageUtils.getScaledIcon("pt/lsts/neptus/plugins/groovy/images/stop.png", 10, 30)) {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) { 
                            if(e.getSource() == stopScript){
                                if(flag)
                                    flag = false;
                            System.out.println("Stopping script execution. . .\n");
                            stopScript.setEnabled(false);
                        }
                        
                    }
                };

          
          openButton = new JButton(selectAction); //Button height: 22 Button width: 137 Button X: 30 Button Y: 5
          stopScript = new JButton(stopAction);
          
          add(openButton);
          add(stopScript);
    }
    
        
    }


