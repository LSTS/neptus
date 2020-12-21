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
 * Author: zp
 * Nov 25, 2013
 */
package pt.lsts.neptus.console.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Vector;
import java.util.concurrent.Future;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang.StringUtils;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.renderer2d.ImageLayer;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name="Image Layers")
@Popup(accelerator=KeyEvent.VK_W, height=300, width=250, name="Image Layers", pos=POSITION.CENTER)
public class ImageLayers extends SimpleRendererInteraction {

    private static final long serialVersionUID = 6903794413087312334L;

    @NeptusProperty(editable = false)
    public String layerFiles = "";

    @NeptusProperty(editable = false)
    public String lastDir = ".";

    private Vector<ImageLayer> layers = new Vector<>();    
    private String note = "";
    private Vector<File> files = new Vector<>();
    private JPanel scroll = new JPanel(new GridLayout(0, 2, 3, 3));


    public ImageLayers(ConsoleLayout cl) {
        super(cl);
        removeAll();
        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        //scroll.setLayout(new GridLayout(0, 2));        
    }

    private void rebuildControls() {
        scroll.removeAll();

        for (final ImageLayer il : layers) {
            scroll.add(new JLabel(il.getName()));
            final JSlider slider = new JSlider(0, 1000, (int)(il.getTransparency() * 1000));
            slider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {   
                    il.setTransparency(slider.getValue()/1000.0);
                }
            });
            slider.setMinimumSize(new Dimension(20, 20));
            scroll.add(slider);                  
        }        
        scroll.doLayout();
        scroll.revalidate();        
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup  = new JPopupMenu();
            popup.add("Add layer from file").addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser chooser = GuiUtils.getFileChooser(lastDir, I18n.text("Neptus image layers"), "layer");
                    int op = chooser.showOpenDialog(getConsole());
                    if (op != JFileChooser.APPROVE_OPTION)
                        return;
                    lastDir = chooser.getSelectedFile().getParent();
                    Future<ImageLayer> il = addLayer(chooser.getSelectedFile());          
                    try {
                        il.get();
                    }
                    catch (Exception ex) {
                        GuiUtils.errorMessage(getConsole(), ex);
                    }
                }
            });

            if (!layers.isEmpty()) {
                JMenu menu = new JMenu("Remove");
                JMenu menu2 = new JMenu("Opacity");
                for (final ImageLayer l : layers) {
                    menu.add(l.getName()).addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            layers.remove(l);
                            layerFiles = StringUtils.join(layers, ",");    
                            rebuildControls();
                        }
                    });

                    menu2.add(l.getName()+"("+l.getTransparency()+")").addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            String s = JOptionPane.showInputDialog(getConsole(), "Enter opacity (1 for opaque, 0 for invisible)", l.getTransparency());
                            if (s == null)
                                return;
                            try {
                                double val = Double.parseDouble(s);
                                if (val < 0)
                                    throw new Exception("Value must be greater or equal to 0");
                                if (val > 1)
                                    throw new Exception("Value must be less or equal to 1");
                                l.setTransparency(val);

                            }
                            catch (Exception ex) {
                                GuiUtils.errorMessage(getConsole(), ex);
                            }
                        }
                    });

                }
                popup.add(menu);
                popup.add(menu2);
            }

            popup.show(source, event.getX(), event.getY());
        }
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        g.setColor(Color.orange);
        g.drawString(note, 50, 20);
        for (ImageLayer il : layers)
            il.paint((Graphics2D)g.create(), renderer);    
    }

    private Future<ImageLayer> addLayer(final File f) {
        final SwingWorker<ImageLayer, Object> worker = new SwingWorker<ImageLayer, Object>() {
            @Override
            protected ImageLayer doInBackground() throws Exception {
                note = "loading layer in "+f.getAbsolutePath();
                ImageLayer il = ImageLayer.read(f);
                layers.add(il);    
                files.add(f);
                note = "";
                layerFiles = StringUtils.join(files, ",");
                rebuildControls();
                return il;
            }
        };
        worker.execute();
        return worker;
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public void initSubPanel() {
        for (String p : layerFiles.split(",")) {
            try {
                File f = new File(p);
                ImageLayer il = ImageLayer.read(f);
                layers.add(il);
                files.add(f);
                layerFiles = StringUtils.join(files, ",");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        rebuildControls();
    }

    @Override
    public void cleanSubPanel() {

    }

}
