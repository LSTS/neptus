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
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.ImageUtils;


@PluginDescription(name = "NVL Runtime Feature", author = "Keila Lima",icon="pt/lsts/neptus/plugins/nvl/images/tide.jpg")
@Popup(pos = Popup.POSITION.BOTTOM_RIGHT, width=600, height=500)
@SuppressWarnings("serial")
public class NVLConsolePanel extends ConsolePanel {
    
    private Border border;
    private JScrollPane outputPanel;
    private JTextArea output;
    private RSyntaxTextArea editor; 
    private File script;
    private JButton select,execButton,stop,saveFile;
    private Thread runningThread;
    RTextScrollPane scroll;
    
    public NVLConsolePanel(ConsoleLayout layout) {
        super(layout);
    }

   
    @Override
    public void initSubPanel() {
        NeptusPlatform.getInstance().associateTo(this);
        
        setLayout(new BorderLayout());
        editor = new RSyntaxTextArea();
        
        //Custom syntax highlight
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory)TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/nvl", "pt.lsts.neptus.plugins.nvl.HighlightSupport");
        editor.setSyntaxEditingStyle("text/nvl");
       // editor.setSyntaxEditingStyle(NVLHighlightSupport.SYNTAX_STYLE_GROOVY);
        editor.setCodeFoldingEnabled(true);
        editor.setPreferredSize(new Dimension(600, 300));
        scroll = new RTextScrollPane(editor);
        
        if (script != null) {
                 editor.setText(FileUtil.getFileAsString(script));    
        }
        
        Action saveAction = new AbstractAction(I18n.text("Save Script as"), ImageUtils.getScaledIcon("pt/lsts/neptus/plugins/nvl/images/save.png", 16, 16)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                File directory = new File("conf/nvl/");
                final JFileChooser fc = new JFileChooser(directory);
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.setFileFilter( new FileNameExtensionFilter("NVL files","nvl"));
                // Demonstrate "Save" dialog:
                int rVal = fc.showSaveDialog(NVLConsolePanel.this);
                if (rVal == JFileChooser.APPROVE_OPTION) {
                  script = fc.getSelectedFile();
                  FileUtil.saveToFile(script.getAbsolutePath(), editor.getText());
                }

            }
        };
                    
        Action selectAction = new AbstractAction(I18n.text("Script File..."), ImageUtils.getScaledIcon("pt/lsts/neptus/plugins/nvl/images/filenew.png", 16, 16)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                File directory = new File("conf/nvl/");
                final JFileChooser fc = new JFileChooser(directory);
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.setFileFilter( new FileNameExtensionFilter("NVL files","nvl"));
                int returnVal = fc.showOpenDialog(NVLConsolePanel.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    script = fc.getSelectedFile();
                    if(fc.getSelectedFile().exists()){
                        NeptusLog.pub().info("Opening: " + script.getName() + "." + "\n");
                        editor.setText(FileUtil.getFileAsString(script));
                    }
                    else {
                            try {
                                if(script.createNewFile()){
                                  
                                  editor.setText(FileUtil.getFileAsString(script));
                                  NeptusLog.pub().info("Creating new script file: " + script.getName() + "." + "\n");                          }
                          }
                            catch(IOException e1){
                                NeptusLog.pub().info("Error creating new script file.\n",e1);
                            }
                        
                    }
                }
            }
        };
        
        //Output panel
        output = new JTextArea();
        border = BorderFactory.createTitledBorder("Script Output");
        //output.setBorder(border);
        output.setEditable(false);
        output.setVisible(true);
        output.append("NLV Runtime Console\n");
        outputPanel = new JScrollPane(output);
        outputPanel.setBorder(border);
        
        
        Action execAction = new AbstractAction(I18n.text("Execute"),ImageUtils.getScaledIcon("pt/lsts/neptus/plugins/groovy/images/forward.png", 16, 16)) {
                    @Override
                    public void actionPerformed(ActionEvent e) {   
                        runningThread = new Thread(){
                           @Override
                           public void run() {
                            FileUtil.saveToFile(script.getAbsolutePath(), editor.getText());
                            NeptusPlatform.getInstance().run(script);
                            stop.setEnabled(false);
                           }
                        };
                        runningThread.start();
                        stop.setEnabled(true);
                    }
        };
        
        Action stopAction = new AbstractAction(I18n.text("Stop "),ImageUtils.getScaledIcon("pt/lsts/neptus/plugins/groovy/images/stop.png", 16, 16)) {
            @Override
            public void actionPerformed(ActionEvent e) {   
                if(runningThread!=null && runningThread.isAlive())
                    runningThread.interrupt();
                output.append("Stopping script "+script+" execution.");
                NeptusLog.pub().warn("Stopping script "+script+" execution.\n");
                stop.setEnabled(false);
            }
};
        //Buttons
        execButton = new JButton(execAction);
        select     = new JButton(selectAction);
        stop       = new JButton(stopAction);
        stop.setEnabled(false);
        saveFile    = new JButton(saveAction);
        
        JButton clear = new JButton(new AbstractAction(I18n.text("Clear Console")) {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                output.setText("");
                            
            }
        });
        
        //Console layout
        JPanel top = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel();
        buttons.add(select);
        buttons.add(saveFile);
        buttons.add(execButton);
        buttons.add(stop);
        top.setPreferredSize(new Dimension(600, 350));
        top.add(buttons,BorderLayout.SOUTH);
        top.add(scroll,BorderLayout.CENTER);

        
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setPreferredSize(new Dimension(600, 150));
        bottom.add(clear,BorderLayout.SOUTH);
        bottom.add(outputPanel,BorderLayout.CENTER);

        add(bottom,BorderLayout.SOUTH);
        add(top,BorderLayout.CENTER);
        
        
//        JProgressBar progressBar = new JProgressBar(SwingConstants.HORIZONTAL);
//        Border border = BorderFactory.createTitledBorder("Testing...");
//        progressBar.setValue(0); //TODO during task exec?
//        progressBar.setStringPainted(true);
//        progressBar.setBorder(border);
//        holder.add(progressBar,BorderLayout.SOUTH);



    }

    @Override
    public void cleanSubPanel() {
        NeptusPlatform.getInstance().detach();
        
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
