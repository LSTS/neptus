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
 * 2010/07/01
 */
package pt.lsts.neptus.plugins.teleoperation;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.InterpolationColorMap;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author ZP
 *
 */
public class TeleoperationPanel extends ConsolePanel implements IPeriodicUpdates {
	
	private static final long serialVersionUID = 1L;
    ColorMap normal = ColorMapFactory.createRedYellowGreenColorMap();
	ColorMap inverted = ColorMapFactory.createInvertedColorMap((InterpolationColorMap)normal);
	protected SimpleTurnGauge turnGauge = new SimpleTurnGauge();
	protected SimpleVerticalGauge motorLeft = new SimpleVerticalGauge();
	protected SimpleVerticalGauge motorRight = new SimpleVerticalGauge();	
	protected SimpleOrientationGauge orientation = new SimpleOrientationGauge();
	protected LocationType destination = null;
	
	public TeleoperationPanel(ConsoleLayout console) {
	    super(console);
		setLayout(new BorderLayout());
		add(motorLeft, BorderLayout.WEST);
		add(motorRight, BorderLayout.EAST);
		add(turnGauge, BorderLayout.SOUTH);
		add(orientation, BorderLayout.CENTER);
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
				JPopupMenu popup = new JPopupMenu();
				popup.add("Head to copied location").addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent arg0) {
						Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

						if ((contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
							try {
								String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
								LocationType lt = new LocationType();
								lt.fromClipboardText(text);
								destination = new LocationType(lt);
							} catch (Exception ex) {
								NeptusLog.pub().error(ex);
							}
						}
					}
				});	
				popup.show(TeleoperationPanel.this, e.getX(), e.getY());
			}
		});
	}
	
	
	@Override
	public long millisBetweenUpdates() {
		return 100;
	}
	
	@Override
	public boolean update() {
		// TODO Auto-generated method stub
		return false;
	}

	public static void main(String[] args) {
		GuiUtils.testFrame(new TeleoperationPanel(null));
	}


    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }


    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
