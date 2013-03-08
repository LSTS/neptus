/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Margarida Faria
 * Feb 13, 2013
 * $Id:: ValidateTideCorrection.java 9933 2013-02-15 03:32:23Z robot            $:
 */
package pt.up.fe.dceg.neptus.plugins.r3d;

import pt.up.fe.dceg.neptus.imc.DesiredPath;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIterator;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.plugins.tidePrediction.Harbors;
import pt.up.fe.dceg.plugins.tidePrediction.TidePredictionFinder;

/**
 * @author Margarida Faria
 *
 */
public class ValidateTideCorrection {
    private final Harbors harbor;
    private final IMraLogGroup source;

    public ValidateTideCorrection(IMraLogGroup source, Harbors harbor) {
        super();
        this.source = source;
        this.harbor = harbor;
    }

    public void printRelevantData() throws Exception {
        System.out.println();
        LsfIndex lsfIndex = source.getLsfIndex();
        LsfIterator<DesiredPath> desiredPathIt = lsfIndex.getIterator(DesiredPath.class);
        // -- Start tide prediction
        TidePredictionFinder tidePrediction = new TidePredictionFinder();

        desiredPathIt.next();
        desiredPathIt.next();
        // Third goto
        System.out.println("\nThird Goto!");
        checkGoto(desiredPathIt, tidePrediction);

        // Forth goto
        System.out.println("\nForth Goto!");
        checkGoto(desiredPathIt, tidePrediction);
        System.out.println();
    }

    private void checkGoto(LsfIterator<DesiredPath> desiredPathIt,
            TidePredictionFinder tidePrediction) throws Exception {
        DesiredPath desiredPathMsg = desiredPathIt.next();
        double gotoTimestamp = desiredPathMsg.getTimestamp();
        System.out.println("Desired path timestamp:   " + gotoTimestamp);
        EstimatedState estimatedStateGoto = (EstimatedState) source.getLsfIndex().getMessageAtOrAfter("EstimatedState",
                0, gotoTimestamp);
        System.out.println("Estimated State timestamp:" + gotoTimestamp);
        loopCode(estimatedStateGoto, tidePrediction);
    }

    private void loopCode(EstimatedState currEstStateMsg,
            TidePredictionFinder tidePrediction) throws Exception {
        double alt;
        double depth;
        // -- loop code
        LocationType estStateMsgLocation;
        double waterColumn, tideOff, terrainAltitude;
        depth = currEstStateMsg.getDepth();
        if (depth < 0) {
            System.out.println("Nothing written");
            return;
        }
        // Take vehicle path info
        estStateMsgLocation = Bathymetry3DGenerator.getLocationIMC5(currEstStateMsg);
        alt = currEstStateMsg.getAlt();
        float currPrediction;
        if (alt < 0 || depth < 1) {
            System.out.println("vehicleDepthVec 0, the end");
            return;
        }
        currPrediction = tidePrediction.getTidePrediction(currEstStateMsg.getDate(), harbor, true);

        tideOff = currPrediction;// - tideOfFirstDepth;
        System.out.println("Tide:" + currPrediction);
        waterColumn = depth + alt;
        terrainAltitude = waterColumn - tideOff;
        System.out.println("Water column:"+waterColumn+" = "+depth+" + "+alt);
        System.out.println("Estimated state:" + estStateMsgLocation.toString() + " terrainAltitude:" + terrainAltitude);
    }
}
