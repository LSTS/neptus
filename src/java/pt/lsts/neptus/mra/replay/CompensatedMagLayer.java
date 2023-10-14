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
 * Author: keila
 * 05/09/2018
 */
package pt.lsts.neptus.mra.replay;

import java.awt.Graphics2D;
import java.io.File;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.TotalMagIntensity;
import pt.lsts.imc.lsf.IndexScanner;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.ColormapOverlay;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.ImageLayer;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * MRA Replay map layer for normalized magnetic measurements
 * The MRAProperty Magnetic threshold defines the value from which the compensated measurements are considered to be
 * normalized.
 * @author keila
 *
 */
@LayerPriority(priority = -50)
@PluginDescription(name = "Magnetic Intensity Layer", icon = "pt/lsts/neptus/mra/replay/magnetometer_icon.png", description = "Magnetic intensity colormap layer in microTeslas from compensated measured values.")
public class CompensatedMagLayer extends ColormapOverlay implements LogReplayLayer {

    private boolean parsed;
    private double lastRaw, lastCompensated;
    private final String rawEntity = "Magnetometer - Raw", compensatedEntity = "Magnetometer - Compensated";

    /**
     * @param name
     * @param cellWidth
     * @param inverted
     * @param transparency
     */
    public CompensatedMagLayer() {
        super("Magnetic", MRAProperties.magCellW, true, 0);
        lastRaw = lastCompensated = -1;
        parsed = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#canBeApplied(pt.lsts.neptus.mra.importers.IMraLogGroup,
     * pt.lsts.neptus.mra.replay.LogReplayComponent.Context)
     */
    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return source.getLsfIndex().containsMessagesOfType("TotalMagIntensity")
                && source.getLsfIndex().containsMessagesOfType("EstimatedState");
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#getName()
     */
    @Override
    public String getName() {
        return I18n.text("Magnetic Intensity Layer");
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#parse(pt.lsts.neptus.mra.importers.IMraLogGroup)
     */
    @Override
    public void parse(IMraLogGroup source) {
        Thread worker = new Thread(CompensatedMagLayer.class.getName() + " " + source.getDir().getParent()) {

            /*
             * (non-Javadoc)
             * 
             * @see java.lang.Thread#run()
             */
            @Override
            public void run() {
                LsfIndex lsfIndex = source.getLsfIndex();
                IndexScanner indexScanner = new IndexScanner(lsfIndex);
                TotalMagIntensity msg;
                while ((msg=indexScanner.next(TotalMagIntensity.class,compensatedEntity)) != null) {
                        lastCompensated = msg.getValue();
                        lastRaw = ((TotalMagIntensity) lsfIndex.getMessageBeforeOrAt(
                                TotalMagIntensity.class.getSimpleName(),rawEntity, indexScanner.getIndex(), msg.getTimestamp())).getValue();    

                        if(lastRaw != -1 && lastCompensated != -1 && Math.abs(lastCompensated - lastRaw) > MRAProperties.magThreshold) {
                            EstimatedState state = (EstimatedState) lsfIndex.getMessageBeforeOrAt(
                                    EstimatedState.class.getSimpleName(), indexScanner.getIndex(), msg.getTimestamp());
                            if(state!=null) {
                                try {
                                    LocationType loc = new LocationType(Math.toDegrees(state.getLat()), Math.toDegrees(state.getLon()));
                                    loc.translatePosition(state.getX(), state.getY(), 0);
                                    addSample(loc, lastCompensated);
                                    parsed = true;
                                }
                                catch (Exception e ) {
                                    NeptusLog.pub().error(I18n.text("Error adding new TotalMagIntensity sample on colormap layer"), e);
                                }
                            }
                        }
                }
                setClamp(false);
                generated = generateImage(ColorMapFactory.createJetColorMap());
                ImageLayer il = getImageLayer();
                setClamp(true);
                try {
                    il.saveToFile(new File(lsfIndex.getLsfFile().getParentFile(),"mra/magnetic.layer"));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.setDaemon(true);
        worker.start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#getObservedMessages()
     */
    @Override
    public String[] getObservedMessages() {
        // TODO Auto-generated method stub
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
     * @see pt.lsts.neptus.colormap.ColormapOverlay#paint(java.awt.Graphics2D,
     * pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if(parsed) {
            setClamp(false);
            super.paint(g, renderer);
            setClamp(true);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.colormap.ColormapOverlay#addSample(pt.lsts.neptus.types.coord.LocationType, double)
     */
    @Override
    public void addSample(LocationType location, double value) {
        // TODO Auto-generated method stub
        super.addSample(location, value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#cleanup()
     */
    @Override
    public void cleanup() {
        generated = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#getVisibleByDefault()
     */
    @Override
    public boolean getVisibleByDefault() {
        // TODO Auto-generated method stub
        return false;
    }

}
