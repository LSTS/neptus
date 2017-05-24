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
 * 17/05/2017
 */
package pt.lsts.neptus.plugins.groovy;

import java.util.ArrayList;
import java.util.List;
import imc_plans_dsl.DSLPlan;
import imc_plans_dsl.Location;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author lsts
 *
 */
public class NeptusIMCDSL extends DSLPlan {
    
    private PlanType neptusPlan;
    private ConsoleLayout neptusConsole;
    /**
     * @param id
     */
    public NeptusIMCDSL(String id) {
        super(id);
    }
    
    public NeptusIMCDSL(ConsoleLayout c){ //constructor to facilitate script
        super("");
        neptusConsole = c.getConsole();
        //Startup by default position: APDL
        locate( new Location(c.getMission().getHomeRef().getLatitudeRads(),c.getMission().getHomeRef().getLongitudeRads()));

    }
    
    public NeptusIMCDSL(ConsoleLayout c,PlanType plan){
        super(plan.getId());
        neptusConsole = c.getConsole();
        neptusPlan = plan;
        try {
            this.locate(new Location(PlanUtil.getFirstLocation(plan).getLatitudeRads(),PlanUtil.getFirstLocation(plan).getLongitudeRads()));
        }
        catch (Exception e) {
            NeptusLog.pub().error(I18n.text(" Error initializing the location in the IMC DSL."),e);
            e.printStackTrace();
        }

    }
    
    public List<Location> midpoints(Location wp1,Location wp2,int maxDist){
        List<Location> result = new ArrayList<>();
            int j=1;
            double distance = (double) wp1.distance(wp2);
            //System.out.println("Distance= "+distance);
            if(distance>=maxDist){
                double aux = distance / maxDist;
                int n = Double.valueOf(aux).intValue() -1;
              //  System.out.println("N= "+n);
                while(n-->=0){
                    result.add(midpoint(wp1,wp2,j*maxDist));
                    j++;
                    
                }
                j=1;
            }
        
        result.add(wp2); //Last waypoint of the plan must be included?
        return result;
    }
    
    public Location midpoint(Location wp1,Location wp2,int maxDist){
        
        double rad = (double)wp1.angle(wp2),lat,lg;
        //double degree = (double) Location.toDeg(rad);
        //degree= degree<0 ? degree+360: degree;//degree*2
        lat = Math.sin(rad)*maxDist;
        lg  = Math.cos(rad)*maxDist;
        
        Location l = (Location) wp1.translateBy(lg, lat); //displacements https://gis.stackexchange.com/questions/5821/calculating-latitude-longitude-x-miles-from-point
        //System.out.println("ADDED new Location: "+l.toString());

        return l;
    }
        
    
    
    public void addToConsole(){
        
        if(neptusConsole!=null){
            if(neptusPlan!=null){
                neptusPlan.setMissionType(neptusConsole.getMission());
                neptusConsole.getMission().addPlan(neptusPlan);
        }
            else{
                neptusPlan=this.asPlanType(neptusConsole);
                neptusConsole.getConsole().getMission().addPlan(neptusPlan);
            }
            neptusConsole.getMission().save(true);
            neptusConsole.updateMissionListeners();
        }
    }
    
    public ConsoleLayout getNeptusConsole(){
       return neptusConsole;
    }

    public DSLPlan getPlan(String name){
        
        if(neptusConsole!=null){
           PlanType p = neptusConsole.getMission().getIndividualPlansList().get("name");
           if(p!=null)
               return new NeptusIMCDSL(neptusConsole,p);
        }
        return null;
    }
    
    public Location initialLocation() {
        if(neptusPlan!=null)
            try {
                
                LocationType loc = PlanUtil.getLocationsAsSequence(neptusPlan).firstElement().getStartLocation();//PlanUtil.getFirstLocation(neptusPlan);
                return new Location(loc.getLatitudeRads(),loc.getLongitudeRads());
                 
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                if(neptusConsole!=null)
                       neptusConsole.getMainPanel(); //TODO Warn user that plan does not exists on console
            }
       return null;
    }
    
    public List<Location> waypoints(){ 
        //PlanUtils.planFromWaypoints(plan_id, lld_locations, speed, units);
        //lanUtils.trajectoryPlan(plan_id, lld_locations, speed, units);
        if(neptusConsole!=null){
            if(neptusPlan==null)
                neptusPlan = this.asPlanType(neptusConsole);
            return waypointsToLocations(PlanUtil.getPlanWaypoints(neptusPlan));

         }
         return null;
                
    }

    /**
     * @param planWaypoints
     * @return
     */
    private List<Location> waypointsToLocations(ArrayList<ManeuverLocation> planWaypoints) {
        List<Location> result = new ArrayList<>();
        for(ManeuverLocation loc: planWaypoints)
            result.add(new Location(loc.getLatitudeRads(),loc.getLongitudeRads()));
            
        return result;
    }

    /**
     * @param console
     * @return
     */
    private PlanType asPlanType(ConsoleLayout console) {
        if(neptusPlan!=null)
            return IMCUtils.parsePlanSpecification(console.getMission(),IMCUtils.generatePlanSpecification(neptusPlan));
        else
            return IMCUtils.parsePlanSpecification(console.getMission(),this.asPlanSpecification());
                 
                  
            
        
//        PlanType plan = new PlanType(console.getConsole().getMission());
//        String previous = null;
//        for(PlanManeuver m: this.getMans()){
//            Maneuver man;// = Maneuver.createFromXML(m.getData().asXml(true));
//            Class<? extends Maneuver> clazz = getClass(m.getData().getClass().getSimpleName());
//            try {
//                
//                man = clazz.newInstance();
//                man.setId(m.getManeuverId());
//                IMCUtils.parseManeuver(m.getData());
//             //   man.loadManeuverXml(m.getData().asXml(false)); //TODO or false?
//                plan.getGraph().addManeuver(man);
//                if(man.isInitialManeuver())
//                    plan.getGraph().setInitialManeuver(man.getId());
//                else //TRANSITIONS
//                    plan.getGraph().addTransition(previous, man.getId(),man.getTransitionCondition(previous));//TODO verify this
//                previous=man.getId();
//            }
//            catch (InstantiationException | IllegalAccessException e) {
//                NeptusLog.pub().error(I18n.text("Illegal Maneuver Instantiation in the IMC DSL."),e);
//                //e.printStackTrace();
//            }
//
//        }
//        return plan;
    }

    /**
     * @param name
     * @return
     */
//    private Class<?extends Maneuver> getClass(String name) {
//        if(name.equalsIgnoreCase("Goto")){
//            return Goto.class;
//        }
//        if(name.equalsIgnoreCase("Loiter"))
//            return Loiter.class;
//        if(name.equalsIgnoreCase("YoYo"))
//            return YoYo.class;
//        if(name.equalsIgnoreCase("PopUp"))
//            return PopUp.class;
//        if(name.equalsIgnoreCase("Launch"))
//            return Launch.class;
//        if(name.equalsIgnoreCase("CompassCalibration"))
//            return CompassCalibration.class;
//        if(name.equalsIgnoreCase("StationKeeping"))
//            return StationKeeping.class;
//        if(name.equalsIgnoreCase("RowsManeuver"))
//            return RowsManeuver.class;
//        
//        return null;
//    }


}
