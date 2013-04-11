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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.LogFactory;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.plugins.tidePrediction.TidePrediction.TIDE_TYPE;
import pt.up.fe.dceg.plugins.tidePrediction.util.DateUtils;

import com.gargoylesoftware.htmlunit.AlertHandler;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HTMLParserListener;
import com.gargoylesoftware.htmlunit.html.HtmlArea;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlMap;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableBody;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;

public class TidePredictionFinder {
    private static final String INSTITUTO_HIDROGRAFICO_URL = "http://www.hidrografico.pt/previsao-mares.php";
    private final WebClient webClient;
    private TidePrediction predictions[];

    /**
     * Set of ids of html tags I needed to get the information
     */
    private enum HTML_IDS{
        SELECT_DATE_YEAR ( "seldateyear"),
        SELECT_DATE_MONTH ( "seldatemonth"),
        SELECT_DATE_DAY ( "seldateday"),
        DIV_WHOLE_UTILITY ( "divshow0"),
        MAP_AREAS ( "FPMap1"),
        TIDES_TABLE("divppd");

        private final String id;

        HTML_IDS(String id){
            this.id = id;
        }

        public String getID(){
            return id;
        }
    }

    /**
     * Initializes web client.
     */
    public TidePredictionFinder() {
        predictions = null;
        webClient = new WebClient();
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog");

        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(java.util.logging.Level.OFF);

        webClient.getOptions().setCssEnabled(false);

        webClient.setIncorrectnessListener(new IncorrectnessListener() {

            @Override
            public void notify(String arg0, Object arg1) {

            }
        });
        webClient.setAlertHandler(new AlertHandler() {
            
            @Override
            public void handleAlert(Page arg0, String arg1) {
                
            }
        });
        webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {

            @Override
            public void timeoutError(HtmlPage arg0, long arg1, long arg2) {

            }

            @Override
            public void scriptException(HtmlPage arg0, ScriptException arg1) {

            }

            @Override
            public void malformedScriptURL(HtmlPage arg0, String arg1, MalformedURLException arg2) {

            }

            @Override
            public void loadScriptError(HtmlPage arg0, URL arg1, Exception arg2) {

            }
        });
        webClient.setHTMLParserListener(new HTMLParserListener() {

            @Override
            public void error(String arg0, URL arg1, String arg2, int arg3, int arg4, String arg5) {
                
            }

            @Override
            public void warning(String arg0, URL arg1, String arg2, int arg3, int arg4, String arg5) {
                
            }
        });

        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
    }

    private void logError(Exception e){
        NeptusLog.pub().info("<###>[ERROR] There was a problem finding the tide prediction.");
        e.printStackTrace();
    }

    /**
     * State the date (day/month/year hour:minute) you need and in which harbor. This fetches the tide predictions from
     * the Portuguese Hydrographic Institute and calculates with a linear approximation the height of the time at the
     * time desired.
     * 
     * @param date when you want to know the tide (day and hour)
     * @param harbor the harbor you are interested in
     * @return the height of the tide
     * @throws Exception
     */
    public Float getTidePrediction(Date date, Harbors harbor, boolean print) throws Exception {
        if (predictions == null
                || (predictions[0].getTimeAndDate().compareTo(date) > 0 || predictions[1].getTimeAndDate().compareTo(
                        date) < 0)) {
            try {
                HtmlPage page = webClient.getPage(INSTITUTO_HIDROGRAFICO_URL);
                if (page == null) {
                    logError(new Exception("Cannot get html page."));
                }
                setHarbor(page, harbor.getCoordinates(), webClient);
                setDate(page, date);
                predictions = findPredictions(page, date);
                webClient.closeAllWindows();
            }
            catch (Exception e) {
                logError(e);
                throw e;
            }
        }

        Float prediction;
        if (predictions[0].getTideType() == TIDE_TYPE.HIGH_TIDE) {
            prediction = ihFuncAfterHighTide(predictions, date);
        }
        else {
            prediction = ihFuncAfterLowTide(predictions, date);
        }
        if (print) {
            NeptusLog.pub().info("<###>For " + date + " in " + harbor.toString());
            System.out.println(predictions[0].toString());
            System.out.println(predictions[1].toString());
        }
        return prediction;
    }


    private Float ihFuncAfterHighTide(TidePrediction predictions[], Date wantedDate) {
        // hHT - H - height on high tide
        // hLT - h - height on low tide
        float hHT = predictions[0].getHeight();
        float hLT = predictions[1].getHeight();
        // hightToLowT - T - time elapsed between previous high tide and low tide
        float hightToLowT = predictions[1].getTimeAndDate().getTime() - predictions[0].getTimeAndDate().getTime();
        // timeUntilNow - t - time elapsed between last high or low tide and desired time
        float timeUntilNow;
        long wantedTime = wantedDate.getTime();
        timeUntilNow = wantedTime - predictions[0].getTimeAndDate().getTime();

        float waterHeight = ((hHT + hLT) / 2 
                + ((hHT - hLT) / 2) * (((float)Math.cos(( ((float)Math.PI) * timeUntilNow) / hightToLowT))) );

        return waterHeight;
    }

    private Float ihFuncAfterLowTide(TidePrediction predictions[], Date wantedDate) {
        // hLT - h - height on low tide
        // hHT - H - height on high tide
        float hLT = predictions[0].getHeight();
        float hHT = predictions[1].getHeight();
        // lowToHighT - T1 - time elapsed between previous low tide and high tide
        float lowToHighT = predictions[1].getTimeAndDate().getTime() - predictions[0].getTimeAndDate().getTime();
        // timeUntilNow - t - time elapsed between last high or low tide and desired time
        float timeUntilNow;
        long wantedTime = wantedDate.getTime();
        timeUntilNow = wantedTime - predictions[0].getTimeAndDate().getTime();

        float waterHeight = ((hHT + hLT) / 2 + ((hLT - hHT) / 2)
                * (((float) Math.cos((((float) Math.PI) * timeUntilNow) / lowToHighT))));


        return waterHeight;
    }


    /**
     * Gets the predictions in the HTML page Not all, only from the 2nd to the 5th (since those are the ones for the
     * selected day)
     * 
     * @param page
     * @param date
     * @return an array with all the info stored in TidePrediction objects
     * @throws InterruptedException (while waiting for page to load after request)
     * @throws ParseException (when reading the heigh of the tide)
     */
    private TidePrediction[] findPredictions(HtmlPage page, Date date) throws InterruptedException, ParseException {
        HtmlDivision tideTableDiv = page.getHtmlElementById(HTML_IDS.TIDES_TABLE.getID());
        DomNode dateHarbor = tideTableDiv.getFirstChild();
        // Tides
        DomNode tidePredictionsNode = dateHarbor.getNextSibling();
        HtmlTableBody tidePredictionsTableBody = ((HtmlTable)tidePredictionsNode).getBodies().get(0);
        Iterator<DomElement> tableElementsIt = tidePredictionsTableBody.getChildElements().iterator();

        // header
        tableElementsIt.next();
        // rest
        TidePrediction tidePrediction, lastTide, nextTide;
        TidePrediction predictions[] = new TidePrediction[2];
        lastTide = nextTide = null;
        while (tableElementsIt.hasNext()) {
            DomElement row = tableElementsIt.next();
            tidePrediction = new TidePrediction(row.getChildElements().iterator());
            if (tidePrediction.getTimeAndDate().before(date) || tidePrediction.getTimeAndDate().equals(date)) {
                lastTide = tidePrediction;
            }
            else if (tidePrediction.getTimeAndDate().after(date)) {
                nextTide = tidePrediction;
                break;
            }
        }

        predictions[0] = lastTide;
        predictions[1] = nextTide;
        return predictions;

    }


    /**
     * Sets the desired harbor by coordinates
     *  
     * @param page 
     * @param coordinatesHarbor to identify the desired harbor
     * @return the new version of the html page
     * @throws IOException (if an IO error occurs on click)
     * @throws InterruptedException 
     */
    private HtmlPage setHarbor(HtmlPage page, String coordinatesHarbor, final WebClient webClient) throws IOException, InterruptedException {
        // get map with the clickable areas
        HtmlMap map = page.getHtmlElementById(HTML_IDS.MAP_AREAS.getID());
        Iterable<DomElement> childAreas = map.getChildElements();
        HtmlArea tempArea;
        for (DomElement htmlElement : childAreas) {
            tempArea = (HtmlArea) htmlElement;
            // find the one we want and click on it!
            if(tempArea.getCoordsAttribute().equals(coordinatesHarbor)){
                HtmlPage resultPage = tempArea.click();
                return resultPage;
            }
        } 
        return null;
    }

    /**
     * Sets the desired date
     * 
     * @param page
     * @throws InterruptedException 
     */
    private Page setDate(HtmlPage page, Date wantedDate) throws InterruptedException {
        Calendar cal = Calendar.getInstance();
        cal.setTime(wantedDate);
        // day
        HtmlSelect select = page.getHtmlElementById(HTML_IDS.SELECT_DATE_DAY.getID());
        page = changeSelectedOption(select, cal.get(Calendar.DAY_OF_MONTH)+"");
        StringBuilder newDate = new StringBuilder(select.getSelectedOptions().get(0).asText());
        // month 
        select = page.getHtmlElementById(HTML_IDS.SELECT_DATE_MONTH.getID());
        String monthNumber = new SimpleDateFormat("M").format(wantedDate);
        page = changeSelectedOption(select, DateUtils.getMonthNameInPortuguese(Integer.parseInt(monthNumber)));
        newDate.append('-');
        newDate.append(select.getSelectedOptions().get(0).asText());
        // year
        select = page.getHtmlElementById(HTML_IDS.SELECT_DATE_YEAR.getID());
        page = changeSelectedOption(select, cal.get(Calendar.YEAR)+"");
        newDate.append('-');
        newDate.append(select.getSelectedOptions().get(0).asText());
        return page;
    }

    /**
     * Changes the option for the desired select
     * 
     * @param select the target to change
     * @param newSelection the new value
     */
    private HtmlPage changeSelectedOption(HtmlSelect select, String newSelection) {
        List<HtmlOption> options = select.getOptions();
        for (HtmlOption htmlOption : options) {
            if (htmlOption.asText().equals(newSelection)){
                return select.setSelectedAttribute(htmlOption, true);
            }
        }
        return null;
    }

    public TidePrediction[] getPredictionsMarks() {
        return predictions;
    }

}
