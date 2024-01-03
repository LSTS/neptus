/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: pdias
 * 26/05/2015
 */
package com.inovaworks;

import java.util.ArrayList;

import javax.swing.ProgressMonitor;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.http.client.HttpClientConnectionHelper;

/**
 * @author pdias
 *
 */
@PluginDescription(name="Inovaworks State Upload Exporter", experimental=true)
public class InovaWorksUploaderExporter implements MRAExporter {

    @NeptusProperty
    private String url = "http://ec2-52-16-31-123.eu-west-1.compute.amazonaws.com:8080/GeoC2/api/sensing/observations";

    @NeptusProperty
    private int messagesMillisSeparation = 10000;
    
    private HttpClientConnectionHelper httpComm;
    private HttpPost postHttpRequest;
    
    private IMraLogGroup log = null;
    
    public InovaWorksUploaderExporter(IMraLogGroup log) {
        httpComm = new HttpClientConnectionHelper();
        this.log = log;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.mra.exporters.MRAExporter#canBeApplied(pt.lsts.neptus.mra.importers.IMraLogGroup)
     */
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        IMraLog estStateLog = source.getLog("EstimatedState");
        return (estStateLog != null);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mra.exporters.MRAExporter#process(pt.lsts.neptus.mra.importers.IMraLogGroup, javax.swing.ProgressMonitor)
     */
    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        if (!canBeApplied(source))
            return I18n.text("No data to process!");
        
        if (PluginUtils.editPluginProperties(this, true)) {
            return I18n.text("Cancelled!");
        }
        
        this.log = source;
        
        LsfIndex lsfIndex = log.getLsfIndex();
        ArrayList<Observation> obsList = null;
        try {
            obsList = ObservationFactory.create(lsfIndex, messagesMillisSeparation);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (obsList != null && !obsList.isEmpty()) {
            httpComm.initializeComm();
            try {
                int i = 0;
                for (Observation observation : obsList) {
                    i++;
                    postHttpRequest = new HttpPost(url);
                    //postHttpRequest.setHeader("Referer", "http://hfradar.ndbc.noaa.gov/tab.php");
                    HttpResponse iGetResultCode = null;
                    try {
//                List <NameValuePair> nvps = new ArrayList <NameValuePair>();
//                nvps.add(new BasicNameValuePair("username", "vip"));
//                nvps.add(new BasicNameValuePair("password", "secret"));
//                httpPost.setEntity(new UrlEncodedFormEntity(nvps));
                        
                        StringEntity  postingString = new StringEntity(observation.toJSON());
                        postHttpRequest.setEntity(postingString);
                        postHttpRequest.setHeader("Content-type", "application/json");
                        
                        long startMillis = System.currentTimeMillis();
                        iGetResultCode = httpComm.getClient().execute(postHttpRequest);
                        httpComm.autenticateProxyIfNeeded(iGetResultCode, null);
                        long endMillis = System.currentTimeMillis();
                        
                        if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                            NeptusLog.pub().info("<###>process [" + iGetResultCode.getStatusLine().getStatusCode() + "] "
                                    + iGetResultCode.getStatusLine().getReasonPhrase() + " code was return from the server"
                                    + "  " + i + " of " + obsList.size() + " | took " + (endMillis - startMillis) + "ms");
//                            if (postHttpRequest != null) {
//                                postHttpRequest.abort();
//                            }
                        }
                        else {
                            NeptusLog.pub().info("<###>process sent " + i + " of " + obsList.size()
                                    + " | took " + (endMillis - startMillis) + "ms");
                            System.out.println(observation.toJSON());
                        }
                        HttpEntity entity1 = iGetResultCode.getEntity();
                        EntityUtils.consume(entity1);
                        postHttpRequest.releaseConnection();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        return e.getMessage();
                    }
                }
                
                return null;
            }
            catch (Exception e) {
                e.printStackTrace();
                return e.getMessage();
            }
            finally {
                httpComm.cleanUp();
            }
            
        }
        else {
            return "No data to send!";
        }
    }
}
