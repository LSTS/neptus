/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Apr 22, 2010
 * $Id:: PlanCreator.java 9615 2012-12-30 23:08:28Z pdias                       $:
 */
package pt.up.fe.dceg.neptus.mp.templates;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation;
import pt.up.fe.dceg.neptus.mp.maneuvers.Goto;
import pt.up.fe.dceg.neptus.mp.maneuvers.LocatedManeuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.Loiter;
import pt.up.fe.dceg.neptus.mp.maneuvers.PopUp;
import pt.up.fe.dceg.neptus.mp.maneuvers.Unconstrained;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.ReflectionUtil;

/**
 * @author zp
 *
 */
public 	
class PlanCreator {
	protected PlanType plan;
	protected ManeuverLocation loc = new ManeuverLocation();
	protected int count = 1;
	
	private LinkedHashMap<String, Class<?>> maneuvers = new LinkedHashMap<String, Class<?>>();
	{
		Class<?> mans[] = ReflectionUtil.listManeuvers();
		for (Class<?> c : mans) {
			maneuvers.put(c.getSimpleName().toLowerCase(), c);
			//System.out.println("Maneuver: "+c.getSimpleName().toLowerCase());
		}			
	}
	
	public PlanCreator(MissionType mission) {
		plan = new PlanType(mission);
	}
	
	public void setDepth(double depth) {
	    setZ(depth, ManeuverLocation.Z_UNITS.DEPTH);
	}
	
	public void setZ(double z, ManeuverLocation.Z_UNITS units) {
	    loc.setZ(z);
	    loc.setZUnits(units);
	}
	
	public void move(double north, double east) {
		loc.translatePosition(north, east, 0);
	}
	
	public void move(double north, double east, double down) {
	    loc.translatePosition(north, east, down);
    }
	
	public void setLocation(LocationType loc) {
		if (loc != null)
			this.loc.setLocation(loc);
	}
	
	public void addTransition(String sourceManeuver, String targetManeuver, String condition) {
	    plan.getGraph().addTransition(sourceManeuver, targetManeuver, condition);
	}
	
	public String addManeuver(String name, Map<Object, Object> obj) {

		LinkedHashMap<String, Object> props = new LinkedHashMap<String, Object>();
		
		for (Object o : obj.keySet())
			props.put(o.toString(), obj.get(o.toString()));
			
		return addManeuver(name, props);	
	}
	
	public String addManeuver(String name, Object...values) {
		LinkedHashMap<String, Object> properties = new LinkedHashMap<String, Object>();
		
		for (int i = 0; i < values.length; i+= 2) {
			
			if (values[i] instanceof String && values[i+1] != null)
				properties.put(values[i].toString(), values[i+1]);
		}
		
		return addManeuver(name, properties);
	}
	
	public String addManeuver(String name) {
		return addManeuver(name, new LinkedHashMap<String, Object>());
	}
		
	public String addManeuver(String name, LinkedHashMap<String, Object> properties) {
		Class<?> man = maneuvers.get(name.toLowerCase());
		if (man != null) {
			return addManeuver(man, properties);
		}
		else {
			System.out.println("The maneuver "+name+" was not found");
		}
		return null;
	}
	
	
	public String addManeuver(Class<?> manClass, LinkedHashMap<String, Object> properties) {
		
	    String before = ""+(count-1);
        String id = ""+count;
        
	  //  System.out.println("[PlanCreator] "+id+" := addManeuver("+manClass.getSimpleName()+", "+properties+")");
	    
		try {
			Maneuver man = (Maneuver) manClass.newInstance();
		
			
			if (properties != null) {
				for (String key : properties.keySet()) {
					String k = key;
					if (Character.isLetter(key.charAt(0))) {
						k = Character.toUpperCase(key.charAt(0)) + key.substring(1);
					}
					try {
						
						for (Method m : man.getClass().getDeclaredMethods()) {
							if (m.getName().equalsIgnoreCase("set"+k) &&
									m.getParameterTypes().length == 1) {
								if (m.getParameterTypes()[0].isAssignableFrom(properties.get(key).getClass())) {
									Object o = m.getParameterTypes()[0].cast(properties.get(key));
									m.invoke(man, o);	
								}
								else if (m.getParameterTypes()[0].isPrimitive()) {
									Class<?> c = m.getParameterTypes()[0];
									Object obj = properties.get(key);										
									Double d = (double) Double.valueOf(obj.toString());
									
									if (c == Integer.TYPE) 
										obj = d.intValue();										
									else if (c == Float.TYPE) 
										obj = d.floatValue();										
									else if (c == Short.TYPE) 
										obj = d.shortValue();										
									else if (c == Byte.TYPE)
										obj = d.byteValue();										
									else if (c == Long.TYPE) 
										obj = d.longValue();										

									m.invoke(man, obj);
								}
								
							}
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			if (man instanceof LocatedManeuver) {
			    ((LocatedManeuver)man).setManeuverLocation(loc.clone());
			}
			
			man.setId(id);
			plan.getGraph().addManeuver(man);
			if (plan.getGraph().getManeuver(before) != null) {
			    addTransition(before, id, "true");	
			}	
			count++;
			return id;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public PlanType getPlan() {
	    return plan;
	}
	
	public String addGoto(LinkedHashMap<String, Object> properties) {
		return addManeuver(Goto.class, properties);
	}
	
	public String addLoiter(LinkedHashMap<String, Object> properties) {
		return addManeuver(Loiter.class, properties);
	}
	
	public String addUnconstrained(LinkedHashMap<String, Object> properties) {
		return addManeuver(Unconstrained.class, properties);
	}
	
	public String addPopup(LinkedHashMap<String, Object> properties) {
		return addManeuver(PopUp.class, properties);
	}		
}
