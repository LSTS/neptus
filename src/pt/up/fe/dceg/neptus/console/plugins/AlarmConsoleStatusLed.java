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
 * 20??/??/??
 * $Id:: AlarmConsoleStatusLed.java 9616 2012-12-30 23:23:22Z pdias       $:
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.util.Vector;

import pt.up.fe.dceg.neptus.gui.StatusLed;

public class AlarmConsoleStatusLed 
extends StatusLed
implements AlarmListener, AlarmProviderOld
{
	private static final long serialVersionUID = 3665429225885707581L;

	AlarmListener parent;
	
	int lastsource=-1;
	
	protected Vector<AlarmProviderOld> listenerVector=new Vector<AlarmProviderOld>();
	
	public AlarmConsoleStatusLed()
	{
		super();
	}
	
	public void addAlarmProvider(AlarmProviderOld a)
	{
		listenerVector.add(a);
	}
	
	public void removeAlarmProvider(AlarmProviderOld a)
	{
		listenerVector.remove(a);
		if(listenerVector.isEmpty())
			setLevel((short)-1,"No Active Alarms");
	}
	
	public void updateAlarmsListeners(AlarmProviderOld alarm)
	{
		
		lastsource=alarm.sourceState();
		
		int max=-1;
		
			
		/*if(alarm.getAlarmState()>0)
		{
			amsg=alarm.getAlarmMessage();
			max=alarm.getAlarmState();
		}*/
		
		for (AlarmProviderOld a : listenerVector)
		{
			int value=a.getAlarmState();
			if(value>max) 
				{
					max=value;
					//amsg=a.getAlarmMessage();
				}
		}
		
		//System.out.println("CHAMOU ALARMES-- min:");
		
		
		String amsg=alarm.getAlarmMessage();
		if(amsg==null)
			setLevel((short)max);
		else
			setLevel((short)max,amsg);
		
		
		if(parent!=null)
			parent.updateAlarmsListeners(this);
	}
	
	public void updateAlarmsListeners()
	{
		int max=-1;
		
		for (AlarmProviderOld a : listenerVector)
		{
			int value=a.getAlarmState();
			if(value>max) 
				{
					max=value;
					//amsg=a.getAlarmMessage();
				}
		}
		
		
		setLevel((short)max);
		
		if(parent!=null)
			parent.updateAlarmsListeners(this);
		
		
	}

	public int getAlarmState() {
		return getLevel();
	}

	public String getAlarmMessage() {
		return getMessage();
	}

	public int sourceState()
	{
		return lastsource;
	}
	
	public AlarmListener getAlarmParent() {
		return parent;
	}

	public void setAlarmParent(AlarmListener p) {
		if(this.parent!=null)
			this.parent.removeAlarmProvider(this);
		this.parent = p;
		if(this.parent!=null)
			this.parent.addAlarmProvider(this);
		
		//p.addAlarmProvider(this);
	}

	protected int getLastsource() {
		return lastsource;
	}

	protected void setLastsource(int lastsource) {
		this.lastsource = lastsource;
	}
	
	protected void clean()
	{
		if(this.parent!=null)
			{
				this.parent.removeAlarmProvider(this);
				this.parent.updateAlarmsListeners();
			}
		setAlarmParent(null);
		
	}
	
	

}
