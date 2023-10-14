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
 * Nov 3, 2014
 */
package pt.lsts.neptus.console.bathymLayer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;

import org.imgscalr.Scalr;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.InterpolationColorMap;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.events.ConsoleEventMissionChanged;
import pt.lsts.neptus.gui.swing.NeptusFileView;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 *
 */
@PluginDescription
public class BathymetryLayer extends ConsoleLayer {

    @NeptusProperty
    public int width = 1000;
    
    @NeptusProperty
    public int height = 1000;
    
    @NeptusProperty
    public double maxDepth = 15;
    
    @NeptusProperty
    public double cellSize = 5;
    
    @NeptusProperty
    public float opacity = 0.75f;
    
    //@NeptusProperty
    public ColorMap colormap = new InterpolationColorMap(new double[] { 0, 0.2, 0.4, 0.6, 0.8, 1.0 }, new Color[] {
            Color.RED.darker().darker(), Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, new Color(64, 0, 128) });
    
    private BufferedImage img;
    private LocationType center;
    
    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    private void reset() {
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);   
        center = new LocationType(getConsole().getMission().getHomeRef()).convertToAbsoluteLatLonDepth();        
    }
    
    @Override
    public void initLayer() {
        
        getConsole().addMenuItem(I18n.text("Tools") + ">" + I18n.text("Bathymetry Layer") + ">" + I18n.text("Reset"), null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reset();                
            }
        });
        
        getConsole().addMenuItem(I18n.text("Tools") + ">" + I18n.text("Bathymetry Layer") + ">" + I18n.text("Import from LSF"), null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser chooser = new JFileChooser();
                chooser.setFileView(new NeptusFileView());
                chooser.setCurrentDirectory(new File(ConfigFetch.getLogsFolder()));
                chooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("LSF log files"), FileUtil.FILE_TYPE_LSF,
                        FileUtil.FILE_TYPE_LSF_COMPRESSED, FileUtil.FILE_TYPE_LSF_COMPRESSED_BZIP2));
                chooser.setApproveButtonText(I18n.text("Open Log"));
                chooser.showOpenDialog(getConsole());
                if (chooser.getSelectedFile() == null)
                    return;
                Thread loader = new Thread() {
                    public void run() {
                        try {
                            LsfIndex index = new LsfIndex(chooser.getSelectedFile());
                            for (EstimatedState s : index.getIterator(EstimatedState.class, 500)) {
                                on(s);
                            }
                            index.cleanup();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                };
                loader.setDaemon(true);
                loader.start();
                
            }
        });
        
        getConsole().addMenuItem(I18n.text("Tools") + ">" + I18n.text("Bathymetry Layer") + ">" + I18n.text("Export Image"), null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser chooser = new JFileChooser();
                chooser.setFileView(new NeptusFileView());
                chooser.setCurrentDirectory(new File(ConfigFetch.getLogsFolder()));
                chooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("PNG Images"), new String[] {"png"}));
                chooser.setApproveButtonText(I18n.text("Save PNG"));
                chooser.showSaveDialog(getConsole());
                if (chooser.getSelectedFile() == null)
                    return;
                Thread loader = new Thread() {
                    public void run() {
                        try {
                            ImageIO.write(img, "PNG", chooser.getSelectedFile());
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                };
                loader.setDaemon(true);
                loader.start();
                
            }
        });
        
        getConsole().addMenuItem(I18n.text("Tools") + ">" + I18n.text("Bathymetry Layer") + ">" + I18n.text("Import Image"), null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(new File(ConfigFetch.getConfigFile()));
                chooser.setFileView(new NeptusFileView());
                chooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("PNG Images"), new String[] {"png"}));
                chooser.setApproveButtonText(I18n.text("Load PNG"));
                chooser.showOpenDialog(getConsole());
                if (chooser.getSelectedFile() == null)
                    return;
                Thread loader = new Thread() {
                    public void run() {
                        try {
                            img = Scalr.resize(ImageIO.read(chooser.getSelectedFile()), width, height);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                };
                loader.setDaemon(true);
                loader.setPriority(Thread.MIN_PRIORITY);
                loader.start();                
            }
        });
        
        try {
            if (new File(ConfigFetch.getLogsFolder() + "/bathym.png").exists()) {
                img = Scalr.resize(ImageIO.read(new File(ConfigFetch.getLogsFolder() + "/bathym.png")), width, height);
                center = new LocationType(getConsole().getMission().getHomeRef());
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }    
    }
    
    @Override
    public void cleanLayer() {
        getConsole().removeMenuItem(I18n.text("Tools") + ">" + I18n.text("Bathymetry Layer"));
        
        try {
            ImageIO.write(img, "PNG", new File(ConfigFetch.getLogsFolder() + "/bathym.png"));
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }        
        img = null;
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        if (center == null)
            reset();
        Point2D pt = renderer.getScreenPosition(center);
        g.translate(pt.getX(), pt.getY());
        g.rotate(-renderer.getRotation());
        double wr = cellSize * width * renderer.getZoom();
        double hr = cellSize * height * renderer.getZoom();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, (int) (-wr / 2), (int) (hr / 2), (int) (wr), (int) (-hr), null, renderer);
    }
    
    public Point2D locToPoint(LocationType loc) {
        
        if (center == null)
            reset();
        
        double[] coords = loc.getOffsetFrom(center);
        coords[0] = (coords[0] + (width / 2.0) * cellSize) / cellSize;
        coords[1] = ((coords[1] + (height / 2.0) * cellSize) / cellSize);
        
        if (coords[0] < 0 || coords[1] < 0)
            return null;
        if (coords[0] > width || coords[1] > height)
            return null;
        return new Point2D.Double(coords[1], coords[0]);
    }
        
    @Subscribe
    public void on(ConsoleEventMissionChanged evy) {
        reset();
    }
    
    @Subscribe
    public void on(EstimatedState state) {
        try {
            Point2D pt = locToPoint(IMCUtils.getLocation(state));
            double width = 0;
            double alt = 0;
            if (state.getAlt() != -1) {
                alt = state.getAlt() + state.getDepth() - TidePredictionFactory.getTideLevel(state.getTimestampMillis());
                width += state.getAlt();
            }
            alt = Math.max(0, alt);
            
            double ang = state.getPsi();
            for (double i = -width-1; i <= width+1; i+=cellSize/3) {
                int x = (int) (pt.getX() + Math.cos(ang) * i/cellSize);
                int y = (int) (pt.getY() - Math.sin(ang) * i/cellSize);
                
                Color c = colormap.getColor(alt / maxDepth);
                
                img.setRGB(x, y, c.getRGB());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
