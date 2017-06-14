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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: keila
 * 09/01/2017
 */

package pt.lsts.neptus.plugins.groovy;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.OutputStream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.ImageUtils;



/**
 * @author lsts
 *
 */
@PluginDescription(name = "Groovy Feature", author = "Keila Lima",icon="pt/lsts/neptus/plugins/groovy/images/groovy.png")
@Popup(pos = POSITION.RIGHT, width=500, height=600)
@SuppressWarnings("serial")
public class GroovyPanel extends ConsolePanel {
    
    private GroovyEngine engine;
    private OutputStream scriptOutput;
    private JButton openButton,stopScript,runScript,clearOutput;
    private Border border;
    private JPanel bottom,buttons;
    private JScrollPane outputPanel;
    private JTextArea output;
    private RSyntaxTextArea editor; 
    
    @NeptusProperty
    File groovyScript = null;
    
    /**
     * @param console
     */
    public GroovyPanel(ConsoleLayout console) {
        super(console);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     * if script is still running, stops it
     */
    @Override
    public void cleanSubPanel() {
            try {
                if(engine.getRunninThread() != null && engine.getRunninThread().isAlive())
                    engine.getRunninThread().interrupt();
                if(scriptOutput!=null)
                    scriptOutput.close();
            }
            catch (Exception e1) {
                NeptusLog.pub().error(I18n.text("Error closing Groovy plugin."),e1);  
            }
            
        }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        removeAll();
        engine = new GroovyEngine(this);
        //Text editor
        setLayout(new BorderLayout(5,5));
        bottom = new JPanel(new BorderLayout());
        buttons = new JPanel();
        output = new JTextArea();
        
        editor = new RSyntaxTextArea();
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
        editor.setCodeFoldingEnabled(true);
        
        if (groovyScript != null) {
            try {
                editor.setText(FileUtil.getFileAsString(groovyScript));    
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                groovyScript = new File("conf/groovy/scripts/temp");
            }
        }
                    
        RTextScrollPane scroll = new RTextScrollPane(editor);
        
        Action selectAction = new AbstractAction(I18n.text("Script File..."), ImageUtils.getScaledIcon("pt/lsts/neptus/plugins/groovy/images/edit_new.png", 16, 16)) {

            @Override
            public void actionPerformed(ActionEvent e) {

                // Create a file chooser
                File directory = new File("conf/groovy/scripts/");
                final JFileChooser fc = new JFileChooser(directory);
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int returnVal = fc.showOpenDialog(GroovyPanel.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    groovyScript = fc.getSelectedFile();
                    NeptusLog.pub().info("Opening: " + groovyScript.getName() + "." + "\n");
                    editor.setText(FileUtil.getFileAsString(groovyScript));
                }
            }
        };
        
        Action stopAction = new AbstractAction(I18n.text("Stop Script"), ImageUtils.getScaledIcon("pt/lsts/neptus/plugins/groovy/images/stop.png", 16, 16)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                engine.stopScript();                
                stopScript.setEnabled(false);
            }
        };
        
        Action runAction = new AbstractAction(I18n.text("Execute Script"), ImageUtils.getScaledIcon("pt/lsts/neptus/plugins/groovy/images/forward.png", 16, 16)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                output.setText("");
                FileUtil.saveToFile(groovyScript.getAbsolutePath(), editor.getText());
                if(!stopScript.isEnabled())
                    stopScript.setEnabled(true);
                engine.runScript(groovyScript.getName());
            }
        };
        
        Action clearAction =  new AbstractAction(I18n.text("Clear Output")){
            @Override
            public void actionPerformed(ActionEvent e) {
                output.setText("");
             }
        };
        
        openButton  = new JButton(selectAction); 
        stopScript  = new JButton(stopAction);
        runScript   = new JButton(runAction);
        clearOutput = new JButton(clearAction);
        
        buttons.add(openButton);
        buttons.add(runScript);
        buttons.add(stopScript);
        buttons.add(clearOutput);
        
        bottom.add(buttons);
        border = BorderFactory.createTitledBorder("Script Output");
        
        //output.setBorder(border);
        output.setEditable(false);
        output.setVisible(true);
//        output.setBackground(Color.BLACK);
        
        output.append("OUTPUT GOES HERE \n");
        outputPanel = new JScrollPane(output);//RSyntaxTextArea("Script Output")
        outputPanel.setPreferredSize(new Dimension(500, 100));
        outputPanel.setBorder(border);
        bottom.add(outputPanel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        add(scroll, BorderLayout.CENTER);
        stopScript.setEnabled(false);
        output.setVisible(true);
}

    /**
     * @param string
     */
    public void appendOutput(String string) {
//        Thread t = new Thread(){
//            @Override
//            public void run(){
            SwingUtilities.invokeLater(new Runnable(){
                public void run(){    
                    try {
                        //System.out.println("\"writeToArea\" running.");
                        output.getDocument().insertString(output.getDocument().getLength(), string, null);
                        output.setCaretPosition(output.getDocument().getLength());
                        //System.out.println("appended"+"\t"+string);
                    }
                    catch (BadLocationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                  }
                    });
//                }
//        };
//    t.start();
    }

 /*   @Subscribe
    public void on(ConsoleEventPlanChange changedPlan) {
        engine.planChange(changedPlan);
    }*/

    @Subscribe
    public void onVehicleStateChanged(ConsoleEventVehicleStateChanged e) {
        engine.vehicleStateChanged(e);
    }

    /**
     * 
     */
    public void disableStopButton() {
        if(stopScript.isEnabled())
            stopScript.setEnabled(false);        
    }

}