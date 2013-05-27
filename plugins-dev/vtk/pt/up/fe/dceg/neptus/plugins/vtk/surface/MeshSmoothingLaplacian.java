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
 * May 27, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.surface;

import pt.up.fe.dceg.neptus.NeptusLog;
import vtk.vtkLODActor;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkSmoothPolyDataFilter;

/**
 * @author hfq
 *  Mesh smoothing based on the vtkSmoothPolyDataFilter algorithm from the VTK library
 */
public class MeshSmoothingLaplacian {
    private vtkPolyData polyData;
    
    private int numIterations = 20;
    private float convergence = 0.0f;
    private float relaxationFactor = 0.01f;
    private boolean featureEdgeSmoothing = false;
    private float featureAngle = 45.f;
    private float edgeAngle = 15.f;
    private boolean boundarySmoothing = true;
    
    public MeshSmoothingLaplacian() {
        
    }
    
    public void performProcessing(PointCloudMesh mesh) {
        try {
            NeptusLog.pub().info("Laplacian Smoothing time start: " + System.currentTimeMillis());
            
            vtkSmoothPolyDataFilter smoother = new vtkSmoothPolyDataFilter();
            smoother.SetInput(mesh.getPolyData());
            smoother.SetNumberOfIterations(numIterations);
            if (convergence != 0.0f) {
                smoother.SetConvergence(convergence);
            }
            smoother.SetRelaxationFactor(relaxationFactor);
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
            
            setPolyData(smoother.GetOutput());
            getPolyData().Update();
            
//            mesh.setPolyData(smoother.GetOutput());
//            mesh.getPolyData().Update();
//            
//            vtkPolyDataMapper mapper = new vtkPolyDataMapper();
//            mapper.SetInputConnection(mesh.getPolyData().GetProducerPort());
//            mapper.Update();
//            
//            mesh.setMeshCloudLODActor(new vtkLODActor());
//            mesh.getMeshCloudLODActor().SetMapper(mapper);
//            mesh.getMeshCloudLODActor().Modified();
            
            NeptusLog.pub().info("Laplacian Smoothing time end: " + System.currentTimeMillis());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the numIterations
     */
    int getNumIterations() {
        return numIterations;
    }

    /**
     * @param numIterations the numIterations to set
     */
    void setNumIterations(int numIterations) {
        this.numIterations = numIterations;
    }

    /**
     * @return the convergence
     */
    float getConvergence() {
        return convergence;
    }

    /**
     * @param convergence the convergence to set
     */
    void setConvergence(float convergence) {
        this.convergence = convergence;
    }

    /**
     * @return the relaxationFactor
     */
    float getRelaxationFactor() {
        return relaxationFactor;
    }

    /**
     * @param relaxationFactor the relaxationFactor to set
     */
    void setRelaxationFactor(float relaxationFactor) {
        this.relaxationFactor = relaxationFactor;
    }

    /**
     * @return the featureEdgeSmooting
     */
    boolean isFeatureEdgeSmooting() {
        return featureEdgeSmoothing;
    }

    /**
     * @param featureEdgeSmooting the featureEdgeSmooting to set
     */
    void setFeatureEdgeSmooting(boolean featureEdgeSmooting) {
        this.featureEdgeSmoothing = featureEdgeSmooting;
    }

    /**
     * @return the featureAngle
     */
    float getFeatureAngle() {
        return featureAngle;
    }

    /**
     * @param featureAngle the featureAngle to set
     */
    void setFeatureAngle(float featureAngle) {
        this.featureAngle = featureAngle;
    }

    /**
     * @return the edgeAngle
     */
    float getEdgeAngle() {
        return edgeAngle;
    }

    /**
     * @param edgeAngle the edgeAngle to set
     */
    void setEdgeAngle(float edgeAngle) {
        this.edgeAngle = edgeAngle;
    }

    /**
     * @return the boundarySmoothing
     */
    boolean isBoundarySmoothing() {
        return boundarySmoothing;
    }

    /**
     * @param boundarySmoothing the boundarySmoothing to set
     */
    void setBoundarySmoothing(boolean boundarySmoothing) {
        this.boundarySmoothing = boundarySmoothing;
    }

    /**
     * @return the polyData
     */
    public vtkPolyData getPolyData() {
        return polyData;
    }

    /**
     * @param polyData the polyData to set
     */
    public void setPolyData(vtkPolyData polyData) {
        this.polyData = polyData;
    }
}
