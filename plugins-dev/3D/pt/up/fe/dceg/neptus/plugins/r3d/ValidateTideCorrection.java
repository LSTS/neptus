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
 * Author: Margarida Faria
 * Feb 13, 2013
 */
package pt.up.fe.dceg.neptus.plugins.r3d;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.imc.DesiredPath;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIterator;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.plugins.tidePrediction.Harbors;
import pt.up.fe.dceg.plugins.tidePrediction.PtHydrographicWeb;

/**
 * @author Margarida Faria
 *
 */
public class ValidateTideCorrection {
    private Harbors harbor;
    private final IMraLogGroup source;

    public ValidateTideCorrection(IMraLogGroup source) {
        super();
        this.source = source;
    }

    // /**
    // * @param harbor the harbor to set
    // */
    // public void setHarbor(Harbors harbor) {
    // this.harbor = harbor;
    // }

    public void printRelevantData() throws Exception {
        NeptusLog.pub().info("<###>");
        LsfIndex lsfIndex = source.getLsfIndex();
        LsfIterator<DesiredPath> desiredPathIt = lsfIndex.getIterator(DesiredPath.class);
        // -- Start tide prediction
        PtHydrographicWeb tidePrediction = new PtHydrographicWeb(harbor);

        desiredPathIt.next();
        desiredPathIt.next();
        // Third goto
        NeptusLog.pub().info("<###>\nThird Goto!");
        checkGoto(desiredPathIt, tidePrediction);

        // Forth goto
        NeptusLog.pub().info("<###>\nForth Goto!");
        checkGoto(desiredPathIt, tidePrediction);
        NeptusLog.pub().info("<###>");
    }

    private void checkGoto(LsfIterator<DesiredPath> desiredPathIt,
 PtHydrographicWeb tidePrediction) throws Exception {
        DesiredPath desiredPathMsg = desiredPathIt.next();
        double gotoTimestamp = desiredPathMsg.getTimestamp();
        NeptusLog.pub().info("<###>Desired path timestamp:   " + gotoTimestamp);
        EstimatedState estimatedStateGoto = (EstimatedState) source.getLsfIndex().getMessageAtOrAfter("EstimatedState",
                0, gotoTimestamp);
        NeptusLog.pub().info("<###>Estimated State timestamp:" + gotoTimestamp);
        loopCode(estimatedStateGoto, tidePrediction);
    }

    private void loopCode(EstimatedState currEstStateMsg,
 PtHydrographicWeb tidePrediction) throws Exception {
        double alt;
        double depth;
        // -- loop code
        LocationType estStateMsgLocation;
        double waterColumn, tideOff, terrainAltitude;
        depth = currEstStateMsg.getDepth();
        if (depth < 0) {
            NeptusLog.pub().info("<###>Nothing written");
            return;
        }
        // Take vehicle path info
        estStateMsgLocation = Bathymetry3DGenerator.getLocationIMC5(currEstStateMsg);
        alt = currEstStateMsg.getAlt();
        float currPrediction;
        if (alt < 0 || depth < 1) {
            NeptusLog.pub().info("<###>vehicleDepthVec 0, the end");
            return;
        }
        currPrediction = tidePrediction.getTidePrediction(currEstStateMsg.getDate(), true);

        tideOff = currPrediction;// - tideOfFirstDepth;
        NeptusLog.pub().info("<###>Tide:" + currPrediction);
        waterColumn = depth + alt;
        terrainAltitude = waterColumn - tideOff;
        NeptusLog.pub().info("<###>Water column:"+waterColumn+" = "+depth+" + "+alt);
        NeptusLog.pub().info("<###>Estimated state:" + estStateMsgLocation.toString() + " terrainAltitude:" + terrainAltitude);
    }
}
