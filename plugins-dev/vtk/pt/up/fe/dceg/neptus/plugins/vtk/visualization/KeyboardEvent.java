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
import java.util.Set;

import org.apache.batik.dom.util.HashTable;

import com.lowagie.text.pdf.hyphenation.TernaryTree.Iterator;

import vtk.vtkLODActor;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;

/**
 * @author hfq
 *
 */
public class KeyboardEvent {
    
    NeptusInteractorStyle neptusInteractorStyle;
    
    static vtkRenderer renderer;
    static vtkRenderWindowInteractor interactor;
    private Hashtable<String, vtkLODActor> hashCloud = new Hashtable<>();
    
    private Set<String> setOfClouds;
    //enum keyEvent {
    //    j, u, plus, minus
    //}
    
    //public keyEvent ke;
    
    //public KeyboardEvent() {
    //    super(renderer, interactor);
    //    System.out.println("veio ao Keyboard Event");
        //this.neptusInteractorStyle = neptusInteractorStyle;
    //}
    
    /**
     * @param neptusInteractorStyle2
     */
    public KeyboardEvent(NeptusInteractorStyle neptusInteractorStyle2, Hashtable<String, vtkLODActor> hashCloud) {
        this.neptusInteractorStyle = neptusInteractorStyle2;
        this.interactor = neptusInteractorStyle2.getInteractor();
        this.hashCloud = hashCloud;
    }

    public void handleEvents(char keyCode) {
        
        switch (keyCode) {
            case 'j':
                takeSnapShot();
                break;
            case 'u':
                System.out.println("enter u");
                Set<String> set = hashCloud.keySet();
                for (String skey : set) {
                    //System.out.println("String from set: " + skey);
                    vtkLODActor tempActor = new vtkLODActor();
                    tempActor = hashCloud.get(skey);
                    tempActor.GetProperty().SetColor(0.0, 1.0, 1.0);
                    tempActor.Modified();
                }
                break;
                
            case '+':
                setOfClouds = hashCloud.keySet();
                for (String sKey : setOfClouds) {
                    vtkLODActor tempActor = new vtkLODActor();
                    tempActor = hashCloud.get(sKey);
                    double pointSize = tempActor.GetProperty().GetPointSize();
                    if (pointSize <= 9) {
                        tempActor.GetProperty().SetPointSize(pointSize + 1);
                        neptusInteractorStyle.interactor.Render();
                    }
                }
                break;
            case '-':
                setOfClouds = hashCloud.keySet();
                for (String sKey : setOfClouds) {
                    vtkLODActor tempActor = new vtkLODActor();
                    tempActor = hashCloud.get(sKey);
                    double pointSize = tempActor.GetProperty().GetPointSize();
                    if (pointSize > 1) {
                        tempActor.GetProperty().SetPointSize(pointSize - 1);
                        neptusInteractorStyle.interactor.Render();
                    }
                    tempActor.GetPropertyKeys();
                }
                break;
            case '1':
                //int numberOfProps = neptusInteractorStyle.renderer.GetNumberOfPropsRendered();
                //System.out.println("numberOfProps: " + numberOfProps);
                
                setOfClouds = hashCloud.keySet();
                for (String sKey : setOfClouds) {
                    //System.out.println("String from set: " + setOfClouds);
                    vtkLODActor tempActor = new vtkLODActor();
                    tempActor = hashCloud.get(sKey);
                    tempActor.GetProperty().SetColor(PointCloudHandlers.getRandomColor());
                    neptusInteractorStyle.interactor.Render();
                }
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
        neptusInteractorStyle.FindPokedRenderer(interactor.GetEventPosition()[0], interactor.GetEventPosition()[1]);
        neptusInteractorStyle.wif.SetInput(interactor.GetRenderWindow());
        neptusInteractorStyle.wif.Modified();           
        neptusInteractorStyle.snapshotWriter.Modified();
               
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssmm").format(Calendar.getInstance().getTimeInMillis());
        timeStamp = "snapshot_" + timeStamp;
        System.out.println("timeStamp: " + timeStamp);
        
        neptusInteractorStyle.snapshotWriter.SetFileName(timeStamp);
        neptusInteractorStyle.snapshotWriter.Write();
        //if (getInteractor().GetKeyCode() == 'j' | getInteractor().GetKeyCode() == 'J') {
            //System.out.println("save screenshot");
            //int pos1 = getInteractor().GetEventPosition()[0];
            //int pos2 = getInteractor().GetEventPosition()[1];
            //System.out.println("Event Position - pos1: " + pos1 + " pos2: " + pos2);
            
        //}
    }
}
