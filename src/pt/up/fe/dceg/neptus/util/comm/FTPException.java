/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Paulo Dias
 * 22/Jun/2005
 */
package pt.up.fe.dceg.neptus.util.comm;

import java.io.IOException;

/**
 * @author Paulo Dias
 */
public class FTPException 
extends IOException
{
    private static final long serialVersionUID = 1L;
    public static final short UNKNOWN  = -1;
    public static final short LOGIN_FAILED  = 0;

    
    private short exceptionCode = -1;
    
    /**
     * 
     */
    public FTPException()
    {
        super();
    }

    /**
     * @param arg0
     */
    public FTPException(String arg0)
    {
        super(arg0);
    }

    public short getExceptionCode()
    {
        return exceptionCode;
    }
    
    /**
     * @param exceptionCode The exceptionCode to set.
     */
    public void setExceptionCode(short exceptionCode)
    {
        this.exceptionCode = exceptionCode;
    }
}
