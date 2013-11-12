// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 

package pt.lsts.neptus.messages;

/**
 * Utility class to wrap "raw data" values.
 * @author Paulo Dias
 */
public final class RawData implements Comparable<RawData>
{
    //public static final int MAX_SHOWN_BYTES = 8;
    byte[] data = null;
    int length = 0;

    public RawData() 
    {}

    public RawData(byte[] bytes) {
        setValue(bytes);
    }

    public RawData(Byte[] bytes) {
        byte[] tmp = new byte[bytes.length];
        int i = 0;
        for (byte b : bytes)
            tmp[i++] = b;
        setValue(tmp);
    }

    public Object getValue() {
        return data;        
    }

    public int getSize() {
        return length;        
    }


    public void setValue(Object obj) {

        if (obj instanceof byte[]) {
            this.data = (byte[])obj;
            this.length = data.length;
        }
        else {
            this.data = new byte[] {};
            this.length = 0;
        }
    }


    public String toString(int numChars) {

        int i = 0;
        StringBuffer ret = new StringBuffer();

        while (i< numChars&& i < length) {        

            ret.append(String.format("%x ", new Object[] {data[i]}));
            i++;        
        }

        if (length > numChars)
            ret.append("...");

        return ret.toString();
    }


    @Override
    public String toString() {

        int i = 0;
        StringBuffer ret = new StringBuffer();

        while (i < length) {       

            ret.append(String.format("%x ", new Object[] {data[i]}));
            i++;        
        }

        return ret.toString();
    }


    /**
     * @return
     */
    public byte[] getAsByteArray()
    {
        byte[] ret = new byte[data.length];
        System.arraycopy(data, 0, ret, 0, data.length);
        return ret;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        return new RawData(getAsByteArray());
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(RawData o)
    {
        return 1;
    }

}
