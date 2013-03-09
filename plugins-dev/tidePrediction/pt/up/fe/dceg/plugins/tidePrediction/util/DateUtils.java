/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by mfaria
 * ??/??/???
 */
package pt.up.fe.dceg.plugins.tidePrediction.util;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
            System.out.println("The date "+dateAndHour+" is not compatible with "+dateRegEx);
            System.out.println(e.getMessage());  
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
