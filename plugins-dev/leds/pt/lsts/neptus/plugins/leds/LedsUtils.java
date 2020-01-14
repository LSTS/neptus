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
 * Author: Zhao
 * 31 de Ago de 2013
 */
package pt.lsts.neptus.plugins.leds;

import javax.swing.ImageIcon;

import pt.lsts.neptus.util.ImageUtils;

/**
 * @author hfq
 * 
 */
public class LedsUtils {
    // protected static final int PANEL_WIDTH = 550;
    protected static final int PANEL_WIDTH = 300;
    protected static final int PANEL_HEIGHT = 530;

    // Leds Brightness in percentage / max brightness value = 255
    protected static final int LED_MIN_BRIGHTNESS = 0;
    protected static final int LED_MAX_BRIGHTNESS = 100;
    protected static final int LED_INIT_BRIGHTNESS = 0;

    protected static final int IMAGE_WIDTH = 100;
    protected static final int IMAGE_HEIGHT = 85;

    protected static final ImageIcon ICON_NONE = ImageUtils.getScaledIcon(
            ImageUtils.getImage("pt/lsts/neptus/plugins/leds/images/leds_clean.png"), LedsUtils.IMAGE_WIDTH,
            LedsUtils.IMAGE_HEIGHT);

    protected static final ImageIcon ICON_LEDS1 = ImageUtils.getScaledIcon(
            ImageUtils.getImage("pt/lsts/neptus/plugins/leds/images/leds1.png"), LedsUtils.IMAGE_WIDTH,
            LedsUtils.IMAGE_HEIGHT);
    protected static final ImageIcon ICON_LEDS2 = ImageUtils.getScaledIcon(
            ImageUtils.getImage("pt/lsts/neptus/plugins/leds/images/leds2.png"), LedsUtils.IMAGE_WIDTH,
            LedsUtils.IMAGE_HEIGHT);
    protected static final ImageIcon ICON_LEDS3 = ImageUtils.getScaledIcon(
            ImageUtils.getImage("pt/lsts/neptus/plugins/leds/images/leds3.png"), LedsUtils.IMAGE_WIDTH,
            LedsUtils.IMAGE_HEIGHT);
    protected static final ImageIcon ICON_LEDS4 = ImageUtils.getScaledIcon(
            ImageUtils.getImage("pt/lsts/neptus/plugins/leds/images/leds4.png"), LedsUtils.IMAGE_WIDTH,
            LedsUtils.IMAGE_HEIGHT);
    protected static final ImageIcon ICON_LEDS_ALL = ImageUtils.getScaledIcon(
            ImageUtils.getImage("pt/lsts/neptus/plugins/leds/images/leds_all.png"), LedsUtils.IMAGE_WIDTH,
            LedsUtils.IMAGE_HEIGHT);

    public static final String[] ledNames = { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" };

    public static int convPercToLedsBright(int perc) {
        return (255 * perc / 100);
    }

    public static int convBrightToPerc(int value) {
        return (100 * value / 255);
    }

}
