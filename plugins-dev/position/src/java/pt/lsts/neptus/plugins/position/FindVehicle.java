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
 * 2009/06/08
 */
package pt.lsts.neptus.plugins.position;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.JLabel;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.gui.OrientationIcon;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.types.coord.BaseOrientations;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(author = "José Pinto, Paulo Dias", name = "FindVehicle", version = "1.1",
        description = "Find vehicle base on base position and orientation.",
        documentation="find-system/find-vehicle.html")
public class FindVehicle extends ConsolePanel implements ConfigurationListener, IPeriodicUpdates,
        MainVehicleChangeListener {

    private final DecimalFormat formatter = new DecimalFormat("0.00");

	@NeptusProperty(name="Use My Heading", editable = true, description="Use My Heading for the base location. Don't forget to set it first.")
	public boolean useMyHeading = true;

	@NeptusProperty(name="Base Orientation", description="Where the operator in the basestation is looking at")
	public BaseOrientations baseOrientation = BaseOrientations.North;
	
	@NeptusProperty(name="Use My Location", editable = true, description="Use My Location for the base location. Don't forget to set it first.")
	public boolean useMyLocation = true;
	
	private static int secondsBeforeMyStatePosOldAge = 30;
	
	@NeptusProperty(name="Base Location", editable = true, description="Where is the base located")
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
	 * @see pt.lsts.neptus.consolebase.SubPanel#postLoadInit()
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
            e.printStackTrace();
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
			baseOrientation = BaseOrientations.convertToBaseOrientationFromRadians(baseOrientationRadians);
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
	
    @Subscribe
	public void mainVehicleChangeNotification(ConsoleEventMainSystemChange evt) {
		display.setTitle(evt.getCurrent());
	}
	
	@Override
	public long millisBetweenUpdates() {
		return 1000;
	}

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}