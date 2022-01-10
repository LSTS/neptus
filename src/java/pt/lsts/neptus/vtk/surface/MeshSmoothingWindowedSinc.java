/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * May 27, 2013
 */
package pt.lsts.neptus.vtk.surface;

import pt.lsts.neptus.NeptusLog;
import vtk.vtkLODActor;
import vtk.vtkPolyDataMapper;
import vtk.vtkWindowedSincPolyDataFilter;

/**
 * @author hfq
 * Mesh Smoothing based on the vtkWindowedSincPolyDataFilter
 */
public class MeshSmoothingWindowedSinc {
    private int numIterations = 20;
    private float passBand = 0.1f;
    private boolean featureEdgeSmoothing = false;
    private float featureAngle = 45.f;
    private float edgeAngle = 15.f;
    private boolean boundarySmoothing = true;
    private boolean normalizeCoordinates = false;
    
    public MeshSmoothingWindowedSinc() {

    }
    
    public void performProcessing(PointCloudMesh mesh) {
        try {
            NeptusLog.pub().info("Smoothing Windowed Sinc time start: " + System.currentTimeMillis());
            
            vtkWindowedSincPolyDataFilter smoother = new vtkWindowedSincPolyDataFilter();
            smoother.SetInput(mesh.getPolyData());
            smoother.SetNumberOfIterations(numIterations);
            smoother.SetPassBand(passBand);
            if (!normalizeCoordinates)
                smoother.NormalizeCoordinatesOff();
            else
                smoother.NormalizeCoordinatesOn();
            if (!featureEdgeSmoothing)
                smoother.FeatureEdgeSmoothingOff();
            else
                smoother.FeatureEdgeSmoothingOn();
            smoother.SetFeatureAngle(featureAngle);
            smoother.SetEdgeAngle(edgeAngle);
            if (boundarySmoothing)
                smoother.BoundarySmoothingOn();
            else
                smoother.BoundarySmoothingOff();
            
            mesh.setPolyData(smoother.GetOutput());
            mesh.getPolyData().Update();
            
            vtkPolyDataMapper mapper = new vtkPolyDataMapper();
            mapper.SetInputConnection(mesh.getPolyData().GetProducerPort());
            mapper.Update();
            
            mesh.setMeshCloudLODActor(new vtkLODActor());
            mesh.getMeshCloudLODActor().SetMapper(mapper);
            mesh.getMeshCloudLODActor().Modified();
            
            NeptusLog.pub().info("Smoothing Windowed Sinc time end: " + System.currentTimeMillis());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the numIterations
     */
    public int getNumIterations() {
        return numIterations;
    }

    /**
     * @param numIterations the numIterations to set
     */
    public void setNumIterations(int numIterations) {
        this.numIterations = numIterations;
    }

    /**
     * @return the passBand
     */
    public float getPassBand() {
        return passBand;
    }

    /**
     * @param passBand the passBand to set
     */
    public void setPassBand(float passBand) {
        this.passBand = passBand;
    }

    /**
     * @return the featureEdgeSmoothing
     */
    public boolean isFeatureEdgeSmoothing() {
        return featureEdgeSmoothing;
    }

    /**
     * @param featureEdgeSmoothing the featureEdgeSmoothing to set
     */
    public void setFeatureEdgeSmoothing(boolean featureEdgeSmoothing) {
        this.featureEdgeSmoothing = featureEdgeSmoothing;
    }

    /**
     * @return the featureAngle
     */
    public float getFeatureAngle() {
        return featureAngle;
    }

    /**
     * @param featureAngle the featureAngle to set
     */
    public void setFeatureAngle(float featureAngle) {
        this.featureAngle = featureAngle;
    }

    /**
     * @return the edgeAngle
     */
    public float getEdgeAngle() {
        return edgeAngle;
    }

    /**
     * @param edgeAngle the edgeAngle to set
     */
    public void setEdgeAngle(float edgeAngle) {
        this.edgeAngle = edgeAngle;
    }

    /**
     * @return the boundarySmoothing
     */
    public boolean isBoundarySmoothing() {
        return boundarySmoothing;
    }

    /**
     * @param boundarySmoothing the boundarySmoothing to set
     */
    public void setBoundarySmoothing(boolean boundarySmoothing) {
        this.boundarySmoothing = boundarySmoothing;
    }

    /**
     * @return the normalizeCoordinates
     */
    public boolean isNormalizeCoordinates() {
        return normalizeCoordinates;
    }

    /**
     * @param normalizeCoordinates the normalizeCoordinates to set
     */
    public void setNormalizeCoordinates(boolean normalizeCoordinates) {
        this.normalizeCoordinates = normalizeCoordinates;
    }
}
