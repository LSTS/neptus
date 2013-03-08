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
 * Nov 26, 2012
 * $Id:: ConsoleEventPositionEstimation.java 9615 2012-12-30 23:08:28Z pdias    $:
 */
package pt.up.fe.dceg.neptus.console.events;

import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zp
 * 
 */
public class ConsoleEventPositionEstimation {

    public enum ESTIMATION_TYPE {
        LBL_RANGES,
        OTHER
    }

    protected ESTIMATION_TYPE type;
    protected Object source;
    protected EstimatedState estimation;

    public ConsoleEventPositionEstimation(Object source, ESTIMATION_TYPE type, LocationType loc) {
        this(source, type, 
                new EstimatedState("lat", loc.getLatitudeAsDoubleValueRads(), "lon",
                loc.getLongitudeAsDoubleValueRads(), "depth", loc.getDepth(), "x", loc.getOffsetNorth(), "y",
                loc.getOffsetEast(), "z", loc.getOffsetDown())
        );
    }

    public ConsoleEventPositionEstimation(Object source, ESTIMATION_TYPE type, EstimatedState state) {
        this.estimation = state;
        this.source = source;
        this.type = type;
    }

    /**
     * @return the type
     */
    public ESTIMATION_TYPE getType() {
        return type;
    }

    /**
     * @return the source
     */
    public Object getSource() {
        return source;
    }

    /**
     * @return the estimation
     */
    public EstimatedState getEstimation() {
        return estimation;
    }
}
