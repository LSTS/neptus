/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2007/08/04
 * $Id:: IMCSerialization.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.mp.maneuvers;

import pt.up.fe.dceg.neptus.imc.IMCMessage;

/**
 * @author pdias
 *
 */
public interface IMCSerialization {
	public IMCMessage serializeToIMC();
	public void parseIMCMessage(IMCMessage message);
}
