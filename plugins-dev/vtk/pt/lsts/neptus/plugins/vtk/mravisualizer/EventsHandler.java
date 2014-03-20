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

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.vtk.events.AEventsHandler;
import pt.lsts.neptus.plugins.vtk.pointcloud.PointCloud;
import vtk.vtkActorCollection;


/**
 * @author hfq
 *
 */
public class EventsHandler extends AEventsHandler {

    private enum ColorMappingRelation {
        XMAP, YMAP, ZMAP, IMAP;
    }
    public ColorMappingRelation colorMapRel;

    public enum SensorTypeInteraction {
        NONE, DVL, MULTIBEAM, ALL;
    }

    private SensorTypeInteraction sensorTypeInteraction = SensorTypeInteraction.NONE;

    public EventsHandler(InteractorStyleVis3D neptusInteractorStyle) {
        super(neptusInteractorStyle);

        init();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.events.AEventsHandler#init()
     */
    @Override
    protected void init() {
        colorMapRel = ColorMappingRelation.ZMAP;
        setHelpMsg();
    }

    //    public void displayLookUpTable() {
    //        if(!getInteractorStyle().lutEnabled) {
    //            //PointCloud<?> pointCloud  = searchForPointCloudOnRenderer();
    //            switch(sensorTypeInteraction) {
    //                case NONE:
    //                    break;
    //                case DVL:
    //                    break;
    //                case MULTIBEAM:
    //                    break;
    //                case ALL:
    //            }
    //        }
    //    }

    protected PointCloud<?> searchForPointCloudOnRenderer() {
        vtkActorCollection actorCollection = new vtkActorCollection();
        actorCollection = getRenderer().GetActors();
        actorCollection.InitTraversal();
        PointCloud<?> pointCloud = null;

        for(int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
            if (actorCollection.GetNextActor().IsA("vtkActor2D") > 0)
                continue;
            // vtkLODActor tempActor = new vtkLODActor();
            // tempActor = (vtkLODActor) actorCollection.GetNextActor();

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

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.events.AEventsHandler#setHelpMsg()
     */
    @Override
    protected void setHelpMsg() {
        // <h1>3D Multibeam Interaction</h1>
        msgHelp = "<html><font size='2'><br><div align='center'><table border='1' align='center'>"
                + "<tr><th>Keys</th><th>" + I18n.text("Description") + "</th></tr>"
                + "<tr><td>p, P</td><td>" + I18n.text("Switch to a point-based representation") + "</td>"
                + "<tr><td>w, W </td><td>" + I18n.text("Switch to a wireframe-based representation, when available") + "</td>"
                + "<tr><td>s, S</td><td>" + I18n.text("Switch to a surface-based representation, when available") + "</td>"
                + "<tr><td>j, J</td><td>" + I18n.text("Take a .PNG snapshot of the current window view") + "</td>"
                + "<tr><td>g, G</td><td>" + I18n.text("Display scale grid (on/off)") + "</td>"
                + "<tr><td>u, U</td><td>" + I18n.text("Display lookup table (on/off)") + "</td>"
                + "<tr><td>r, R</td><td>" + I18n.text("Reset camera view along the current view direction") + "</td>"
                + // (to viewpoint = {0, 0, 0} -> center {x, y, z}\n");
                "<tr><td>i, I</td><td>" + I18n.text("Information about rendered cloud") + "</td>"
                + "<tr><td>f, F</td><td>" + I18n.text("Fly Mode - point with mouse cursor the direction and press 'f' to fly") + "</td>"
                + "<tr><td>+/-</td><td>" + I18n.text("Increment / Decrement overall point size") + "</td>"
                + "<tr><td>3</td><td>" + I18n.text("Toggle into an out of stereo mode") + "</td>"
                + "<tr><td>7</td><td>" + I18n.text("Color gradient in relation with X coords (north)") + "</td>"
                + "<tr><td>8</td><td>" + I18n.text("Color gradient in relation with Y coords (west)") + "</td>"
                + "<tr><td>9</td><td>" + I18n.text("Color gradient in relation with Z coords (depth)") + "</td>"
                + "<tr><th>Mouse</th><th>" + I18n.text("Description") + "</th></tr>"
                +
                // rotate the camera around its focal point. The rotation is in the direction defined from the center of
                // the renderer's viewport towards the mouse position
                "<tr><td>" + I18n.text("Left mouse button</td><td>") + I18n.text("Rotate camera around its focal point") + "</td>"
                + "<tr><td>" + I18n.text("Middle mouse button") + "</td><td>" + I18n.text("Pan camera") + "</td>"
                + "<tr><td>" + I18n.text("Right mouse button") + "</td><td>" + I18n.text("Zoom (In/Out) the camera") + "</td>"
                + "<tr><td>" + I18n.text("Mouse wheel") + "</td><td>" + I18n.text("Zoom (In/Out) the camera - Static focal point") + "</td>";
    }
}
