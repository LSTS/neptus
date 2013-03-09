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
 * 2010/05/07
 */
package pt.up.fe.dceg.neptus.plugins;

/**
 * @author pdias
 *
 */
public interface IAbortSenderProvider {
	/**
	 * To send abort to the main vehicle
     * @return
     */
    public boolean sendAbortRequest();

    /**
     * To send abort to the system. This might not be implemented.
     * @return
     */
	public boolean sendAbortRequest(String system) throws Exception;
}