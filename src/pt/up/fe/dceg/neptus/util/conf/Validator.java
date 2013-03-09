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
 * 2006/12/21
 */
package pt.up.fe.dceg.neptus.util.conf;

/**
 * @author Paulo Dias
 *
 */
public interface Validator
{
    /**
     * @param newValue
     * @return <code>null</code> if no error exists or a 
     *         <code>String</code> with the error. 
     */
    public String validate(Object newValue);
    
    /**
     * @return A textual description of the valid values.
     */
    public String validValuesDesc();
}