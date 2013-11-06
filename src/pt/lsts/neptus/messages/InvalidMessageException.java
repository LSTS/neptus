// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 

package pt.lsts.neptus.messages;

/**
 * Exception thrown when application tries to create an invalid message.
 * or the network message format is incorrect or corrupt.
 * @author Eduardo Marques
 */
public class InvalidMessageException extends java.lang.RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     * @param desc Error description
     */
    public InvalidMessageException(String desc){ super(desc); } 
}
