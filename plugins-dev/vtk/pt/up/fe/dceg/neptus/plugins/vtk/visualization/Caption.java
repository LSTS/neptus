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
 * May 4, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.visualization;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import vtk.vtkLODActor;
import vtk.vtkRenderer;
import vtk.vtkTextActor;

/**
 * @author hfq
 *
 */
public class Caption {
    public Boolean captionEnabled = false;

    private int xPosScreen;
    private int YPosScreen;
    
    private int numberOfPoints;
    private String cloudName;
    private double[] bounds;
    private double[] bounds2;
    
//    private Boolean showNumberOfPoints = false;
//    private Boolean showCloudName = false;
//    private Boolean showCloudBounds = false;
//    private Boolean showLatAndLon = false;
    
    private vtkRenderer renderer;
    
    //private vtkTextActor captionActor;
    private vtkTextActor captionNumberOfPointsActor;
    private vtkTextActor captionCloudNameActor;
    private vtkTextActor captionCloudBoundsActor;
    private vtkTextActor captionLatLonActor;
    

    /**
     * @param xPosScreen
     * @param yPosScreen
     * @param numberOfPoints
     * @param cloudName
     * @param bounds
     * @param renderer
     */
    public Caption(int xPosScreen, int yPosScreen, int numberOfPoints, String cloudName, double[] bounds, vtkRenderer renderer) {
        this.xPosScreen = xPosScreen;
        this.YPosScreen = yPosScreen;
        this.numberOfPoints = numberOfPoints;
        this.cloudName = cloudName;
        this.bounds = bounds;
        this.renderer = renderer;
        
        setCaptionNumberOfPointsActor(new vtkTextActor());
        setCaptionCloudNameActor(new vtkTextActor());
        setCaptionCloudBoundsActor(new vtkTextActor());
        setCaptionLatLonActor(new vtkTextActor());
        
        buildCaptionActor();
        captionEnabled = true;
    }

    /**
     * 
     */
    private void buildCaptionActor() {       
        //captionNumberOfPointsActor.GetTextProperty().SetJustificationToLeft();
        //captionNumberOfPointsActor.GetTextProperty().SetVerticalJustificationToTop();
        captionNumberOfPointsActor.GetTextProperty().SetFontSize(9);
        captionNumberOfPointsActor.GetTextProperty().SetBold(1);
        captionNumberOfPointsActor.GetTextProperty().BoldOn();
        captionNumberOfPointsActor.GetTextProperty().ItalicOn();
        //captionNumberOfPointsActor.UseBorderAlignOn();
        captionNumberOfPointsActor.GetTextProperty().SetColor(1.0, 0.0, 0.0);
        captionNumberOfPointsActor.SetDisplayPosition(xPosScreen, YPosScreen);      
        captionNumberOfPointsActor.SetInput("Number of Points: " + String.valueOf(numberOfPoints));
        
        //captionCloudNameActor.GetTextProperty().SetJustificationToLeft();
        //captionCloudNameActor.GetTextProperty().SetVerticalJustificationToTop();
        captionCloudNameActor.GetTextProperty().SetBold(1);
        captionCloudNameActor.GetTextProperty().BoldOn();
        captionCloudNameActor.GetTextProperty().SetItalic(1);
        captionCloudNameActor.GetTextProperty().ItalicOn();      
        //captionCloudNameActor.UseBorderAlignOn();
        captionCloudNameActor.GetTextProperty().SetFontSize(9);
        captionCloudNameActor.GetTextProperty().SetColor(1.0, 0.0, 0.0);
        captionCloudNameActor.SetDisplayPosition(xPosScreen, YPosScreen - 13);      
        captionCloudNameActor.SetInput("Point Cloud Name: " + cloudName);
        
        captionCloudBoundsActor.GetTextProperty().SetShadowOffset(1, 1);
        captionCloudBoundsActor.GetTextProperty().SetFontSize(9);
        captionCloudBoundsActor.GetTextProperty().SetBold(1);
        captionCloudBoundsActor.GetTextProperty().ItalicOn();
        captionCloudBoundsActor.GetTextProperty().SetColor(1.0, 0.0, 0.0);
        captionCloudBoundsActor.SetDisplayPosition(xPosScreen, YPosScreen - 56);
        DecimalFormat f = new DecimalFormat("##.00");
        captionCloudBoundsActor.SetInput("Bounds (meters): " + "\n" + "minX: " + f.format(bounds[0]) + "\t maxX: " + f.format(bounds[1]) + "\n" + "minY: " + f.format(bounds[2]) + "\t maxY: " + f.format(bounds[3]) + "\n" + "minZ: " + f.format(bounds[4]) + "\t maxZ: " + f.format(bounds[5]));     
    }

    /**
     * @return the captionNumberOfPointsActor
     */
    public vtkTextActor getCaptionNumberOfPointsActor() {
        return captionNumberOfPointsActor;
    }

    /**
     * @param captionNumberOfPointsActor the captionNumberOfPointsActor to set
     */
    private void setCaptionNumberOfPointsActor(vtkTextActor captionNumberOfPointsActor) {
        this.captionNumberOfPointsActor = captionNumberOfPointsActor;
    }

    /**
     * @return the captionCloudNameActor
     */
    public vtkTextActor getCaptionCloudNameActor() {
        return captionCloudNameActor;
    }

    /**
     * @param captionCloudNameActor the captionCloudNameActor to set
     */
    private void setCaptionCloudNameActor(vtkTextActor captionCloudNameActor) {
        this.captionCloudNameActor = captionCloudNameActor;
    }

    /**
     * @return the captionCloudBoundsActor
     */
    public vtkTextActor getCaptionCloudBoundsActor() {
        return captionCloudBoundsActor;
    }

    /**
     * @param captionCloudBoundsActor the captionCloudBoundsActor to set
     */
    private void setCaptionCloudBoundsActor(vtkTextActor captionCloudBoundsActor) {
        this.captionCloudBoundsActor = captionCloudBoundsActor;
    }

    /**
     * @return the captionLatLonActor
     */
    public vtkTextActor getCaptionLatLonActor() {
        return captionLatLonActor;
    }

    /**
     * @param captionLatLonActor the captionLatLonActor to set
     */
    private void setCaptionLatLonActor(vtkTextActor captionLatLonActor) {
        this.captionLatLonActor = captionLatLonActor;
    }
    
}
