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
 * Author: mfaria
 * ??/??/???
 */
package pt.up.fe.dceg.plugins.tidePrediction;

import java.util.ArrayList;
import java.util.Date;

import pt.up.fe.dceg.neptus.NeptusLog;

public abstract class TidePredictionFinder {
    protected ArrayList<TidePrediction> predictions;

    protected void logError(Exception e) {
        NeptusLog.pub().info("<###>[ERROR] There was a problem finding the tide prediction.", e);
    }

    protected Float ihFuncAfterHighTide(ArrayList<TidePrediction> predictions, Date wantedDate) {
        // hHT - H - height on high tide
        // hLT - h - height on low tide
        float hHT = predictions.get(0).getHeight();
        float hLT = predictions.get(1).getHeight();
        // hightToLowT - T - time elapsed between previous high tide and low tide
        float hightToLowT = predictions.get(1).getTimeAndDate().getTime()
                - predictions.get(0).getTimeAndDate().getTime();
        // timeUntilNow - t - time elapsed between last high or low tide and desired time
        float timeUntilNow;
        long wantedTime = wantedDate.getTime();
        timeUntilNow = wantedTime - predictions.get(0).getTimeAndDate().getTime();

        float waterHeight = ((hHT + hLT) / 2 
                + ((hHT - hLT) / 2) * (((float)Math.cos(( ((float)Math.PI) * timeUntilNow) / hightToLowT))) );

        return waterHeight;
    }

    protected Float ihFuncAfterLowTide(ArrayList<TidePrediction> predictions, Date wantedDate) {
        // hLT - h - height on low tide
        // hHT - H - height on high tide
        float hLT = predictions.get(0).getHeight();
        float hHT = predictions.get(1).getHeight();
        // lowToHighT - T1 - time elapsed between previous low tide and high tide
        float lowToHighT = predictions.get(1).getTimeAndDate().getTime()
                - predictions.get(0).getTimeAndDate().getTime();
        // timeUntilNow - t - time elapsed between last high or low tide and desired time
        float timeUntilNow;
        long wantedTime = wantedDate.getTime();
        timeUntilNow = wantedTime - predictions.get(0).getTimeAndDate().getTime();

        float waterHeight = ((hHT + hLT) / 2 + ((hLT - hHT) / 2)
                * (((float) Math.cos((((float) Math.PI) * timeUntilNow) / lowToHighT))));


        return waterHeight;
    }

    public abstract Float getTidePrediction(Date date, boolean print) throws Exception;

    public ArrayList<TidePrediction> getPredictionsMarks() {
        return predictions;
    }

}
