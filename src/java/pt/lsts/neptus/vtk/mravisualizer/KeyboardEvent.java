/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Apr 18, 2013
 */
package pt.lsts.neptus.vtk.mravisualizer;

import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Set;

import pt.lsts.neptus.vtk.events.AKeyboardEvent;
import pt.lsts.neptus.vtk.pointcloud.APointCloud;
import pt.lsts.neptus.vtk.pointcloud.PointCloudHandlerXYZ;
import pt.lsts.neptus.vtk.pointcloud.PointCloudHandlerXYZI;
import pt.lsts.neptus.vtk.visualization.AInteractorStyleTrackballCamera;
import pt.lsts.neptus.vtk.visualization.Canvas;
import vtk.vtkAbstractPropPicker;
import vtk.vtkActorCollection;
import vtk.vtkAssemblyPath;
import vtk.vtkLODActor;

/**
 * @author hfq
 */
public class KeyboardEvent extends AKeyboardEvent {
    private InteractorStyleVis3D interactorStyle;

    private final EventsHandler events;

    private final LinkedHashMap<String, APointCloud<?>> linkedHashMapCloud;

    private Set<String> setOfClouds;

    private APointCloud<?> pointCloud;

    // private vtkLODActor marker = new vtkLODActor();
    private boolean markerEnabled = false;

    private enum ColorMappingRelation {
        xMap,
        yMap,
        zMap,
        iMap
    }

    public ColorMappingRelation colorMapRel;

    /**
     * @param canvas
     * @param linkedHashMapCloud
     * @param neptusInteractorStyle
     */
    public KeyboardEvent(Canvas canvas, LinkedHashMap<String, APointCloud<?>> linkedHashMapCloud,
            InteractorStyleVis3D interactorStyle, EventsHandler events) {
        super(canvas);
        setInteractorStyle(interactorStyle);
        this.events = events;
        this.linkedHashMapCloud = linkedHashMapCloud;
        colorMapRel = ColorMappingRelation.zMap; // on creation map color map is z related

        // canvas.addKeyListener(this);
    }

