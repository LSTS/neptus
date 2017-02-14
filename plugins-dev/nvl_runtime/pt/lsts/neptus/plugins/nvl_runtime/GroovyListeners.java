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

import java.util.ArrayList;
import java.util.HashMap;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import com.google.common.eventbus.Subscribe;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBAdapter;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBState;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

/**
 * @author lsts
 *
 */

@SuppressWarnings("serial")
public class GroovyListeners extends InteractionAdapter {
    
    
    private HashMap<String,VehicleType> vehicles; 
    private HashMap<String,PlanType> plans; //PlanType listas planos
    private ArrayList<LocationType> locs;  //POI method that subscribe POI/MarkElement
    private Binding binds;
    
    private GroovyShell shell;
    private CompilerConfiguration config;
    private Thread thread;
    private ImportCustomizer customizer;

    /**
     * @param console
     */
    public GroovyListeners(ConsoleLayout console) {
        super(console);
        add_console_vars();
        
    }
    
void add_console_vars() {
        
        this.vehicles = new HashMap<String,VehicleType>();
        this.plans    = new HashMap<String,PlanType>();
        this.locs     = new ArrayList<LocationType>();

        //vehicles in the system before opening the plugin
        for(ImcSystem vec: ImcSystemsHolder.lookupActiveSystemVehicles())
               this.vehicles.put(vec.getName(),VehiclesHolder.getVehicleById(vec.getName()));
        this.plans.putAll(getConsole().getMission().getIndividualPlansList());
        System.out.println("Initial Vehicle size= "+vehicles.size());
        System.out.println("Initial Plans size= "+plans.size());

        this.binds = new Binding();
        this.binds.setVariable("vehicles_id", vehicles.keySet().toArray());
        this.binds.setVariable("plans_id", plans.keySet().toArray());
        this.binds.setVariable("locs", locs);
        System.out.println("Initialized all vars");
    }
    

    @Subscribe 
    public void on(EstimatedState state){
        
    }
    
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
    
    

    //Plan Database listener methods
    protected PlanDBAdapter planDBListener = new PlanDBAdapter() {
        @Override
        public void dbCleared() {
        }

        @Override
        public void dbInfoUpdated(PlanDBState updatedInfo) {
            
            String planId = updatedInfo.getLastChangeName();
            addPlan(planId);
            getBinds().setVariable("plans_id",getPlans().keySet().toArray());
            System.out.println("Added Plan: "+planId);
        }

        @Override
        public void dbPlanReceived(PlanType plan) {
            plan.setMissionType(getConsole().getMission());
            getConsole().getMission().addPlan(plan);
            getConsole().getMission().save(true);
            getConsole().updateMissionListeners();

        }

        @Override
        public void dbPlanRemoved(String planId) {
        }

        @Override
        public void dbPlanSent(String planId) {
        }
    };
    
    
/*    LocationType loc = new LocationType();
        loc.setLatitudeRads(msg.getLat());
        loc.setLongitudeRads(msg.getLon());
        loc.setOffsetNorth(msg.getX());
        loc.setOffsetEast(msg.getY());
        loc.convertToAbsoluteLatLonDepth();
        
        locs.add(loc);
        System.out.println("New POI location Added to Binds");
        binds.setVariable("locs", locs);

 * /
 
    
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
        return this.shell;
    }

    /**
     * @return the plans
     */
    public HashMap<String, PlanType> getPlans() {
        return plans;
    }

}
