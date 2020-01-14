/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Apr 1, 2015
 */
package convcao.com.agent.test;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.Announce;
import pt.lsts.imc.Distance;
import pt.lsts.imc.EntityInfo;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.comm.transports.ImcUdpTransport;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;

/**
 * @author zp
 *
 */
public class AcousticReplay {
    
    private int destPort = 6001;
    private String destHost = "localhost";

    private EstimatedState lastState = new EstimatedState();
    private LinkedHashMap<String, Distance> lastDistances = new LinkedHashMap<>();
    private ImcUdpTransport transport = new ImcUdpTransport(9999, IMCDefinition.getInstance());
    
    public AcousticReplay(String destHost, int destPort) {
        this.destHost = destHost;
        this.destPort = destPort;        
    }
    
    @Periodic(millisBetweenUpdates = 10000)
    public void sendData() {
        lastState.setTimestampMillis(System.currentTimeMillis());
        //transport.sendMessage(destHost, destPort, lastState);
        
        Distance[] beams = new Distance[4];
        beams[0] = lastDistances.get("DVL Beam 0");
        beams[1] = lastDistances.get("DVL Beam 1");
        beams[2] = lastDistances.get("DVL Beam 2");
        beams[3] = lastDistances.get("DVL Beam 3");
        
        for (int i = 0; i < 4; i++) {
            if (beams[i] != null) {
                beams[i].setTimestampMillis(System.currentTimeMillis());
                System.out.println(beams[i]);
                transport.sendMessage(destHost, destPort, beams[i]);
            }
        }
    }
    
    @Subscribe
    public void on(EntityInfo el) {
        IMCDefinition.getInstance().getResolver().setEntityName(el.getSrc(), el.getSrcEnt(), el.getLabel());
        el.setTimestampMillis(System.currentTimeMillis());
        transport.sendMessage(destHost, destPort, el);      
    }
    
    @Subscribe
    public void on(Announce el) {
        IMCDefinition.getInstance().getResolver().addEntry(el.getSrc(), el.getSysName());
        el.setServices("");
        el.setTimestampMillis(System.currentTimeMillis());
        //transport.sendMessage(destHost, destPort, el);
      //  System.out.println(el);
        
    }
    
//    @Subscribe
//    public void on(Heartbeat el) {
//        el.setTimestampMillis(System.currentTimeMillis());
//        transport.sendMessage(destHost, destPort, el);
//        System.out.println(el);
//        
//    }
    
    @Subscribe
    public void on(EstimatedState state) {
         lastState = state;
    }
    
    @Subscribe
    public void on(Distance d) {        
        
        lastDistances.put(d.getEntityName(), d);        
    }
    
    /**
     * Just for testing purposes. This method will start broadcasting all messages in the log according to time
     * separations.
     * 
     * @param timeMultiplier If 1.0 is used, the separation between messages will be approximately the same as
     *            real-time. If this value is higher, the replay will be done faster in the same proportion.
     */
    public static void replay(LsfIndex index, Object listener) {
        
        EventBus bus = new EventBus();
        bus.register(listener);
        int numMessages = index.getNumberOfMessages();

        int generatorSrcId = index.getMessage(0).getHeader().getInteger("src");

        long localStartTime = System.currentTimeMillis();
        long lastPrint = 0;
        
        TimeZone tz = TimeZone.getTimeZone("UTC");

        DateFormat dfGMT = DateFormat.getTimeInstance(DateFormat.DEFAULT);
        dfGMT.setTimeZone(tz);

        for (int i = 0; i < numMessages - 1; i++) {
            long curTime = ((long) (1000.0 * (index.timeOf(i) - index.getStartTime()))) + localStartTime;
            long sleep_millis = curTime - System.currentTimeMillis();
            IMCMessage m = index.getMessage(i);
            bus.post(m);
            //transport.sendMessage("127.0.0.1", 6002, m);
            int src = m.getHeader().getInteger("src");

            if (src == generatorSrcId && sleep_millis > 5 && index.timeOf(i + 1) > index.timeOf(i)) {
                try {
                    Thread.sleep(sleep_millis);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            long time = 1000 * (long) index.timeOf(i);
            if (src == generatorSrcId && time > lastPrint) {
                System.out.println(dfGMT.format(new Date(time)));
                lastPrint = time;
            }
        }
        
        bus.unregister(listener);
    }
    
    public static void main(String[] args) throws Exception {
        String logFile = "/home/zp/workspace/logs/094442_c2_search-lowfreq/Data.lsf";
        AcousticReplay replay = new AcousticReplay("localhost", 6001);
        PeriodicUpdatesService.registerPojo(replay);
        LsfIndex index = new LsfIndex(new File(logFile));
        replay(index, replay);
        PeriodicUpdatesService.unregisterPojo(replay);
    }
}
