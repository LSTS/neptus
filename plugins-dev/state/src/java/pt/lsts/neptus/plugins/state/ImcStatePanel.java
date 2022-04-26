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
 * Aug 9, 2012
 */
package pt.lsts.neptus.plugins.state;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenuItem;

import pt.lsts.neptus.comm.manager.CommManagerStatusChangeListener;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name="IMC State Panel")
public class ImcStatePanel extends ConsolePanel implements CommManagerStatusChangeListener {


    private static final long serialVersionUID = 1L;
    protected LinkedHashMap<String, JMenuItem> menus = new LinkedHashMap<String, JMenuItem>();
    private final String menuPath = I18n.text("Advanced") + ">" + I18n.text("Incoming messages") + ">";

    /**
     * @param console
     */
    public ImcStatePanel(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void initSubPanel() {
            ImcSystem[] systems = ImcSystemsHolder.lookupAllSystems();
            
            for (ImcSystem s : systems) {                
                ImageIcon icon = null;
                if (s.getVehicle() != null)
                    icon = s.getVehicle().getIcon();
                
                createMenu(s.getName(), icon);
            }
            
            ImcMsgManager.getManager().addStatusListener(this);
    }
    @Override
    public void managerStatusChanged(int status, String msg) {        
        // nothing
    }
    
    protected JMenuItem createMenu(String system, ImageIcon icon) {
        
        final ImcSystem sys = ImcSystemsHolder.lookupSystemByName(system);
        if (sys == null)
            return null;
        
        if (icon == null)
            icon = ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass()));
        
        final Image sysIco = icon.getImage(); 
        
        JMenuItem item = addMenuItem(menuPath + system, icon,
                new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {                
                pt.lsts.imc.gui.ImcStatePanel statePanel = new pt.lsts.imc.gui.ImcStatePanel(ImcMsgManager.getManager().getState(sys.getId()));
                JFrame frm = new JFrame(sys.getName()+" messages");
                frm.setSize(800, 600);
                frm.getContentPane().add(statePanel);                
                frm.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                GuiUtils.centerOnScreen(frm);
                frm.setIconImage(sysIco);
                frm.setVisible(true);
            }
        });
        
        menus.put(system, item);        
        return item;
    }
    
    protected void removeMenu(String system) {
        removeMenuItem(menuPath + system);
    }
    
    @Override
    public void managerSystemAdded(String systemId) {
        createMenu(systemId, null);
    }
    
    @Override
    public void managerSystemRemoved(String systemId) {
        removeMenu(systemId);
    }
    
    @Override
    public void managerSystemStatusChanged(String systemId, int status) {
        
        
    }
    
    @Override
    public void managerVehicleAdded(VehicleType vehicle) {
        createMenu(vehicle.getId(), vehicle.getIcon());
    }
    
    @Override
    public void managerVehicleRemoved(VehicleType vehicle) {
        removeMenu(vehicle.getId());
    }
    
    @Override
    public void managerVehicleStatusChanged(VehicleType vehicle, int status) {
        
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
    
    
}
