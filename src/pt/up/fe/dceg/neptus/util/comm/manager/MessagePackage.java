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
 * 24/06/2011
 */
package pt.up.fe.dceg.neptus.util.comm.manager;

/**
 * @author pdias
 *
 */
public class MessagePackage<Mi, M> {
    private Mi info;
    private M message;
    
    /**
     * 
     */
    public MessagePackage(Mi info, M message) {
        this.info = info;
        this.message = message;
    }
    
    /**
     * @return the info
     */
    public Mi getInfo() {
        return info;
    }
    
    /**
     * @return the message
     */
    public M getMessage() {
        return message;
    }
}

