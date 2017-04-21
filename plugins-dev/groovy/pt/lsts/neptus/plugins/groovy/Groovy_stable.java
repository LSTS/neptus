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

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import com.google.common.eventbus.Subscribe;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventMissionChanged;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ImageUtils;



/**
 * @author lsts
 *
 */
@PluginDescription(name = "Groovy Feature", author = "Keila Lima")
@Popup(pos = POSITION.RIGHT, width=200, height=200)
@SuppressWarnings("serial")
public class Groovy_stable extends InteractionAdapter {

    private JButton openButton,stopScript;
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
        this.customizer = new ImportCustomizer();
        this.customizer.addImports("pt.lsts.imc.net.IMCProtocol","pt.lsts.imc.net.Consume","pt.lsts.neptus.types.coord.LocationType");
        this.customizer.addStarImports("pt.lsts.imc","pt.lsts.neptus.imc.dsl","pt.lsts.neptus.types.map"); //this.getClass().classLoader.rootLoader.addURL(new File("file.jar").toURL())
        this.config.addCompilationCustomizers(customizer);
        this.binds = new Binding();
        this.binds.setVariable("vehicles_id", vehicles.keySet().toArray());
        this.binds.setVariable("plans_id", plans.keySet().toArray());
        this.binds.setVariable("locations", locations.values().toArray());
        this.binds.setVariable("console", getConsole()); //TODO NOTIFY the existing binding to be used in the script
        this.binds.setVariable("result", null);
        try {
            //Description/notification: "Place your groovy scripts in the folder script of the plugin"
            this.engine = new GroovyScriptEngine("plugins-dev/groovy/pt/lsts/neptus/plugins/groovy/scripts/");
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
            //TODO from planqueue plugin 
//            if (pdbControl != null)
//                pdbControl.removeListener(planDBListener);

        }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        removeAll();
        add_console_vars();