    @Override
    public void handleEvents(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_J:
                events.takeSnapShot("Bathymetry_");
                break;
            case KeyEvent.VK_U:
                try {
                    // canvas.lock();
                    if (!interactorStyle.lutEnabled) {
                        vtkActorCollection actorCollection = new vtkActorCollection();
                        actorCollection = getRenderer().GetActors();
                        actorCollection.InitTraversal();

                        for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                            // vtkLODActor tempActor = new vtkLODActor();

                            if (actorCollection.GetNextActor().IsA("vtkActor2D") > 0)
                                continue;
                            // tempActor = (vtkLODActor) actorCollection.GetNextActor();

                            setOfClouds = linkedHashMapCloud.keySet();
                            for (String skey : setOfClouds) {
                                pointCloud = linkedHashMapCloud.get(skey);
                                switch (colorMapRel) {
                                    case xMap:
                                        interactorStyle.getScalarBar().setUpScalarBarLookupTable(
                                                ((PointCloudHandlerXYZ) pointCloud.getColorHandler()).getLutX());
                                        break;
                                    case yMap:
                                        interactorStyle.getScalarBar().setUpScalarBarLookupTable(
                                                ((PointCloudHandlerXYZ) pointCloud.getColorHandler()).getLutY());
                                        break;
                                    case zMap:
                                        interactorStyle.getScalarBar().setUpScalarBarLookupTable(
                                                ((PointCloudHandlerXYZ) pointCloud.getColorHandler()).getLutZ());
                                        break;
                                    case iMap:
                                        if (pointCloud.getColorHandler() instanceof PointCloudHandlerXYZI) {
                                            interactorStyle.getScalarBar().setUpScalarBarLookupTable(
                                                    ((PointCloudHandlerXYZI) pointCloud.getColorHandler()).getLutI());
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                        getCanvas().lock();
                        getRenderer().AddActor(interactorStyle.getScalarBar().getScalarBarActor());
                        getCanvas().unlock();
                        interactorStyle.lutEnabled = true;
                    }
                    else {
                        getCanvas().lock();
                        getRenderer().RemoveActor(interactorStyle.getScalarBar().getScalarBarActor());
                        getCanvas().unlock();
                        interactorStyle.lutEnabled = false;
                    }
                    getCanvas().lock();
                    getCanvas().Render();
                    // interactor.Render();
                    getCanvas().unlock();
                }
                catch (Exception e6) {
                    e6.printStackTrace();
                }
                break;
            case KeyEvent.VK_G:
                try {
                    // canvas.lock();
                    if (!interactorStyle.gridEnabled) {
                        interactorStyle.gridActor.TopAxisVisibilityOn();
                        getCanvas().lock();
                        getRenderer().AddViewProp(interactorStyle.gridActor);
                        getCanvas().unlock();
                        interactorStyle.gridEnabled = true;
                    }
                    else {
                        getCanvas().lock();
                        getRenderer().RemoveViewProp(interactorStyle.gridActor);
                        getCanvas().unlock();
                        interactorStyle.gridEnabled = false;
                    }
                    getCanvas().lock();
                    // interactor.Render();
                    getCanvas().Render();
                    getCanvas().unlock();
                }
                catch (Exception e5) {
                    e5.printStackTrace();
                }
                break;
                // case KeyEvent.VK_C: // FIXME - not good enough, better check this one for a better implementation.
                // problems: seems to be disconected of the rendered actor
                // try {
                //
                // if (!neptusInteractorStyle.compassEnabled) {
                // canvas.lock();
                // neptusInteractorStyle.compass.addCompassToVisualization(interactor);
                // canvas.unlock();
                // neptusInteractorStyle.compassEnabled = true;
                // }
                // else {
                // canvas.lock();
                // neptusInteractorStyle.compass.removeCompassFromVisualization(interactor);
                // canvas.unlock();
                // neptusInteractorStyle.compassEnabled = false;
                // }
                // canvas.lock();
                // canvas.Render();
                // canvas.unlock();
                // }
                // catch (Exception e4) {
                // e4.printStackTrace();
                // }
                // break;
            case KeyEvent.VK_M:
                try {
                    getCanvas().lock();
                    if (!markerEnabled) {
                        markerEnabled = true;
                        // neptusInteractorStyle.renderer.AddActor(marker);
                    }
                    else {
                        markerEnabled = false;
                    }
                    getCanvas().unlock();
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
                break;
            case KeyEvent.VK_I:
                //                if (!captionEnabled) {
                //                    try {
                //                        getCanvas().lock();
                //                        captionInfo = new InfoPointcloud2DText(4, 250, linkedHashMapCloud.get("multibeam")
                //                                .getNumberOfPoints(), linkedHashMapCloud.get("multibeam").getCloudName(),
                //                                linkedHashMapCloud.get("multibeam").getBounds(), linkedHashMapCloud.get("multibeam")
                //                                .getMemorySize());
                //
                //                        getRenderer().AddActor(captionInfo.getCaptionNumberOfPointsActor());
                //                        getRenderer().AddActor(captionInfo.getCaptionCloudNameActor());
                //                        getRenderer().AddActor(captionInfo.getCaptionMemorySizeActor());
                //                        getRenderer().AddActor(captionInfo.getCaptionCloudBoundsActor());
                //
                //                        captionEnabled = true;
                //                        getCanvas().unlock();
                //                    }
                //                    catch (Exception e) {
                //                        e.printStackTrace();
                //                    }
                //                }
                //                else {
                //                    try {
                //                        getCanvas().lock();
                //                        getRenderer().RemoveActor(captionInfo.getCaptionNumberOfPointsActor());
                //                        getRenderer().RemoveActor(captionInfo.getCaptionCloudNameActor());
                //                        getRenderer().RemoveActor(captionInfo.getCaptionMemorySizeActor());
                //                        getRenderer().RemoveActor(captionInfo.getCaptionCloudBoundsActor());
                //                        captionEnabled = false;
                //                        getCanvas().Render();
                //                        getCanvas().unlock();
                //                    }
                //                    catch (Exception e) {
                //                        e.printStackTrace();
                //                    }
                //                }
                break;
            case KeyEvent.VK_PLUS: // increment size of rendered cell point
                try {

                    vtkActorCollection actorCollection = new vtkActorCollection();
                    actorCollection = getRenderer().GetActors();
                    actorCollection.InitTraversal();

                    for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                        vtkLODActor tempActor = new vtkLODActor();
                        tempActor = (vtkLODActor) actorCollection.GetNextActor();
                        setOfClouds = linkedHashMapCloud.keySet();
                        for (String sKey : setOfClouds) {
                            pointCloud = linkedHashMapCloud.get(sKey);
                            if (tempActor.equals(pointCloud.getCloudLODActor())) {
                                double pointSize = tempActor.GetProperty().GetPointSize();
                                if (pointSize <= 9.0) {
                                    getCanvas().lock();
                                    tempActor.GetProperty().SetPointSize(pointSize + 1);
                                    getCanvas().Render();
                                    getCanvas().unlock();
                                }
                            }
                        }
                    }

                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case KeyEvent.VK_MINUS: // case '-': // decrement size of rendered cell point
                try {
                    vtkActorCollection actorCollection = new vtkActorCollection();
                    actorCollection = getRenderer().GetActors();
                    actorCollection.InitTraversal();

                    for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                        vtkLODActor tempActor = new vtkLODActor();
                        tempActor = (vtkLODActor) actorCollection.GetNextActor();
                        setOfClouds = linkedHashMapCloud.keySet();
                        for (String sKey : setOfClouds) {
                            pointCloud = linkedHashMapCloud.get(sKey);
                            if (tempActor.equals(pointCloud.getCloudLODActor())) {
                                double pointSize = tempActor.GetProperty().GetPointSize();
                                if (pointSize > 1.0) {
                                    getCanvas().lock();
                                    tempActor.GetProperty().SetPointSize(pointSize - 1);
                                    getCanvas().Render();
                                    getCanvas().unlock();
                                }
                            }
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case KeyEvent.VK_7: // color map X axis related
                try {

                    if (!(colorMapRel == ColorMappingRelation.xMap)) {
                        setOfClouds = linkedHashMapCloud.keySet();
                        for (String sKey : setOfClouds) {
                            pointCloud = linkedHashMapCloud.get(sKey);
                            pointCloud.getPolyData().GetPointData()
                            .SetScalars(((PointCloudHandlerXYZ) pointCloud.getColorHandler()).getColorsX());
                            if (interactorStyle.lutEnabled)
                                interactorStyle.getScalarBar().setUpScalarBarLookupTable(
                                        ((PointCloudHandlerXYZ) pointCloud.getColorHandler()).getLutX());
                            colorMapRel = ColorMappingRelation.xMap;

                        }
                        getCanvas().lock();
                        getCanvas().Render();
                        getCanvas().unlock();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case KeyEvent.VK_8: // color map Y axis related
                try {
                    if (!(colorMapRel == ColorMappingRelation.yMap)) {
                        setOfClouds = linkedHashMapCloud.keySet();
                        for (String sKey : setOfClouds) {
                            pointCloud = linkedHashMapCloud.get(sKey);
                            pointCloud.getPolyData().GetPointData()
                            .SetScalars(((PointCloudHandlerXYZ) pointCloud.getColorHandler()).getColorsY());
                            if (interactorStyle.lutEnabled)
                                interactorStyle.getScalarBar().setUpScalarBarLookupTable(
                                        ((PointCloudHandlerXYZ) pointCloud.getColorHandler()).getLutY());
                            colorMapRel = ColorMappingRelation.yMap;
                        }
                        getCanvas().lock();
                        getCanvas().Render();
                        getCanvas().unlock();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case KeyEvent.VK_9: // color map Z axis related
                try {
                    if (!(colorMapRel == ColorMappingRelation.zMap)) {
                        setOfClouds = linkedHashMapCloud.keySet();
                        for (String sKey : setOfClouds) {
                            pointCloud = linkedHashMapCloud.get(sKey);
                            pointCloud.getPolyData().GetPointData()
                            .SetScalars(((PointCloudHandlerXYZ) pointCloud.getColorHandler()).getColorsZ());
                            if (interactorStyle.lutEnabled)
                                interactorStyle.getScalarBar().setUpScalarBarLookupTable(
                                        ((PointCloudHandlerXYZ) pointCloud.getColorHandler()).getLutZ());
                            colorMapRel = ColorMappingRelation.zMap;
                        }
                        getCanvas().lock();
                        getCanvas().Render();
                        getCanvas().unlock();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case KeyEvent.VK_R:
                try {
                    getCanvas().lock();
                    // renderer.GetActiveCamera().SetPosition(0.0 ,0.0 ,100);
                    getRenderer().GetActiveCamera().SetViewUp(0.0, 0.0, -1.0);
                    getRenderer().ResetCamera();
                    getCanvas().Render();
                    getCanvas().unlock();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case KeyEvent.VK_F:
                getCanvas().lock();

                vtkAssemblyPath path = null;
                interactorStyle.FindPokedRenderer(getInteractor().GetEventPosition()[0], getInteractor()
                        .GetEventPosition()[1]);
                getInteractor().GetPicker().Pick(getInteractor().GetEventPosition()[0],
                        getInteractor().GetEventPosition()[1], 0.0, getRenderer());

                vtkAbstractPropPicker picker;
                if ((picker = (vtkAbstractPropPicker) getInteractor().GetPicker()) != null) {
                    path = picker.GetPath();
                }
                if (path != null) {
                    getInteractor().FlyTo(getRenderer(), picker.GetPickPosition()[0], picker.GetPickPosition()[1],
                            picker.GetPickPosition()[2]);
                }
                getCanvas().unlock();
                break;
            default:
                break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.plugins.vtk.visualization.AKeyboardEvent#setInteractorStyle(pt.lsts.neptus.plugins.vtk.visualization
     * .AInteractorStyleTrackballCamera)
     */
    @Override
    protected void setInteractorStyle(AInteractorStyleTrackballCamera interactorStyle) {
        this.interactorStyle = (InteractorStyleVis3D) interactorStyle;
    }
}
