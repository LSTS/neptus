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

import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import vtk.vtkRenderer;
import vtk.vtkTextActor;

/**
 * @author hfq
 *
 */
public class Caption {
    
    private int xPosScreen;
    private int YPosScreen;
    
    private Boolean showNumberOfPoints = false;
    private Boolean showCloudName = false;
    private Boolean showCloudBounds = false;
    private Boolean showLatAndLon = false;
    
    private PointCloud<PointXYZ> pointCloud;
    private vtkRenderer renderer;
    
    private vtkTextActor captionActor;
    
    public Caption (int xPosScreen, int yPosScreen, Boolean showNumberOfPoints,
            Boolean showCloudName, Boolean showCloudBounds, Boolean showLatAndLon,
            PointCloud<PointXYZ> pointCloud, vtkRenderer renderer) {
            
        this.xPosScreen = xPosScreen;
        this.YPosScreen = yPosScreen;
        this.showNumberOfPoints = showNumberOfPoints;
        this.showCloudName = showCloudName;
        this.showCloudBounds = showCloudBounds;
        this.showLatAndLon = showLatAndLon;
        this.renderer = renderer;
        captionActor = new vtkTextActor();
        
        buildCaptionActor();
    }

    /**
     * 
     */
    private void buildCaptionActor() {
        captionActor.GetTextProperty().SetJustificationToLeft();
        captionActor.GetTextProperty().SetVerticalJustificationToTop();
        captionActor.GetTextProperty().SetShadowOffset(1, 1);
        captionActor.UseBorderAlignOn();
        captionActor.GetTextProperty().SetColor(1.0, 0.0, 0.0);
        captionActor.SetDisplayPosition(20, 2);
        
        System.out.println("entrou no build Caption");
        
        if(showNumberOfPoints) {
            System.out.println("foi ao show number of points");
            //int nPoints = pointCloud.getNumberOfPoints();
            int nPoints = 434;
            captionActor.SetInput("Number of Points: " + String.valueOf(nPoints));
        }
    }

    /**
     * @return the captionActor
     */
    public vtkTextActor getCaptionActor() {
        return captionActor;
    }

    /**
     * @param captionActor the captionActor to set
     */
    private void setCaptionActor(vtkTextActor captionActor) {
        this.captionActor = captionActor;
    }
    
}
