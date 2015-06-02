package pt.lsts.neptus.hyperspectral;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;

/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsmarques
 * 13 May 2015
 */

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "HyperSpectral Data Viewer", author = "tsmarques", version = "0.1")
@LayerPriority(priority = 40)
public class HyperspectralViewer extends ConsoleLayer {
    public static final int MIN_FREQ = 0;
    public static final int MAX_FREQ = 640; /* also frame's width */
    /* frames will be FRAME_WIDTH x MAX_FREQ px */
    private static final int FRAME_HEIGHT = 250;
    private static final float FRAME_OPACITY = 0.9f;
    /* draw frames with opacity */
    private final AlphaComposite alcom = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, FRAME_OPACITY);
    
    private ConsoleLayout console;
    private float selectedWavelength;
    
    /* testing */
    private Image frame;

    public HyperspectralViewer() {
        this.console = getConsole();
        selectedWavelength = 0;
                
        /* testing */
        File imgFile = new File("../hyperspec-data/1431432615.6176.bmp");
        try {
            frame = ImageIO.read(imgFile);
        }
        catch (IOException e) { e.printStackTrace(); }
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        int posX = 0;
        int posY = (renderer.getHeight() - FRAME_HEIGHT) / 2;
        
        g.setComposite(alcom);
        g.drawImage(frame, posX, posY, null);
    }
    
    @Override
    public void initLayer() {

    }


    @Override
    public void cleanLayer() {
        
    }

    @Override
    public boolean userControlsOpacity() { return false; }
}