        Action selectAction = new AbstractAction(I18n.text("Select Groovy Script")) {

            @Override
            public void actionPerformed(ActionEvent e) {    

                //Handle open button action.
                if (e.getSource() == openButton) {

                    //Create a file chooser
                    File directory = new File("plugins-dev/groovy/pt/lsts/neptus/plugins/groovy/scripts/");
                    final JFileChooser fc = new JFileChooser(directory);
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

                    int returnVal = fc.showOpenDialog(Groovy_stable.this);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        if(!stopScript.isEnabled()){
                            stopScript.setEnabled(true);
                        }
                        File groovy_script = fc.getSelectedFile();
                        post(Notification.info("Groovy Feature", "Opening: " + groovy_script.getName() + "." + "\n"));

                        thread = new Thread() {

                            @Override
                            public void run() {
                                
                                try {
                                    PrintStream output = new PrintStream(new FileOutputStream(new File("plugins-dev/groovy/pt/lsts/neptus/plugins/groovy/scripts/outputs/"+groovy_script.getName()+System.currentTimeMillis())));
                                    //PrintStream output = showOutput(groovy_script.getName());           
                                    getBinds().setProperty("out",output);
                                    //TODO result = output.toString();
                                    //getBinds().setVariable("result",output.toString());
                                    engine.run(groovy_script.getName(), binds);
                                    if(stopScript.isEnabled())
                                        stopScript.setEnabled(false);
                                }
                                
                                //System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out))); -> see http://stackoverflow.com/questions/5339499/resetting-standard-output-stream
                                //shell.getContext().getVariable()
                                
                              
                                catch (Exception   e) { //CompilationFailedException | ResourceException | ScriptException
                                    //TODO notify script exit
                                      NeptusLog.pub().error("Exception Caught during execution of script: "+groovy_script.getName(),e);
                                      if(thread.isAlive())
                                          thread.interrupt();
                                      if(stopScript.isEnabled())
                                          stopScript.setEnabled(false);
                                      //e.printStackTrace();
                                      }
                                  catch(ThreadDeath e){
                                      //TODO notify script exit
                                  }
                            }
                        };

                        thread.start();
                        
                    } 
                }
            }
        };

        Action stopAction = new AbstractAction(I18n.text("Stop Script"), ImageUtils.getScaledIcon("pt/lsts/neptus/plugins/groovy/images/stop.png", 10, 30)) {

            @Override
            public void actionPerformed(ActionEvent e) {

                if(e.getSource() == stopScript){
                    if(thread.isAlive()){
                      
                        thread.interrupt();
                        
                    }
                    stopScript.setEnabled(false);

                }

            }
        };

        openButton = new JButton(selectAction); //Button height: 22 Button width: 137 Button X: 30 Button Y: 5
        stopScript = new JButton(stopAction);

        add(openButton);
        add(stopScript);
        stopScript.setEnabled(false);
    }
    
    @Subscribe
    public void on(ConsoleEventMissionChanged new_mission) {
        if(!new_mission.getOld().equals(new_mission.getCurrent())){
            System.out.println("Got here some how");
         TreeMap<String, PlanType>  ps = new_mission.getCurrent().getIndividualPlansList();
         for( PlanType p: ps.values()){
             if(!plans.containsValue(p)){
                 plans.put(p.getId(),p);
                 System.out.println(p.getId()+" added.");
                 }
         }
         //Plan removed
         if(!plans.keySet().containsAll(new_mission.getCurrent().getIndividualPlansList().keySet()))
             for(String id: plans.keySet())
                 if(!ps.keySet().contains(id)){
                     plans.remove(id);
                     System.out.println(id+" removed.");
                 }
        }
    }
    
 /*   @Subscribe
    public void on(ConsoleEventPlanChange newPlan) {
            plans.put(newPlan.getCurrent().getId(), newPlan.getCurrent());
            //System.out.println("New Plan!! "+newPlan.getCurrent().getId());
            //TODO mission type change to check removed 
    }*/


    @Subscribe
    public void onVehicleStateChanged(ConsoleEventVehicleStateChanged e) {
        switch (e.getState()) {
            case SERVICE: //case CONNECTED
                if (ImcSystemsHolder.getSystemWithName(e.getVehicle()).isActive()) {
                    //add new vehicle
                    if(!vehicles.containsKey(e.getVehicle())) {
                        vehicles.put(e.getVehicle(),VehiclesHolder.getVehicleById(e.getVehicle()));
                        this.binds.setVariable("vehicles_id",vehicles.keySet().toArray()); //binds.vehicles=vehicles;
                        //System.out.println("Added "+e.getVehicle()+" Size: "+vehicles.keySet().size());
                    }
                }
                break;
            case ERROR:
                if(vehicles.containsKey(e.getVehicle())){
                    this.vehicles.remove(e.getVehicle());
                    this.binds.setVariable("vehicles_id",vehicles.keySet().toArray());
                    //System.out.println("Removed "+e.getVehicle()+" Size: "+vehicles.keySet().size());
                }
                break;
            case DISCONNECTED:
                if(vehicles.containsKey(e.getVehicle())){
                    this.vehicles.remove(e.getVehicle());
                    this.binds.setVariable("vehicles_id",vehicles.keySet().toArray());
                    //System.out.println("Removed "+e.getVehicle()+" Size: "+vehicles.keySet().size());
                }
                break;
            case CALIBRATION:// or case MANEUVER
                if(vehicles.containsKey(e.getVehicle())){
                    this.vehicles.remove(e.getVehicle());
                    this.binds.setVariable("vehicles_id",vehicles.keySet().toArray());
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

/*@Subscribe
public void onConsoleEventPlanChange(ConsoleEventPlanChange plan) {
    //switch(plan) {

    //}

}*/

// public void consume(PlanControlState pcstate) -> Plugin do Manuel para atualizar o estado do plano no veiculo

/* @Subscribe
public void on(EstimatedState msg) {

}*/    
//MapChangeListener listener = null;
// getConsole().getMission().getMapsList().values();
//getConsole().getMission().generateMapGroup().addChangeListener(listener);
