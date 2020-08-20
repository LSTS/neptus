/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Aug 20, 2020
 */
package pt.lsts.neptus.plugins.hmapping;

import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;

/**
 * @author zp
 *
 */
public class CoordinateConverter {

    private CoordinateTransform trans;
    private ProjCoordinate tmp = new ProjCoordinate();
    private String sourceCRS, targetCRS;
        
    private CoordinateConverter(String sourceCRS, String targetCRS) {
        CoordinateReferenceSystem source = new CRSFactory().createFromName(sourceCRS);
        CoordinateReferenceSystem target = new CRSFactory().createFromName(targetCRS);
        this.sourceCRS = sourceCRS;
        this.targetCRS = targetCRS;
        trans = new CoordinateTransformFactory().createTransform(source, target);
    }

    public CoordinateConverter reversed() {
        return new CoordinateConverter(targetCRS, sourceCRS);
    }
    
    public double[] transform(double x, double y, double z) {
        return transform(new double[] { x, y, z });
    }

    public double[] transform(double x, double y) {
        return transform(new double[] { x, y });
    }

    public double[] transform(double[] input) {
        ProjCoordinate coord = new ProjCoordinate(input[0], input[1], 0);
        coord.z = input.length > 2 ? input[2] : 0;
        trans.transform(coord, tmp);
        return new double[] { tmp.x, tmp.y, tmp.z };
    }   
}
