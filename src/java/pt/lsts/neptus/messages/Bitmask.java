// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 


package pt.lsts.neptus.messages;

import java.util.LinkedHashMap;

import pt.lsts.neptus.NeptusLog;

/**
 * Utility class to wrap bitmask values.
 */
public class Bitmask extends Number implements Comparable<Bitmask> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private LinkedHashMap<Long, String> possibleValues;
    private long currentValue;
    private String name;
    
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

    public Bitmask(LinkedHashMap<Long, String> possibleValues, long currentValue) {
        this.possibleValues = possibleValues;
        this.currentValue = currentValue;
    }

    public int compareTo(Bitmask o) {
        return (int)(currentValue-o.currentValue);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Bitmask) {
            return currentValue == ((Bitmask) obj).getCurrentValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        //return intValue();
        return (int)(currentValue ^ (currentValue >>> 32));
    }


    public long setBit(String bitName, boolean value) {
        if (possibleValues.containsValue(bitName)) {
            for (Long l : possibleValues.keySet()) {
                if (possibleValues.get(l).equals(bitName)) {
                    if (value) {
                        currentValue = currentValue | l;
                    }
                    else {
                        long reverse = -1 ^ l;
                        currentValue = currentValue & reverse;
                    }
                }
            }
        }
        else {
        	System.err.println("Bitmask: bit not found: "+bitName);
        }
        return currentValue;
    }

    public boolean isSet(String bitName) {
        if (possibleValues.containsValue(bitName)) {
            for (Long l : possibleValues.keySet()) {
                if (possibleValues.get(l).equals(bitName)) {
                    return (currentValue & l) != 0;
                }
            }
        }
        return false;
    }

    public long flip(String bitName) {    
        setBit(bitName, !isSet(bitName));
        return currentValue;
    }

    @Override
    public String toString() {
    	
    	if (currentValue == 0)
        	return " - ";
        
    	boolean hasTextBefore = false;
        StringBuilder sb = new StringBuilder();
        for (Long l : possibleValues.keySet()) {
            if ((l & currentValue) != 0)
                if (hasTextBefore)
                    sb.append(" | "+possibleValues.get(l));
                else {
                    sb.append(possibleValues.get(l));
                    hasTextBefore = true;
                }
        }
        
        
        return sb.toString();
    }

    public static void main(String[] args) {
        LinkedHashMap<Long, String> posVals = new LinkedHashMap<Long, String>();
        posVals.put((long)1, "Bit1");
        posVals.put((long)2, "Bit2");
        posVals.put((long)4, "Bit3");
        posVals.put((long)8, "Bit4");
        Bitmask bm = new Bitmask(posVals, 6);
        NeptusLog.pub().info("<###> "+bm);
        bm.setBit("Bit4", true);
        NeptusLog.pub().info("<###> "+bm);
        bm.setBit("Bit2", false);
        NeptusLog.pub().info("<###> "+bm);
    }

    public long getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(long currentValue) {
        this.currentValue = currentValue;
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
                || slzType.equals("int16_t")){
            slzNumber = Short.valueOf((short) currentValue);
        }
        else if(slzType.equals("int32_t")
             || slzType.equals("uint16_t")){
          slzNumber = Integer.valueOf((int) currentValue);
        }else {
          slzNumber = Long.valueOf(currentValue);
        }
        return slzNumber;
    }

    public LinkedHashMap<Long, String> getPossibleValues() {
        return possibleValues;
    }

    public void setPossibleValues(LinkedHashMap<Long, String> possibleValues) {
        this.possibleValues = possibleValues;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }    
}
