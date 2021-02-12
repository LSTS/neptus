/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Jul 7, 2010
 */
package pt.lsts.neptus.plugins.position;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JOptionPane;

import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
@PluginDescription(icon="pt/lsts/neptus/plugins/position/position.png",name="Vehicle Position Clipboard Transferer", author="zp")
public class PositionClipboardTransferer extends ConsolePanel {

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
				NeptusLog.pub().info("<###>get position from "+answer);
				
				ImcSystemState state = ImcMsgManager.getManager().getState(getConsole().getSystem((String)answer).getVehicle().getId());
				if (state == null)
					return;
				double lat = Math.toDegrees(state.getDouble("GpsFix.GPS.lat"));
				double lon = Math.toDegrees(state.getDouble("GpsFix.GPS.lon"));
				
				LocationType loc = new LocationType();
				loc.setLatitudeDegs(lat);
				loc.setLongitudeDegs(lon);
				
				NeptusLog.pub().info("<###> "+loc);
				
				CoordinateUtil.copyToClipboard(loc);
			}
		});
	}

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
