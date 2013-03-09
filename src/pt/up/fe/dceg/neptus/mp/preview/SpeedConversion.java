/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Oct 24, 2011
 */
package pt.up.fe.dceg.neptus.mp.preview;

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
