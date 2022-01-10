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
 * Mar 17, 2014
 */
package pt.lsts.neptus.vtk.events;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.vtk.mravisualizer.VtkOptions;
import pt.lsts.neptus.vtk.utils.Utils;
import pt.lsts.neptus.vtk.visualization.AInteractorStyleTrackballCamera;
import pt.lsts.neptus.vtk.visualization.Canvas;
import vtk.vtkPNGWriter;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;
import vtk.vtkTextActor;
import vtk.vtkWindowToImageFilter;

/**
 * @author hfq
 * 
 */
public abstract class AEventsHandler {

    private Canvas canvas;
    private AInteractorStyleTrackballCamera interactorStyle;
    private vtkRenderer renderer;
    private vtkRenderWindowInteractor interactor;

    // A PNG Writer for screenshot captures
    protected vtkPNGWriter snapshotWriter;
    // Internal Window to image Filter. Needed by a snapshotWriter object
    protected vtkWindowToImageFilter wif;

    private vtkTextActor textProcessingActor;
    private static String TEXT_PROCESS_ACTOR = I18n.text("Processing Data");

    private vtkTextActor textZExagInfoActor;
    private static String TEXT_ZEXAG_INFO_ACTOR = I18n.textf("Depth multiplied by: %currenZexag",
            VtkOptions.zExaggeration);

    protected String msgHelp = "";

    private IMraLogGroup source;

    private static final String SNAPSHOT_FILE_EXT = ".png";

    /**
     * @param canvas
     * @param renderer
     * @param interactor
     * @param interactorStyle
     */
    public AEventsHandler(Canvas canvas, vtkRenderer renderer, vtkRenderWindowInteractor interactor,
            AInteractorStyleTrackballCamera interactorStyle, IMraLogGroup source) {
        this.canvas = canvas;
        this.renderer = renderer;
        this.interactor = interactor;
        this.interactorStyle = interactorStyle;
        this.source = source;

        setTextProcessingActor(new vtkTextActor());
        setTextZExagInfoActor(new vtkTextActor());

        // Create the image filter and PNG writer objects
        wif = new vtkWindowToImageFilter();
        snapshotWriter = new vtkPNGWriter();
        snapshotWriter.SetInputConnection(wif.GetOutputPort());

        buildTextZExagInfoActor();
        buildTextProcessingActor();
    }

    /**
     * @param interactorStyle
     * @param source
     */
    public AEventsHandler(AInteractorStyleTrackballCamera interactorStyle, IMraLogGroup source) {
        this(interactorStyle.getCanvas(), interactorStyle.getCanvas().GetRenderer(), interactorStyle.getCanvas()
                .getRenderWindowInteractor(), interactorStyle, source);
    }

    /**
     * Initial params configurations
     */
    protected abstract void init();

