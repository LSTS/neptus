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
 * Jan 29, 2014
 */
package pt.lsts.neptus.plugins.vtk.mravisualizer;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.lsts.neptus.plugins.vtk.utils.Utils;
import pt.lsts.neptus.plugins.vtk.visualization.Canvas;
import vtk.vtkActorCollection;
import vtk.vtkLODActor;
import vtk.vtkPNGWriter;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;
import vtk.vtkWindowToImageFilter;


/**
 * @author hfq
 *
 */
public class EventsHandler {
    private NeptusInteractorStyle neptusInteractorStyle;
    private vtkRenderer renderer;
    private vtkRenderWindowInteractor interactor;
    private Canvas canvas;

    private enum ColorMappingRelation {
        XMAP, YMAP, ZMAP, IMAP;
    }
    public ColorMappingRelation colorMapRel;

    public enum SensorTypeInteraction {
        NONE, DVL, MULTIBEAM, ALL;
    }

    private SensorTypeInteraction sensorTypeInteraction = SensorTypeInteraction.NONE;

    // A PNG Writer for screenshot captures
    protected vtkPNGWriter snapshotWriter = new vtkPNGWriter();
    // Internal Window to image Filter. Needed by a snapshotWriter object
    protected vtkWindowToImageFilter wif = new vtkWindowToImageFilter();

    public EventsHandler(NeptusInteractorStyle neptusInteractorStyle) {
        this.neptusInteractorStyle = neptusInteractorStyle;
        this.canvas = neptusInteractorStyle.getCanvas();
        this.renderer = neptusInteractorStyle.getCanvas().GetRenderer();
        this.interactor = neptusInteractorStyle.getCanvas().getRenderWindowInteractor();

        init();
    }

    /**
     * 
     */
    private void init() {
        colorMapRel = ColorMappingRelation.ZMAP; // on creation map color map is z related

        // Create the image filter and PNG writer objects
        wif = new vtkWindowToImageFilter();
        snapshotWriter = new vtkPNGWriter();
        snapshotWriter.SetInputConnection(wif.GetOutputPort());
    }

    /**
     * Syncronously take a snapshot of a 3D view Saves on neptus directory
     */
    public void takeSnapShot() {
        Utils.goToAWTThread(new Runnable() {

            @Override
            public void run() {
                try {
                    neptusInteractorStyle.FindPokedRenderer(interactor.GetEventPosition()[0],
                            interactor.GetEventPosition()[1]);
                    wif.SetInput(interactor.GetRenderWindow());
                    wif.Modified();
                    snapshotWriter.Modified();

                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssmm").format(Calendar.getInstance()
                            .getTimeInMillis());
                    timeStamp = "snapshot_" + timeStamp;
                    NeptusLog.pub().info("timeStamp: " + timeStamp);

                    snapshotWriter.SetFileName(timeStamp);

                    if (!canvas.isWindowSet()) {
                        canvas.lock();
                        canvas.Render();
                        canvas.unlock();
                    }

                    canvas.lock();
                    wif.Update();
                    canvas.unlock();

                    snapshotWriter.Write();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void displayLookUpTable() {
        if(!neptusInteractorStyle.lutEnabled) {
            //PointCloud<?> pointCloud  = searchForPointCloudOnRenderer();
            switch(sensorTypeInteraction) {
                case NONE:
                    break;
                case DVL:
                    break;
                case MULTIBEAM:
                    break;
                case ALL:
            }
        }
    }

    private PointCloud<?> searchForPointCloudOnRenderer() {
        vtkActorCollection actorCollection = new vtkActorCollection();
        actorCollection = renderer.GetActors();
        actorCollection.InitTraversal();
        PointCloud<?> pointCloud = null;

        for(int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
            if (actorCollection.GetNextActor().IsA("vtkActor2D") > 0)
                continue;
            vtkLODActor tempActor = new vtkLODActor();
            tempActor = (vtkLODActor) actorCollection.GetNextActor();
            //            setOfClouds = linkedHashMapCloud.keySet();
            //            for (String sKey : setOfClouds) {
            //                pointCloud = linkedHashMapCloud.get(sKey);
            //                if (tempActor.equals(pointCloud.getCloudLODActor())) {
            //                    double pointSize = tempActor.GetProperty().GetPointSize();
            //                    if (pointSize <= 9.0) {
            //                        canvas.lock();
            //                        tempActor.GetProperty().SetPointSize(pointSize + 1);
            //                        canvas.Render();
            //                        canvas.unlock();
            //                    }
            //                }
            //            }

        }

        return pointCloud;
    }

    /**
     * @return the sensorTypeInteraction
     */
    public SensorTypeInteraction getSensorTypeInteraction() {
        return sensorTypeInteraction;
    }

    /**
     * @param sensorTypeInteraction the sensorTypeInteraction to set
     */
    public void setSensorTypeInteraction(SensorTypeInteraction sensorTypeInteraction) {
        this.sensorTypeInteraction = sensorTypeInteraction;
    }
}
