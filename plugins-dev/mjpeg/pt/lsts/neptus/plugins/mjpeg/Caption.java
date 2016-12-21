/*
 * Copyright (c) 2004-2016 OceanScan - Marine Systems & Technology, Lda.
 * Polo do Mar do UPTEC, Avenida da Liberdade, 4450-718 Matosinhos, Portugal
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
 */

package pt.lsts.neptus.plugins.mjpeg;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.util.DateTimeUtil;

/**
 * This class implements a dynamically sized caption containing telemetry data that can be used as an overlay.
 *
 * @author Ricardo Martins
 */
public class Caption {
    /** Text font. */
    private static final Font font = new Font("Arial", Font.BOLD, 14);
    /** Box background color. */
    private static final Color backgroundColor = new Color(0, 0, 0, 128);
    /** Text color. */
    private static final Color foregroundColor = Color.white;
    /** Box margin. */
    private static final int boxMargin = 10;
    /** Box roundness. */
    private static final int boxArc = 10;
    /** Text padding within box. */
    private static final int textPadding = 5;
    /** Spacing between lines. */
    private static final int lineSpacing = 2;
    /** Size of line of text. */
    private static final int lineHeight = font.getSize() + lineSpacing;
    /** Text start position (X). */
    private static final int textStartX = boxMargin + textPadding;
    /** Text start position (Y). */
    private static final int textStartY = boxMargin + textPadding + font.getSize();
    /** List of strings to render. */
    private final List<String> lines = new ArrayList<>();
    /** Computed width of the box. */
    private int boxWidth = -1;
    /** Computed height of the box. */
    private int boxHeight = -1;

    /**
     * Draws telemetry information into a canvas.
     *
     * @param state telemetry data.
     * @param canvas destination canvas.
     */
    public void draw(SystemPositionAndAttitude state, Graphics2D canvas) {
        addLines(state, canvas);
        drawLines(canvas);
    }

    /**
     * Draws the lines of the telemetry information into a canvas.
     *
     * @param canvas destination canvas.
     */
    private void drawLines(Graphics2D canvas) {
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setColor(backgroundColor);
        canvas.fill(new RoundRectangle2D.Double(boxMargin, boxMargin, boxWidth, boxHeight, boxArc, boxArc));
        canvas.setColor(foregroundColor);
        canvas.setFont(font);

        int y = textStartY;
        for (String line : lines) {
            canvas.drawString(line, textStartX, y);
            y += lineHeight;
        }
    }

    /**
     * Clears cached dimensions and telemetry lines.
     */
    private void clear() {
        lines.clear();
        boxWidth = -1;
        boxHeight = boxMargin + textPadding;
    }

    /**
     * Process telemetry information into lines.
     *
     * @param state telemetry information.
     * @param canvas destination canvas.
     */
    private void addLines(SystemPositionAndAttitude state, Graphics2D canvas) {
        final long timeStamp = state.getTime();

        clear();

        // Latitude.
        final double latitude = state.getPosition().getLatitudeDegs();
        addLine(CoordinateUtil.latitudeAsString(latitude, false, 2), canvas);

        // Longitude.
        final double longitude = state.getPosition().getLongitudeDegs();
        addLine(CoordinateUtil.longitudeAsString(longitude, true), canvas);

        addLine(I18n.text("Time") + ": " + DateTimeUtil.formatTime(timeStamp), canvas);
        addLine(I18n.text("Depth") + ": " + formatDepth(state.getPosition().getDepth()), canvas);
        addLine(I18n.text("Altitude") + ": " + formatAltitude(state.getAltitude()), canvas);
        addLine(I18n.text("Roll") + ": " + formatNumber(Math.toDegrees(state.getRoll())), canvas);
        addLine(I18n.text("Pitch") + ": " + formatNumber(Math.toDegrees(state.getPitch())), canvas);
        addLine(I18n.text("Yaw") + ": " + formatNumber(Math.toDegrees(state.getYaw())), canvas);
        addLine(I18n.text("Speed") + ": " + formatNumber(state.getU()), canvas);
    }

    /**
     * Adds a line to the internal list of lines and updates the maximum width and height of the caption.
     *
     * @param line text line.
     * @param canvas graphic object where the text will be rendered.
     */
    private void addLine(String line, Graphics2D canvas) {
        int width = (int)Math.ceil(canvas.getFontMetrics(font).getStringBounds(line, canvas).getWidth()) + textPadding * 2;

        if (width > this.boxWidth)
            this.boxWidth = width;

        this.boxHeight += lineHeight;

        lines.add(line);
    }

    private static String formatAltitude(double number) {
        return (number < 0) ? "N/A" : formatNumber(number);
    }

    private static String formatDepth(double number) {
        return (number < 0) ? "0" : formatNumber(number);
    }

    private static String formatNumber(double number) {
        return Double.isNaN(number) ? "N/A" : String.format(Locale.US, "%.1f", number);
    }
}
