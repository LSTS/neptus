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
 * Author: lsts
 * 09/01/2017
 */

package pt.lsts.neptus.plugins.groovy;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.google.common.eventbus.Subscribe;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.ImageUtils;



/**
 * @author lsts
 *
 */
@PluginDescription(name = "Groovy Feature", author = "Keila Lima")
@Popup(pos = POSITION.RIGHT, width=500, height=500, accelerator='y')
@SuppressWarnings("serial")
public class Groovy_stable extends ConsolePanel {

    private JButton openButton,stopScript,runScript;
    //Collections used to make Map thread safe
    private Map<String,VehicleType> vehicles = Collections.synchronizedMap(new HashMap<>()); 
    private Map<String,PlanType> plans = Collections.synchronizedMap(new HashMap<>()); 
    private Map<String,LocationType> locations = Collections.synchronizedMap(new HashMap<>());
    private Optional<String>  result; //bindig variable to process output in the script
    private Binding binds; //verify use of @TypeChecked
    private GroovyScriptEngine engine;
    private CompilerConfiguration config;
    private Thread thread;
    private ImportCustomizer customizer;
    private final String basescript = "new Plan(console).with{ \nlocate new Location(console.getMission().getHomeRef().getLatitudeRads(),console.getMission().getHomeRef().getLongitudeRads())\n";
    private RSyntaxTextArea editor; 
    
    @NeptusProperty
    File groovyScript = null;
    /**
     * @param console
     */
    public Groovy_stable(ConsoleLayout console) {
        super(console);
        
    }

    void add_console_vars() {
               
        for(ImcSystem vec: ImcSystemsHolder.lookupActiveSystemVehicles())
            this.vehicles.put(vec.getName(),VehiclesHolder.getVehicleById(vec.getName()));
        
        this.plans.putAll(getConsole().getMission().getIndividualPlansList());
       
        //POI/MarkElement
        for( MarkElement mark:MapGroup.getMapGroupInstance(getConsole().getMission()).getAllObjectsOfType(MarkElement.class)){
            locations.put(mark.getId(),mark.getPosition());
        }
        
        this.config = new CompilerConfiguration();
        //this.config.setScriptBaseClass("plugins-dev/groovy/pt/lsts/neptus/plugins/groovy/scripts/basescript");
        this.customizer = new ImportCustomizer();
        this.customizer.addImports("pt.lsts.imc.net.IMCProtocol","pt.lsts.imc.net.Consume","pt.lsts.neptus.types.coord.LocationType","pt.lsts.neptus.imc.dsl.Plan","pt.lsts.neptus.imc.dsl.Location");
        this.customizer.addStarImports("pt.lsts.imc","pt.lsts.neptus.imc.dsl","pt.lsts.neptus.types.map"); //this.getClass().classLoader.rootLoader.addURL(new File("file.jar").toURL())
        this.config.addCompilationCustomizers(customizer);
        this.binds = new Binding();
        this.binds.setVariable("vehicles", vehicles.keySet().toArray());
        this.binds.setVariable("plans", plans.keySet().toArray());
        this.binds.setVariable("locations", locations.values().toArray());
        this.binds.setVariable("console", getConsole()); //TODO NOTIFY the existing binding to be used in the script
        this.binds.setVariable("result", null);
        try {
            //Description/notification: "Place your groovy scripts in the folder script of the plugin"
            this.engine = new GroovyScriptEngine("conf/groovy/scripts/");
            this.engine.setConfig(this.config);

        }
        catch (IOException e) {
            //e.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     * if script is still running, stops it
     */
    @Override
    public void cleanSubPanel() {
            try {
                if(thread.isAlive() && thread != null)
                    thread.interrupt();
            }
            catch (Exception e1) {
                //e1.printStackTrace();
            }
        }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        removeAll();
        add_console_vars();
        
        //Text editor
        setLayout(new BorderLayout());
        JPanel bottom = new JPanel();
        editor = new RSyntaxTextArea();
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
        editor.setCodeFoldingEnabled(true);
        
        if (groovyScript != null) {
            try {
                editor.setText(FileUtil.getFileAsString(groovyScript));    
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                groovyScript = null;
            }
        }
                    
        RTextScrollPane scroll = new RTextScrollPane(editor);
        
        Action selectAction = new AbstractAction(I18n.text("Script File..."), ImageUtils.getScaledIcon("pt/lsts/neptus/plugins/groovy/images/filenew.png", 16, 16)) {

            @Override
            public void actionPerformed(ActionEvent e) {

                // Create a file chooser
                File directory = new File("conf/groovy/scripts/");
                final JFileChooser fc = new JFileChooser(directory);
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int returnVal = fc.showOpenDialog(Groovy_stable.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    groovyScript = fc.getSelectedFile();
                    Notification.info("Groovy Feature", "Opening: " + groovyScript.getName() + "." + "\n");
                    editor.setText(FileUtil.getFileAsString(groovyScript));
                }
            }
        };
        
        Action stopAction = new AbstractAction(I18n.text("Stop Script"), ImageUtils.getScaledIcon("pt/lsts/neptus/plugins/groovy/images/stop.png", 16, 16)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopScript();                
            }
        };
        
        Action runAction = new AbstractAction(I18n.text("Execute Script"), ImageUtils.getScaledIcon("pt/lsts/neptus/plugins/groovy/images/forward.png", 16, 16)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                runScript();           
            }
        };
        
        openButton = new JButton(selectAction); 
        stopScript = new JButton(stopAction);
        runScript = new JButton(runAction);

