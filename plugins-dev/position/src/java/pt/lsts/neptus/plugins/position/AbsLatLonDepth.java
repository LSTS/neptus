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
 * 2009/06/03
 */
package pt.lsts.neptus.plugins.position;

import java.awt.Color;
import java.awt.GridLayout;
import java.text.DecimalFormat;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.coord.egm96.EGM96Util;

/**
 * @author zp
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Absolute Location Display", icon = "pt/lsts/neptus/plugins/position/position.png", 
        description = "Displays the current vehicle's absolute location (WGS84 coordinates)")
public class AbsLatLonDepth extends ConsolePanel implements ConfigurationListener, IPeriodicUpdates, NeptusMessageListener {

	public enum EnOrientation {Horizontal, Vertical};
	public enum EnZMode {Depth, Altitude, Auto, Invisible};
	public enum EnZRel {HomeRef, WGS84, ASL};
	
	private long lastUpdate = 0;
	
	@NeptusProperty(name="Update interval", description="Interval between updates in milliseconds")
	public long millisBetweenUpdates = 100;
	
	@NeptusProperty(name="Orientation", description="How to show the displays")
	public EnOrientation orientation = EnOrientation.Vertical;
	
	@NeptusProperty(name="Show Seconds", description="Also display seconds in coordinates")
	public boolean showSeconds = false;
	
	@NeptusProperty(name="Z mode", description="How to show the Z coordinate")
	public EnZMode zmode = EnZMode.Depth;

	@NeptusProperty(name="Z relative mode", description="If the Z is relative to the HomeRef")
	public EnZRel isZRel = EnZRel.HomeRef;

	@NeptusProperty(name="Font Size", description="The font size. Use '0' for automatic.")
	public int fontSize = DisplayPanel.DEFAULT_FONT_SIZE;
	
	private final DisplayPanel displayLat, displayLon, displayDepth;
	private final DecimalFormat formatter = new DecimalFormat("0.00");

	private IMCMessage estimatedState = null;
	
	public AbsLatLonDepth(ConsoleLayout console) {
	    super(console);
		displayLat = new DisplayPanel(I18n.text("latitude"));
		displayLon = new DisplayPanel(I18n.text("longitude"));
		displayDepth = new DisplayPanel(I18n.text("altitude"));
		
		displayLat.setFontSize(fontSize);
		displayLon.setFontSize(fontSize);
		displayDepth.setFontSize(fontSize);
		
		removeAll();
		if (orientation == EnOrientation.Horizontal)
			setLayout(new GridLayout(1,0));
		else
			setLayout(new GridLayout(0,1));
		
		add(displayLat);
		add(displayLon);
		add(displayDepth);
	}

	/* (non-Javadoc)
	 * @see pt.lsts.neptus.plugins.NeptusMessageListener#getObservedMessages()
	 */
	@Override
	public String[] getObservedMessages() {
	    return new String[] { "EstimatedState" };
	}
	
	/* (non-Javadoc)
	 * @see pt.lsts.neptus.plugins.NeptusMessageListener#messageArrived(pt.lsts.neptus.imc.IMCMessage)
	 */
	@Override
	public void messageArrived(IMCMessage message) {
	    estimatedState = message.cloneMessage();
	}
		
	@Override
	public void propertiesChanged() {
		removeAll();
		if (orientation == EnOrientation.Horizontal)
			setLayout(new GridLayout(1,0));
		else
			setLayout(new GridLayout(0,1));
		
		displayLat.setFontSize(fontSize);
		displayLon.setFontSize(fontSize);
		displayDepth.setFontSize(fontSize);
		
		add(displayLat);
		add(displayLon);
		if (zmode != EnZMode.Invisible)
			add(displayDepth);
		
		switch (zmode) {
		case Depth:
			if (isZRel == EnZRel.WGS84) {
				displayDepth.setTitle(I18n.text("depth"));
			}
			else if (isZRel == EnZRel.ASL) {
				displayDepth.setTitle(I18n.text("depth (ASL)"));
				/// Above Sea Level
				displayDepth.setToolTipText(I18n.text("ASL"));
			}
			else {
				displayDepth.setTitle(I18n.text("depth (Home)"));
				displayDepth.setToolTipText(I18n.text("Relative to HomeRef"));
			}
			break;
		case Altitude:
			if (isZRel == EnZRel.WGS84) {
				displayDepth.setTitle(I18n.text("altitude"));
			}
			else if (isZRel == EnZRel.ASL) {
				displayDepth.setTitle(I18n.text("altitude (ASL)"));
                /// Above Sea Level
				displayDepth.setToolTipText(I18n.text("ASL"));
			}
			else {
				displayDepth.setTitle(I18n.text("altitude (Home)"));
				displayDepth.setToolTipText(I18n.text("Relative to HomeRef"));
			}
			break;
		default:
			break;
		}
		
		invalidate();
		revalidate();
	}
	
	@Override
	public long millisBetweenUpdates() {
		return millisBetweenUpdates;
	}
	
	boolean connected = true;
	
	@Override
	public boolean update() {
		
		if (connected && System.currentTimeMillis() - lastUpdate > 3000 ) {
			displayDepth.setFontColor(Color.red.darker());
			displayLat.setFontColor(Color.red.darker());
			displayLon.setFontColor(Color.red.darker());
			connected = false;
		}
		
		if (!connected && System.currentTimeMillis() - lastUpdate < 3000) {
			displayDepth.setFontColor(Color.black);
			displayLat.setFontColor(Color.black);
			displayLon.setFontColor(Color.black);
			connected = true;
		}

		
		// Code added to not use the tree

		try {
		    ImcSystem sys = ImcSystemsHolder.lookupSystemByName(getMainVehicleId());
		    if (sys != null) {
		        LocationType loc = sys.getLocation();
		        long locMillis = sys.getLocationTimeMillis();

		        short SYS = 0, EST_STATE = 1, NONE = -1;
		        short mode = -1;

		        IMCMessage tmpEState = estimatedState;
		        
		        if (loc == null && tmpEState == null)
		            mode = NONE;
		        else if (loc != null && tmpEState == null)
		            mode = SYS;
		        else if (loc != null && tmpEState != null) {
		            if (locMillis >= tmpEState.getTimestampMillis())
		                mode = SYS;
		            else
		                mode = EST_STATE;
		        }

                double lat = 0, lon = 0, depth = 0;

		        if (mode == SYS) {
		            lat = loc.getLatitudeRads();
		            lon = loc.getLongitudeRads();
		            depth = loc.getDepth();
		        }

		        if( mode != NONE) {
		            switch (isZRel) {
		                case ASL:
		                    double alt = -depth;
		                    double heightBase = EGM96Util.calcHeight(Math.toDegrees(lat), Math.toDegrees(lon));
		                    depth = -1 * (alt - heightBase);
		                    break;
		                case HomeRef:
		                default:
		                    MissionType miss = getConsole().getMission();
	                        if (miss != null) {
	                            LocationType homeRef = miss.getHomeRef().getNewAbsoluteLatLonDepth();
	                            depth = depth - homeRef.getDepth();
	                        }
		                    break;
		            }

		            switch (zmode) {
		                case Altitude:
		                    depth = -depth;
		                    break;
		                case Auto:
		                    if (depth < 0) {
		                        if (isZRel == EnZRel.WGS84) {
		                            displayDepth.setTitle(I18n.text("altitude"));
		                        }
		                        else if (isZRel == EnZRel.ASL) {
		                            displayDepth.setTitle(I18n.text("altitude (ASL)"));
		                            displayDepth.setToolTipText(I18n.text("ASL"));
		                        }
		                        else {
		                            displayDepth.setTitle(I18n.text("altitude (Home)"));
		                            displayDepth.setToolTipText(I18n.text("Relative to HomeRef"));
		                        }
		                        depth = -depth;
		                    }
		                    else {
		                        if (isZRel == EnZRel.WGS84) {
		                            displayDepth.setTitle(I18n.text("depth"));
		                        }
		                        else if (isZRel == EnZRel.ASL) {
		                            displayDepth.setTitle(I18n.text("depth (ASL)"));
		                            displayDepth.setToolTipText(I18n.text("ASL"));
		                        }
		                        else {
		                            displayDepth.setTitle(I18n.text("depth (Home)"));
		                            displayDepth.setToolTipText(I18n.text("Relative to HomeRef"));
		                        }
		                    }
		                    break;
		                default:
		                    break;
		            }

		            displayLat.setText(CoordinateUtil.latitudeAsPrettyString(Math.toDegrees(lat)));
		            displayLon.setText(CoordinateUtil.longitudeAsPrettyString(Math.toDegrees(lon)));

		            displayDepth.setText(formatter.format(depth));

		            lastUpdate = locMillis;
		        }
		    }
		}
		catch (Exception e) {
		    e.printStackTrace();
		}
		
		return true;
	}
	
	@Subscribe
	public void mainVehicleChangeNotification(ConsoleEventMainSystemChange change) {
	    estimatedState = null;
	}
	
	@Override
	public void cleanSubPanel() {
	    estimatedState = null;
	}

    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
