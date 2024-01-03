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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * Oct 24, 2011
 */
package pt.lsts.neptus.mp.preview;

/**
 * @see https://whale.fe.up.pt/svn/dune/trunk/programs/yoyocalc.cpp
 * @author zp
 *
 */
public class SpeedConversion {

    protected static final double TOMPS_COEFA = 0.4449;
    protected static final double TOMPS_COEFB = -1.1723;
    protected static final double TOMPS_COEFC = 1.9588;
    protected static final double TOMPS_COEFD = 0.000;

    protected static final double TORPM_COEFA = -0.3024;
    protected static final double TORPM_COEFB = 0.9409;
    protected static final double TORPM_COEFC = 0.1120;
    protected static final double TORPM_COEFD = 0.0000;

    public static final double MAX_SPEED = 2;
    
    protected static double polynom3(double a, double b, double c, double d, double x) {
        return a*x*x*x+b*x*x+c*x+d;
    }
    
    public static double convertRpmtoMps(double rpm) {        
        return convertPercentageToMps(convertRpmtoPercentage(rpm));
    }
    
    public static double convertMpstoRpm(double mps) {
        return convertPercentagetoRpm(convertMpsToPercentage(mps));
    }
    
    public static double convertPercentagetoRpm(double percentage) {
        return percentage * 40;
    }
    
    public static double convertRpmtoPercentage(double rpm) {
        return rpm/40;
    }
    
    public static double convertPercentageToMps(double percentage) {
        double act = percentage / 100;
        return 6*act;
    }
    
    public static double convertMpsToPercentage(double mps) {
        double act = mps/ 6;
        return act * 100;        
    }    
}
