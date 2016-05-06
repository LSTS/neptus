/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * 06/05/2016
 */
package pt.lsts.neptus.historicdata;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import pt.lsts.imc.HistoricData;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.historic.DataStore;
import pt.lsts.neptus.NeptusLog;

/**
 * @author zp
 *
 */
public class HistoricWebAdapter {
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private String getURL = "http://ripples.lsts.pt/datastore/search";
    private String postURL = "http://ripples.lsts.pt/datastore";
    private HttpClient client = new HttpClient();
    private DataStore dataStore;
    private long lastPoll = System.currentTimeMillis() - 1200 * 1000;
    
    public HistoricWebAdapter(DataStore dataStore) {
        this.dataStore = dataStore;        
    }
    
    public Future<Boolean> upload() {
        return executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                HistoricData data = dataStore.pollData(0, 64000);
                try {
                    PostMethod post = new PostMethod(postURL);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    IMCOutputStream out = new IMCOutputStream(baos);
                    out.writeMessage(data);
                    out.close();
                    ByteArrayRequestEntity body = new ByteArrayRequestEntity(baos.toByteArray());
                    post.setRequestEntity(body);
                    int response = client.executeMethod(post);
                    if (response != 200)
                        throw new Exception("HTTP Status "+response+": "+post.getResponseBodyAsString());
                    NeptusLog.pub().info("Uploaded local data to the web.");
                    return true;
                }
                catch (Exception e) {
                    dataStore.addData(data);
                    e.printStackTrace();
                    NeptusLog.pub().error(e);
                    return false;
                }
            } 
        });
    }
    
    
    
    public Future<HistoricData> download(HistoricData data) {
        return executor.submit(new Callable<HistoricData>() {
            
            @Override
            public HistoricData call() throws Exception {
                
                try {
                    
                }
                catch (Exception e) {
                    
                }
                
                return null;
            }                        
        });
    }
    
}
