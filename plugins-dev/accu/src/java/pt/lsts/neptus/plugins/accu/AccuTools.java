/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * Apr 15, 2011
 */
package pt.lsts.neptus.plugins.accu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.JOptionPane;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.plugins.MultiSystemIMCMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name="Android CCU Tools", icon="pt/lsts/neptus/plugins/accu/accu.png")
public class AccuTools extends ConsolePanel {

    private static final long serialVersionUID = -5015167316733219875L;

    @NeptusProperty(editable = false)
    private String destination = "accu-10688";

    /**
     * @param console
     */
    public AccuTools(ConsoleLayout console) {
        super(console);
    }


    private MultiSystemIMCMessageListener listener = new MultiSystemIMCMessageListener(this.getClass()
            .getSimpleName() + " [" + Integer.toHexString(hashCode()) + "]") {
        public void messageArrived(ImcId16 id, IMCMessage msg) {            
//            msg.dump(System.out);
//            msg.getMessage("beacon0").dump(System.out);
//            msg.getMessage("beacon1").dump(System.out);
            
            final IMCMessage message = msg;

            Thread t = new Thread() {
                public void run() {
                    Vector<IMCMessage> configs = new Vector<IMCMessage>();
                    Vector<TransponderElement> transponders = MapGroup.getMapGroupInstance(getConsole().getMission()).getAllObjectsOfType(TransponderElement.class);
                    LinkedHashMap<String, TransponderElement> tHash = new LinkedHashMap<String, TransponderElement>();

                    for (TransponderElement el : transponders)            
                        tHash.put(el.getId().toLowerCase(), el);        

                    for (int i = 0; i < 6; i++) {
                        IMCMessage tmp = message.getMessage("beacon"+i);
                        if (tmp != null)
                            configs.add(tmp);
                    }

                    if (!configs.isEmpty()) {   
                        int option = JOptionPane.showConfirmDialog(
                                getConsole(),
                                "Received a message with transponder configurations from "
                                + ImcSystemsHolder.lookupSystem(new ImcId16(message.getHeader().getInteger("src")))
                                        +". Replace current transponder configuration?", "LBL configuration received", JOptionPane.YES_NO_OPTION);

                        if (option == JOptionPane.YES_OPTION) {
                            for (IMCMessage cfg : configs) {
                                String tid = cfg.getAsString("beacon").toLowerCase();
                                TransponderElement elem;
                                LocationType loc = new LocationType();
                                loc.setLatitudeDegs(Math.toDegrees(cfg.getDouble("lat")));
                                loc.setLongitudeDegs(Math.toDegrees(cfg.getDouble("lon")));
                                loc.setDepth(cfg.getDouble("depth"));
                                if (tHash.containsKey(tid)) {
                                    elem = tHash.get(tid);
                                    elem.setCenterLocation(loc);
                                }
                                else {
                                    MapType map =  MapGroup.getMapGroupInstance(getConsole().getMission()).getMaps()[0];
                                    elem = new TransponderElement(map.getMapGroup(), map);
                                    elem.setId(tid);
                                    elem.setCenterLocation(loc);         
                                    elem.setConfiguration(elem.getId()+".conf");
                                    elem.setMapGroup(MapGroup.getMapGroupInstance(getConsole().getMission()));                        
                                    MapGroup.getMapGroupInstance(getConsole().getMission()).getMaps()[0].addObject(elem);                        
                                }
                                NeptusLog.pub().debug("Setting "+elem.getId()+" transponder in the console");
                                elem.getMapGroup().warnListeners(new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED));
                            }
                        }
                    }   
                };
            };
            t.start();
        };
        
    };

    @Override
    public void cleanSubPanel() {
        listener.clean();
    }

    public void initSubPanel() 
    {  
        removeAll();
        setVisibility(false);      
        addMenuItem("Tools>Android CCU>Send map", ImageUtils.getIcon("pt/lsts/neptus/plugins/accu/accu_map.png"), new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                if (getConsole().getMission() == null) {
                    GuiUtils.errorMessage(getConsole(), "Send Map", "Cannot send map with no mission loaded in the console");
                    return;
                }

                try {
                    IMCMessage msg = AccuUtils.getAccuMap(getConsole().getMission());
                    ImcSystem[] systems = ImcSystemsHolder.lookupSystemCCUs();
                    
                    if (systems.length == 0) {
                        GuiUtils.errorMessage(getConsole(), "Send Map", "No active CCUs");
                        return;
                    }
                    String[] names = new String[systems.length];
                    for (int i = 0; i < systems.length; i++)
                        names[i] = systems[i].getName();

                    Object opt = JOptionPane.showInputDialog(getConsole(),"Please enter the address of the destination", "Send Map", JOptionPane.QUESTION_MESSAGE, null, names, destination);
                    if (opt != null) {
                        destination = opt.toString();
                        send(destination, msg);
                    }
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                }
            }
        });

        addMenuItem("Tools>Android CCU>Send Current LBL Config", ImageUtils.getIcon("pt/lsts/neptus/plugins/accu/accu_map.png"), new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                if (getConsole().getMission() == null) {
                    GuiUtils.errorMessage(getConsole(), "Send LBL Config", "Cannot send LBL setup with no mission loaded in the console");
                    return;
                }

                try {
                    IMCMessage msg = IMCUtils.getLblConfig(getConsole().getMission());
                    if (msg == null) {
                        GuiUtils.errorMessage("Send LBL Config", "Unable to generate LblConfig IMC message");
                        return;
                    }
                    ImcSystem[] systems = ImcSystemsHolder.lookupSystemCCUs();
                    if (systems.length == 0) {
                        GuiUtils.errorMessage(getConsole(), "Send LBL Config", "No active CCUs");
                        return;
                    }
                    String[] names = new String[systems.length];
                    for (int i = 0; i < systems.length; i++)
                        names[i] = systems[i].getName();

                    Object opt = JOptionPane.showInputDialog(getConsole(),"Please enter the address of the destination", "Send LBL Config", JOptionPane.QUESTION_MESSAGE, null, names, destination);
                    if (opt != null) {
                        destination = opt.toString();
                        send(destination, msg);
                    }
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                }
            }
        });

        listener.setMessagesToListen("LblConfig");
        listener.setSystemToListen();
    }

    public static void main(String[] args) {
        ConsoleParse.testSubPanel(AccuTools.class);
    }
}