    /**
     * Syncronously take a snapshot of a 3D view Saves on neptus directory
     */
    public void takeSnapShot(final String prefixSnapshotName) {
        Utils.goToAWTThread(new Runnable() {

            @Override
            public void run() {
                try {
                    interactorStyle.FindPokedRenderer(interactor.GetEventPosition()[0],
                            interactor.GetEventPosition()[1]);
                    wif.SetInput(interactor.GetRenderWindow());
                    wif.Modified();
                    snapshotWriter.Modified();

                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssmm").format(Calendar.getInstance()
                            .getTimeInMillis());
                    timeStamp = "snapshot_" + timeStamp;
                    NeptusLog.pub().info("Snapshot timeStamp: " + timeStamp);

                    if (source != null)
                        snapshotWriter.SetFileName(source.getDir().getAbsolutePath() + "/" + prefixSnapshotName + timeStamp + SNAPSHOT_FILE_EXT);
                    else {
                        NeptusLog.pub().info("Source is not defined, image will be saved on neptus root file.");
                        snapshotWriter.SetFileName(timeStamp + prefixSnapshotName + SNAPSHOT_FILE_EXT);
                    }

                    if (!canvas.isWindowSet()) {
                        canvas.lock();
                        canvas.Render();
                        canvas.unlock();
                    }

                    canvas.lock();
                    wif.Update();
                    canvas.unlock();

                    NeptusLog.pub().info("Snapshot saved: " + snapshotWriter.GetFileName());
                    snapshotWriter.Write();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void resetViewport() {
        try {
            canvas.lock();
            renderer.ResetCamera();
            renderer.GetActiveCamera().SetViewUp(0.0, 0.0, -1.0);
            canvas.Render();
            canvas.unlock();
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Set Msg to be added on help User interactiors keyboard shortcuts Use msgHelp String var to assign text
     */
    protected abstract void setHelpMsg();

    /**
     * @return msgHelp
     */
    public String getMsgHelp() {
        return msgHelp;
    }

    /**
     * Build vtk 2D text Zexag Info Actor
     */
    private void buildTextZExagInfoActor() {
        textZExagInfoActor.GetTextProperty().BoldOn();
        textZExagInfoActor.GetTextProperty().ItalicOn();
        textZExagInfoActor.GetTextProperty().SetColor(1.0, 1.0, 1.0);
        textZExagInfoActor.GetTextProperty().SetFontFamilyToArial();
        textZExagInfoActor.GetTextProperty().SetFontSize(12);
        textZExagInfoActor.SetInput(TEXT_ZEXAG_INFO_ACTOR);
        textZExagInfoActor.VisibilityOn();
    }

    /**
     * Build vtk 2D text procesing actor
     */
    private void buildTextProcessingActor() {
        textProcessingActor.GetTextProperty().BoldOn();
        textProcessingActor.GetTextProperty().ItalicOn();
        textProcessingActor.GetTextProperty().SetFontSize(40);
        textProcessingActor.GetTextProperty().SetColor(1.0, 1.0, 1.0);
        textProcessingActor.GetTextProperty().SetFontFamilyToArial();
        textProcessingActor.SetInput(TEXT_PROCESS_ACTOR);
        textProcessingActor.VisibilityOn();
    }

    /**
     * @return the canvas
     */
    protected Canvas getCanvas() {
        return canvas;
    }

    /**
     * @param canvas the canvas to set
     */
    protected void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    /**
     * @return the interactorStyle
     */
    protected AInteractorStyleTrackballCamera getInteractorStyle() {
        return interactorStyle;
    }

    /**
     * @param interactorStyle the interactorStyle to set
     */
    protected void setInteractorStyle(AInteractorStyleTrackballCamera interactorStyle) {
        this.interactorStyle = interactorStyle;
    }

    /**
     * @return the renderer
     */
    protected vtkRenderer getRenderer() {
        return renderer;
    }

    /**
     * @param renderer the renderer to set
     */
    protected void setRenderer(vtkRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * @return the interactor
     */
    protected vtkRenderWindowInteractor getInteractor() {
        return interactor;
    }

    /**
     * @param interactor the interactor to set
     */
    protected void setInteractor(vtkRenderWindowInteractor interactor) {
        this.interactor = interactor;
    }

    /**
     * @return the textProcessingActor
     */
    public vtkTextActor getTextProcessingActor() {
        return textProcessingActor;
    }

    /**
     * @param textProcessingActor the textProcessingActor to set
     */
    protected void setTextProcessingActor(vtkTextActor textProcessingActor) {
        this.textProcessingActor = textProcessingActor;
    }

    /**
     * @return the textZExagInfoActor
     */
    public vtkTextActor getTextZExagInfoActor() {
        return textZExagInfoActor;
    }

    /**
     * @param textZExagInfoActor the textZExagInfoActor to set
     */
    protected void setTextZExagInfoActor(vtkTextActor textZExagInfoActor) {
        this.textZExagInfoActor = textZExagInfoActor;
    }

    /**
     * @return the source
     */
    protected IMraLogGroup getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    protected void setSource(IMraLogGroup source) {
        this.source = source;
    }
}
