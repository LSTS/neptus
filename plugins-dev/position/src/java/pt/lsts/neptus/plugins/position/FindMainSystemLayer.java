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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * Jun 11, 2014
 */
package pt.lsts.neptus.plugins.position;

import java.awt.Color;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.gui.OrientationIcon;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.BaseOrientations;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author pdias
 *
 */
@PluginDescription(author = "Paulo Dias", name = "Find Main System", version = "1.0", 
icon = "pt/lsts/neptus/plugins/position/findsys.png", description = "Points to where to look to find the main system.")
@LayerPriority(priority = 182)
public class FindMainSystemLayer extends ConsoleLayer {

    private static final Color COLOR_WHITE_200 = ColorUtils.setTransparencyToColor(Color.WHITE, 200);
    private static final Color COLOR_GREEN_DARK_220 = ColorUtils.setTransparencyToColor(Color.GREEN.darker(), 220);
    private static final Color COLOR_GREEN_DARK_100 = ColorUtils.setTransparencyToColor(Color.GREEN.darker(), 100);
    private static final Color COLOR_RED_DARK_100 = ColorUtils.setTransparencyToColor(Color.RED.darker(), 100);
    private static final Color COLOR_BLACK_200 = ColorUtils.setTransparencyToColor(Color.BLACK, 200);
    private static final Color COLOR_BLACK_100 = ColorUtils.setTransparencyToColor(Color.BLACK, 100);

    private static final int RECT_WIDTH = 80;
    private static final int RECT_HEIGHT = 80;
    private static final int MARGIN_INT = 5;
    private static final int MARGIN_EXT = 5;

    private static int secondsBeforeMyStatePosOldAge = 30;

    private final OrientationIcon icon = new OrientationIcon(80, 2) {{ 
        setBackgroundColor(COLOR_RED_DARK_100);
        setForegroundColor(COLOR_GREEN_DARK_100);
    }};

    private double baseOrientationRadians = 0; 
    private BaseOrientations absHeadingRadsToLookOrientation = BaseOrientations.North;

    private double absDistanceToLook = Double.NaN;
    private double absHeadingRadsToLook = Double.NaN;
    
    private boolean oldData = true;
    
    private JLabel toDraw = new JLabel();

    public FindMainSystemLayer() {
        toDraw.setForeground(Color.WHITE);
        toDraw.setHorizontalTextPosition(JLabel.CENTER);
        toDraw.setHorizontalAlignment(JLabel.CENTER);
        toDraw.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        toDraw.setBounds(0, 0, RECT_WIDTH, RECT_HEIGHT);
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
        if (sys == null || LocationType.ABSOLUTE_ZERO.equals(sys.getLocation())) {
            clearData();
        }
        else {
            try {
                lt.setLocation(sys.getLocation());
                if (System.currentTimeMillis() - sys.getLocationTimeMillis() > secondsBeforeMyStatePosOldAge * 1000)
                    oldData |= true;
            }
            catch (Exception e) {
                clearData();
                return true;
            }

            LocationType baseLocation = MyState.getLocation();
            if (LocationType.ABSOLUTE_ZERO.equals(baseLocation)) {
                clearData();
                return true;
            }
            baseOrientationRadians = MyState.getHeadingInRadians();

            if (System.currentTimeMillis() - MyState.getLastLocationUpdateTimeMillis() < secondsBeforeMyStatePosOldAge * 1000)
                oldData |= true;

            absDistanceToLook = baseLocation.getHorizontalDistanceInMeters(lt);
            double angleRads = baseLocation.getXYAngle(lt);
            absHeadingRadsToLook = AngleUtils.nomalizeAngleRads2Pi(angleRads);
            absHeadingRadsToLookOrientation = BaseOrientations.convertToBaseOrientationFromRadians(absHeadingRadsToLook);
            icon.setAngleRadians(angleRads - baseOrientationRadians);
        }

        return true;
    }

    private void clearData() {
        icon.setAngleRadians(Double.NaN);
        baseOrientationRadians = Double.NaN;
        absDistanceToLook = Double.NaN;
        absHeadingRadsToLook = Double.NaN;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        boolean validData = true;
        if (Double.isNaN(absDistanceToLook) || Double.isInfinite(absHeadingRadsToLook))
            validData = false;
        
        Graphics2D g2 = (Graphics2D) g.create();
        
        StringBuilder txt = new StringBuilder("<html><div align='center'><b>");
        if (!validData) {
            txt.append("?");
        }
        else {
            txt.append(absHeadingRadsToLookOrientation.getAbbrev()).append("<br/>");
            txt.append(Math.round(Math.toDegrees(absHeadingRadsToLook))).append("\u00B0<br/>");
            int dc = 0;
            if (absDistanceToLook >= 1E3)
                dc = 2;
            if (absDistanceToLook >= 100E3)
                dc = 1;
            txt.append(MathMiscUtils.parseToEngineeringNotation(absDistanceToLook, dc)).append("m");
        }
        txt.append("</b></div></html>");
        toDraw.setText(txt.toString());

        g2.setColor(COLOR_BLACK_200);
        g2.drawRoundRect(renderer.getWidth() - RECT_WIDTH - (MARGIN_INT + MARGIN_EXT), 300 - RECT_HEIGHT
                - (MARGIN_INT + MARGIN_EXT), RECT_WIDTH + MARGIN_INT, RECT_HEIGHT + MARGIN_INT, 20, 20);
        g2.setColor(COLOR_BLACK_100);
        g2.fillRoundRect(renderer.getWidth() - RECT_WIDTH - (MARGIN_INT + MARGIN_EXT), 300 - RECT_HEIGHT
                - (MARGIN_INT + MARGIN_EXT), RECT_WIDTH + MARGIN_INT, RECT_HEIGHT + MARGIN_INT, 20, 20);
        g2.translate(renderer.getWidth() - RECT_WIDTH - (MARGIN_INT + MARGIN_EXT), 300 - RECT_HEIGHT
                - (MARGIN_INT + MARGIN_EXT));
        
        if (oldData) {
            icon.setForegroundColor(COLOR_GREEN_DARK_100);
            icon.setBackgroundColor(COLOR_RED_DARK_100);
        }
        else {
            icon.setForegroundColor(COLOR_GREEN_DARK_220);
            icon.setBackgroundColor(COLOR_WHITE_200);
        }
        if (validData)
            icon.paintIcon(null, g2, 0, 0);
        
        toDraw.setForeground(!oldData ? COLOR_WHITE_200 : COLOR_BLACK_200);
        toDraw.paint(g2);
        toDraw.setForeground(oldData ? COLOR_WHITE_200 : COLOR_BLACK_200);
        g2.translate(1, 1);
        toDraw.paint(g2);

        g2.dispose();
    }
}
