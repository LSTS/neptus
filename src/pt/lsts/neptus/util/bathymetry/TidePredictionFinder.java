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
 * Author: mfaria
 * ??/??/???
 */
package pt.lsts.neptus.util.bathymetry;

import java.util.ArrayList;
import java.util.Date;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.bathymetry.TidePrediction.TIDE_TYPE;

public abstract class TidePredictionFinder {
    protected ArrayList<TidePrediction> predictions;

    protected void logError(Exception e) {
        NeptusLog.pub().info("<###>[ERROR] There was a problem finding the tide prediction.", e);
    }

    /**
     * Applies interpolation formula for the case the date is after a hight tide.
     * 
     * @param indexFirstTide
     * @param wantedDate
     * @return
     */
    protected Float ihFuncAfterHighTide(int indexFirstTide, Date wantedDate) {
        TidePrediction firstTide = predictions.get(indexFirstTide);
        TidePrediction secondTide = predictions.get(indexFirstTide + 1);
        // hHT - H - height on high tide
        // hLT - h - height on low tide
        float hHT = firstTide.getHeight();
        float hLT = secondTide.getHeight();
        // hightToLowT - T - time elapsed between previous high tide and low tide
        float hightToLowT = secondTide.getTimeAndDate().getTime() - firstTide.getTimeAndDate().getTime();
        // timeUntilNow - t - time elapsed between last high or low tide and desired time
        float timeUntilNow;
        long wantedTime = wantedDate.getTime();
        timeUntilNow = wantedTime - firstTide.getTimeAndDate().getTime();

        float waterHeight = ((hHT + hLT) / 2 
                + ((hHT - hLT) / 2) * (((float)Math.cos(( ((float)Math.PI) * timeUntilNow) / hightToLowT))) );

        return waterHeight;
    }

    /**
     * Applies interpolation formula for the case the date is after a hight tide.
     * 
     * @param indexFirstTide
     * @param wantedDate
     * @return
     */
    protected Float ihFuncAfterLowTide(int indexFirstTide, Date wantedDate) {
        // hLT - h - height on low tide
        // hHT - H - height on high tide
        TidePrediction firstTide = predictions.get(indexFirstTide);
        TidePrediction secondTide = predictions.get(indexFirstTide + 1);
        float hLT = firstTide.getHeight();
        float hHT = secondTide.getHeight();
        // lowToHighT - T1 - time elapsed between previous low tide and high tide
        float lowToHighT = secondTide.getTimeAndDate().getTime() - firstTide.getTimeAndDate().getTime();
        // timeUntilNow - t - time elapsed between last high or low tide and desired time
        float timeUntilNow;
        long wantedTime = wantedDate.getTime();
        timeUntilNow = wantedTime - firstTide.getTimeAndDate().getTime();

        float waterHeight = ((hHT + hLT) / 2 + ((hLT - hHT) / 2)
                * (((float) Math.cos((((float) Math.PI) * timeUntilNow) / lowToHighT))));


        return waterHeight;
    }

    /**
     * Method that each class that implements this one must provide that transforms a date into a height.
     * 
     * @param date
     * @param print
     * @return returns 0 in case the tide cannot be predicted
     * @throws Exception
     */
    public abstract Float getTidePrediction(Date date, boolean print) throws Exception;

    /**
     * Decides which formula to apply (based o the previous tide).
     * 
     * @param date
     * @param iTide
     * @return
     */
    protected Float findPrediction(Date date, int iTide) {
        Float prediction;
        
        if (predictions.get(iTide).getTideType() == TIDE_TYPE.HIGH_TIDE) {
            prediction = ihFuncAfterHighTide(iTide, date);
        }
        else {
            prediction = ihFuncAfterLowTide(iTide, date);
        }
        return prediction;
    }

    public ArrayList<TidePrediction> getPredictionsMarks() {
        return predictions;
    }

}
