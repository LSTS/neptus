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
package pt.lsts.neptus.plugins.tidePrediction.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import pt.lsts.neptus.NeptusLog;

/**
 * Helper methods that can be hand for several situations
 */
public class DateUtils {
    /**
     * Creates a date object from a String expecting the String in the same format it has 
     * on the website (yyyy-MM-dd HH:mm)
     * 
     * @param dateAndHour The date in String format
     * @return An object date with the time and day of prediction
     * @throws ParseException when the String is not in the format "yyyy-MM-dd HH:mm"
     */
    public static Date getDate(String dateAndHour) throws ParseException{
        String[] split = dateAndHour.split(" ");
        DateFormat formatter ;
        String dateRegEx = "yyyy-MM-dd HH:mm";
        dateAndHour = split[1]+" "+split[2];
        formatter = new SimpleDateFormat(dateRegEx);
        Date date;
        try {
            date = formatter.parse(dateAndHour);
            return date;
        }
        catch (ParseException e) {
            NeptusLog.pub().info("<###>The date "+dateAndHour+" is not compatible with "+dateRegEx);
            NeptusLog.pub().info("<###> "+e.getMessage());  
            throw e;
        }
    }
    
    public static String getMonthNameInPortuguese(int month){
        switch(month){
            case 1: return "Janeiro";
            case 2: return "Fevereiro";
            case 3: return "Março";
            case 4: return "Abril";
            case 5: return "Maio";
            case 6: return "Junho";
            case 7: return "Julho";
            case 8: return "Agosto";
            case 9: return "Setembro";
            case 10: return "Outubro";
            case 11: return "Novembro";
            case 12: return "Dezembro";
            default: throw new  IllegalArgumentException("The month has to be betweeb 1 and 12, not "+month);
        }
    }
}
