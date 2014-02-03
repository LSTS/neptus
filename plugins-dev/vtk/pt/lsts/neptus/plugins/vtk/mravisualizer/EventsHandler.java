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
import pt.lsts.neptus.plugins.vtk.utils.Utils;
import pt.lsts.neptus.plugins.vtk.visualization.Canvas;
import vtk.vtkRenderWindowInteractor;


/**
 * @author hfq
 *
 */
public class EventsHandler {
    private NeptusInteractorStyle neptusInteractorStyle;
    private vtkRenderWindowInteractor interactor;
    private Canvas canvas;

    public EventsHandler(NeptusInteractorStyle neptusInteractorStyle) {
        this.neptusInteractorStyle = neptusInteractorStyle;
        this.canvas = neptusInteractorStyle.getCanvas();
        this.interactor = neptusInteractorStyle.getCanvas().getRenderWindowInteractor();
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
                    neptusInteractorStyle.wif.SetInput(interactor.GetRenderWindow());
                    neptusInteractorStyle.wif.Modified();
                    neptusInteractorStyle.snapshotWriter.Modified();

                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssmm").format(Calendar.getInstance()
                            .getTimeInMillis());
                    timeStamp = "snapshot_" + timeStamp;
                    NeptusLog.pub().info("timeStamp: " + timeStamp);

                    neptusInteractorStyle.snapshotWriter.SetFileName(timeStamp);

                    if (!canvas.isWindowSet()) {
                        canvas.lock();
                        canvas.Render();
                        canvas.unlock();
                    }

                    canvas.lock();
                    neptusInteractorStyle.wif.Update();
                    canvas.unlock();

                    neptusInteractorStyle.snapshotWriter.Write();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
