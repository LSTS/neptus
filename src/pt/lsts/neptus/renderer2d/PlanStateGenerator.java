/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * 9/Dez/2004
 */
package pt.up.fe.dceg.neptus.renderer2d;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author ZP
 */
public class PlanStateGenerator implements ActionListener {

	//MissionPlan plan;
	PlanType planType;
	Renderer[] renderers;
	
	Timer timer = null;
	SystemPositionAndAttitude curState, initialState;
	String curManeuverID;
	Maneuver curManeuver;
	ActionListener listener = null;
	
	/**
	 * Creates a new StateGenerator for the given plan and vehicle
	 * @param initialState The initial location and rotation of the vehicle
	 * @param planType The individual plan to animate
	 * @param renderer The renderer component that will show the animation
	 */
	public PlanStateGenerator(SystemPositionAndAttitude initialState, PlanType planType, Renderer renderer) {
	    this.planType = planType;
	    this.renderers = new Renderer[] {renderer};
	    this.initialState = (SystemPositionAndAttitude) initialState.clone();
	    this.curState = initialState;
	    curManeuverID = planType.getGraph().getInitialManeuverId();
	    curManeuver = planType.getGraph().getManeuver(curManeuverID);
	}
	
	public PlanStateGenerator(SystemPositionAndAttitude initialState, PlanType planType, Renderer[] renderers) {
	    this.planType = planType;
	    this.renderers = renderers;
	    this.initialState = (SystemPositionAndAttitude) initialState.clone();
	    this.curState = initialState;
	    curManeuverID = planType.getGraph().getInitialManeuverId();
	    try {
	    	curManeuver = (Maneuver) planType.getGraph().getManeuver(curManeuverID).clone();
	    }
	    catch (NullPointerException e) {
	    	System.err.println("Unable to find the initial maneuver ("+curManeuverID+").");
	    	GuiUtils.errorMessage(null, e);
	    	NeptusLog.pub().error(this, e);
	    }
	}
	
	public void setActionListener(ActionListener listener) {
		this.listener = listener;
	}
	
	public void startGenerating(int delay) {
		for (int i = 0; i < renderers.length; i++)
			renderers[i].vehicleStateChanged(planType.getVehicleType().getId(), curState);
		timer = new Timer(delay, this);
		timer.start();
		sendAction("Mission started");
	}
	
	public void setDelay(int delay) {
		boolean wasRunning = false;
		if (timer != null ) {
			wasRunning = timer.isRunning();
			timer.stop();
		}
		timer = new Timer(delay, this);
		
		if (wasRunning)	
			timer.start();
	}
	
	
	public void restart() {
		timer.stop();
		this.curState = (SystemPositionAndAttitude) initialState.clone();
		curManeuverID = planType.getGraph().getInitialManeuverId();
		curManeuver = (Maneuver) planType.getGraph().getManeuver(curManeuverID).clone();		
		for (int i = 0; i < renderers.length; i++)
			renderers[i].vehicleStateChanged(planType.getVehicleType().getId(), curState);
		timer.start();
		sendAction("Mission restarted");
	}
	
	public void pause() {
		timer.stop();
		sendAction("Mission paused");
	}
	
	public void resume() {
		if (timer == null)
			return;
		timer.start();
		sendAction("Resumed mission execution");
	}
	
	
	// The timer has ticked... update the vehiclestate!
	public void actionPerformed(ActionEvent evt) {
		
		// Calculates the next vehicle state
		curState = curManeuver.ManeuverFunction(curState);
		
		// Refreshes the Renderer according to the current vehicle state
		for (int i = 0; i < renderers.length; i++)
			renderers[i].vehicleStateChanged(planType.getVehicleType().getId(), curState);
		
		// Verifies if the current maneuver has ended...
		if (curManeuver.hasEnded()) {
			String[] reachIds = planType.getGraph().getReacheableManeuvers(curManeuverID);
			// If there are no transitions for the current maneuver, the mission ended
			
			if (reachIds == null) {
				sendAction("Abnormal mission termination.");
				timer.stop();
			}
			
			if (reachIds.length == 0) {
				sendAction("The mission ended with success.");
				timer.stop();
			}
			// Change the current maneuver according to the transition
			else {
				NeptusLog.pub().info("<###>Transition to "+reachIds[0]);
				curManeuverID = reachIds[0];
				curManeuver = (Maneuver) planType.getGraph().getManeuver(curManeuverID).clone();
				sendAction("Transition to "+curManeuverID+".");
			}
		}

	}
	
	public void sendAction(String text) {
		if (listener != null) {
			listener.actionPerformed(
					new ActionEvent(this, ActionEvent.ACTION_PERFORMED, text)
				);
		}
	}
}