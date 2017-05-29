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
 * 26/05/2017
 */
package pt.lsts.neptus.plugins.groovy;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

/**
 * @author lsts
 *
 */
public class GroovyEngine {
    
    //Collections used to make Map thread safe
    private Map<String,VehicleType> vehicles = Collections.synchronizedMap(new HashMap<>()); 
    private Map<String,PlanType> plans = Collections.synchronizedMap(new HashMap<>()); 
    private Map<String,LocationType> locations = Collections.synchronizedMap(new HashMap<>());
    private Binding binds; //verify use of @TypeChecked
    private GroovyScriptEngine engine;
    private CompilerConfiguration config;
    private Thread runningThread;
    private ImportCustomizer customizer;
    private GroovyPanel console;
    private final Writer  scriptOutput;
    private  PrintWriter ps;
    public Thread getRunninThread(){
        return runningThread;
            
    }
    
    public GroovyEngine(GroovyPanel c){
        this.console = c;
        add_console_vars();
        scriptOutput = new Writer(){
//            @Override
//            public void write(int b) throws IOException {
//                System.out.println("Output: "+String.valueOf((char)b));
//                console.appendOutput(String.valueOf((char)b));
//            }

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                    //System.out.println("Char buffer: "+ String.valueOf(cbuf, off, len));
                    console.appendOutput(String.valueOf(cbuf, off, len));

                
            }

            @Override
            public void flush() throws IOException {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void close() throws IOException {
                // TODO Auto-generated method stub
                
            }
    };
    ps = new PrintWriter(scriptOutput);
        
  }
    
    /**
     * 
     */
    private void add_console_vars() {
        this.binds = new Binding();
        this.binds.setVariable("vehicles",ImcSystemsHolder.lookupActiveSystemVehicles());       
//        for(ImcSystem vec: ImcSystemsHolder.lookupActiveSystemVehicles())
//            this.vehicles.put(vec.getName(),VehiclesHolder.getVehicleById(vec.getName()));
        this.binds.setVariable("plans", console.getConsole().getMission().getIndividualPlansList());
      
     
        //POI/MarkElement
        for( MarkElement mark:MapGroup.getMapGroupInstance(console.getConsole().getMission()).getAllObjectsOfType(MarkElement.class)){
            locations.put(mark.getId(),mark.getPosition());
        }

        this.config = new CompilerConfiguration();
        this.customizer = new ImportCustomizer();
        this.customizer.addImports("pt.lsts.imc.net.IMCProtocol","pt.lsts.neptus.types.coord.LocationType");
        this.customizer.addImport("Plan", NeptusIMCDSL.class.getName());
        this.customizer.addStarImports("pt.lsts.imc","imc_plans_dsl","pt.lsts.neptus.types.map"); //this.getClass().classLoader.rootLoader.addURL(new File("file.jar").toURL())
        this.config.addCompilationCustomizers(customizer);
//        this.binds.setVariable("vehicles", vehicles.keySet().toArray());
//        this.binds.setVariable("plans", plans.keySet().toArray());
        this.binds.setVariable("locations", locations.values().toArray());
        this.binds.setVariable("console", console.getConsole()); //TODO NOTIFY the existing binding to be used in the script
        this.binds.setVariable("result", null);
        try {
            //Description/notification: "Place your groovy scripts in the folder script of the plugin"
            this.engine = new GroovyScriptEngine("conf/groovy/scripts/",console.getClass().getClassLoader());//new GroovyScriptEngine("conf/groovy/scripts/");
            
            this.engine.setConfig(this.config);

        }
        catch (IOException e) {
            //e.printStackTrace();
        }
    }
    
    public void stopScript() {
        if(runningThread != null && runningThread.isAlive()){
            runningThread.interrupt();
        }
        console.disableStopButton();
       
    }

    
    public void runScript(String groovyScript) {
        
        runningThread = new Thread() {

            @Override
            public void run() {
                try {
                    binds.setProperty("out",ps);
                    engine.run(groovyScript, binds);
                    
                }
                catch (Exception   e) { //CompilationFailedException | ResourceException | ScriptException
                      NeptusLog.pub().error("Exception Caught during execution of script: "+groovyScript,e);e.printStackTrace();
                      console.appendOutput("Error: \n\t"+e.getMessage());
                      console.disableStopButton();
                      stopScript();
                      
                      }
                  catch(ThreadDeath e){ 
                      NeptusLog.pub().info("Exiting script execution: "+groovyScript);
                  }
            }
        };
        runningThread.start();

    }

    /**
     * @param changedPlan
     */
    public void planChange(ConsoleEventPlanChange changedPlan) {
        if(changedPlan.getCurrent() == null){
            if(!console.getConsole().getMission().getIndividualPlansList().containsKey(changedPlan.getOld().getId())){
                //System.out.println("Plan "+changedPlan.getOld().getId()+" removed.");
                plans.remove(changedPlan.getOld().getId());
                binds.setVariable("plans", plans.keySet().toArray());

            }
            else{
                plans.put(changedPlan.getCurrent().getId(), changedPlan.getCurrent());
                binds.setVariable("plans", plans.keySet().toArray());
            }
        }
        
    }

    /**
     * @param e
     */
    public void vehicleStateChanged(ConsoleEventVehicleStateChanged e) {
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


        this.plans.put(planId, console.getConsole().getMission().getIndividualPlansList().get(planId));
    }
    
    /**
     * @return the plans
     */
    public HashMap<String, PlanType> getPlans() {
        return (HashMap<String, PlanType>) plans;
    }
    
    
 /*   @Subscribe
    public void on(PlanControlState pcs) {
        if(!pcs.getLastOutcome().equals(LAST_OUTCOME.FAILURE) && pcs.getState().equals(STATE.EXECUTING)){
            states.put(pcs.getPlanId(), pcs);
           
        }
        else if(pcs.getLastOutcome().equals(LAST_OUTCOME.SUCCESS)){
            states.remove(pcs.getPlanId());
            
        }
            
    }*/
}
