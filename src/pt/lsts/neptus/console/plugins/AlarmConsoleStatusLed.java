/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.console.plugins;

import java.util.Vector;

import pt.lsts.neptus.gui.StatusLed;

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
		
		//NeptusLog.pub().info("<###>CHAMOU ALARMES-- min:");
		
		
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
