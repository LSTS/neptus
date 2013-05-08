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
 * Apr 18, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.visualization;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.batik.dom.util.HashTable;

import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;

import com.jme3.renderer.Camera;
import com.jogamp.newt.event.KeyEvent;
import com.lowagie.text.pdf.hyphenation.TernaryTree.Iterator;

import vtk.vtkAbstractPropPicker;
import vtk.vtkActorCollection;
import vtk.vtkAssemblyPath;
import vtk.vtkCollectionIterator;
import vtk.vtkLODActor;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;
import vtk.vtkScalarsToColors;

/**
 * @author hfq
 *  FIXME add keys to change mode (trackball, joystick..)
 */
public class KeyboardEvent {   
    private NeptusInteractorStyle neptusInteractorStyle;
    
    private static vtkRenderer renderer;
    private static vtkRenderWindowInteractor interactor;
    //private Hashtable<String, vtkLODActor> hashCloud = new Hashtable<>();
    //private LinkedHashMap<String, vtkLODActor> linkedHashMapCloud = new LinkedHashMap<>();
    private LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud = new LinkedHashMap<>();
    
    private Set<String> setOfClouds;
    
    private PointCloud<PointXYZ> pointCloud;
      
    private vtkLODActor marker = new vtkLODActor();
    private boolean markerEnabled = false;
    
    private enum colorMappingRelation {
        xMap, yMap, zMap
    }  
    public colorMappingRelation colorMapRel;
    
    private Caption captionInfo;
    private Boolean captionEnabled = false;
    
    private static final boolean VTKIS_ANIMEOFF = false;
    private static final boolean VTKIS_ANIMEON = true;
    
    private boolean AnimeState;
        
    /**
     * @param neptusInteractorStyle
     * @param linkedHashMapCloud
     */
    public KeyboardEvent(NeptusInteractorStyle neptusInteractorStyle, LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud) {
        this.neptusInteractorStyle = neptusInteractorStyle;
        this.interactor = neptusInteractorStyle.getInteractor();
        this.linkedHashMapCloud = linkedHashMapCloud;
    }

