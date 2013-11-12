// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 

package pt.lsts.neptus.messages;

import java.util.LinkedHashMap;

/**
 * Utility class to wrap enumerated values.
 */
public class Enumerated extends Number implements Comparable<Enumerated> {
    private static final long serialVersionUID = 1L;
    private final LinkedHashMap<Long, String> possibleValues;
    private long currentValue;


	public long longValue() {
		return currentValue;
	}
	
	public double doubleValue() {
		return currentValue;
	}
	
	public float floatValue() {
		return currentValue;
	}
	
	public int intValue() {
		return (int)currentValue;
	}
	
    public Enumerated(LinkedHashMap<Long, String> possibleValues, long currentValue) {
        this.possibleValues = possibleValues;
        this.currentValue = currentValue;
    }

    public int compareTo(Enumerated o) {
        return (int)(currentValue-o.currentValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Enumerated) {
            return currentValue == ((Enumerated) obj).getCurrentValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        //return intValue();
        return (int)(currentValue ^ (currentValue >>> 32));
    }
    
    public LinkedHashMap<Long, String> getPossibleValues() {
        return possibleValues;
    }

    public long getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(long value) throws InvalidFieldValueException {
        if (possibleValues.containsKey(value))
            this.currentValue = value;
        else {
            throw new InvalidFieldValueException("Enumeration does not hold the entered value: "+value);
        }
    }
    
    public void setCurrentValue(String value) throws InvalidFieldValueException {
    	if (possibleValues.containsValue(value)) {    		
    		for (Long key : possibleValues.keySet()) {
    			if (possibleValues.get(key).equals(value)) {
    				this.currentValue = key;
    				break;
    			}    			
    		}
    	}
    	else {
    		 throw new InvalidFieldValueException("Enumeration does not hold the entered (String) value: "+value);
    	}
    }

    @Override
    public String toString() {
        if (!possibleValues.containsKey(currentValue))
            return ""+currentValue+" (Unknown value)";

        return possibleValues.get(currentValue);
    }
    /**
     * Get enumerated value according to serialization type.
     * @param slzType the serialization type
     * @return the correct object encoding the value with the correct
     * Java serialization type (short, int or long)
     */
    public Number getValueAs(String slzType){
        Number slzNumber;
        if(slzType.equals("uint8_t") 
                || slzType.equals("int8_t")
                || slzType.equals("int16_t"))
        {
            slzNumber = new Short((short) currentValue);
        }
        else
        if(slzType.equals("int32_t")
        || slzType.equals("uint16_t")) {
           slzNumber = new Integer((int) currentValue);
        }
        else{
           slzNumber = new Long(currentValue);
        }
        return slzNumber;
    }
}
