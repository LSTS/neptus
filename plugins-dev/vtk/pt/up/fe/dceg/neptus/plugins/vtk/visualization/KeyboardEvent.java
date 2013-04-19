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
    public KeyboardEvent(NeptusInteractorStyle neptusInteractorStyle2) {
        this.neptusInteractorStyle = neptusInteractorStyle2;
        this.interactor = neptusInteractorStyle2.getInteractor();
    }

    public void handleEvents(char keyCode) {
        
        switch (keyCode) {
            case 'j':
                takeSnapShot();
                break;
            case 'u':
                break;
            case '1':
                int numberOfProps = neptusInteractorStyle.renderer.GetNumberOfPropsRendered();
                System.out.println("numberOfProps: " + numberOfProps);
                //int hashcode = neptusInteractorStyle.hashCloud.hashCode();
                //System.out.println("HashCode: " + hashcode);
                //String elementes = neptusInteractorStyle.hashCloud.elements().toString();
                //System.out.println("hashtable elements: " + elementes);
                break;
            default:
                System.out.println("not a keyEvent");
                break;
            
        }

        
        
        if (keyCode == 'j') {
            takeSnapShot();
        }
        //switch 
        
        //case 'j':
        //    saveScreenshot();
        //    break;       
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
