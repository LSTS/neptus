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
 * Author: zp
 * 05/05/2016
 */
package pt.lsts.neptus.historicdata;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import pt.lsts.imc.HistoricCTD;
import pt.lsts.imc.HistoricData;
import pt.lsts.imc.HistoricEvent;
import pt.lsts.imc.HistoricSonarData;
import pt.lsts.imc.HistoricTelemetry;
import pt.lsts.imc.historic.DataSample;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorBar;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.WorldImage;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ImageElement;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;

/**
 * @author zp
 *
 */
@PluginDescription(name="Historic Ground Overlay", icon="pt/lsts/neptus/historicdata/rewind_icon.png")
@LayerPriority(priority=-10)
public class HistoricGroundOverlay extends ConsoleLayer {

    private WorldImage imgTemp = new WorldImage(3, ColorMapFactory.createJetColorMap());
    private WorldImage imgCond = new WorldImage(3, ColorMapFactory.createJetColorMap());
    private WorldImage imgDepth = new WorldImage(3, ColorMapFactory.createJetColorMap());
    private WorldImage imgAltitude = new WorldImage(3, ColorMapFactory.createJetColorMap());
    private WorldImage imgPitch = new WorldImage(3, ColorMapFactory.createJetColorMap());
    private DATA_TYPE cache = null;
    private ImageElement image = null;
    private DATA_TYPE typeToPaint = DATA_TYPE.None;

    private boolean processing = false;
    private final Object processingLock = new Object();

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
                    double val = ((HistoricTelemetry) sample.getSample()).getPitch() * (360.0 / 65535);
                    if (val > 180)
                        val -= 360;
                    imgPitch.addPoint(loc, val);
                    double alt = ((HistoricTelemetry) sample.getSample()).getAltitude();
                    double depth = sample.getzMeters() < 0? -sample.getzMeters() : 0;
                    if (alt > 0)
                        imgAltitude.addPoint(loc, depth + alt
                                - TidePredictionFactory.getTideLevel(sample.getTimestampMillis()));                          
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
            synchronized (processingLock) {
                if (!processing) {
                    processing = true;
                    Thread t = new Thread("Historic Overlay: Generate image") {
                        @Override
                        public void run() {
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
                                image = null;
                            else
                                image = pivot.asImageElement();
                            cache = dataType;
                            processing = false;
                        }
                    };
                    t.start();
                }                
            }
            return null;
        }
    }

    public static enum DATA_TYPE {
        Conductivity,
        Temperature,
        Altitude,
        Depth,
        Pitch,
        None
    }



    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        ImageElement elem = getImage(typeToPaint);
        g.setTransform(renderer.getIdentity());
        if (elem != null) {
            elem.paint(g, renderer, renderer.getRotation());
            g.setComposite(AlphaComposite.SrcOver);
            paintLegend(g, renderer);
        }
    }

    public void paintLegend(Graphics2D g, StateRenderer2D renderer) {        
        g.setTransform(renderer.getIdentity());
        String text = "";
        WorldImage pivot = null;
        switch (getTypeToPaint()) {
            case Depth:
                pivot = imgDepth;
                text = I18n.text("Depth (m)");
                break;
            case Altitude:
                pivot = imgAltitude;
                text = I18n.text("Bathym. (m)");
                break;
            case Conductivity:
                text = I18n.text("Cond. (m)");
                pivot = imgCond;
                break;
            case Pitch:
                text = I18n.text("Pitch (deg)");
                pivot = imgPitch;
                break;
            case Temperature:
                text = I18n.text("Temp. (ºC)");
                pivot = imgTemp;
                break;
            default:
                break;
        }

        if (pivot == null)
            return;

        g.setColor(new Color(255,255,255,100));
        g.fillRoundRect(10, 10, 100, 170, 10, 10);

        ColorBar cb = new ColorBar(ColorBar.VERTICAL_ORIENTATION, pivot.getColormap());
        cb.setSize(15, 80);
        g.setColor(Color.black);
        Font prev = g.getFont();
        g.setFont(new Font("Helvetica", Font.BOLD, 14));
        g.setFont(prev);
        g.translate(15, 45);
        cb.paint(g);
        g.translate(-10, -15);

        try {
            g.drawString(GuiUtils.getNeptusDecimalFormat(2).format(pivot.getMaxValue()), 28, 20);
            g.drawString(GuiUtils.getNeptusDecimalFormat(2).format((pivot.getMaxValue()+pivot.getMinValue())/2), 28, 60);
            g.drawString(GuiUtils.getNeptusDecimalFormat(2).format(pivot.getMinValue()), 28, 100);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            e.printStackTrace();
        }
        g.translate(10, 120);
        g.drawString(text, 0, 15);
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

    /**
     * @return the typeToPaint
     */
    public DATA_TYPE getTypeToPaint() {
        return typeToPaint;
    }

    public void setColormap(ColorMap colormap) {
        imgTemp.setColormap(colormap);
        imgAltitude.setColormap(colormap);
        imgCond.setColormap(colormap);
        imgDepth.setColormap(colormap);
        imgPitch.setColormap(colormap);
    }

    /**
     * @param typeToPaint the typeToPaint to set
     */
    public void setTypeToPaint(DATA_TYPE typeToPaint) {
        this.typeToPaint = typeToPaint;
    }

    public void resetImage() {
        cache = null;
    }
}
