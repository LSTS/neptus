package pt.lsts.neptus.hyperspectral;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.HyperSpecData;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
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
@PluginDescription(name = "Hyperspectral Layer", author = "tsmarques", version = "0.1")
@LayerPriority(priority = 40)
public class HyperspectralViewer extends ConsoleLayer implements ConfigurationListener {   
    public static final int MIN_FREQ = 0;
    public static final int MAX_FREQ = 640; /* also frame's width */
    /* frames will be FRAME_WIDTH x MAX_FREQ px */
    public static final int FRAME_HEIGHT = 250;
    
    
    private static final float FRAME_OPACITY = 0.9f;
    /* draw frames with opacity */
    private final AlphaComposite composite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, FRAME_OPACITY);
    private final AffineTransform transform = new AffineTransform();
    
    private ConsoleLayout console;
    private String mainSys = "";
      
    
    private boolean firstPaint = true;
    private BufferedImage dataDisplay; /* image currently being displayed */
    
    @NeptusProperty(editable = true, name = "Hyperspectral wavelength", userLevel = LEVEL.REGULAR)
    private double wavelengthProperty = 0;   
    private double selectedWavelength = -1;
    private Queue<byte[]> dataSet;
    

    public HyperspectralViewer() {
        this.console = getConsole();
        initDisplayedImage();
        dataSet = new LinkedList<byte[]>();
    }
    
    private void initDisplayedImage() {
        dataDisplay = new BufferedImage(MAX_FREQ, FRAME_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics g = dataDisplay.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, MAX_FREQ, FRAME_HEIGHT);
        g.dispose();
    }
    
    /* request data with new wavelength */
    private void requestWavelength() {
        /* TODO: send request for specified wavelength data to the vehicle */
    }
    
    @Override
    public void propertiesChanged() {
        if(!(wavelengthProperty >= MIN_FREQ && wavelengthProperty <= MAX_FREQ))
            return;
        
        if(wavelengthProperty != selectedWavelength) {          
            selectedWavelength = wavelengthProperty;
            
            initDisplayedImage();
            requestWavelength();
        }
    }
    
    
    private void updateDisplay(byte[] frameBytes) {
        BufferedImage newFrame = HyperspecUtils.rawToBuffImage(frameBytes);
        
        if(newFrame == null)
            return;
        
        /* remove oldest frame */
        dataDisplay = dataDisplay.getSubimage(1, 0, MAX_FREQ - 1, FRAME_HEIGHT);
        dataDisplay = HyperspecUtils.joinBufferedImage(dataDisplay, newFrame, MAX_FREQ, FRAME_HEIGHT);
    }
    
        
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if(firstPaint) {
            int newX = -((MAX_FREQ / 2)) + (FRAME_HEIGHT / 2);
            int newY = (renderer.getHeight() - FRAME_HEIGHT) / 2;
            
            transform.translate(newX, newY);
            transform.rotate(Math.toRadians(-90), dataDisplay.getWidth() / 2, dataDisplay.getHeight() / 2);
                        
            firstPaint = false;
        }
        else {
            synchronized(dataDisplay) {
                g.setColor(Color.red);
                g.drawString(selectedWavelength + " nm", (FRAME_HEIGHT / 2) - 30, (FRAME_HEIGHT / 2) - 60);
                g.setTransform(transform);
                g.setComposite(composite);
                g.drawImage(dataDisplay, 0, 0, renderer);
            }
        }
    }
    
    
    @Subscribe
    public void on(HyperSpecData msg){
        if(!msg.getSourceName().equals(mainSys))
            return;
        
        synchronized(dataDisplay) { updateDisplay(msg.getData()); }
    }
    
//    @Subscribe
//    public void on(EstimatedState state) {
//        if(!state.getSourceName().equals(mainSys))
//            return;
//    }
    

    @Subscribe
    public void on(ConsoleEventMainSystemChange ev) {
        mainSys = getConsole().getMainSystem();
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
