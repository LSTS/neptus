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
 * May 20, 2013
 */
package pt.lsts.neptus.vtk.visualization;

import pt.lsts.neptus.i18n.I18n;
import vtk.vtkScalarBarActor;
import vtk.vtkScalarsToColors;
import vtk.vtkTextProperty;

/**
 * @author hfq
 * 
 */
public class ScalarBar {

    private vtkScalarBarActor scalarBarActor;

    private String scalarBarTitle;

    /**
     * 
     */
    public ScalarBar() {
        this(I18n.text("Color Map"));
    }

    /**
     * 
     * @param title
     */
    public ScalarBar(String title) {
        setScalarBarActor(new vtkScalarBarActor());
        setScalarBarTitle(title);
    }

    /**
     * 
     */
    public void setScalarBarHorizontalProperties() {
        getScalarBarActor().SetOrientationToHorizontal();
        getScalarBarActor().SetPosition(0.15, 0.01);
        getScalarBarActor().SetWidth(0.7);
        getScalarBarActor().SetHeight(0.1);
        // getScalarBarActor().SetNumberOfLabels(getScalarBarActor().GetNumberOfLabels() * 2);
        getScalarBarActor().SetNumberOfLabels(9);
        getScalarBarActor().UseOpacityOn();
        getScalarBarActor().SetTitle(scalarBarTitle);

        vtkTextProperty textProp = new vtkTextProperty();
        textProp = getScalarBarActor().GetLabelTextProperty();
        textProp.SetFontFamilyToArial();
        textProp.BoldOn();
        textProp.ItalicOn();
        textProp.SetOpacity(0.9);
        textProp.SetFontSize(8);
        textProp.ShadowOn();

        getScalarBarActor().SetLabelTextProperty(textProp);
        getScalarBarActor().SetTitleTextProperty(textProp);
    }

    /**
     * 
     */
    public void setScalarBarVerticalProperties() {
        getScalarBarActor().SetOrientationToVertical();
        getScalarBarActor().SetPosition(0.9, 0.1);
        getScalarBarActor().SetWidth(0.1);
        getScalarBarActor().SetHeight(0.8);
        getScalarBarActor().SetNumberOfLabels(9);
        getScalarBarActor().UseOpacityOn();
        getScalarBarActor().SetTitle(scalarBarTitle);

        vtkTextProperty textProp = new vtkTextProperty();
        textProp = getScalarBarActor().GetLabelTextProperty();
        textProp.SetFontFamilyToArial();
        textProp.BoldOn();
        textProp.ItalicOn();
        textProp.SetOpacity(0.9);
        textProp.SetFontSize(8);
        textProp.ShadowOn();

        getScalarBarActor().SetLabelTextProperty(textProp);
        getScalarBarActor().SetTitleTextProperty(textProp);
    }

    public void setUpScalarBarLookupTable(vtkScalarsToColors lut) {
        getScalarBarActor().SetLookupTable(lut);
        getScalarBarActor().Modified();
    }

    /**
     * @return the scalarBarActor
     */
    public vtkScalarBarActor getScalarBarActor() {
        return scalarBarActor;
    }

    /**
     * @param scalarBarActor the scalarBarActor to set
     */
    private void setScalarBarActor(vtkScalarBarActor scalarBarActor) {
        this.scalarBarActor = scalarBarActor;
    }

    /**
     * @return the scalarBarTitle
     */
    public String getScalarBarTitle() {
        return scalarBarTitle;
    }

    /**
     * @param scalarBarTitle the scalarBarTitle to set
     */
    public void setScalarBarTitle(String scalarBarTitle) {
        this.scalarBarTitle = scalarBarTitle;
    }

}
