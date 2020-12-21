/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author Paulo Dias
 * 
 */
public class StatusLed extends JPanel implements MouseListener {
    private static final long serialVersionUID = -4028301444416809125L;

    /** unknown value */
    public static final short LEVEL_NONE = -2;
    public static final short LEVEL_OFF = -1;
    public static final short LEVEL_0 = 0;
    public static final short LEVEL_1 = 1;
    public static final short LEVEL_2 = 2;
    public static final short LEVEL_3 = 3;
    public static final short LEVEL_4 = 4;

    private final ImageIcon LED_NOT = new ImageIcon(ImageUtils.getScaledImage("images/led_not.png", 14, 14));
    private final ImageIcon LED_OFF = new ImageIcon(ImageUtils.getScaledImage("images/led_none.png", 14, 14));
    private final ImageIcon LED_GREEN = new ImageIcon(ImageUtils.getScaledImage("images/led_green.png", 14, 14));
    private final ImageIcon LED_BLUE = new ImageIcon(ImageUtils.getScaledImage("images/led_blue.png", 14, 14));
    private final ImageIcon LED_YELLOW = new ImageIcon(ImageUtils.getScaledImage("images/led_yellow.png", 14, 14));
    private final ImageIcon LED_ORANGE = new ImageIcon(ImageUtils.getScaledImage("images/led_orange.png", 14, 14));
    private final ImageIcon LED_RED = new ImageIcon(ImageUtils.getScaledImage("images/led_red.png", 14, 14));

    private final ImageIcon LED_NOT_BIG = new ImageIcon(ImageUtils.getScaledImage("images/led_not.png", 44, 44)); // 44x44
    private final ImageIcon LED_OFF_BIG = new ImageIcon(ImageUtils.getImage("images/bola_none.png"));
    private final ImageIcon LED_GREEN_BIG = new ImageIcon(ImageUtils.getImage("images/bola_green.png"));
    private final ImageIcon LED_BLUE_BIG = new ImageIcon(ImageUtils.getImage("images/bola_blue.png"));
    private final ImageIcon LED_YELLOW_BIG = new ImageIcon(ImageUtils.getImage("images/bola_yellow.png"));
    private final ImageIcon LED_ORANGE_BIG = new ImageIcon(ImageUtils.getImage("images/bola_orange.png"));
    private final ImageIcon LED_RED_BIG = new ImageIcon(ImageUtils.getImage("images/bola_red.png"));

    private final Color COLOR_NOT = Color.BLACK;
    private final Color COLOR_OFF = Color.GRAY;
    private final Color COLOR_GREEN = new Color(0, 200, 125);
    private final Color COLOR_BLUE = Color.BLUE;
    public static final Color COLOR_YELLOW = new Color(200, 200, 0);
    public static final Color COLOR_ORANGE = new Color(255, 180, 0);
    public static final Color COLOR_RED = Color.RED;

    private final ImageIcon IMC_GREEN = new ImageIcon(ImageUtils.getImage("images/imc_green.png"));
    private final ImageIcon IMC_RED = new ImageIcon(ImageUtils.getImage("images/imc_red.png"));
    private final ImageIcon IMC_OFF = new ImageIcon(ImageUtils.getImage("images/imc_off.png"));

    private final ImageIcon SNAPSHOT_ON = new ImageIcon(ImageUtils.getScaledImage("images/menus/snapshot.png",
            14, 14));
    private  final ImageIcon SNAPSHOT_OFF = new ImageIcon(ImageUtils.getScaledImage(
            "images/menus/snapshot_off.png", 14, 14));

    private  final ImageIcon WEB_PUBLISH_ON = new ImageIcon(ImageUtils.getScaledImage(
            "images/buttons/webup_enabled.png", 14, 14));
    private final ImageIcon WEB_PUBLISH_OFF = new ImageIcon(ImageUtils.getScaledImage(
            "images/buttons/webup_disabled.png", 14, 14));

    private LinkedHashMap<Short, Color> levelsColors;
    private LinkedHashMap<Short, ImageIcon> levelsIcons;

    private JLabel statusIndicator = null;

    private short level = LEVEL_NONE;

