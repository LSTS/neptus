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
 * Author: pdias
 * 31/01/2015
 */
package pt.lsts.neptus.console.shapefiles;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFileChooser;

import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.OffScreenLayerImageControl;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 *
 */
@PluginDescription(name="Shape Files Layer", author="Paulo Dias", version="0.1")
@LayerPriority(priority = -60)
public class ShapeFilesLayer extends ConsoleLayer implements ConfigurationListener {

    private OffScreenLayerImageControl offScreenImageControl = new OffScreenLayerImageControl();

    private ArrayList<ShapeFileObject> shapeFiles = new ArrayList<ShapeFileObject>();

    private String menuItemAddShapeFile = I18n.text("Tools") + ">" + I18n.text("Shape File Add");
    
    private File lastOpenFolder = null;
    
    public ShapeFilesLayer() {
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
        
        ActionListener addShapeFileAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (lastOpenFolder == null) {
                    lastOpenFolder = new File(ConfigFetch.getConfigFile());
                }
                JFileChooser chooser = GuiUtils.getFileChooser(lastOpenFolder, I18n.text("Shape File"), "shp"); 
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setMultiSelectionEnabled(true);
                int op = chooser.showOpenDialog(getConsole());
                if (op == JFileChooser.APPROVE_OPTION) {
                    File[] selFiles = chooser.getSelectedFiles();
                    for (File fx : selFiles) {
                        boolean skip = false;
                        for (ShapeFileObject so : shapeFiles) {
                            if (so.getName().equalsIgnoreCase(fx.getName())) {
                                skip = true;
                                break;
                            }
                        }
                        if (!skip) {
                            ShapeFileObject shObj1 = ShapeFileLoader.loadShapeFileObject(fx);
                            if (shObj1 != null) {
                                shObj1.setColor(ColorUtils.setTransparencyToColor(Color.PINK, 150));
                                shapeFiles.add(shObj1);
                                offScreenImageControl.triggerImageRebuild();
                            }
                            
                            lastOpenFolder = fx;
                        }
                    }
                }
            }
        };
        getConsole().addMenuItem(menuItemAddShapeFile, null, addShapeFileAction);
        
//        String d1 = "C:\\Users\\pdias\\workspace-java\\TestShapefilesLoader\\testdata\\OSPAR_Inner_Boundary.shp";
//        String d2 = "C:\\Users\\pdias\\workspace-java\\TestShapefilesLoader\\testdata\\OSPAR_Outer_Boundary.shp";
//        String d3 = "C:\\Users\\pdias\\workspace-java\\TestShapefilesLoader\\testdata\\OSPAR_Regions_pg.shp";
//        
//        ShapeFileObject shObj1 = ShapeFileLoader.loadShapeFileObject(new File(d1));
//        ShapeFileObject shObj2 = ShapeFileLoader.loadShapeFileObject(new File(d2));
//        ShapeFileObject shObj3 = ShapeFileLoader.loadShapeFileObject(new File(d3));
//        
//        if (shObj1 != null) {
//            shObj1.setColor(ColorUtils.setTransparencyToColor(Color.PINK.darker(), 150));
//            shapeFiles.add(shObj1);
//        }
//        if (shObj2 != null) {
//            shObj2.setColor(ColorUtils.setTransparencyToColor(Color.YELLOW, 150));
//            shapeFiles.add(shObj2);
//        }
//        if (shObj3 != null) {
//            shObj3.setColor(ColorUtils.setTransparencyToColor(Color.PINK, 75));
//            shapeFiles.add(shObj3);
//        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#cleanLayer()
     */
    @Override
    public void cleanLayer() {
        getConsole().removeMenuItem(menuItemAddShapeFile.split(">"));
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        
        boolean recreateImage = offScreenImageControl.paintPhaseStartTestRecreateImageAndRecreate(g, renderer);
        if (recreateImage) {
            Graphics2D g2 = offScreenImageControl.getImageGraphics();
            paintShapes(renderer, g2);
        }            
        offScreenImageControl.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRenderer(g, renderer);
        
        // Legend
        Graphics2D gl = (Graphics2D) g.create();
        gl.translate(10, 60);
        gl.setColor(Color.WHITE);
        gl.drawString(getName(), 0, 0); // (int)pt.getX()+17, (int)pt.getY()+2
        gl.dispose();
    }

    /**
     * @param renderer
     * @param g2
     */
    private void paintShapes(StateRenderer2D renderer, Graphics2D g) {
        for (ShapeFileObject shapeFileObject : shapeFiles) {
            shapeFileObject.paintObject(renderer, g);
        }
    }
}
