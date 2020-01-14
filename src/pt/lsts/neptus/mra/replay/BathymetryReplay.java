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
 * Author: José Pinto
 * Dec 5, 2012
 */
package pt.lsts.neptus.mra.replay;

import java.awt.Graphics2D;
import java.io.File;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.ColormapOverlay;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.ImageLayer;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;
import pt.lsts.neptus.util.bathymetry.TidePredictionFinder;

/**
 * @author zp
 *
 */
@LayerPriority(priority=-60)
@PluginDescription(icon="pt/lsts/neptus/mra/replay/color.png")
public class BathymetryReplay extends ColormapOverlay implements LogReplayLayer {

    @NeptusProperty(name="Cell width")
    public int cellWidth = 8;

    private LsfIndex index;
    private boolean parsed = false, parsing = false; 

    public BathymetryReplay() {
        super("Bathymetry", 50, true, 0);              
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return (source.getLsfIndex().getDefinitions().getVersion().compareTo("5.0.0") >= 0 && source.getLsfIndex().containsMessagesOfType("Distance"));
    }

    @Override
    public void cleanup() {
        generated = scaled = null;
    }

    @Override
    public String getName() {
        return I18n.text("Bathymetry layer");
    }


    @Override
    public String[] getObservedMessages() {
        return new String[0];
    }


    @Override
    public boolean getVisibleByDefault() {
        return false;
    }

    @Override
    public void onMessage(IMCMessage message) {

    }

    @Override
    public void parse(IMraLogGroup source) {
        this.index = source.getLsfIndex();
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (!parsed) {
            super.cellWidth = cellWidth;
            parsed = true;
            parsing = true;
            new Thread(new Runnable() {
                @Override
                public void run() {

                    TidePredictionFinder finder = TidePredictionFactory.create(index);

                    if (index.getDefinitions().getVersion().compareTo("5.0.0") >= 0) {
                        for (EstimatedState state : index.getIterator(EstimatedState.class)) {
                            if (state.getAlt() < 0 || state.getDepth() < MRAProperties.minDepthForBathymetry || Math.abs(state.getTheta()) > Math.toDegrees(10))
                                continue;
                            LocationType loc = new LocationType(Math.toDegrees(state.getLat()), Math.toDegrees(state.getLon()));
                            loc.translatePosition(state.getX(), state.getY(), 0);
                            if (finder == null)
                                addSample(loc, state.getAlt() + state.getDepth());
                            else {
                                try {
                                    addSample(loc, state.getAlt() + state.getDepth() - finder.getTidePrediction(state.getDate(), false));
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    generated = generateImage(ColorMapFactory.createJetColorMap());
                    ImageLayer il = getImageLayer();
                    try {
                        il.saveToFile(new File(index.getLsfFile().getParentFile(),"mra/dvl.layer"));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    parsing = false;

                }
            }, "Bathymetry overlay").start();
        }

        if (!parsing)
            super.paint(g, renderer);
    }

}
