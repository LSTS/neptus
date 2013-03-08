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
 * $Id:: TimeBehavior.java 9616 2012-12-30 23:23:22Z pdias                $:
 */
package pt.up.fe.dceg.neptus.renderer3d;

import java.util.Enumeration;

import javax.media.j3d.Behavior;
import javax.media.j3d.WakeupCondition;
import javax.media.j3d.WakeupOnElapsedTime;

public class TimeBehavior extends Behavior {
	private WakeupCondition timeOut;

	private ProjectionObj ms;

	private boolean isStopped = false;

	public TimeBehavior(int timeDelay, ProjectionObj ms) {
		this.ms = ms;
		timeOut = new WakeupOnElapsedTime(timeDelay);
	}

	public void initialize() {
		wakeupOn(timeOut);
	}

	public void processStimulus(@SuppressWarnings("rawtypes") Enumeration criteria) { // ignore criteria
		if (!isStopped) {
			ms.refreshVideoMap();
			wakeupOn(timeOut);
		}
	}

	public void stopUpdate() {
		isStopped = true;
	}

}
