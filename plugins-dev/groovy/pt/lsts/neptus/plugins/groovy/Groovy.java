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
import java.io.IOException;
import java.util.HashMap;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import com.google.common.eventbus.Subscribe;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import pt.lsts.imc.PlanControlState;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
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
@Popup(pos = POSITION.RIGHT, width=200, height=200, accelerator='y')
@SuppressWarnings("serial")
public class Groovy extends InteractionAdapter {
       
    
    private JButton openButton,stopScript;
    private HashMap<String,VehicleType> vehicles; 
    private HashMap<String,PlanType> plans; //PlanType listas planos
    private HashMap<String,LocationType> locs;
    private Binding binds; //verify use of @TypeChecked
    private GroovyShell shell;
    private GroovyScriptEngine engine;
    private CompilerConfiguration config;
    private Thread thread;
    private ImportCustomizer customizer;
    
    /**
     * @param console
     */
    public Groovy(ConsoleLayout console) {
        super(console);
        //init_vars();
    }

    void add_console_vars() {
    	
        this.vehicles = new HashMap<String,VehicleType>();
        this.plans    = new HashMap<String,PlanType>();
        this.locs     = new HashMap<String,LocationType>();

      //vehicles in the system before opening the plugin
        for(ImcSystem vec: ImcSystemsHolder.lookupActiveSystemVehicles())
               this.vehicles.put(vec.getName(),VehiclesHolder.getVehicleById(vec.getName()));
        
        this.plans.putAll(getConsole().getMission().getIndividualPlansList());

        //POI/MarkElement
        for( MarkElement mark:MapGroup.getMapGroupInstance(getConsole().getMission()).getAllObjectsOfType(MarkElement.class)){
            locs.put(mark.getId(),mark.getPosition());
        //this.locs.putAll((Map<? extends String, ? extends LocationType>) );
        }
        
        System.out.println("Initial Vehicle size= "+vehicles.size());
        System.out.println("Initial Plans size= "+plans.size());
        System.out.println("Initial Locations size= "+locs.size());

        this.config = new CompilerConfiguration();
        this.customizer = new ImportCustomizer();
        this.customizer.addImports("pt.lsts.imc.net.IMCProtocol","pt.lsts.imc.net.Consume"); // in classpath
        this.customizer.addStarImports("pt.lsts.imc");
        this.config.addCompilationCustomizers(customizer);
        this.binds = new Binding();
        this.binds.setVariable("vehicles_id", vehicles.keySet().toArray());
        this.binds.setVariable("plans_id", plans.keySet().toArray());
        this.binds.setVariable("locs", locs.keySet().toArray());//?
        //this.config.setScriptBaseClass("Basescript");
        this.shell = new GroovyShell(this.binds,this.config);
        try {
          //Description/notification: "Place your groovy scripts in the folder script of the plugin"

            this.engine = new GroovyScriptEngine("plugins-dev/groovy/pt/lsts/neptus/plugins/groovy/scripts/");
            this.engine.setConfig(config);
            
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("Initialized all vars");
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
        add_console_vars();
        

        
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
                    int returnVal = fc.showOpenDialog(Groovy.this);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File groovy_script = fc.getSelectedFile();
                        
                        System.out.println("Opening: " + groovy_script.getName() + "." + "\n");
                        
                        
                                
                                //shell.evaluate(groovy_script); //shell.getContext().getVariable();
                                thread = new Thread() {
                                    
                                   @Override
                                    public void run() {
                                          
                                        
                                        try {
                                            
                                            Object output = engine.run(groovy_script.getName(), binds);
                                            //getShell().run(groovy_script,args);  String[] args = null;
                                            //shell.evaluate(initScripts + groovy_script)
                                            //engine.loadScriptByName
                                        }
                                        catch (CompilationFailedException | ResourceException | ScriptException e) {
                                            e.printStackTrace();
                                        }
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
                                thread.interrupt();//thread.stop();
                                //shell.invokeMethod("System.exit", "0"); Stops whole app -> JVM
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
    
    
    /*@Subscribe
    public void onConsoleEventPlanChange(ConsoleEventPlanChange plan) {
        //switch(plan) {
            
        //}
        
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
                        System.out.println("Added "+e.getVehicle()+" Size: "+vehicles.keySet().size());
                        
                    }
                }
                break;
            case ERROR:
                System.out.println(e.getVehicle()+" IN ERROR");
                if(vehicles.containsKey(e.getVehicle())){
                    this.vehicles.remove(e.getVehicle());
                    this.binds.setVariable("vehicles_id",vehicles.keySet().toArray());
                    System.out.println("Removed "+e.getVehicle()+" Size: "+vehicles.keySet().size());
                }
                break;
            case DISCONNECTED:
                System.out.println(e.getVehicle()+" DISCONNECTED");
                if(vehicles.containsKey(e.getVehicle())){
                    this.vehicles.remove(e.getVehicle());
                    this.binds.setVariable("vehicles_id",vehicles.keySet().toArray());
                    System.out.println("Removed "+e.getVehicle()+" Size: "+vehicles.keySet().size());
                }
                break;
           case CALIBRATION:// or case MANEUVER
               if(vehicles.containsKey(e.getVehicle())){
                   this.vehicles.remove(e.getVehicle());
                   this.binds.setVariable("vehicles_id",vehicles.keySet().toArray());
                   System.out.println("Removed "+e.getVehicle()+" Size: "+vehicles.keySet().size());
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
     * Add new  plan to console mission
     * @param planId of the plan
     */
    public void addPlanToConsole(PlanType plan) {
        plan.setMissionType(getConsole().getMission());
        getConsole().getMission().getIndividualPlansList().put(plan.getId(), plan);
        getConsole().getMission().save(false);
        getConsole().warnMissionListeners();
        getConsole().getMission().addPlan(plan);
    }

    /**
     * @return the binds
     */
    public Binding getBinds() {
        return binds;
    }

    /**
     * @return the shell
     */
    public GroovyShell getShell() {
        return shell;
    }

    /**
     * @return the plans
     */
    public HashMap<String, PlanType> getPlans() {
        return plans;
    }


        
}

/*Plan Database listener methods

protected PlanDBAdapter planDBListener = new PlanDBAdapter() {
    @Override
    public void dbCleared() {
    }


    public void dbInfoUpdated(PlanDBState updatedInfo) {
        
        String planId = updatedInfo.getChangeSname();
        addPlan(planId);
        getBinds().setVariable("plans_id",getPlans().keySet().toArray());
        System.out.println("1.Added Plan: "+planId);
    }

    @Override
    public void dbPlanReceived(PlanType plan) {
        plan.setMissionType(getConsole().getMission());
        getConsole().getMission().addPlan(plan);
        getConsole().getMission().save(true);
        getConsole().updateMissionListeners();
        addPlan(plan.getId());
        getBinds().setVariable("plans_id",getPlans().keySet().toArray());
        System.out.println("2.Added Plan: "+plan.getId());

    }

    @Override
    public void dbPlanRemoved(String planId) {
        System.out.println("dbPlanRemoved");
    }

    @Override
    public void dbPlanSent(String planId) {
        System.out.println("dbPlanSent");
    }
};



*/

// public void consume(PlanControlState pcstate) -> Plugin do Manuel para atualizar o estado do plano no veiculo

/* @Subscribe
public void on(EstimatedState msg) {

}*/    
//MapChangeListener listener = null;
//TODO getConsole().getMission().getMapsList().values();
//getConsole().getMission().generateMapGroup().addChangeListener(listener);
