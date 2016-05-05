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
 * 05/05/2016
 */
package pt.lsts.neptus.historicdata;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.Heartbeat;
import pt.lsts.imc.HistoricDataQuery;
import pt.lsts.imc.HistoricDataQuery.TYPE;
import pt.lsts.imc.historic.DataStore;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;

/**
 * This plugin is used to retrieve and display historic data 
 * stored in the vehicles and on the web (Ripples)
 * @author zp
 *
 */
@PluginDescription(name="Historic Data", experimental=true)
public class HistoricDataInteraction extends ConsoleInteraction {

    private DataStore dataStore = new DataStore();
    private LinkedHashMap<String, Long> lastPollTime = new LinkedHashMap<>();
    private int req_id = 1;
    
    @NeptusProperty(name="Seconds between periodic data requests")
    private int secsBetweenPolling = 60;
        
    @Override
    public void initInteraction() {
        
    }

    @Override
    public void cleanInteraction() {
        
    }
    
    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source){
        if (SwingUtilities.isRightMouseButton(event))
            rightClick(event.getPoint(), source);        
    }
    
    @Subscribe
    public void on(Heartbeat beat) {
        if (ImcSystemsHolder.lookupSystem(beat.getSrc()).getType() == SystemTypeEnum.VEHICLE) {
            if (lastPollTime.containsKey(beat.getSourceName())) {
                if (System.currentTimeMillis() - lastPollTime.get(beat.getSourceName()) < secsBetweenPolling * 1000) {
                    return;
                }
            }
            pollDataFrom(beat.getSourceName());            
        }
    }
    
    private void pollDataFrom(String system) {
        HistoricDataQuery req = new HistoricDataQuery();
        req.setReqId(req_id++);
        req.setType(TYPE.QUERY);
        req.setMaxSize(64000);
        NeptusLog.pub().info("Polling historic data from "+system);
        lastPollTime.put(system, System.currentTimeMillis());
        getConsole().getImcMsgManager().sendMessageToSystem(req, system);
    }
    
    @Subscribe
    public void on(HistoricDataQuery query) {
        if (query.getType() == HistoricDataQuery.TYPE.REPLY) {
            dataStore.addData(query.getData());
            HistoricDataQuery clear = new HistoricDataQuery();
            clear.setType(TYPE.CLEAR);
            clear.setData(null);
            clear.setReqId(query.getReqId());
            NeptusLog.pub().info("Clearing received data from "+query.getSourceName());
            getConsole().getImcMsgManager().sendMessageToSystem(clear, query.getSourceName());
            
            // If message's size is near the requested size, retrieve more data
            if (query.getSize() > 50000)
                pollDataFrom(query.getSourceName());            
        }
    }
    
    private void rightClick(Point2D point, StateRenderer2D source) {
        JPopupMenu popup = new JPopupMenu();
        popup.add(I18n.text("Poll historic data")).addActionListener(this::pollHistoricData);
        popup.add(I18n.text("Clear local data")).addActionListener(this::clearLocalData);
        popup.add(I18n.text("Upload local data")).addActionListener(this::uploadLocalData);
        popup.show(source, (int) point.getX(), (int) point.getY());
    }
    
    private void pollHistoricData(ActionEvent evt) {
        pollDataFrom(getConsole().getMainSystem());        
    }
    
    private void clearLocalData(ActionEvent evt) {
        //TODO
    }
    
    private void uploadLocalData(ActionEvent evt) {
        //TODO
    }    

    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        // TODO
    }
}
