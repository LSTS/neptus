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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: meg
 * May 15, 2013
 */
package pt.up.fe.dceg.neptus.util.bathymetry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import pt.up.fe.dceg.neptus.util.bathymetry.TidePrediction.TIDE_TYPE;

/**
 * @author meg
 *
 */
public class LocalData extends TidePredictionFinder {
    private final int minChars = 23;
    private final File tides;

    public LocalData(File file) {
        this.tides = file;
        if (file != null) {
            try {
                predictions = new ArrayList<TidePrediction>();
                buildKnowledge();
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public Float getTidePrediction(Date date, boolean print) throws Exception {
        // Are there enough tide predictions?
        if (predictions == null || predictions.size() < 2) {
            return 0f;
        }

        TidePrediction tide;
        int first = -1;
        boolean foundSecond = false;
        int iTide;
        Date tideDate;
        for (iTide = 0; iTide < predictions.size() && !foundSecond; iTide++) {
            tide = predictions.get(iTide);
            tideDate = tide.getTimeAndDate();
            if (tideDate.equals(date))
                return tide.getHeight();
            else if (tideDate.before(date)) {
                // date before tide
                // iTide has the index of the first tide to consider
                first = iTide;
            }
            else if (tideDate.after(date)) {
                // date after tide
                foundSecond = true;
            }
        }
        if (!foundSecond || first == -1)
            return 0f;

        return findPrediction(date, first);
    }



    protected void buildKnowledge() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(tides.toString()));
        try {
            String line = br.readLine();

            String[] timeTokens, lineTokens, dateTokens;
            GregorianCalendar timeCal, dayCal;
            Float height, decimal;
            int i;
            while (line != null && line.length() >= minChars) {
                lineTokens = line.split(" ");
                dateTokens = lineTokens[0].split("-");
                dayCal = new GregorianCalendar(new Integer(dateTokens[2]), new Integer(dateTokens[1]) - 1, new Integer(
                        dateTokens[0]));
                i = 1;
                while (i < lineTokens.length) {
                    timeTokens = lineTokens[i].split("h");
                    timeCal = (GregorianCalendar) dayCal.clone();
                    timeCal.add(Calendar.HOUR_OF_DAY, new Integer(timeTokens[0]));
                    timeCal.add(Calendar.MINUTE, new Integer(timeTokens[1]));
                    i++;
                    timeTokens = lineTokens[i].split("[:.:]");
                    height = new Float(timeTokens[0]);
                    decimal = new Float(timeTokens[1]);
                    for (int j = 0; j < timeTokens.length; j++) {
                        decimal = decimal / 10;
                    }
                    height += decimal;
                    i++;
                    char tide = lineTokens[i].charAt(0);
                    switch (tide) {
                        case 'L':
                            predictions.add(new TidePrediction(height, timeCal.getTime(), TIDE_TYPE.LOW_TIDE));
                            break;
                        case 'H':
                            predictions.add(new TidePrediction(height, timeCal.getTime(), TIDE_TYPE.HIGH_TIDE));
                            break;
                    }
                    i++;
                }
                line = br.readLine();
            }
        }
        finally {
            br.close();
        }
    }
}
