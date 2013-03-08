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
 * Nov 29, 2011
 * $Id:: PlanRotated.java 9615 2012-12-30 23:08:28Z pdias                       $:
 */
package pt.up.fe.dceg.neptus.plugins.planning.edit;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.LocatedManeuver;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.AngleCalc;
import pt.up.fe.dceg.neptus.util.coord.MapTileUtil;

/**
 * @author zp
 *
 */
public class PlanRotated extends AbstractUndoableEdit {

    private static final long serialVersionUID = 1L;
    protected PlanType plan;
    protected LocatedManeuver pivot;
    protected double angle;

    public PlanRotated(PlanType plan, LocatedManeuver pivot, double angle) {
        this.plan = plan;     
        this.pivot = pivot;
        this.angle = angle;                
    }

    @Override
    public boolean canUndo() {
        return true;
    }

    @Override
    public boolean canRedo() {
        return true;
    }

    @Override
    public String getPresentationName() {
        return "Rotate the plan";
    }

    @Override
    public void undo() throws CannotUndoException {
        for (Maneuver m : plan.getGraph().getAllManeuvers()) {
            if (m != pivot && m instanceof LocatedManeuver) {
                LocatedManeuver satellite = (LocatedManeuver) m;
                double[] top = pivot.getManeuverLocation().getDistanceInPixelTo(satellite.getManeuverLocation(), MapTileUtil.LEVEL_OFFSET);
                double[] topR = AngleCalc.rotate(2 * -angle, top[0], top[1], false); 
                double deltaX = topR[0];
                double deltaY = topR[1];
                LocationType lt = new LocationType(pivot.getManeuverLocation());
                lt.translateInPixel(deltaX, deltaY, MapTileUtil.LEVEL_OFFSET);
                lt.setAbsoluteDepth(satellite.getManeuverLocation().getAllZ());
                satellite.getManeuverLocation().setLocation(lt);
            }
        }
    }

    @Override
    public void redo() throws CannotRedoException {      
        for (Maneuver m : plan.getGraph().getAllManeuvers()) {
            if (m != pivot && m instanceof LocatedManeuver) {
                LocatedManeuver satellite = (LocatedManeuver) m;
                double[] top = pivot.getManeuverLocation().getDistanceInPixelTo(satellite.getManeuverLocation(), MapTileUtil.LEVEL_OFFSET);
                double[] topR = AngleCalc.rotate(2 * angle, top[0], top[1], false); 
                double deltaX = topR[0];
                double deltaY = topR[1];
                LocationType lt = new LocationType(pivot.getManeuverLocation());
                lt.translateInPixel(deltaX, deltaY, MapTileUtil.LEVEL_OFFSET);
                lt.setAbsoluteDepth(satellite.getManeuverLocation().getAllZ());
                satellite.getManeuverLocation().setLocation(lt);
            }
        }   
    }

    /**
     * @return the plan
     */
    public PlanType getPlan() {
        return plan;
    }     
    
    /**
     * @return the pivot
     */
    public LocatedManeuver getPivot() {
        return pivot;
    }
    
    /**
     * @return the angle
     */
    public double getAngle() {
        return angle;
    }
}
