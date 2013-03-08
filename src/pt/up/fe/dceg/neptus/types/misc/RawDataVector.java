/*
 * Created in 12/Abr/2006
 * $Id:: RawDataVector.java 8431 2012-09-21 17:43:43Z jqcorreia           $:
 */
package pt.up.fe.dceg.neptus.types.misc;

import java.util.Vector;

import pt.up.fe.dceg.neptus.messages.RawData;

/**
 * @author Paulo Dias
 *
 */
@SuppressWarnings("serial")
public class RawDataVector<T> extends Vector<T> implements Comparable<RawDataVector<T>>
{
    private static final int MAX_PRINTABLE_DATA = 10;
    private int maxPrintableData = MAX_PRINTABLE_DATA;

    /**
     * @return
     */
    public int getMaxPrintableData()
    {
    	return maxPrintableData;
    }

    /**
     * @param maxPrintableData
     */
    public void setMaxPrintableData(int maxPrintableData)
    {
        this.maxPrintableData = (maxPrintableData < 0) ? (maxPrintableData * -1)
                : maxPrintableData;
    }

    @Override
    public synchronized String toString()
    {
        String data = toStringComplete();
        if (data.length() > maxPrintableData)
        {
            data = data.substring(0, (maxPrintableData) / 2 * 3)
                    + " ... "
                    + data.substring(data.length() - maxPrintableData / 2 * 3
                            - 2);
        }
        return data;
    }
    
    public synchronized String toStringComplete()
    {
        if (this.isEmpty())
            return super.toString();
        else if (this.firstElement() instanceof Byte)
        {
            String ret = "";
            for (Object elem : this)
            {
                Byte elemB = (Byte) elem;
                int val = (elemB < 0)?((Byte.MAX_VALUE+1) * 2 + elemB):(elemB);
                ret += " " + Integer.toHexString(val);
            }
            return ret;
        }
        else
            return super.toString();
    }
    
    
    /* (non-Javadoc)
     * @see java.util.Collection#toArray()
     */
    @Override
    public synchronized Byte[] toArray()
    {
        
        if (this.isEmpty())
            return new Byte[0];
        else if (this.firstElement() instanceof Byte)
        {
            Byte[] elemB = new Byte[this.size()];
            return super.toArray(elemB);
        }
        else
            return new Byte[0];
    }

    
    /**
     * @return
     */
    public RawData toRawData()
    {
    	return new RawData(this.toArray());
    }
    
    /**
     * @param arg0
     * @return
     */
    public int compareTo(RawDataVector<T> arg0) 
    throws ClassCastException 
    {
        try
        {
            @SuppressWarnings("unused") 
            RawDataVector<T> comp = (RawDataVector<T>) arg0;
        }
        catch (RuntimeException e)
        {
            ClassCastException ccexp = new ClassCastException("The type should be RawDataType" +
                    " and was found" + arg0);
            ccexp.initCause(e);
            throw ccexp;
        }
        return 1;
    }
    
    /**
     * @param o
     * @return
     */
    public synchronized boolean addAll(T[] o)
    {
        boolean res = true;
        for (T e : o)
        {
            res = res && super.add(e);
        }
        return res;
    }
    
    @SuppressWarnings("unchecked")
    public synchronized boolean addAll(byte[] o)
    {
        boolean res = true;
        for (byte el : o)
        {
            T valueOf;
            try
            {
                valueOf = (T) Byte.valueOf(el);
            }
            catch (Exception exp)
            {
                return false;
            }
            T el1 = valueOf;
            res = res && super.add(el1);
        }
        return res;
    }
}
