/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * May 24, 2013
 */
package pt.lsts.neptus.vtk.visualization;

import pt.lsts.neptus.i18n.I18n;
import vtk.vtkLODActor;
import vtk.vtkLinearExtrusionFilter;
import vtk.vtkPolyDataMapper;
import vtk.vtkVectorText;

/**
 * @author hfq
 * 
 */
public class Text3D {
    private vtkLODActor text3dActor;

    /**
     * 
     */
    public Text3D() {
        setText3dActor(new vtkLODActor());
    }

    /**
     * 
     * @param msgInput
     * @param posX
     * @param posY
     * @param posZ
     * @param scale
     */
    public void buildText3D(String msgInput, double posX, double posY, double posZ, double scale) {
        String msg = I18n.text(msgInput);

        vtkVectorText vectText = new vtkVectorText();
        vectText.SetText(I18n.text(msg));

        vtkLinearExtrusionFilter extrude = new vtkLinearExtrusionFilter();
        extrude.SetInputConnection(vectText.GetOutputPort());
        extrude.SetExtrusionTypeToNormalExtrusion();
        extrude.SetVector(0.0, 0.0, 1.0);
        extrude.SetScaleFactor(0.5);

        vtkPolyDataMapper txtMapper = new vtkPolyDataMapper();
        txtMapper.SetInputConnection(extrude.GetOutputPort());

        text3dActor.SetMapper(txtMapper);
        text3dActor.SetPosition(posX, posY, posZ);
        text3dActor.SetScale(scale);
    }

    /**
     * @return the text3dActor
     */
    public vtkLODActor getText3dActor() {
        return text3dActor;
    }

    /**
     * @param text3dActor the text3dActor to set
     */
    public void setText3dActor(vtkLODActor text3dActor) {
        this.text3dActor = text3dActor;
    }

}
