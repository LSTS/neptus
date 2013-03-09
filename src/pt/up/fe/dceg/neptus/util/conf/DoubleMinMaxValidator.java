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
 * 2006/12/26
 */
package pt.up.fe.dceg.neptus.util.conf;

/**
 * @author Paulo Dias
 *
 */
public class DoubleMinMaxValidator implements Validator
{

	private double min = Double.MIN_VALUE, max = Double.MAX_VALUE;
    private boolean minClosed = true, maxClosed = true;
    
    /**
     * Validates <code>min <= val <= max</code>.
     * @param min
     * @param max
     */
    public DoubleMinMaxValidator(double min, double max)
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
    public DoubleMinMaxValidator(double min, double max, boolean minClosed, boolean maxClosed)
    {
        this.min = min;
        this.max = max;
        this.minClosed = minClosed;
        this.maxClosed = maxClosed;
    }

    /**
     * Validates <code>min <= val <= max</code>.
     * @param val
     * @param isMax if true    min=Double.MIN_VALUE
     *              else false max=Double.MAX_VALUE
     */
    public DoubleMinMaxValidator(double val, boolean isMax)
    {
        if(isMax)
            max = val;
        else
            min = val;
    }

    /**
     * Validates <code>min <= val <= max</code>.
     * @param val
     * @param isMax if true    min=Double.MIN_VALUE
     *              else false max=Double.MAX_VALUE
     * @param isClosed
     */
    public DoubleMinMaxValidator(double val, boolean isMax, boolean isClosed)
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
     * @see pt.up.fe.dceg.neptus.util.conf.Validator#validate(java.lang.Object)
     */
    public String validate(Object newValue)
    {
        try
        {
        	double val = (Double) newValue;
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
		System.out.println(new DoubleMinMaxValidator(1.0, 10.0).validate(20.0));
		System.out.println(new DoubleMinMaxValidator(1.0, 10.0, true, false).validate(10.0));
		System.out.println(new DoubleMinMaxValidator(1.0, 10.0, true, false).validate(1.0));
		System.out.println(new DoubleMinMaxValidator(1.0, 10.0, true, true).validate(10.0));
		System.out.println(new DoubleMinMaxValidator(1.0, 10.0, false, true).validate(1.0));
	}
}
