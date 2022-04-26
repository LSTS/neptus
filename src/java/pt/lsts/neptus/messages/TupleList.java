// ----------------------------------------------------------------------------
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ----------------------------------------------------------------------------
package pt.lsts.neptus.messages;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pt.lsts.neptus.NeptusLog;

/**
 * Tuple list datum in the form of 'name1=value;name2=value2;'.
 * @author Eduardo Marques
 */
public class TupleList implements Comparable<TupleList>
{

	@Override
	public int compareTo(TupleList o) {
		return toString().compareTo(o.toString());
	}

   /**
    * Map used to keep tuples indexed by name.
    */
   private final LinkedHashMap<String,String> map =
	   	new LinkedHashMap<String,String>();

    /**
     * Constructor.
     */
    public TupleList()
    {

    }

    /**
     * Constructor from serialization string.
     * This constructor calls {@link #parse(String)}.
     */
    public TupleList(String spec)
    {
      parse(spec);
    }

    /**
     * Parse tuple list values from serialization string.
     * Parsed values are added to the tuple list.
     */
    public void parse(String spec)
    {
      if(spec == null || spec.equals("")) return;

      for(String p : spec.split(";"))
      {
        String[] kv = p.split("=",2);
        map.put(kv[0],kv[1]);
      }
    }
    
    public void parse(LinkedHashMap<String, ?> spec)
    {
      if(spec == null) 
          return;

      map.clear();
      for(Entry<String, ?> e : spec.entrySet())
          map.put(e.getKey(), ""+e.getValue());       
    }

    /**
     * Convert tuple list to string.
     * @return string representation of tuple list
     */
    public String toString()
    {
      String representation="";
      for(Map.Entry<String,String> entry : map.entrySet())
      {
        representation +=
          String.format("%s=%s;", entry.getKey(), entry.getValue());
      }
      return representation;
    }

    /**
     * Get tuple list size.
     * @return number of entries in tuple list.
     */
    public int size()
    {
      return map.size();
    }

    /**
     * Get keys in the tuple list.
     */
    public Set<String> keys()
    {
      return map.keySet();
    }

    /**
     * Clear all values from the tuple list.
     */
    public void clear()
    {
      map.clear();
    }

    /**
     * Set a value in the tuple list.
     * @param key Tuple key
     * @param value Tuple value
     */
    public void set(String key, Object value)
    {
      map.put(key, value.toString());
    }

    /**
     * Get a value from the tuple list.
     * @param key Tuple key
     * @param clazz Tuple class
     * @return tuple value associated with tuple key
     * @throws InvalidFieldValueException if the specified class type
     * is not compatible with the tuple value
     */
    @SuppressWarnings("unchecked")
    public <T extends Comparable<?>> T get(String key, Class<T> clazz)
    throws InvalidFieldValueException
    {
      try
      {
        String value = map.get(key);
        if(value == null) return null;
        Constructor<?> ctor =
          clazz.getDeclaredConstructor(String.class);
        return (T) ctor.newInstance(value);
      }
      catch(Exception e)
      {
        throw new InvalidFieldValueException(e);
      }
    }


    /**
     * Get a value from the tuple list.
     * @param key Tuple key
     * @return tuple value associated with tuple key
     */
    public String get(String key)
    {
        return map.get(key);
    }


    public static void main(String[] args) throws InvalidFieldValueException
    {
      TupleList tl = new TupleList("x=0;y=abc;z=0.00;w=92.2;");
      NeptusLog.pub().info("<###> "+tl.get("z", Float.class));
      tl.set("z", 3.0f);
      tl.set("flag", true);
      NeptusLog.pub().info("<###> "+tl.get("z", Float.class));
      NeptusLog.pub().info("<###> "+tl.get("flag", Boolean.class));
      NeptusLog.pub().info("<###> "+tl);
    }
}
