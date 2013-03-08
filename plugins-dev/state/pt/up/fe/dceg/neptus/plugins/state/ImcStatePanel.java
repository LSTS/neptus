/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Aug 9, 2012
 * $Id:: ImcStatePanel.java 10012 2013-02-21 14:23:45Z pdias                    $:
 */
package pt.up.fe.dceg.neptus.plugins.state;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenuItem;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.CommManagerStatusChangeListener;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

/**
 * @author zp
 *
 */
@PluginDescription(name="IMC State Panel")
public class ImcStatePanel extends SimpleSubPanel implements CommManagerStatusChangeListener {


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
                pt.up.fe.dceg.neptus.imc.gui.ImcStatePanel statePanel = new pt.up.fe.dceg.neptus.imc.gui.ImcStatePanel(ImcMsgManager.getManager().getState(sys.getId()));
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
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
    
    
}
