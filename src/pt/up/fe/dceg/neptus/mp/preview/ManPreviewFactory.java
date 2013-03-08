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
 * Oct 11, 2011
 * $Id:: ManPreviewFactory.java 9880 2013-02-07 15:23:52Z jqcorreia             $:
 */
package pt.up.fe.dceg.neptus.mp.preview;

import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mp.maneuvers.Elevator;
import pt.up.fe.dceg.neptus.mp.maneuvers.FollowTrajectory;
import pt.up.fe.dceg.neptus.mp.maneuvers.Goto;
import pt.up.fe.dceg.neptus.mp.maneuvers.Loiter;
import pt.up.fe.dceg.neptus.mp.maneuvers.PopUp;
import pt.up.fe.dceg.neptus.mp.maneuvers.RowsManeuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.StationKeeping;

/**
 * @author zp
 *
 */
public class ManPreviewFactory {

    protected static LinkedHashMap<Class<Maneuver>, Class<IManeuverPreview<Maneuver>>> registeredPreviews = new LinkedHashMap<Class<Maneuver>, Class<IManeuverPreview<Maneuver>>>();

    public static IManeuverPreview<?> getPreview(Maneuver maneuver, String vehicleId, SystemPositionAndAttitude state, Object manState) {
        if (maneuver.getClass() == Goto.class) {
            GotoPreview prev = new GotoPreview();
            prev.init(vehicleId, (Goto)maneuver, state, manState);
            return prev;
        }
        else if (maneuver.getClass() == PopUp.class) {
            PopupPreview prev = new PopupPreview();
            prev.init(vehicleId, (PopUp)maneuver, state, manState);
            return prev;
        }
        else if (maneuver.getClass() == StationKeeping.class) {
            StationKeepingPreview prev = new StationKeepingPreview();
            prev.init(vehicleId, (StationKeeping)maneuver, state, manState);
            return prev;
        }
        else if (maneuver.getClass() == Loiter.class) {
            LoiterPreview prev = new LoiterPreview();
            prev.init(vehicleId, (Loiter)maneuver, state, manState);
            return prev;
        }
        else if (maneuver.getClass() == RowsManeuver.class) {
            RowsManeuverPreview prev = new RowsManeuverPreview();
            prev.init(vehicleId, (RowsManeuver)maneuver, state, manState);
            return prev;
        }
        else if (maneuver.getClass() == Elevator.class) {
            ElevatorPreview prev = new ElevatorPreview();
            prev.init(vehicleId, (Elevator)maneuver, state, manState);
            return prev;
        }
        else if (FollowTrajectory.class.isAssignableFrom(maneuver.getClass())) {
            FollowTrajectoryPreview prev = new FollowTrajectoryPreview();
            prev.init(vehicleId, (FollowTrajectory)maneuver, state, manState);
            return prev;
        }
        
        
        return null;
    }    
}


