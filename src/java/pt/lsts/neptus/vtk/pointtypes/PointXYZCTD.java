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
 * Jun 3, 2014
 */
package pt.lsts.neptus.vtk.pointtypes;

/**
 * @author hfq
 * 
 */
public class PointXYZCTD extends APoint {
    private double salinity;
    private double temperature;
    private double pressure;

    /**
     * 
     */
    public PointXYZCTD() {
        super();
        setSalinity(0.0);
        setTemperature(0.0);
        setPressure(0.0);
    }

    /**
     * @param x
     * @param y
     * @param z
     * @param salinity
     * @param temperature
     * @param pressure
     */
    public PointXYZCTD(double x, double y, double z, double salinity, double temperature, double pressure) {
        super(x, y, z);
        setSalinity(salinity);
        setTemperature(temperature);
        setPressure(pressure);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.vtk.pointtypes.APoint#toString()
     */
    @Override
    public String toString() {
        return getX() + " " + getY() + " " + getZ() + " " + getSalinity() + " " + getTemperature() + " "
                + getPressure();
    }

    /**
     * @return the salinity
     */
    public double getSalinity() {
        return salinity;
    }

    /**
     * @param salinity the salinity to set
     */
    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    /**
     * @return the temperature
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * @param temperature the temperature to set
     */
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    /**
     * @return the pressure
     */
    public double getPressure() {
        return pressure;
    }

    /**
     * @param pressure the pressure to set
     */
    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

}
