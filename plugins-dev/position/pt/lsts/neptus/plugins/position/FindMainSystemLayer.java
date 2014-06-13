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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * Jun 11, 2014
 */
package pt.lsts.neptus.plugins.position;

import java.awt.Color;
import java.awt.Graphics2D;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.position.FindVehicle.BaseOrientations;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ColorUtils;

/**
 * @author pdias
 *
 */
@PluginDescription(author = "Paulo Dias", name = "Find Main System", version = "0.9", 
icon = "pt/lsts/neptus/plugins/position/findsys.png", description = "Points to where to look to find the main system.")
@LayerPriority(priority = 182)
public class FindMainSystemLayer extends ConsoleLayer {

    private static int secondsBeforeMyStatePosOldAge = 30;

    private final DecimalFormat formatter = new DecimalFormat("0.00");
    private final OrientationIcon icon = new OrientationIcon(80, 2) {{ 
        setBackgroundColor(ColorUtils.setTransparencyToColor(Color.WHITE, 100));
        setForegroundColor(ColorUtils.setTransparencyToColor(Color.GREEN.darker(), 100));
    }};

    private double baseOrientationRadians = 0; 
    private BaseOrientations baseOrientation = BaseOrientations.North;

    private double absDistanceToLook = Double.NaN;
    private double absHeadingRadsToLook = Double.NaN;
    
    private boolean oldData = true;
    
    private JLabel toDraw = new JLabel();

    public FindMainSystemLayer() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#userControlsOpacity()
     */
    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#initLayer()
     */
    @Override
    public void initLayer() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#cleanLayer()
     */
    @Override
    public void cleanLayer() {

    }
    
    @Periodic(millisBetweenUpdates = 1000)
    public boolean update() {
        LocationType lt = new LocationType();

        oldData = false;
        ImcSystem sys = ImcSystemsHolder.lookupSystemByName(getConsole().getMainSystem());
        if (sys == null) {
            icon.setAngleRadians(Double.NaN);
            baseOrientationRadians = Double.NaN;
            absDistanceToLook = Double.NaN;
            absHeadingRadsToLook = Double.NaN;
        }
        else {
            try {
                lt.setLocation(sys.getLocation());
                if (System.currentTimeMillis() - sys.getLocationTimeMillis() > secondsBeforeMyStatePosOldAge * 1000)
                    oldData |= true;;
            }
            catch (Exception e) {
                icon.setAngleRadians(Double.NaN);
                baseOrientationRadians = Double.NaN;
                absDistanceToLook = Double.NaN;
                absHeadingRadsToLook = Double.NaN;
                return true;
            }
            
            LocationType baseLocation = MyState.getLocation();
            baseOrientationRadians = MyState.getHeadingInRadians();
            baseOrientation = FindVehicle.convertToBaseOrientation(baseOrientationRadians);
            baseOrientation.getAbbrev();
            
            if (System.currentTimeMillis() - MyState.getLastLocationUpdateTimeMillis() < secondsBeforeMyStatePosOldAge * 1000)
                oldData |= true;;
                
                absDistanceToLook = baseLocation.getHorizontalDistanceInMeters(lt);
                double angleRads = baseLocation.getXYAngle(lt);
                absHeadingRadsToLook = angleRads - baseOrientationRadians;
                icon.setAngleRadians(absHeadingRadsToLook);
        }

        return true;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (Double.isNaN(absDistanceToLook))
            return;
        
        Graphics2D g2 = (Graphics2D) g.create();
        
        String txt = baseOrientation.getAbbrev();
        toDraw.setText(txt);
        toDraw.setForeground(Color.white);
        toDraw.setHorizontalTextPosition(JLabel.CENTER);
        toDraw.setHorizontalAlignment(JLabel.LEFT);
        toDraw.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        g2.translate(renderer.getWidth() - 10 - icon.getIconWidth(), 100);
        
        if (oldData)
            icon.setBackgroundColor(ColorUtils.setTransparencyToColor(Color.RED.darker(), 100));
        else
            icon.setBackgroundColor(ColorUtils.setTransparencyToColor(Color.WHITE.darker(), 100));
        icon.paintIcon(null, g2, 0, 0);
        
//        toDraw.setBounds(0, 0, RECT_WIDTH, RECT_HEIGHT);
        toDraw.paint(g2);

        
        g2.dispose();
    }
}
