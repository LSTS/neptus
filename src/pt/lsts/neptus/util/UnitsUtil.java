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
 * Author: pdias
 * 07/04/2017
 */
package pt.lsts.neptus.util;

/**
 * A conversion constants utility.
 * 
 * @author pdias
 *
 */
public class UnitsUtil {

    // Lenght
    /** Conversion factor: m to inch. */
    public static final double METER_TO_INCH = 39.37007874;
    /** Conversion factor: m to feet. */
    public static final double METER_TO_FEET = 3.280839895;
    /** Conversion factor: m to yard. */
    public static final double METER_TO_YARD = 1.0936132983;
    /** Conversion factor: m to mile. */
    public static final double METER_TO_MILE = 0.0006213711922;
    /** Conversion factor: m to nautical mile. */
    public static final double METER_TO_NMILE = 0.0005399568035;
    
    // Temperature
    /**
     * Conversion factor: Celsius to Fahrenheit. T(°F) = T(°C) × {@link #CELSIUS_TO_FAHRENHEIT_MULTIPLY_PART} +
     * {@link #CELSIUS_TO_FAHRENHEIT_ADDITION_PART}
     */
    public static final double CELSIUS_TO_FAHRENHEIT_MULTIPLY_PART = 1.8;
    /**
     * Conversion factor: Celsius to Fahrenheit. T(°F) = T(°C) × {@link #CELSIUS_TO_FAHRENHEIT_MULTIPLY_PART} +
     * {@link #CELSIUS_TO_FAHRENHEIT_ADDITION_PART}
     */
    public static final double CELSIUS_TO_FAHRENHEIT_ADDITION_PART = 32;
    /** Conversion factor: Celsius to Kelvin. */
    public static final double CELSIUS_TO_KELVIN = 274.15;
    
    // Speed
    /** Conversion factor: m/s to knot. http://www.bipm.org/en/publications/si-brochure/table8.html */
    public static final double MS_TO_KNOT = 3.6 / 1.852; // 1.9438444924406047516198704103672
    /** Conversion factor: m/s to km/h. */
    public static final double MS_TO_KMH = 3.6;
    /** Conversion factor: m/s to miles/h. */
    public static final double MS_TO_MPH = 2.2369362921;
    
    private UnitsUtil() {
    }
}
