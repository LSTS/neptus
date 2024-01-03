/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: 
 * 15/Jan/2005
 */
package pt.lsts.neptus.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.CoordinateSystemsHolder;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.mission.MapMission;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.TemplateFileVehicle;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.xsl.TransformDocument;

/**
 * @author Paulo Dias
 *
 */
public class TestMissionLoad
{

    public void run ()
    {
        //setup
        ConfigFetch.initialize();
        
        CoordinateSystemsHolder csh = 
            new CoordinateSystemsHolder(ConfigFetch.getCoordinateSystemsConfigLocation());
        NeptusLog.pub().debug("CoordinateSystems #: " + csh.size());
        
        LinkedList<?> vl = ConfigFetch.getVehiclesList();
        //VehiclesHolder vehiclesHolder = new VehiclesHolder();
        ListIterator<?> li = vl.listIterator();
        while (li.hasNext())
        {
            String hr = (String) li.next();
            VehicleType v = new VehicleType(ConfigFetch.resolvePath(hr));
            if (v.getCoordinateSystem() == null)
            {
                CoordinateSystem cs = (CoordinateSystem) csh
                        .getCoordinateSystemList().get(
                                v.getCoordinateSystemLabel());
                if (cs != null)
                    v.setCoordinateSystem(cs);
            }
            VehiclesHolder.addVehicle(v);
        }
        NeptusLog.pub().debug("Vehicles #: " + VehiclesHolder.size());
        
        
        //load the mission to memory
        MissionType mission = new MissionType(
                ConfigFetch.resolvePath("missions/isurus-mission-teste-lsts.xml"));
        NeptusLog.pub().debug("Mission::Id: " +
                mission.getId());
        NeptusLog.pub().debug("Mission::Name: " +
                mission.getName());
        NeptusLog.pub().debug("Mission::Type: " +
                mission.getType());
        NeptusLog.pub().debug("Mission::Description: " +
                mission.getDescription());
        NeptusLog.pub().debug("Mission::Notes #: " +
                mission.getNotesList().size());

        NeptusLog.pub().debug("Mission::Vehicles #: " +
                mission.getVehiclesList().size());
        NeptusLog.pub().debug("Mission::Maps #: " +
                mission.getMapsList().size());
        NeptusLog.pub().debug("Mission::HomeRef Id: " +
                mission.getHomeRef().getId());
        
        /*
        LinkedHashMap mlist = mission.getMapsList();
        Collection mcol = mlist.values();
        Iterator it = mcol.iterator();
        while (it.hasNext())
        {
            MapMission mm = (MapMission) it.next();
            //mm.loadMap(ConfigFetch.resolvePath(mm.getHref()));
            mm.loadMap();
            //mm.getMap().
        }
        */
       
        //proccess simple mission
        VehicleType vehicleM;
        PlanType indPlan = 
            (PlanType) mission.getIndividualPlansList()
            	.values().iterator().next();
        String v = indPlan.getVehicle();
        vehicleM = (VehicleType) VehiclesHolder.getVehiclesList().get(v);
        
        String maneuversXML = indPlan.getGraph().getGraphAsManeuversSeq();
        
        TemplateFileVehicle tfile = (TemplateFileVehicle) vehicleM
        								.getTransformationXSLTTemplates()
        									.values().iterator().next();
        String xsltToPass = tfile.getHref();
        xsltToPass = ConfigFetch.resolvePath(xsltToPass);
        
        String outToPass = tfile.getOutputFileName();
        
        NeptusLog.pub().debug("Mission::XSLT: " +
                xsltToPass);
        NeptusLog.pub().debug("Mission::OUT: " +
                outToPass);
        
        Hashtable<String, String> styleSheetParam = new Hashtable<String, String>();
        styleSheetParam.put("vehicle-file", 
                new File(vehicleM.getOriginalFilePath()).toURI().toASCIIString());
        styleSheetParam.put("mission-file",
                new File(mission.getOriginalFilePath()).toURI().toASCIIString());
        
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(maneuversXML.getBytes());
        
        TransformDocument processor = new TransformDocument();

		processor.setDebug (true);
		
		FileUtil.backupFile(outToPass);
		
		processor.doTransformation(
		        processor.createStreamSource(bais),
		        processor.createStreamSource(xsltToPass),
		        processor.createStreamResult(outToPass),
		        styleSheetParam);
		
		NeptusLog.pub().debug("Mission as XML:\n" 
		        + FileUtil.getAsPrettyPrintFormatedXMLString(mission.asDocument()));
		
		if (VehiclesHolder.size() > 0)
		{
			Iterator<?> iter = VehiclesHolder.getVehiclesList().values().iterator();
		    while (iter.hasNext())
            {
                VehicleType ve = (VehicleType) iter.next();
                NeptusLog.pub().debug("Vehicle "
                        + ve.getId()
                        + " as XML:\n"
                        + FileUtil.getAsPrettyPrintFormatedXMLString(ve
                                .asDocument()));
		    }
		        
		}
		
        Iterator<?> it = mission.getMapsList().values().iterator();
        while (it.hasNext())
        {
            MapType mp = ((MapMission) it.next()).getMap();
            NeptusLog.pub().debug("Map "
                    + mp.getId()
                    + " as XML:\n"
                    + FileUtil.getAsPrettyPrintFormatedXMLString(mp
                            .asDocument()));
        }

    }
    
    public static void main(String[] args)
    {
        TestMissionLoad tr = new TestMissionLoad();
        tr.run();
        NeptusLog.pub().info("<###> "+System.getProperty("user.name", "unknown"));
        NeptusLog.pub().info("<###> "+System.getProperty("user.home", "."));
        NeptusLog.pub().info("<###> "+System.getProperty("user.dir", "."));
    }
}
