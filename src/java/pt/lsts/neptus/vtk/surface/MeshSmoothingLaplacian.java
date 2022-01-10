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

import vtk.vtkPolyData;
import vtk.vtkSmoothPolyDataFilter;

/**
 * @author hfq
 *  Mesh smoothing based on the vtkSmoothPolyDataFilter algorithm from the VTK library
 *  It's a filter that adjusts point coordinates using Laplacian smoothing. The effect is to "relax" the mesh, making the
 *  cells better shaped and the vertices more evenly distribuited.
 */
public class MeshSmoothingLaplacian {
    private vtkPolyData polyData;

    // number of of iteretaion over each vertex
    private int numIterations = 40;

    private float convergence = 0.0f;
    private float relaxationFactor = 0.08f;
    private boolean featureEdgeSmoothing = false;
    private float featureAngle = 90.f;
    private float edgeAngle = 5.f;
    private boolean boundarySmoothing = false;

    /**
     * 
     */
    public MeshSmoothingLaplacian() {

    }

    /**
     * 
     * @param numIterations
     * @param convergence
     * @param featureEdgeSmoothing
     * @param featureAngle
     * @param edgeAngle
     * @param boundarySmoothing
     */
    public MeshSmoothingLaplacian(int numIterations, float convergence, boolean featureEdgeSmoothing,
            float featureAngle, float edgeAngle, boolean boundarySmoothing) {
        setNumIterations(numIterations);
        setConvergence(convergence);
        setFeatureEdgeSmoothing(featureEdgeSmoothing);
        setFeatureAngle(featureAngle);
        setEdgeAngle(edgeAngle);
        setBoundarySmoothing(boundarySmoothing);
    }

    /**
     * 
     * @param mesh
     */
    public void performProcessing(PointCloudMesh mesh) {
        try {
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
     * @return the convergence
     */
    public float getConvergence() {
        return convergence;
    }

    /**
     * @param convergence the convergence to set
     */
    public void setConvergence(float convergence) {
        this.convergence = convergence;
    }

    /**
     * @return the relaxationFactor
     */
    public float getRelaxationFactor() {
        return relaxationFactor;
    }

    /**
     * @param relaxationFactor the relaxationFactor to set
     */
    public void setRelaxationFactor(float relaxationFactor) {
        this.relaxationFactor = relaxationFactor;
    }

    /**
     * @return the featureEdgeSmooting
     */
    public boolean isFeatureEdgeSmoothing() {
        return featureEdgeSmoothing;
    }

    /**
     * @param featureEdgeSmooting the featureEdgeSmooting to set
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
