/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Apr 8, 2013
 */
package pt.lsts.neptus.plugins.vtk.pointtypes;

/**
 * @author hfq
 *
 */
public class PointXYZI extends APoint {
    private int intensity;

    public PointXYZI() {
        super();
        setIntensity(0);
    }

    public PointXYZI (double x, double y, double z, int intensity) {
        super(x, y, z);
        setIntensity(intensity);
    }

    /**
     * @return the intensity
     */
    public int getIntensity() {
        return intensity;
    }

    /**
     * @param intensity the intensity to set
     */
    private void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.pointtypes.APoint#toString()
     */
    @Override
    public String toString() {
        return getX() + " " + getY() + " " + getZ() + " " + getIntensity();
    }
}
