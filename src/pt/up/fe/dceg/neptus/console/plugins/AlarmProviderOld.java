/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: AlarmProviderOld.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.console.plugins;

public interface AlarmProviderOld {

	
	/* FIXME I would like to see more (and clearer) information on this interface ... */
	
	public static final short LEVEL_NONE = -2;
    public static final short LEVEL_OFF  = -1;
    public static final short LEVEL_0    =  0;
    public static final short LEVEL_1    =  1;
    public static final short LEVEL_2    =  2;
    public static final short LEVEL_3    =  3;
    public static final short LEVEL_4    =  4;
    
    public int getAlarmState(); // his own value
	public String getAlarmMessage(); // message
	public int sourceState(); //value that arrives from other children's of him 
}
