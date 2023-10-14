/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: hfq
 * Apr 7, 2014
 */
package pt.lsts.neptus.mra.replay;

import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Temperature;
import pt.lsts.imc.lsf.IndexScanner;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.ColormapOverlay;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.DateTimeUtil;

/**
 * @author hfq
 * 
 */
@LayerPriority(priority = -60)
@PluginDescription(name = "Temperature Replay", icon = "pt/lsts/neptus/plugins/ctd/thermometer.png")
public class TemperatureReplay extends ColormapOverlay implements LogReplayLayer {

    @NeptusProperty(name = "Cell width")
    public static int cellWidth = 1;

    private ColorMap cm = ColorMapFactory.createJetColorMap();
    private static final String TEMP_IMG_FILE_PATH = "mra/temperature.png";

    private boolean isParsed = false;

    /**
     * Constructor
     */
    public TemperatureReplay() {
        super("Temperature", cellWidth, true, 0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D,
     * pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (isParsed)
            super.paint(g, renderer);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#canBeApplied(pt.lsts.neptus.mra.importers.IMraLogGroup,
     * pt.lsts.neptus.mra.replay.LogReplayComponent.Context)
     */
    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return source.getLsfIndex().getEntityId("CTD") != 255
                && source.getLsfIndex().containsMessagesOfType("Temperature");
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#getName()
     */
    @Override
    public String getName() {
        return I18n.text("Temperature Replay");
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#parse(pt.lsts.neptus.mra.importers.IMraLogGroup)
     */
    @Override
    public void parse(final IMraLogGroup source) {

        Thread t = new Thread(TemperatureReplay.class.getSimpleName() + " " + source.getDir().getParent()) {

            @Override
            public void run() {

                LsfIndex lsfIndex = source.getLsfIndex();
                IndexScanner indexScanner = new IndexScanner(lsfIndex);

                while (true) {
                    Temperature temp = indexScanner.next(Temperature.class, "CTD");

                    if (temp == null)
                        break;

                    EstimatedState state = (EstimatedState) lsfIndex.getMessageBeforeOrAt(
                            EstimatedState.class.getSimpleName(), indexScanner.getIndex(), temp.getTimestamp());

                    if (state == null) {
                        NeptusLog.pub().warn(String.format("No location found for %s at %s!", temp.getMessageType(), 
                                DateTimeUtil.milliSecondsToFormatedString(temp.getTimestampMillis())));
                        continue;
                    }

                    LocationType loc = new LocationType(Math.toDegrees(state.getLat()), Math.toDegrees(state.getLon()));
                    loc.translatePosition(state.getX(), state.getY(), 0);

                    try {
                        addSample(loc, temp.getValue());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                generated = generateImage(cm);

                if (source.getFile(TEMP_IMG_FILE_PATH) == null) {
                    try {
                        NeptusLog.pub().info(
                                "Recording " + lsfIndex.getLsfFile().getParentFile() + "/" + TEMP_IMG_FILE_PATH);
                        ImageIO.write(generated, "PNG", new File(lsfIndex.getLsfFile().getParentFile() + "/"
                                + TEMP_IMG_FILE_PATH));
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                isParsed = true;
            }
        };
        t.setDaemon(true);
        t.start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#getObservedMessages()
     */
    @Override
    public String[] getObservedMessages() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#onMessage(pt.lsts.imc.IMCMessage)
     */
    @Override
    public void onMessage(IMCMessage message) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#getVisibleByDefault()
     */
    @Override
    public boolean getVisibleByDefault() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#cleanup()
     */
    @Override
    public void cleanup() {
        generated = null;
        cm = null;
    }

}