    public void handleEvents(int keyCode) {
        
        switch (keyCode) {
            case KeyEvent.VK_J:     //case 'j':
                takeSnapShot();
                break;
            case KeyEvent.VK_U: //case 'u':
                try {
                    if(!neptusInteractorStyle.lutEnabled) {
                        vtkActorCollection actorCollection = new vtkActorCollection();
                        actorCollection = neptusInteractorStyle.renderer.GetActors();
                        actorCollection.InitTraversal();
                        
                        for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                            vtkLODActor tempActor = new vtkLODActor();
                            tempActor = (vtkLODActor) actorCollection.GetNextActor();
                            setOfClouds = linkedHashMapCloud.keySet();
                            for (String skey : setOfClouds) {
                                pointCloud = linkedHashMapCloud.get(skey);
                                vtkScalarsToColors lut = pointCloud.getCloudLODActor().GetMapper().GetLookupTable();
                                neptusInteractorStyle.lutActor.SetLookupTable(lut);
                                neptusInteractorStyle.lutActor.SetUseBounds(true);
                                neptusInteractorStyle.lutActor.SetNumberOfLabels(9);
                                neptusInteractorStyle.lutActor.Modified();
                            }
                        }
                        neptusInteractorStyle.renderer.AddActor(neptusInteractorStyle.lutActor);
                        neptusInteractorStyle.lutEnabled = true;
                    }
                    else {
                        neptusInteractorStyle.renderer.RemoveActor(neptusInteractorStyle.lutActor);
                        neptusInteractorStyle.lutEnabled = false;
                    }
                    neptusInteractorStyle.interactor.Render();
                }
                catch (Exception e6) {
                    e6.printStackTrace();
                }
                break;             
            case KeyEvent.VK_G: // case 'g':
                try {
                    if (!neptusInteractorStyle.gridEnabled) {
                        neptusInteractorStyle.gridActor.TopAxisVisibilityOn();
                        neptusInteractorStyle.renderer.AddViewProp(neptusInteractorStyle.gridActor);
                        neptusInteractorStyle.gridEnabled = true;
                    }
                    else {
                        neptusInteractorStyle.renderer.RemoveViewProp(neptusInteractorStyle.gridActor);
                        neptusInteractorStyle.gridEnabled = false;
                    }
                    neptusInteractorStyle.interactor.Render();
                }
                catch (Exception e5) {
                    e5.printStackTrace();
                }
                break;
            case KeyEvent.VK_C: // case 'c':     // FIXME - not good enough, better check this one for a better implementation. problems: seems to be disconected of the rendered actor
                try {
                    if (!neptusInteractorStyle.compassEnabled) {
                        neptusInteractorStyle.compass.addCompassToVisualization(interactor);
                        neptusInteractorStyle.compassEnabled = true;
                    }
                    else {
                        neptusInteractorStyle.compass.removeCompassFromVisualization(interactor);
                        neptusInteractorStyle.compassEnabled = false;
                    }
                }
                catch (Exception e4) {
                    e4.printStackTrace();
                }
                break;
            case KeyEvent.VK_W: //case 'w':
                try {
                    if (!neptusInteractorStyle.wireframeRepEnabled) {
                        neptusInteractorStyle.wireframeRepEnabled = true;
                        neptusInteractorStyle.solidRepEnabled = false;
                        neptusInteractorStyle.pointRepEnabled = false;
                        
                        System.out.println("wireframe rep enabled");
                        
                        setOfClouds = linkedHashMapCloud.keySet();
                        for (String sKey : setOfClouds) {
                            vtkLODActor tempActor = new vtkLODActor();
                            pointCloud = linkedHashMapCloud.get(sKey);
                            tempActor = pointCloud.getCloudLODActor();
                            tempActor.GetProperty().SetRepresentationToWireframe();
                        }
                    }
                }
                catch (Exception e3) {
                    e3.printStackTrace();
                }
                break;
            case KeyEvent.VK_S: //case 's':
                try {
                    if (!neptusInteractorStyle.solidRepEnabled) {
                        neptusInteractorStyle.solidRepEnabled = true;
                        neptusInteractorStyle.wireframeRepEnabled = false;
                        neptusInteractorStyle.pointRepEnabled = false;
                        
                        System.out.println("solid rep enabled");
                        
                        for (String sKey : setOfClouds) {
                            vtkLODActor tempActor = new vtkLODActor();
                            pointCloud = linkedHashMapCloud.get(sKey);
                            tempActor = pointCloud.getCloudLODActor();
                            tempActor.GetProperty().SetRepresentationToSurface();
                        }
                    }
                }
                catch (Exception e2) {
                    e2.printStackTrace();
                }
                break;
            case KeyEvent.VK_P: //case 'p':
                try {
                    if (!neptusInteractorStyle.pointRepEnabled) {
                        neptusInteractorStyle.pointRepEnabled = true;
                        neptusInteractorStyle.solidRepEnabled = false;
                        neptusInteractorStyle.wireframeRepEnabled = false;
                        
                        System.out.println("point rep enabled");
                        
                        for (String sKey : setOfClouds) {
                            vtkLODActor tempActor = new vtkLODActor();
                            pointCloud = linkedHashMapCloud.get(sKey);
                            tempActor = pointCloud.getCloudLODActor();
                            tempActor.GetProperty().SetRepresentationToPoints();
                        }
                    }
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
                break;
            case KeyEvent.VK_M: //case 'm':
                if(!markerEnabled) {
                    markerEnabled = true;
                    //neptusInteractorStyle.renderer.AddActor(marker);                 
                }
                else {
                    markerEnabled = false;
                }
                break;
            case KeyEvent.VK_I: //case 'i':
                if (!captionEnabled) {
                    try {
                        vtkActorCollection actorCollection = new vtkActorCollection();
                        actorCollection = neptusInteractorStyle.renderer.GetActors();
                        actorCollection.InitTraversal();
                        for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                            vtkLODActor tempActor = new vtkLODActor();
                            tempActor = (vtkLODActor) actorCollection.GetNextActor();
                            setOfClouds = linkedHashMapCloud.keySet();
                            for (String sKey : setOfClouds) {
                                vtkLODActor tempActorFromHashMap = new vtkLODActor();
                                pointCloud = linkedHashMapCloud.get(sKey);
                                tempActorFromHashMap = pointCloud.getCloudLODActor();
                                if (tempActor.equals(tempActorFromHashMap)) {
                                    captionInfo = new Caption(4, 150, pointCloud.getNumberOfPoints(), pointCloud.getCloudName(), 
                                            pointCloud.getBounds(), pointCloud.getMemorySize(), neptusInteractorStyle.renderer);
                                    neptusInteractorStyle.renderer.AddActor(captionInfo.getCaptionNumberOfPointsActor());
                                    neptusInteractorStyle.renderer.AddActor(captionInfo.getCaptionCloudNameActor());
                                    neptusInteractorStyle.renderer.AddActor(captionInfo.getCaptionMemorySizeActor());
                                    neptusInteractorStyle.renderer.AddActor(captionInfo.getCaptionCloudBoundsActor());
                                    neptusInteractorStyle.interactor.Render();
                                }
                            }
                        }
                        captionEnabled = true;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        neptusInteractorStyle.renderer.RemoveActor(captionInfo.getCaptionNumberOfPointsActor());
                        neptusInteractorStyle.renderer.RemoveActor(captionInfo.getCaptionCloudNameActor());
                        neptusInteractorStyle.renderer.RemoveActor(captionInfo.getCaptionMemorySizeActor());
                        neptusInteractorStyle.renderer.RemoveActor(captionInfo.getCaptionCloudBoundsActor());
                        captionEnabled = false;
                        neptusInteractorStyle.interactor.Render();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case KeyEvent.VK_PLUS: // case '+':   // increment size of rendered cell point
                try {
                    vtkActorCollection actorCollection = new vtkActorCollection();
                    actorCollection = neptusInteractorStyle.renderer.GetActors();
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
                                   tempActor.GetProperty().SetPointSize(pointSize + 1);
                                   neptusInteractorStyle.interactor.Render();
                               }
                            }
                        }
                    }                  
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case KeyEvent.VK_MINUS: //  case '-':   // decrement size of rendered cell point
                try {
                    vtkActorCollection actorCollection = new vtkActorCollection();
                    actorCollection = neptusInteractorStyle.renderer.GetActors();
                    actorCollection.InitTraversal();
                    
                    for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                        vtkLODActor tempActor = new vtkLODActor();
                        tempActor = (vtkLODActor) actorCollection.GetNextActor();
                        setOfClouds = linkedHashMapCloud.keySet();
                        for (String sKey : setOfClouds) {
                            if (tempActor.equals(pointCloud.getCloudLODActor())) {
                                double pointSize = tempActor.GetProperty().GetPointSize();
                                if (pointSize > 1.0) {
                                    tempActor.GetProperty().SetPointSize(pointSize - 1);
                                    neptusInteractorStyle.interactor.Render();
                                }
                            }
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
//            case '1':
//                //int numberOfProps = neptusInteractorStyle.renderer.GetNumberOfPropsRendered();
//                //System.out.println("numberOfProps: " + numberOfProps);           
//                setOfClouds = linkedHashMapCloud.keySet();
//                for (String sKey : setOfClouds) {
//                    //System.out.println("String from set: " + setOfClouds);
//                    vtkLODActor tempActor = new vtkLODActor();
//                    tempActor = linkedHashMapCloud.get(sKey);
//                    //tempActor.GetProperty().SetColor(PointCloudHandlers.getRandomColor());                
//                }
//                neptusInteractorStyle.interactor.Render();
//                break;
            case KeyEvent.VK_7: // case '7':
                try {
                    setOfClouds = linkedHashMapCloud.keySet();
                    for (String sKey : setOfClouds) {
                        vtkLODActor tempActor = new vtkLODActor();
                        pointCloud = linkedHashMapCloud.get(sKey);
                        tempActor = pointCloud.getCloudLODActor();
                    }
                    
                    neptusInteractorStyle.interactor.Render();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case KeyEvent.VK_8:     // case '8' -> not yet implemented    
                break;
            case KeyEvent.VK_9:     // case '9' -> not yet implemented
                break;
            case KeyEvent.VK_R: //case 'r':
                neptusInteractorStyle.renderer.ResetCamera();
                break;
            case KeyEvent.VK_H: //case KeyEvent.VK_F:
                AnimeState = VTKIS_ANIMEON;
                
                vtkAssemblyPath path = null;
                neptusInteractorStyle.FindPokedRenderer(neptusInteractorStyle.interactor.GetEventPosition()[0],
                        neptusInteractorStyle.interactor.GetEventPosition()[1]);
                neptusInteractorStyle.interactor.GetPicker().Pick(neptusInteractorStyle.interactor.GetEventPosition()[0],
                        neptusInteractorStyle.interactor.GetEventPosition()[1],
                        0.0, neptusInteractorStyle.renderer);
                
                vtkAbstractPropPicker picker;
                if ((picker=(vtkAbstractPropPicker)neptusInteractorStyle.interactor.GetPicker()) != null) {
                    path = picker.GetPath();
                }
                if (path != null) {
                    neptusInteractorStyle.interactor.FlyTo(neptusInteractorStyle.renderer, picker.GetPickPosition()[0], picker.GetPickPosition()[1], picker.GetPickPosition()[2]);
                }
                AnimeState = VTKIS_ANIMEOFF;
                
                //neptusInteractorStyle.renderer
                break;
            default:
                System.out.println("not a keyEvent");
                break; 
        }
    }
    
    /**
     * for now saves on neptus directory
     */
    void takeSnapShot() {
        try {
            neptusInteractorStyle.FindPokedRenderer(interactor.GetEventPosition()[0], interactor.GetEventPosition()[1]);
            neptusInteractorStyle.wif.SetInput(interactor.GetRenderWindow());
            neptusInteractorStyle.wif.Modified();           
            neptusInteractorStyle.snapshotWriter.Modified();
                   
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssmm").format(Calendar.getInstance().getTimeInMillis());
            timeStamp = "snapshot_" + timeStamp;
            System.out.println("timeStamp: " + timeStamp);
            
            neptusInteractorStyle.snapshotWriter.SetFileName(timeStamp);
            neptusInteractorStyle.snapshotWriter.Write();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
