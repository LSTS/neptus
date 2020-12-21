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
 * Author: hfq
 * May 4, 2013
 */
package pt.lsts.neptus.vtk.visualization;

import java.text.DecimalFormat;

import vtk.vtkTextActor;

/**
 * @author hfq Class sets up a information caption from the renderer pointcloud
 */
public class InfoPointcloud2DText {
    private Boolean captionEnabled = false;

    private int xPosScreen;
    private int YPosScreen;

    private int numberOfPoints;
    private String cloudName;
    private double[] bounds;
    private int memorySize;

    private vtkTextActor captionNumberOfPointsActor;
    private vtkTextActor captionCloudNameActor;
    private vtkTextActor captionCloudBoundsActor;
    private vtkTextActor captionMemorySizeActor;
    private vtkTextActor captionLatLonActor;

    /**
     * Constructor
     * 
     * @param xPosScreen
     * @param yPosScreen
     * @param numberOfPoints
     * @param cloudName
     * @param bounds
     * @param memorySize
     * @param renderer
     */
    public InfoPointcloud2DText(int xPosScreen, int yPosScreen, int numberOfPoints, String cloudName, double[] bounds, int memorySize) {
        this.xPosScreen = xPosScreen;
        this.YPosScreen = yPosScreen;
        this.numberOfPoints = numberOfPoints;
        this.cloudName = cloudName;
        this.bounds = bounds;
        this.memorySize = memorySize;

        setCaptionNumberOfPointsActor(new vtkTextActor());
        setCaptionCloudNameActor(new vtkTextActor());
        setCaptionCloudBoundsActor(new vtkTextActor());
        setCaptionLatLonActor(new vtkTextActor());
        setCaptionMemorySizeActor(new vtkTextActor());

        buildCaptionActor();
        setCaptionEnabled(true);
    }

    /**
     * Sets up all components for 2D text actors
     */
    private void buildCaptionActor() {
        try {
            // captionNumberOfPointsActor.GetTextProperty().SetJustificationToLeft();
            // captionNumberOfPointsActor.GetTextProperty().SetVerticalJustificationToTop();
            captionNumberOfPointsActor.GetTextProperty().BoldOn();
            captionNumberOfPointsActor.GetTextProperty().ItalicOn();
            captionNumberOfPointsActor.GetTextProperty().SetFontSize(9);
            captionNumberOfPointsActor.GetTextProperty().SetColor(1.0, 0.0, 0.0);
            captionNumberOfPointsActor.SetDisplayPosition(xPosScreen, YPosScreen);
            captionNumberOfPointsActor.SetInput("Number of Points: " + String.valueOf(numberOfPoints));

            // captionCloudNameActor.GetTextProperty().SetJustificationToLeft();
            // captionCloudNameActor.GetTextProperty().SetVerticalJustificationToTop();
            captionCloudNameActor.GetTextProperty().BoldOn();
            captionCloudNameActor.GetTextProperty().ItalicOn();
            captionCloudNameActor.GetTextProperty().SetFontSize(9);
            captionCloudNameActor.GetTextProperty().SetColor(1.0, 0.0, 0.0);
            captionCloudNameActor.SetDisplayPosition(xPosScreen, YPosScreen - 14);
            captionCloudNameActor.SetInput("Point Cloud Name: " + cloudName);

            captionMemorySizeActor.GetTextProperty().BoldOn();
            captionMemorySizeActor.GetTextProperty().ItalicOn();
            captionMemorySizeActor.GetTextProperty().SetFontSize(9);
            captionMemorySizeActor.GetTextProperty().SetColor(1.0, 0.0, 0.0);
            captionMemorySizeActor.SetDisplayPosition(xPosScreen, YPosScreen - 30);
            captionMemorySizeActor.SetInput("Memory Size (kB): " + String.valueOf(memorySize));

            captionCloudBoundsActor.GetTextProperty().ItalicOn();
            captionCloudBoundsActor.GetTextProperty().BoldOn();
            captionCloudBoundsActor.GetTextProperty().SetFontSize(9);
            captionCloudBoundsActor.GetTextProperty().SetColor(1.0, 0.0, 0.0);
            captionCloudBoundsActor.SetDisplayPosition(xPosScreen, YPosScreen - 72);
            DecimalFormat f = new DecimalFormat("##.00");
            captionCloudBoundsActor
            .SetInput("Bounds (meters): " + "\n" + "minX: " + f.format(bounds[0]) + "     maxX: "
                    + f.format(bounds[1]) + "\n" + "minY: " + f.format(bounds[2]) + "     maxY: "
                    + f.format(bounds[3]) + "\n" + "minZ: " + f.format(bounds[4]) + "     maxZ: "
                    + f.format(bounds[5]));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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

    /**
     * @return the captionMemorySizeActor
     */
    public vtkTextActor getCaptionMemorySizeActor() {
        return captionMemorySizeActor;
    }

    /**
     * @param captionMemorySizeActor the captionMemorySizeActor to set
     */
    private void setCaptionMemorySizeActor(vtkTextActor captionMemorySizeActor) {
        this.captionMemorySizeActor = captionMemorySizeActor;
    }

    /**
     * @return the captionEnabled
     */
    public Boolean getCaptionEnabled() {
        return captionEnabled;
    }

    /**
     * @param captionEnabled the captionEnabled to set
     */
    public void setCaptionEnabled(Boolean captionEnabled) {
        this.captionEnabled = captionEnabled;
    }
}
