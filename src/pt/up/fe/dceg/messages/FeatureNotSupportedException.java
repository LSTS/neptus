// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 

package pt.up.fe.dceg.messages;

/**
 * Exception thrown when application tries to access an unsupported 
 * optional feature for a message (e.g. sequence ids, timestampts ...).
 * @author Eduardo Marques
 */
public class FeatureNotSupportedException extends java.lang.RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     * @param desc Error description.
     */
    public FeatureNotSupportedException(String desc){ super(desc); } 
}
