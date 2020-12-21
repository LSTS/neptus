/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 2006/12/22
 */
package pt.lsts.neptus.util.conf;

import pt.lsts.neptus.NeptusLog;

/**
 * @author Paulo Dias
 *
 */
public class IntegerMinMaxValidator implements Validator
{

	private int min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;
    private boolean minClosed = true, maxClosed = true;
    
    /**
     * Validates <code>min <= val <= max</code>.
     * @param min
     * @param max
     */
    public IntegerMinMaxValidator(int min, int max)
    {
        this.min = min;
        this.max = max;
    }

    /**
     * Validates <code>min <= val <= max</code>. The equal assert is set by the
     *  flags <code>minClosed</code> and <code>maxClosed</code>.
     * @param min
     * @param max
     * @param minClosed
     * @param maxClosed
     */
    public IntegerMinMaxValidator(int min, int max, boolean minClosed, boolean maxClosed)
    {
        this.min = min;
        this.max = max;
        this.minClosed = minClosed;
        this.maxClosed = maxClosed;
    }

    /**
     * Validates <code>min <= val <= max</code>.
     * @param val
     * @param isMax if true    min=Integer.MIN_VALUE
     *              else false max=Integer.MAX_VALUE
     */
    public IntegerMinMaxValidator(int val, boolean isMax)
    {
        if(isMax)
            max = val;
        else
            min = val;
    }

    /**
     * Validates <code>min <= val <= max</code>.
     * @param val
     * @param isMax if true    min=Integer.MIN_VALUE
     *              else false max=Integer.MAX_VALUE
     * @param isClosed
     */
    public IntegerMinMaxValidator(int val, boolean isMax, boolean isClosed)
    {
        if(isMax)
        {
            max = val;
            maxClosed = isClosed;
        }
        else
        {
            min = val;
            minClosed = isClosed;
        }
    }


    /* (non-Javadoc)
     * @see pt.lsts.neptus.util.conf.Validator#validate(java.lang.Object)
     */
    public String validate(Object newValue)
    {
        try
        {
            int val = (Integer) newValue;
            if ((minClosed?val >= min:val > min) && (maxClosed?val <= max:val < max))
                return null;
            else
                return "The value should be between " + min
						+ (minClosed ? " included" : "") + " and " + max
						+ (maxClosed ? " included" : "") + ". You typed: " + val
						+ ".";
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }
    
    @Override
    public String validValuesDesc() {
    	String ret = ((minClosed)?"[":"]")+min+", "+max+((maxClosed)?"]":"[");
    	return ret;
    }

    public static void main(String[] args) {
		NeptusLog.pub().info("<###> "+new IntegerMinMaxValidator(1, 10).validate(20));
		NeptusLog.pub().info("<###> "+new IntegerMinMaxValidator(1, 10, true, false).validate(10));
		NeptusLog.pub().info("<###> "+new IntegerMinMaxValidator(1, 10, true, false).validate(1));
		NeptusLog.pub().info("<###> "+new IntegerMinMaxValidator(1, 10, true, true).validate(10));
		NeptusLog.pub().info("<###> "+new IntegerMinMaxValidator(1, 10, false, true).validate(1));
	}
}