        bottom.add(openButton);
        bottom.add(runScript);
        bottom.add(stopScript);
        
        add(bottom, BorderLayout.SOUTH);
        add(scroll, BorderLayout.CENTER);
        stopScript.setEnabled(false);
        
    }
    private void writeScriptContent(BufferedWriter fileW, File groovy_script) {
        BufferedReader fileR;
        try {
            fileR = new BufferedReader(new FileReader(groovy_script));
            int c;
            while((c = fileR.read())!=-1) {
                fileW.write(c);
            }
            fileR.close();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            NeptusLog.pub().error(I18n.text("Error generating script from: "+groovy_script.getName()), e);
            //e.printStackTrace();
        }
    }
    
    private void stopScript() {
        if(thread != null && thread.isAlive()){
            thread.interrupt();                        
        }
        stopScript.setEnabled(false);
    }
    
    private void runScript() {
        thread = new Thread() {

            @Override
            public void run() {
                try {
//                    System.out.println("Exec da script!");
//                    FileUtil.saveToFile(groovyScript.getAbsolutePath(), editor.getText());
//                    Object output = engine.run(groovyScript.getName(), binds);
//                    if(stopScript.isEnabled())
//                        stopScript.setEnabled(false);
                    
                    String name = groovyScript.getName()+System.currentTimeMillis();
                    PrintStream output = new PrintStream(new FileOutputStream(new File("plugins-dev/groovy/pt/lsts/neptus/plugins/groovy/outputs/"+name)));
                    getBinds().setProperty("out",output);
                    //getBinds().setVariable("result",output.toString());
                    File generatedScript = new File("conf/groovy/scripts/"+name);
                    BufferedWriter fileW = new BufferedWriter(new FileWriter(generatedScript));
                    fileW.write(basescript);
                    writeScriptContent(fileW,groovyScript);
                    fileW.write("\n}");
                    fileW.close();
                    engine.run(generatedScript.getName(), binds);
                    if(stopScript.isEnabled())
                        stopScript.setEnabled(false);
                    generatedScript.delete();
                   

                }
                catch (Exception   e) { //CompilationFailedException | ResourceException | ScriptException
                    //TODO notify script exit
                      NeptusLog.pub().error("Exception Caught during execution of script: "+groovyScript.getName(),e);
                      if(thread.isAlive())
                          thread.interrupt();
                      if(stopScript.isEnabled())
                          stopScript.setEnabled(false);

                      }
                  catch(ThreadDeath e){ 
                      NeptusLog.pub().info("Exiting script execution: "+groovyScript.getName());
                  }
            }
        };

        thread.start();
    }
    

    @Subscribe
    public void on(ConsoleEventPlanChange changedPlan) {
        if(changedPlan.getCurrent() == null){
            if(!this.getConsole().getMission().getIndividualPlansList().containsKey(changedPlan.getOld().getId())){
                //System.out.println("Plan "+changedPlan.getOld().getId()+" removed.");
                plans.remove(changedPlan.getOld().getId());
                binds.setVariable("plans", plans.keySet().toArray());

            }
            else{
                plans.put(changedPlan.getCurrent().getId(), changedPlan.getCurrent());
                binds.setVariable("plans", plans.keySet().toArray());
                //System.out.println("New Plan!! "+changedPlan.getCurrent().getId());
                
            }
        }
    }


    @Subscribe
    public void onVehicleStateChanged(ConsoleEventVehicleStateChanged e) {
        switch (e.getState()) {
            case SERVICE: //case CONNECTED
                if (ImcSystemsHolder.getSystemWithName(e.getVehicle()).isActive()) {
                    //add new vehicle
                    if(!vehicles.containsKey(e.getVehicle())) {
                        vehicles.put(e.getVehicle(),VehiclesHolder.getVehicleById(e.getVehicle()));
                        binds.setVariable("vehicles", vehicles.keySet().toArray());
                        //System.out.println("Added "+e.getVehicle()+" Size: "+vehicles.keySet().size());
                    }
                }
                break;
            case ERROR:
                if(vehicles.containsKey(e.getVehicle())){
                    vehicles.remove(e.getVehicle());
                    binds.setVariable("vehicles", vehicles.keySet().toArray());
                    //System.out.println("Removed "+e.getVehicle()+" Size: "+vehicles.keySet().size());
                }
                break;
            case DISCONNECTED:
                if(vehicles.containsKey(e.getVehicle())){
                    vehicles.remove(e.getVehicle());
                    binds.setVariable("vehicles", vehicles.keySet().toArray());
                    //System.out.println("Removed "+e.getVehicle()+" Size: "+vehicles.keySet().size());
                }
                break;
            case CALIBRATION:// or case MANEUVER
                if(vehicles.containsKey(e.getVehicle())){
                    vehicles.remove(e.getVehicle());
                    binds.setVariable("vehicles", vehicles.keySet().toArray());
                    //System.out.println("Removed "+e.getVehicle()+" Size: "+vehicles.keySet().size());
                }
                break;

            default:
                break;
        }
    }
    /**
     * Add new updated plan to bind variable
     * @param planId of the plan
     */
    public void addPlan(String planId) {


        this.plans.put(planId, getConsole().getMission().getIndividualPlansList().get(planId));
    }

    /**
     * @return the binds
     */
    public Binding getBinds() {
        return binds;
    }

    /**
     * @return the plans
     */
    public HashMap<String, PlanType> getPlans() {
        return (HashMap<String, PlanType>) plans;
    }
}