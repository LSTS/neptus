/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Apr 11, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.visualization;

import vtk.vtkCommand;
import vtk.vtkInteractorStyle;
import vtk.vtkPNGWriter;
import vtk.vtkPanel;
import vtk.vtkRenderWindow;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkWindowToImageFilter;

/**
 * @author hfq
 *
 */
public class Window {
    
    private vtkCommand mouseCommmand;
    private vtkCommand keyboardCommand;
    private vtkInteractorStyle style;
    
    private vtkWindowToImageFilter wif;
    private vtkPNGWriter pngWriter;
    private String wifName;
    
    private vtkRenderWindow renWin;
    private vtkRenderWindowInteractor renWinInteractor;
    private String windowName;
    
    /**
     * Ideia: include snapshots with the interactor
     * @param renWin
     * @param interactor
     * @param windowName
     */
    public Window(vtkPanel panel) {     
        System.out.println("window 1"); 
        // a Render Window
        System.out.println("window 2"); 
        setRenWin(new vtkRenderWindow());
        // an Interactor
        System.out.println("window 3"); 
        setRenWinInteractor(new vtkRenderWindowInteractor());
        // a style interactor
        System.out.println("window 4"); 
        setStyle(new vtkInteractorStyle());
        
        System.out.println("window 5"); 
        
        getRenWin().AddRenderer(panel.GetRenderer());
        
        System.out.println("window 6");
        
        setUpRenWin();
        
        System.out.println("window 7");
        setUpRenWinInteractor();
        
        System.out.println("window 8");
        setUpInteractorStyle();
        
        System.out.println("window 9");
    }
    
    /**
     * Configures the Render Window
     */
    private void setUpRenWinInteractor() {
        getRenWinInteractor().SetRenderWindow(getRenWin());
    }

    /**
     * Configure the Interactor Style
     */
    private void setUpInteractorStyle() {
        getStyle().UseTimersOn();
        getStyle().SetInteractor(getRenWinInteractor());
    }

    /**
     * Configures the Render Window
     */
    private void setUpRenWin() {
        try {
            windowName = "viewportNeptus";
            getRenWin().SetWindowName(windowName);
            getRenWin().AlphaBitPlanesOff();
            getRenWin().PointSmoothingOff();
            getRenWin().LineSmoothingOff();
            getRenWin().PointSmoothingOff();
            getRenWin().SwapBuffersOn();
            getRenWin().SetStereoTypeToAnaglyph();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * still have to create a callback for this (keyboard event)
     */
    public void takeSnapShot() {
        
        wifName = "snapshot";
            
        wif = new vtkWindowToImageFilter();
        pngWriter = new vtkPNGWriter();
        
        getRenWinInteractor().FindPokedRenderer(getRenWinInteractor().GetEventPosition()[0], getRenWinInteractor().GetEventPosition()[1]);
        wif.SetInput(getRenWinInteractor().GetRenderWindow());
        wif.Modified(); // Update the WindowToImageFilter
        
        pngWriter.Modified();
        pngWriter.SetFileName(wifName);
        pngWriter.Write();
        //wifName = new String();
    }

    /**
     * @return the interactor style
     */
    public vtkInteractorStyle getStyle() {
        return style;
    }

    /**
     * @param style the style to set
     */
    private void setStyle(vtkInteractorStyle style) {
        this.style = style;
    }

    /**
     * @return the renWin
     */
    public vtkRenderWindow getRenWin() {
        return renWin;
    }

    /**
     * @param renWin the renWin to set
     */
    private void setRenWin(vtkRenderWindow renWin) {
        this.renWin = renWin;
    }

    /**
     * @return the renWinInteractor
     */
    public vtkRenderWindowInteractor getRenWinInteractor() {
        return renWinInteractor;
    }

    /**
     * @param renWinInteractor the renWinInteractor to set
     */
    private void setRenWinInteractor(vtkRenderWindowInteractor renWinInteractor) {
        this.renWinInteractor = renWinInteractor;
    }

}
