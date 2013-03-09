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
 * Jul 7, 2010
 */
package pt.up.fe.dceg.neptus.plugins.position;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JOptionPane;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.state.ImcSysState;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

/**
 * @author zp
 *
 */
@PluginDescription(icon="pt/up/fe/dceg/neptus/plugins/position/position.png",name="Vehicle Position Clipboard Transferer", author="zp")
public class PositionClipboardTransferer extends SimpleSubPanel {

    private static final long serialVersionUID = 1L;
    protected boolean initCalled = false;

    /**
     * @param console
     */
    public PositionClipboardTransferer(ConsoleLayout console) {
        super(console);
    }

	@Override
	public void initSubPanel() {
		if (initCalled)
			return;
		initCalled = true;
		
		addMenuItem("Tools>Copy Vehicle Position", ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass())), new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				 ImcSystem[] vehicles = ImcSystemsHolder.lookupActiveSystemVehicles();
				 Vector<String> vids = new Vector<String>();
				 
				 for (ImcSystem s : vehicles) {
					 vids.add(s.getName());
				 }
				 
				Object answer = JOptionPane.showInputDialog(getConsole(), "Please choose vehicle where to copy position from", "Copy vehicle position", JOptionPane.OK_CANCEL_OPTION,
						ImageUtils.getIcon(PluginUtils.getPluginIcon(PositionClipboardTransferer.class)),vids.toArray(new String[0]), getConsole().getMainSystem());
				if (answer == null)
					return;
				System.out.println("get position from "+answer);
				
				ImcSysState state = ImcMsgManager.getManager().getState(getConsole().getSystem((String)answer).getVehicle());
				if (state == null)
					return;
				double lat = Math.toDegrees(state.getDouble("GpsFix.GPS.lat"));
				double lon = Math.toDegrees(state.getDouble("GpsFix.GPS.lon"));
				
				LocationType loc = new LocationType();
				loc.setLatitude(lat);
				loc.setLongitude(lon);
				
				System.out.println(loc);
				
				CoordinateUtil.copyToClipboard(loc);
			}
		});
	}

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
