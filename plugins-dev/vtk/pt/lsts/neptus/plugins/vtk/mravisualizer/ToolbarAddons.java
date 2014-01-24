/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Author: hfq
 * May 30, 2013
 */
package pt.lsts.neptus.plugins.vtk.mravisualizer;

import pt.lsts.neptus.i18n.I18n;
import vtk.vtkTextActor;

/**
 * @author hfq
 *
 */
public class ToolbarAddons {

    private int currentZexagge = 0;
    
    public ToolbarAddons() {
    }

    /**
     * @param textZExagInfoActor
     * 
     */
    public void buildTextZExagInfoActor(vtkTextActor textZExagInfoActor) {
        textZExagInfoActor.GetTextProperty().BoldOn();
        textZExagInfoActor.GetTextProperty().ItalicOn();
        textZExagInfoActor.GetTextProperty().SetColor(1.0, 1.0, 1.0);
        textZExagInfoActor.GetTextProperty().SetFontFamilyToArial();
        textZExagInfoActor.GetTextProperty().SetFontSize(12);
        textZExagInfoActor.SetInput(I18n.textf("Depth multipled by: %currentZexagge", getCurrentZexagge()));   //  
        textZExagInfoActor.VisibilityOn();
    }

    /**
     * @param textProcessingActor
     * 
     */
    public void buildTextProcessingActor(vtkTextActor textProcessingActor) {

       textProcessingActor.GetTextProperty().BoldOn();
       textProcessingActor.GetTextProperty().ItalicOn();
       textProcessingActor.GetTextProperty().SetFontSize(40);
       textProcessingActor.GetTextProperty().SetColor(1.0, 1.0, 1.0);
       textProcessingActor.GetTextProperty().SetFontFamilyToArial();
       //textProcessingActor.GetTextProperty().SetJustificationToCentered();
       textProcessingActor.SetInput(I18n.text("Processing data..."));
       textProcessingActor.VisibilityOn();   
    }
    
    public String msgHelp() {
        
        String msgHelp;        
        //<h1>3D Multibeam Interaction</h1>
        msgHelp = "<html><font size='2'><br><div align='center'><table border='1' align='center'>" +
                "<tr><th>Keys</th><th>Description</th></tr>" +
                "<tr><td>p, P</td><td>Switch to a point-based representation</td>" +
                "<tr><td>w, W </td><td>Switch to a wireframe-based representation, when available</td>" +
                "<tr><td>s, S</td><td>Switch to a surface-based representation, when available</td>" +
                "<tr><td>j, J</td><td>Take a .PNG snapshot of the current window view</td>" +
                "<tr><td>g, G</td><td>Display scale grid (on/off)</td>" +
                "<tr><td>u, U</td><td>Display lookup table (on/off)</td>" +
                "<tr><td>r, R</td><td>Reset camera view along the current view direction</td>" +    // (to viewpoint = {0, 0, 0} -> center {x, y, z}\n");
                "<tr><td>i, I</td><td>Information about rendered cloud</td>" +
                "<tr><td>f, F</td><td>Fly Mode - point with mouse cursor the direction and press 'f' to fly</td>" +
                "<tr><td>+/-</td><td>Increment / Decrement overall point size</td>" +
                "<tr><td>3</td><td>Toggle into an out of stereo mode</td>" +
                "<tr><td>7</td><td>Color gradient in relation with X coords (north)</td>" +
                "<tr><td>8</td><td>Color gradient in relation with Y coords (west)</td>" +
                "<tr><td>9</td><td>Color gradient in relation with Z coords (depth)</td>" +
                "<tr><th>Mouse</th><th>Description</th></tr>" +
                // rotate the camera around its focal point. The rotation is in the direction defined from the center of the renderer's viewport towards the mouse position
                "<tr><td>Left mouse button</td><td>Rotate camera around its focal point</td>" +
                "<tr><td>Middle mouse button</td><td>Pan camera</td>" +
                "<tr><td>Right mouse button</td><td>Zoom (In/Out) the camera</td>" +
                "<tr><td>Mouse wheel</td><td>Zoom (In/Out) the camera - Static focal point</td>";
        
        return msgHelp;
    }

    /**
     * @return the currentZexagge
     */
    public int getCurrentZexagge() {
        return currentZexagge;
    }

    /**
     * @param currentZexagge the currentZexagge to set
     */
    public void setCurrentZexagge(int currentZexagge) {
        this.currentZexagge = currentZexagge;
    }
    
}