    /**
     * This is the default constructor
     */
    public StatusLed() {
        super();
        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setLayout(new BorderLayout());

        LinkedHashMap<Short, ImageIcon> levelsIconsTemp;
        LinkedHashMap<Short, Color> levelsColorsTemp;

        levelsIconsTemp = new LinkedHashMap<Short, ImageIcon>();
        levelsIconsTemp.put((short) -2, LED_NOT);
        levelsIconsTemp.put((short) -1, LED_OFF);
        levelsIconsTemp.put((short) 0, LED_GREEN);
        levelsIconsTemp.put((short) 1, LED_ORANGE);
        levelsIconsTemp.put((short) 2, LED_RED);

        levelsColorsTemp = new LinkedHashMap<Short, Color>();
        levelsColorsTemp.put((short) -2, COLOR_NOT);
        levelsColorsTemp.put((short) -1, COLOR_OFF);
        levelsColorsTemp.put((short) 0, COLOR_GREEN);
        levelsColorsTemp.put((short) 1, COLOR_ORANGE);
        levelsColorsTemp.put((short) 2, COLOR_RED);

        levelsIcons = new LinkedHashMap<Short, ImageIcon>(levelsIconsTemp);
        levelsColors = new LinkedHashMap<Short, Color>(levelsColorsTemp);
        statusIndicator = new JLabel(LED_NOT);
        statusIndicator.addMouseListener(this);
        statusIndicator.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 1));
        this.add(statusIndicator, java.awt.BorderLayout.CENTER);
    }

    /**
     * @return Returns the level.
     */
    public short getLevel() {
        return level;
    }

    /**
     * @param level The level to set.
     */
    public void setLevel(short level) {
        ImageIcon icon1 = levelsIcons.get(level);
        if (icon1 != null) {
            this.level = level;
            statusIndicator.setIcon(icon1);
            statusIndicator.setToolTipText("Level " + this.level);
            repaint();
        }
        else {
            this.level = LEVEL_NONE;
            icon1 = levelsIcons.get(this.level);
            statusIndicator.setIcon(icon1);
            statusIndicator.setToolTipText("Level " + this.level);
            repaint();
        }
    }

    /**
     * @param level The level to set.
     * @param message The message to show for this level.
     */
    public void setLevel(short level, String message) {
        setLevel(level);
        statusIndicator.setToolTipText(message);
    }

    public String getMessage() {
        return statusIndicator.getToolTipText();
    }

    public void setMessage(String msg) {
        statusIndicator.setToolTipText(msg);
    }

    public Color getColorLevel(int n) {
        Color ret = levelsColors.get((short) n);
        if (ret == null)
            return Color.BLACK;
        else
            return ret;

    }

    public void mouseClicked(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void mousePressed(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void mouseReleased(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void mouseEntered(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void mouseExited(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    public LinkedHashMap<Short, Color> getLevelsColors() {
        return new LinkedHashMap<Short, Color>(levelsColors);
    }

    public LinkedHashMap<Short, ImageIcon> getLevelsIcons() {
        return new LinkedHashMap<Short, ImageIcon>(levelsIcons);
    }

    /**
     * Levels '-2' and '-1' should always exist.
     * 
     * @param newLevelsIcons
     * @param newLevelsColors
     * @return
     */
    public boolean changeLevels(LinkedHashMap<Short, ImageIcon> newLevelsIcons,
            LinkedHashMap<Short, Color> newLevelsColors) {
        if (newLevelsIcons.size() != newLevelsColors.size())
            return false;

        levelsIcons = newLevelsIcons;
        levelsColors = newLevelsColors;
        return true;
    }

    public void made5LevelIndicator() {
        levelsIcons = new LinkedHashMap<Short, ImageIcon>();
        levelsIcons.put((short) -2, LED_NOT);
        levelsIcons.put((short) -1, LED_OFF);
        levelsIcons.put((short) 0, LED_GREEN);
        levelsIcons.put((short) 1, LED_BLUE);
        levelsIcons.put((short) 2, LED_YELLOW);
        levelsIcons.put((short) 3, LED_ORANGE);
        levelsIcons.put((short) 4, LED_RED);

        levelsColors = new LinkedHashMap<Short, Color>();
        levelsColors.put((short) -2, COLOR_NOT);
        levelsColors.put((short) -1, COLOR_OFF);
        levelsColors.put((short) 0, COLOR_GREEN);
        levelsColors.put((short) 1, COLOR_BLUE);
        levelsColors.put((short) 2, COLOR_YELLOW);
        levelsColors.put((short) 3, COLOR_ORANGE);
        levelsColors.put((short) 4, COLOR_RED);
    }

    public void made3LevelIndicator() {
        levelsIcons = new LinkedHashMap<Short, ImageIcon>();
        levelsIcons.put((short) -2, LED_NOT);
        levelsIcons.put((short) -1, LED_OFF);
        levelsIcons.put((short) 0, LED_GREEN);
        levelsIcons.put((short) 1, LED_ORANGE);
        levelsIcons.put((short) 2, LED_RED);

        levelsColors = new LinkedHashMap<Short, Color>();
        levelsColors.put((short) -2, COLOR_NOT);
        levelsColors.put((short) -1, COLOR_OFF);
        levelsColors.put((short) 0, COLOR_GREEN);
        levelsColors.put((short) 1, COLOR_ORANGE);
        levelsColors.put((short) 2, COLOR_RED);
    }

    public void made2LevelIndicator() {
        levelsIcons = new LinkedHashMap<Short, ImageIcon>();
        levelsIcons.put((short) -2, LED_NOT);
        levelsIcons.put((short) -1, LED_OFF);
        levelsIcons.put((short) 0, LED_GREEN);
        levelsIcons.put((short) 1, LED_RED);

        levelsColors = new LinkedHashMap<Short, Color>();
        levelsColors.put((short) -2, COLOR_NOT);
        levelsColors.put((short) -1, COLOR_OFF);
        levelsColors.put((short) 0, COLOR_GREEN);
        levelsColors.put((short) 1, COLOR_RED);
    }

    public void made1LevelIndicator() {
        levelsIcons = new LinkedHashMap<Short, ImageIcon>();
        levelsIcons.put((short) -2, LED_NOT);
        levelsIcons.put((short) -1, LED_OFF);
        levelsIcons.put((short) 0, LED_BLUE);

        levelsColors = new LinkedHashMap<Short, Color>();
        levelsColors.put((short) -2, COLOR_NOT);
        levelsColors.put((short) -1, COLOR_OFF);
        levelsColors.put((short) 0, COLOR_BLUE);
    }

    public void made5LevelIndicatorBig() {
        levelsIcons = new LinkedHashMap<Short, ImageIcon>();
        levelsIcons.put((short) -2, LED_NOT_BIG);
        levelsIcons.put((short) -1, LED_OFF_BIG);
        levelsIcons.put((short) 0, LED_GREEN_BIG);
        levelsIcons.put((short) 1, LED_BLUE_BIG);
        levelsIcons.put((short) 2, LED_YELLOW_BIG);
        levelsIcons.put((short) 3, LED_ORANGE_BIG);
        levelsIcons.put((short) 4, LED_RED_BIG);

        levelsColors = new LinkedHashMap<Short, Color>();
        levelsColors.put((short) -2, COLOR_NOT);
        levelsColors.put((short) -1, COLOR_OFF);
        levelsColors.put((short) 0, COLOR_GREEN);
        levelsColors.put((short) 1, COLOR_BLUE);
        levelsColors.put((short) 2, COLOR_YELLOW);
        levelsColors.put((short) 3, COLOR_ORANGE);
        levelsColors.put((short) 4, COLOR_RED);
    }

    public void made3LevelIndicatorBig() {
        levelsIcons = new LinkedHashMap<Short, ImageIcon>();
        levelsIcons.put((short) -2, LED_NOT_BIG);
        levelsIcons.put((short) -1, LED_OFF_BIG);
        levelsIcons.put((short) 0, LED_GREEN_BIG);
        levelsIcons.put((short) 1, LED_ORANGE_BIG);
        levelsIcons.put((short) 2, LED_RED_BIG);

        levelsColors = new LinkedHashMap<Short, Color>();
        levelsColors.put((short) -2, COLOR_NOT);
        levelsColors.put((short) -1, COLOR_OFF);
        levelsColors.put((short) 0, COLOR_GREEN);
        levelsColors.put((short) 1, COLOR_ORANGE);
        levelsColors.put((short) 2, COLOR_RED);
    }

    public void made2LevelIndicatorBig() {
        levelsIcons = new LinkedHashMap<Short, ImageIcon>();
        levelsIcons.put((short) -2, LED_NOT_BIG);
        levelsIcons.put((short) -1, LED_OFF_BIG);
        levelsIcons.put((short) 0, LED_GREEN_BIG);
        levelsIcons.put((short) 1, LED_RED_BIG);

        levelsColors = new LinkedHashMap<Short, Color>();
        levelsColors.put((short) -2, COLOR_NOT);
        levelsColors.put((short) -1, COLOR_OFF);
        levelsColors.put((short) 0, COLOR_GREEN);
        levelsColors.put((short) 1, COLOR_RED);
    }

    public void made1LevelIndicatorBig() {
        levelsIcons = new LinkedHashMap<Short, ImageIcon>();
        levelsIcons.put((short) -2, LED_NOT_BIG);
        levelsIcons.put((short) -1, LED_OFF_BIG);
        levelsIcons.put((short) 0, LED_BLUE_BIG);

        levelsColors = new LinkedHashMap<Short, Color>();
        levelsColors.put((short) -2, COLOR_NOT);
        levelsColors.put((short) -1, COLOR_OFF);
        levelsColors.put((short) 0, COLOR_BLUE);
    }

    public void madeImc2LevelIndicator() {
        levelsIcons = new LinkedHashMap<Short, ImageIcon>();
        levelsIcons.put((short) -2, LED_NOT);
        levelsIcons.put((short) -1, IMC_OFF);
        levelsIcons.put((short) 0, IMC_GREEN);
        levelsIcons.put((short) 1, IMC_RED);

        levelsColors = new LinkedHashMap<Short, Color>();
        levelsColors.put((short) -2, COLOR_NOT);
        levelsColors.put((short) -1, COLOR_OFF);
        levelsColors.put((short) 0, COLOR_GREEN);
        levelsColors.put((short) 1, COLOR_RED);
    }

    public void made1LevelSnapshot() {
        levelsIcons = new LinkedHashMap<Short, ImageIcon>();
        levelsIcons.put((short) -2, LED_NOT);
        levelsIcons.put((short) -1, SNAPSHOT_OFF);
        levelsIcons.put((short) 0, SNAPSHOT_ON);

        levelsColors = new LinkedHashMap<Short, Color>();
        levelsColors.put((short) -2, COLOR_NOT);
        levelsColors.put((short) -1, COLOR_OFF);
        levelsColors.put((short) 0, COLOR_BLUE);
    }

    public void made1LevelWebPublish() {
        levelsIcons = new LinkedHashMap<Short, ImageIcon>();
        levelsIcons.put((short) -2, LED_NOT);
        levelsIcons.put((short) -1, WEB_PUBLISH_OFF);
        levelsIcons.put((short) 0, WEB_PUBLISH_ON);

        levelsColors = new LinkedHashMap<Short, Color>();
        levelsColors.put((short) -2, COLOR_NOT);
        levelsColors.put((short) -1, COLOR_OFF);
        levelsColors.put((short) 0, COLOR_BLUE);
    }

    public static void main(String[] args) throws InterruptedException {
        JPanel tPanel = new JPanel();
        StatusLed sl = new StatusLed();
        tPanel.add(sl);
        JLabel jl = new JLabel("");
        tPanel.add(jl);
        JFrame frame = GuiUtils.testFrame(tPanel);
        frame.setSize(100, 100);

        sl.made5LevelIndicator();
        for (short i = -2; i < 5; i++) {
            sl.setLevel(i);
            jl.setText("Level:" + i);
            Thread.sleep(1000);
        }

        Thread.sleep(1000);
        sl.made5LevelIndicatorBig();
        for (short i = -2; i < 5; i++) {
            sl.setLevel(i);
            jl.setText("Level:" + i);
            Thread.sleep(1000);
        }

        Thread.sleep(1000);
        for (short i = -2; i < 5; i++) {
            sl.setLevel(i);
            jl.setText("Level:" + i);
            Thread.sleep(1000);
        }

        Thread.sleep(1000);
        sl.madeImc2LevelIndicator();
        for (short i = -2; i < 5; i++) {
            sl.setLevel(i);
            jl.setText("Level:" + i);
            Thread.sleep(1000);
        }

    }
}
