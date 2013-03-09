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
 * 2009/06/08
 */
package pt.up.fe.dceg.neptus.plugins.position;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.JLabel;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.MainVehicleChangeListener;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mystate.MyState;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.AngleCalc;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

/**
 * @author zp
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(author = "José Pinto, Paulo Dias", name = "FindVehicle", version = "1.1",
        description = "Find vehicle base on base position and orientation.",
        documentation="find-system/find-vehicle.html")
public class FindVehicle extends SimpleSubPanel implements ConfigurationListener, IPeriodicUpdates,
        MainVehicleChangeListener {

    public enum BaseOrientations {
        North, NorthEast, East, SouthEast, South, SouthWest, West, NorthWest;
        public String getAbbrev() {
            return this.name().replaceAll("[a-z]", "");
        }
    }

	private final DecimalFormat formatter = new DecimalFormat("0.00");

	@NeptusProperty(name="Use My Heading", hidden=true, description="Use My Heading for the base location. Don't forget to set it first.")
	public boolean useMyHeading = true;

	@NeptusProperty(name="Base Orientation", description="Where the operator in the basestation is looking at")
	public BaseOrientations baseOrientation = BaseOrientations.North;
	
	@NeptusProperty(name="Use My Location", hidden=true, description="Use My Location for the base location. Don't forget to set it first.")
	public boolean useMyLocation = true;
	
	private static int secondsBeforeMyStatePosOldAge = 30;
	
	@NeptusProperty(name="Base Location", hidden=true, description="Where is the base located")
	public LocationType baseLocation = new LocationType();
	
	@NeptusProperty(name="Font Size", description="The font size. Use '0' for automatic.")
	public int fontSize = DisplayPanel.DEFAULT_FONT_SIZE;

	private double baseOrientationRadians = 0;	
	private final OrientationIcon icon = new OrientationIcon(30,2);
	private final DisplayPanel display = new DisplayPanel("");
	
	public FindVehicle(ConsoleLayout console) {
	    super(console);
		setLayout(new BorderLayout());
		display.setTitle(I18n.text("find vehicle"));
		display.setIcon(icon);
		display.setHorizontalAlignment(JLabel.TRAILING);
		add(display, BorderLayout.CENTER);
		display.setFontSize(fontSize);
	}
	
	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.consolebase.SubPanel#postLoadInit()
	 */
	@Override
	public void initSubPanel() {
        addMenuItem(I18n.text("Settings")+">"+I18n.text("Base Location Settings"), null, new ActionListener() {
	        @Override
            public void actionPerformed(ActionEvent e) {
	            PropertiesEditor.editProperties(FindVehicle.this, getConsole(), true);
	        }
	    });
        addMenuItem(I18n.text("Advanced")+">"+I18n.text("Reset Base Heading"), null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MyState.setHeadingInRadians(baseOrientationRadians);
            }
        });
	}
	
	@Override
	public void propertiesChanged() {
		//baseOrientation increments are 45º
		baseOrientationRadians = Math.toRadians(baseOrientation.ordinal()*45);
		if (useMyHeading)
		    MyState.setHeadingInRadians(baseOrientationRadians);
		display.setFontSize(fontSize);
		
		invalidate();
		revalidate();
	}
	
	@Override
	public boolean update() {
		LocationType lt = new LocationType();

		boolean oldPos = false;
		ImcSystem sys = ImcSystemsHolder.lookupSystemByName(getMainVehicleId());
		try {
            lt.setLocation(sys.getLocation());
            if (System.currentTimeMillis() - sys.getLocationTimeMillis() > secondsBeforeMyStatePosOldAge * 1000)
                oldPos = true;
        }
        catch (Exception e) {
            icon.setAngleRadians(Double.NaN);
            display.setText(I18n.text("pos? "));
            display.setToolTipText(I18n.text("unknown vehicle position"));
            display.setActive(true);
            return true;
        }
		
		if (useMyLocation) {
			baseLocation.setLocation(MyState.getLocation());
			display.setTitle(I18n.textf("find vehicle (base: %orientation)", baseOrientation.getAbbrev()));
			if (System.currentTimeMillis() - MyState.getLastLocationUpdateTimeMillis() < secondsBeforeMyStatePosOldAge * 1000)
                display.setActive(true);
			else
			    display.setActive(false);
		}
		else {
		    display.setTitle(I18n.text("find vehicle"));
            display.setActive(true);
		}
		if (useMyHeading) {
			baseOrientationRadians = MyState.getAxisAnglesRadians()[2];
			baseOrientation = convertToBaseOrientation(baseOrientationRadians);
		}
		double distance = baseLocation.getHorizontalDistanceInMeters(lt);
		double angleRads = baseLocation.getXYAngle(lt);
        icon.setAngleRadians(angleRads - baseOrientationRadians);
		display.setText((int)distance+" m ");
		if (oldPos)
		    display.setForeground(Color.RED.darker());
		else
		    display.setForeground(Color.BLACK);
		display.setToolTipText(formatter.format(Math.toDegrees(angleRads))+"\u00B0");
		return true;
	}
	
	/**
	 * @param baseOrientationRadians2
	 */
	public static BaseOrientations convertToBaseOrientation(double baseOrientationRadians) {
		double headingDegrees = Math.toDegrees(baseOrientationRadians);
		headingDegrees = AngleCalc.nomalizeAngleDegrees360(headingDegrees);
		if(headingDegrees >= -22.5 && headingDegrees <= 22.5)
			return BaseOrientations.North;
		else if(headingDegrees > 22.5 && headingDegrees < 67.5)
			return BaseOrientations.NorthEast;
		else if(headingDegrees >= 67.5 && headingDegrees <= 112.5)
			return BaseOrientations.East;
		else if(headingDegrees > 112.5 && headingDegrees < 157.5)
			return BaseOrientations.SouthEast;
		else if(headingDegrees >= 157.5 && headingDegrees <= 202.5)
			return BaseOrientations.South;
		else if(headingDegrees > 202.5 && headingDegrees < 247.5)
			return BaseOrientations.SouthWest;
		else if(headingDegrees >= 247.5 && headingDegrees <= 292.5)
			return BaseOrientations.West;
		else if(headingDegrees > 292.5 && headingDegrees < 337.5)
			return BaseOrientations.NorthWest;
		return BaseOrientations.North;
	}

	@Override
	public void mainVehicleChangeNotification(String id) {
		display.setTitle(id);
	}
	
	@Override
	public long millisBetweenUpdates() {
		return 1000;
	}

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}