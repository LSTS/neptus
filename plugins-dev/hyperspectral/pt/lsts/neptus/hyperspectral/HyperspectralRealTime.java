package pt.lsts.neptus.hyperspectral;

import java.awt.AlphaComposite;
import java.awt.Color;
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
import pt.lsts.neptus.hyperspectral.display.VerticalLayer;
import pt.lsts.neptus.hyperspectral.utils.HyperspecUtils;
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
public class HyperspectralRealTime extends ConsoleLayer implements ConfigurationListener {   
    public static final int MIN_FREQ = 0;
    public static final int MAX_FREQ = 640; /* also frame's width */
    /* frames will be FRAME_WIDTH x MAX_FREQ px */
    public static final int FRAME_HEIGHT = 250;
    
    private ConsoleLayout console;
    private String mainSys = "";

    private boolean firstPaint = true;
    private VerticalLayer dataDisplay; /* image currently being displayed */
    
    @NeptusProperty(editable = true, name = "Hyperspectral wavelength", userLevel = LEVEL.REGULAR)
    private double wavelengthProperty = 0;   
    private double selectedWavelength = -1;
    private Queue<byte[]> dataSet;
    

    public HyperspectralRealTime() {
        this.console = getConsole();
        initDisplay();
        dataSet = new LinkedList<byte[]>();
    }
    
    private void initDisplay() {
        dataDisplay = new VerticalLayer(MAX_FREQ, FRAME_HEIGHT);
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
            
            initDisplay();
            requestWavelength();
        }
    }   
        
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        synchronized(dataDisplay) {
            if(firstPaint) {
                int posX = -((MAX_FREQ / 2)) + (FRAME_HEIGHT / 2);
                int posY = (renderer.getHeight() - FRAME_HEIGHT) / 2;

                dataDisplay.positionLayer(posX, posY);
                firstPaint = false;
            }
            else
                dataDisplay.display(g, renderer);
        }
    }
    
    
    @Subscribe
    public void on(HyperSpecData msg) {
        if(!msg.getSourceName().equals(mainSys))
            return;
        
        synchronized(dataDisplay) {
            dataDisplay.update(msg.getData());
        }
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
        mainSys = getConsole().getMainSystem();
    }
    
    @Override
    public void cleanLayer() {
        
    }

    @Override
    public boolean userControlsOpacity() { return false; }

}
