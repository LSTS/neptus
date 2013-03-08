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
 * 2009/06/03
 * $Id:: AbsLatLonDepth.java 10012 2013-02-21 14:23:45Z pdias             $:
 */
package pt.up.fe.dceg.neptus.plugins.position;

import java.awt.Color;
import java.awt.GridLayout;
import java.text.DecimalFormat;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;
import pt.up.fe.dceg.neptus.util.coord.egm96.EGM96Util;

/**
 * @author zp
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Absolute Location Display", icon = "pt/up/fe/dceg/neptus/plugins/position/position.png", 
        description = "Displays the current vehicle's absolute location (WGS84 coordinates)")
public class AbsLatLonDepth extends SimpleSubPanel implements ConfigurationListener, IPeriodicUpdates, NeptusMessageListener {

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
	 * @see pt.up.fe.dceg.neptus.plugins.NeptusMessageListener#getObservedMessages()
	 */
	@Override
	public String[] getObservedMessages() {
	    return new String[] { "EstimatedState" };
	}
	
	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.plugins.NeptusMessageListener#messageArrived(pt.up.fe.dceg.neptus.imc.IMCMessage)
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
		            lat = loc.getLatitudeAsDoubleValueRads();
		            lon = loc.getLongitudeAsDoubleValueRads();
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

		            displayLat.setText(CoordinateUtil.latitudeAsPrettyString(Math.toDegrees(lat), showSeconds));
		            displayLon.setText(CoordinateUtil.longitudeAsPrettyString(Math.toDegrees(lon), showSeconds));

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
	
	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#mainVehicleChangeNotification(java.lang.String)
	 */
	@Override
	public void mainVehicleChangeNotification(String id) {
	    estimatedState = null;
	}
	
	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
	 */
	@Override
	public void cleanSubPanel() {
	    estimatedState = null;
	}

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
