/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 15/Jan/2005
 * $Id:: TestMissionLoad.java 9616 2012-12-30 23:23:22Z pdias             $:
 */
package pt.up.fe.dceg.neptus.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.types.coord.CoordinateSystemsHolder;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.mission.MapMission;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.types.vehicle.TemplateFileVehicle;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
import pt.up.fe.dceg.neptus.util.xsl.TransformDocument;

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
        System.out.println(System.getProperty("user.name", "unknown"));
        System.out.println(System.getProperty("user.home", "."));
        System.out.println(System.getProperty("user.dir", "."));
    }
}
