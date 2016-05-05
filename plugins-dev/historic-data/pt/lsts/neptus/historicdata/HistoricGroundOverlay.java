/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * 05/05/2016
 */
package pt.lsts.neptus.historicdata;

import java.awt.Graphics2D;

import pt.lsts.imc.HistoricCTD;
import pt.lsts.imc.HistoricData;
import pt.lsts.imc.HistoricEvent;
import pt.lsts.imc.HistoricSonarData;
import pt.lsts.imc.HistoricTelemetry;
import pt.lsts.imc.historic.DataSample;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.mra.WorldImage;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ImageElement;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;

/**
 * @author zp
 *
 */
public class HistoricGroundOverlay extends ConsoleLayer {

    private WorldImage imgTemp = new WorldImage(3, ColorMapFactory.createJetColorMap());
    private WorldImage imgCond = new WorldImage(3, ColorMapFactory.createJetColorMap());
    private WorldImage imgDepth = new WorldImage(3, ColorMapFactory.createJetColorMap());
    private WorldImage imgAltitude = new WorldImage(3, ColorMapFactory.createJetColorMap());
    private WorldImage imgPitch = new WorldImage(3, ColorMapFactory.createJetColorMap());
    private DATA_TYPE cache = null;
    private ImageElement image = null;
    private DATA_TYPE typeToPaint = DATA_TYPE.Altitude;
    
    public void clear() {
        imgTemp = new WorldImage(3, ColorMapFactory.createJetColorMap());
        imgCond = new WorldImage(3, ColorMapFactory.createJetColorMap());
        imgDepth = new WorldImage(3, ColorMapFactory.createJetColorMap());
        imgAltitude = new WorldImage(3, ColorMapFactory.createJetColorMap());
        imgPitch = new WorldImage(3, ColorMapFactory.createJetColorMap());
        cache = null;
        image = null;
    }
    
    public void process(HistoricData incoming) {
        for (DataSample sample : DataSample.parseSamples(incoming)) {
            LocationType loc = new LocationType(sample.getLatDegs(), sample.getLonDegs());
            loc.setDepth(sample.getzMeters());
            switch (sample.getSample().getMgid()) {
                case HistoricCTD.ID_STATIC:
                    imgDepth.addPoint(loc, ((HistoricCTD) sample.getSample()).getDepth());
                    imgCond.addPoint(loc, ((HistoricCTD) sample.getSample()).getConductivity());
                    imgTemp.addPoint(loc, ((HistoricCTD) sample.getSample()).getTemperature());
                    break;
                case HistoricSonarData.ID_STATIC:
                    
                    break;
                case HistoricTelemetry.ID_STATIC:
                    imgPitch.addPoint(loc, ((HistoricTelemetry) sample.getSample()).getPitch() * (360.0 / 65535));
                    double alt = ((HistoricTelemetry) sample.getSample()).getAltitude();
                    if (alt > 0)
                        imgAltitude.addPoint(loc, sample.getzMeters() + alt
                                + TidePredictionFactory.getTideLevel(sample.getTimestampMillis()));                          
                    break;
                case HistoricEvent.ID_STATIC:
                default:
                    break;
            }
        }
        cache = null;
    }
    
    public ImageElement getImage(DATA_TYPE dataType) {
        if (cache == dataType)
            return image;
        else {
            
            WorldImage pivot = null;
            
            switch (dataType) {
                case Conductivity:
                    pivot = imgCond;
                    break;
                case Temperature:
                    pivot = imgTemp;
                    break;
                case Altitude:
                    pivot = imgAltitude;
                    break;
                case Depth:
                    pivot = imgDepth;
                    break;
                case Pitch:
                    pivot = imgPitch;
                default:
                    break;
            }
            
            if (pivot == null)
                return null;
            
            image = pivot.asImageElement();
            cache = dataType;
            return image;
        }
    }
    
    public static enum DATA_TYPE {
        Conductivity,
        Temperature,
        Altitude,
        Depth,
        Pitch
    }
    
    
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        ImageElement elem = getImage(typeToPaint);
        g.setTransform(renderer.getIdentity());
        if (elem != null)
            elem.paint(g, renderer, renderer.getRotation());
    }

    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    @Override
    public void initLayer() {
        
    }

    @Override
    public void cleanLayer() {
        clear();
    }
}
