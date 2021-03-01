/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: keila
 * 17/05/2017
 */
package pt.lsts.neptus.plugins.groovy;

import java.util.ArrayList;
import java.util.List;

import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.dsl.DSLPlan;
import pt.lsts.imc.dsl.Location;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.NameNormalizer;

/**
 * @author lsts
 *
 */
public class PlanScript extends DSLPlan {
    
    private PlanType neptusPlan;
    private ConsoleLayout neptusConsole;
    /**
     * @param id
     */
    public PlanScript(String id) {
        super(id);
        
    }
    
    public PlanScript(ConsoleLayout c){ //constructor to facilitate script
        super(NameNormalizer.getRandomID("IMCDSLPlan"));
        neptusConsole = c.getConsole();
        //Startup by default position: APDL
        locate( new Location(c.getMission().getHomeRef().getLatitudeRads(),c.getMission().getHomeRef().getLongitudeRads()));

    }
    
    public PlanScript(ConsoleLayout c,PlanType plan){
        super(plan.getId());
        neptusConsole = c.getConsole();
        neptusPlan = plan;
        super.setPlanSpec((PlanSpecification) plan.asIMCPlan(true));
        try {
            this.locate(convertToLocation(PlanUtil.getFirstLocation(plan)));
        }
        catch (Exception e) {
            NeptusLog.pub().error(I18n.text(" Error initializing the location in the IMC DSL."),e);
            //e.printStackTrace();
        }

    }
    
    
    /**
     * 
     * @param wp1 - start waypoint
     * @param wp2 - end waypoint
     * @param maxDist - popup distance
     * @param popup_waypoint - add popup at original waypoint besides the distance
     * @return
     */
    public static List<Location> midpoints(Location wp1,Location wp2,int maxDist,boolean popup_waypoint){
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
        if(popup_waypoint)
            result.add(wp2); //Last waypoint of the plan must be included
        return result;
    }
    
    public static List<Location> midpoints(Location wp1,Location wp2,int maxDist){
        return  midpoints(wp1,wp2,maxDist,true);
    }
    
    public static Location midpoint(Location wp1,Location wp2,double maxDist){
        
        double rad = (double)wp1.angle(wp2),lat,lg;
        //double degree = (double) Location.toDeg(rad);
        //degree= degree<0 ? degree+360: degree;//degree*2
        lat = Math.sin(rad)*maxDist;
        lg  = Math.cos(rad)*maxDist;
        Location l = (Location) wp1.translateBy(lg, lat); //displacements https://gis.stackexchange.com/questions/5821/calculating-latitude-longitude-x-miles-from-point
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
               return new PlanScript(neptusConsole,p);
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
               NeptusLog.pub().error(e.getMessage(), e);
                //e.printStackTrace();
                
            }
       return null;
    }
    
    public List<Location> waypoints(){ 
        //PlanUtils.planFromWaypoints(plan_id, lld_locations, speed, units);
        //lanUtils.trajectoryPlan(plan_id, lld_locations, speed, units);
        if(neptusConsole!=null){
            if(neptusPlan==null)
                neptusPlan = this.asPlanType(neptusConsole);
            else
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
            result.add(convertToLocation(loc));
            
        return result;
    }

    /**
     * @param loc
     * @return
     */
    public static Location convertToLocation(LocationType loc) {
        
        Location l = new Location(loc.getLatitudeRads(),loc.getLongitudeRads());
        l = (Location)l.translateBy(loc.getOffsetNorth(), loc.getOffsetEast());
        return l;
    }

    /**
     * @param console
     * @return
     */
    public PlanType asPlanType(ConsoleLayout console) {
        if(neptusPlan!=null)
            return neptusPlan;//IMCUtils.parsePlanSpecification(console.getMission(),IMCUtils.generatePlanSpecification(neptusPlan));
        else{
            PlanType p = IMCUtils.parsePlanSpecification(console.getMission(),this.asPlanSpecification()); //TODO validate generated IMCMessage
            for(String id: this.getVehicles_id())
                p.addVehicle(id);
            System.out.println("Original PlanSpecification:\n"+this.asPlanSpecification().asJSON());
            System.out.println("\n\n\n\nPlanType as IMCPlan:\n"+p.asIMCPlan(true));
           /* println "Plan Specification:\n"+ps.toString()
            println ps.asJSON()
            println ps.asXmlStripped(1,true)*/
            
            return p;
        }
        
    }
//    public Map<Double,String> getVehiclesRangeSorted(String [] avVehicles){
//        Map<Double,String> result = new HashMap<>();
//        for(String vehicle: avVehicles){
//            PlanCompatibility.availablePayloads(VehiclesHolder.getVehicleById(vehicle)).contains("Sidescan"); //TODO or Multibeam?
////            for(Maneuver m: plan.getGraph().getAllManeuvers()){
////                ManeuverPayloadConfig cfg = new ManeuverPayloadConfig(vehicle, m, null);
////                for (Property p : cfg.getProperties()) {
////                    if ("Active".equals(p.getName()) && Boolean.TRUE.equals(p.getValue())) {
////                        
////                    }
////                }
////            }
//        }
//        
//        return Collections.emptyMap();
//    }
//    
//    public Map<Double,String> getVehiclesRangeSorted(String payload,String [] avVehicles){
//        if(payload==null || payload.isEmpty())
//            return getVehiclesRangeSorted(avVehicles);
//        
//        Map<Double,String> result = new HashMap<>();
//        for(String vehicle: avVehicles){
//            PlanCompatibility.availablePayloads(VehiclesHolder.getVehicleById(vehicle)).contains(payload);
//        }
//        
//        return Collections.emptyMap();
//    }
//    
//    public List<Double> vehiclesEstimatedTime(String[] avVehicles,PlanType plan){
//        List<Double> result = new ArrayList<>();
//      //LocationType previousPos = ImcMsgManager.getManager().
//        for(String vehicle: avVehicles){
//            
//            ImcMsgManager.getManager().getState(ImcSystemsHolder.getSystemWithName(vehicle).getName()).last(VehicleState.class);
//            try {
//                result.add(Double.valueOf(PlanUtil.getEstimatedDelay(null, plan))); //calculates estimated time from plan initial location if previousPos = null
//            }
//            catch (Exception e) {
//                NeptusLog.pub().error("Error trying to estimate time spent by vehicle to cover area.\n",e);
//            }
//        }
//        
//        Collections.sort(result, new Comparator<Double>() {
//            @Override
//            public int compare(Double c1, Double c2) {
//                return Double.compare(c1.doubleValue(), c2.doubleValue());
//            }
//        });
//       
//        
//        //PlanUtil.estimatedTime(mans, speedRpmRatioSpeed, speedRpmRatioRpms);
//        //PlanUtil.getDelayStr(previousPos, plan);
//        
//        return result;
//    }

    /**
     * @param loc
     * @return
     */
    public static LocationType fromLocation(Location loc) {
        return new LocationType(Math.toDegrees(loc.getLatitude()), Math.toDegrees(loc.getLongitude()));
    }


}
