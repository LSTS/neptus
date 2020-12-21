// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 

package pt.lsts.neptus.messages;

/**
 * Exception thrown when application tries to access an invalid message field.
 * @author Eduardo Marques
 */
public class InvalidFieldException extends java.lang.Exception {
    private static final long serialVersionUID = -7308995026279257817L;

    /**
     * Constructor
     * @param desc Error description
     */
    public InvalidFieldException(String desc){ super(desc); } 
}
