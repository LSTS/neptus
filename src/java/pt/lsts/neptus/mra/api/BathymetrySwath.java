/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: jqcorreia
 * Apr 2, 2013
 */
package pt.lsts.neptus.mra.api;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;

import java.awt.image.BufferedImage;

/**
 * @author jqcorreia
 *
 */
public class BathymetrySwath {
    long timestamp;
    private int numBeams;
    
    SystemPositionAndAttitude pose;
    BathymetryPoint data[];

    private BufferedImage image;
    
    
    /**
     * @param timestamp
     * @param pose
     * @param data
     */
    public BathymetrySwath(long timestamp, SystemPositionAndAttitude pose, BathymetryPoint[] data) {
        this.timestamp = timestamp;
        this.pose = pose;
        this.data = data;
        this.numBeams = data != null ? data.length : 0;
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the pose
     */
    public SystemPositionAndAttitude getPose() {
        return pose;
    }

    /**
     * @param pose the pose to set
     */
    public void setPose(SystemPositionAndAttitude pose) {
        this.pose = pose;
    }

    /**
     * @return the data
     */
    public BathymetryPoint[] getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(BathymetryPoint[] data) {
        this.data = data;
    }

    /**
     * @return the numBeams
     */
    public int getNumBeams() {
        return numBeams;
    }

    /**
     * @param numBeams the numBeams to set
     */
    public void setNumBeams(int numBeams) {
        this.numBeams = numBeams;
    }
    
    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public BufferedImage asBufferedImage() {
        return image;
    }
    
}
