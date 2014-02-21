/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto and pdias
 * 2006/08/08
 */
package pt.lsts.neptus.console.plugins;

import gnu.io.CommPortIdentifier;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXPanel;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gps.GPSConnection;
import pt.lsts.neptus.gps.GPSListener;
import pt.lsts.neptus.gps.GPSState;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.painters.SubPanelTitlePainter;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.planeditor.IEditorMenuExtension;
import pt.lsts.neptus.planeditor.IMapPopup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.serial.PortSelector;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.coord.egm96.EGM96Util;

import com.l2fprod.common.swing.JLinkButton;

@SuppressWarnings("serial")
@PluginDescription(icon="images/buttons/gpsbutton.png", name="GPS Panel")
@Deprecated
public class GPSPanel extends ConsolePanel implements GPSListener,
		IPeriodicUpdates, IEditorMenuExtension, SubPanelChangeListener {

	@NeptusProperty (name="Use for My Location setting")
	public boolean useForMyLocationSetting = true;

	@NeptusProperty (name="HDOP max", description="HDOP max for use for My Location")
	public double hdopMax = 4;

	@NeptusProperty (name="Use heading")
	public boolean useHeading = true;

	@NeptusProperty (name="Use heading filtering with speed")
	public boolean useHeadingFilterWithSpeed = true;

	@NeptusProperty (name="Ignore heading if speed < X m/s")
	public double useHeadingWithSpeedLessThan = 0.1;

	@NeptusProperty (name="Use altitude (ASL)")
	public boolean useAltitude = false;

	private JLabel statusLabel = new JLabel();
	private JLinkButton connButton = new JLinkButton("connect");
	private SubPanelTitlePainter backPainter;

	private GPSConnection gpsConn = null;
	
	private GPSState gpsState = null;
	
	private Vector<IMapPopup> renderersPopups = new Vector<IMapPopup>();

	
	public GPSPanel(ConsoleLayout console) {
		super(console);
		
		backPainter = new SubPanelTitlePainter("GPS Device");
		
		this.setSize(123, 69);
		setLayout(new BorderLayout(2,2));				
		
		JXPanel holder = new JXPanel();
		holder.setLayout(new BorderLayout(0,0));
        holder.setBackgroundPainter(backPainter);
        this.setLayout(new BorderLayout());
        this.add(holder);
        
        holder.add(statusLabel, BorderLayout.CENTER);		
		statusLabel.setText("<html><font color='red'>Not Connected</font></blockquote></html>");
		//this.setBorderTitle("GPS Device");        
		JPanel dummy = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		dummy.setOpaque(false);
		connButton.setFocusPainted(false);
		connButton.setMargin(new Insets(0,0,0,0));		
		connButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread() {
				    @Override
				    public void run() {
				        connectToggle();
				    }
				}.start();	
			}
		});
		dummy.add(connButton);
		holder.add(dummy, BorderLayout.SOUTH);
		
		setPreferredSize(new Dimension(200, 75));
		setMinimumSize(new Dimension(200, 75));
	}
	
	/* (non-Javadoc)
	 * @see pt.lsts.neptus.plugins.SimpleSubPanel#init()
	 */
	@Override
	public void initSubPanel() {

		renderersPopups = getConsole().getSubPanelsOfInterface(IMapPopup.class);
		for (IMapPopup str2d : renderersPopups) {
			str2d.addMenuExtension(this);
		}

	}
	
	/* (non-Javadoc)
	 * @see pt.lsts.neptus.consolebase.SubPanelChangeListener#subPanelChanged(pt.lsts.neptus.consolebase.SubPanelChangeEvent)
	 */
	@Override
	public void subPanelChanged(SubPanelChangeEvent panelChange) {

		if (panelChange == null)
			return;

		renderersPopups = getConsole().getSubPanelsOfInterface(IMapPopup.class);

		if (ReflectionUtil.hasInterface(panelChange.getPanel().getClass(),
				IMapPopup.class)) {

			IMapPopup sub = (IMapPopup) panelChange.getPanel();

			if (panelChange.added()) {
				renderersPopups.add(sub);
				IMapPopup str2d = sub;
				if (str2d != null) {
					str2d.addMenuExtension(this);
				}
			}

			if (panelChange.removed()) {
				renderersPopups.remove(sub);
				IMapPopup str2d = sub;
				if (str2d != null) {
					str2d.removeMenuExtension(this);
				}
			}
		}
	}

	
	private void connectToggle() {
		try {
            connButton.setEnabled(false);

            if (gpsConn == null) {
            	//CommPortIdentifier commID = PortSelector.showSerialPortSelectionDialog();
            	PortSelector ps = PortSelector.showSerialPortSelectionDialog(
            			SwingUtilities.getWindowAncestor(this), true);
            	CommPortIdentifier commID = ps.getSelectedPort();

            	if (commID == null) {
            		connButton.setSelected(false);
            		connButton.setEnabled(true);
            		return;
            	}
            	NeptusLog.pub().info("<###> "+commID.getName() + "   " + ps.getSerialPortParameters().getBaudrate());
            	gpsConn = new GPSConnection(commID.getName(), ps
            			.getSerialPortParameters().getBaudrate());// 9600
            	gpsConn.connect();
            	gpsConn.addGPSListener(this);
            	connButton.setText("disconnect");
                connButton.setEnabled(true);
            }
            else {
            	gpsConn.removeGPSListener(this);
            	gpsConn.disconnect();
            	gpsConn = null;
            	connButton.setText("connect");			
                connButton.setEnabled(true);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            connButton.setEnabled(true);
        }
	}

	/* (non-Javadoc)
	 * @see pt.lsts.neptus.plugins.SimpleSubPanel#clean()
	 */
	@Override
	public void cleanSubPanel() {
		
		if (gpsConn != null) {
			new Thread() {
				public void run() {
					try {
						gpsConn.removeGPSListener(GPSPanel.this);
						gpsConn.disconnect();
						gpsConn = null;
					} catch (Exception e) {
						e.printStackTrace();
					}					
				};
			};
		}
	}
	
	public void GPSStateChanged(GPSState oldState, GPSState newState) {
		gpsState = newState;
		
		statusLabel.setText("<html>"+
				CoordinateUtil.latitudeAsString(newState.getLatitude())+ " " + CoordinateUtil.longitudeAsString(newState.getLongitude())+"<br>"+
				"Heading: "+GuiUtils.getNeptusDecimalFormat(2).format(newState.getHeading())+"\u00B0 "+
				"Altitude: "+GuiUtils.getNeptusDecimalFormat(2).format(newState.getAltitude())+
				"m <br>HDOP: "+GuiUtils.getNeptusDecimalFormat(1).format(newState.getHdop())+""+
				" Speed: "+GuiUtils.getNeptusDecimalFormat(1).format(newState.getSpeed()*1000/3600)+"m/s"+
				"</html>");
	}

	@Override
	public long millisBetweenUpdates() {
		return 200;
	}

	@Override
	public boolean update() {
		if (!useForMyLocationSetting)
			return true;
		GPSState gps = gpsState;
		if (gps == null)
				return true;
		
		if (gps.getHdop() > hdopMax)
			return true;
		
		LocationType loc = new LocationType();
		loc.setLatitudeDegs(gps.getLatitude());
		loc.setLongitudeDegs(gps.getLongitude());
		if (useAltitude) {
			double alt = gps.getAltitude();
			double heightBase = EGM96Util.calcHeight(loc.getLatitudeDegs(), loc.getLongitudeDegs());
			loc.setDepth(-1*(alt+heightBase));
		}
		else {
			loc.setDepth(0);
		}
		if (useHeading
				&& (!useHeadingFilterWithSpeed || gps.getSpeed() > useHeadingWithSpeedLessThan)) {
			MyState.setLocationAndAxis(loc, gps.getHeading());
		}
		else {
			if (!useHeadingFilterWithSpeed)
				MyState.setLocationAndAxis(loc, 0);
		}
		
		return true;
	}

	/* (non-Javadoc)
	 * @see pt.lsts.neptus.planeditor.IEditorMenuExtension#getApplicableItems(pt.lsts.neptus.types.coord.LocationType, pt.lsts.neptus.planeditor.IMapPopup)
	 */
	@Override
	public Collection<JMenuItem> getApplicableItems(LocationType loc,
			IMapPopup source) {
		
		Vector<JMenuItem> menus = new Vector<JMenuItem>();
		
		JMenu myLocMenu = new JMenu("GPS Panel");
		menus.add(myLocMenu);

		AbstractAction copy = new AbstractAction("Copy ") {
			@Override
			public void actionPerformed(ActionEvent e) {
				LocationType loc = new LocationType();
				loc.setLatitudeDegs(gpsState.getLatitude());
				loc.setLongitudeDegs(gpsState.getLongitude());
				if (useAltitude) {
					double alt = gpsState.getAltitude();
					double heightBase = EGM96Util.calcHeight(loc.getLatitudeDegs(), loc.getLongitudeDegs());
					loc.setDepth(-1*(alt+heightBase));
				}
				else {
					loc.setDepth(0);
				}
				ClipboardOwner owner = new ClipboardOwner() {
					public void lostOwnership(Clipboard clipboard,
							Transferable contents) {
					};
				};
				Toolkit.getDefaultToolkit()
						.getSystemClipboard()
						.setContents(
								new StringSelection(loc
										.getClipboardText()), owner);
			}
		};
		JMenuItem jmc = new JMenuItem(copy);
		if (gpsState == null)
			jmc.setEnabled(false);
		else
			jmc.setEnabled(true);
		myLocMenu.add(jmc);

		AbstractAction settings = new AbstractAction("Settings") {
			@Override
			public void actionPerformed(ActionEvent e) {
				PropertiesEditor.editProperties(GPSPanel.this, getConsole(), true);
			}
		};
		myLocMenu.add(new JMenuItem(settings));
		
		return menus;
	}

	
	public static void main(String[] args) {
		GuiUtils.testFrame(new GPSPanel(null), "tesd");
	}
}
