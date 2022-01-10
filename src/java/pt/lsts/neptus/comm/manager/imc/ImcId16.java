/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 2009/??/??
 */
package pt.lsts.neptus.comm.manager.imc;

import java.util.StringTokenizer;

import pt.lsts.neptus.NeptusLog;

/**
 * This class manages IMC 16 bits IDs of the form xx:xx (hexadecimal)
 * @author Paulo Dias
 */
public class ImcId16 extends Number implements Comparable<ImcId16>{

	private static final long serialVersionUID = 4540568234437157049L;

	public static final long MAX_VALUE = 0xFFFF;

	public static final ImcId16 BROADCAST_ID = new ImcId16(0xFFF0);
    public static final ImcId16 ANNOUNCE     = new ImcId16(0x0000);
    public static final ImcId16 NULL_ID      = new ImcId16(0xFFFF);

    private long id = 0;

    public ImcId16(Object o) throws Exception {
    	if (o == null) {
    		throw new Exception("IMC Id not valid: null");
    	}
    	if (o instanceof Number) {
    		id = ((Number)o).longValue();
    	}
    	else {
    		try {
    			this.id = parseImcId16(o.toString());
    		}
    		catch (Exception e) {
    			throw new Exception("IMC Id not valid: "+o.toString());
    		}
    	}
    }
    
    public ImcId16(long id) {
        this.id = id;
    }

    public ImcId16(String idString) {
        try {
			this.id = parseImcId16(idString);
		} catch (NumberFormatException e) {
		    NeptusLog.pub().error(e.getMessage());
			this.id = 0xFFFF;
		}
    }

    /**
     * Return true if id different from {@link #NULL_ID} or {@link #BROADCAST_ID} or {@link #ANNOUNCE}.
     * 
     * @param id
     * @return
     */
    public static boolean isValidIdForSource(ImcId16 id) {
        return !ImcId16.NULL_ID.equals(id) && !ImcId16.BROADCAST_ID.equals(id) && !ImcId16.ANNOUNCE.equals(id);
    }

    /**
     * Return true if id different from {@link #NULL_ID} or {@link #BROADCAST_ID} or {@link #ANNOUNCE}.
     * 
     * @param id
     * @return
     */
    public static boolean isValidIdForSource(long id) {
        return ImcId16.NULL_ID.longValue() != id && ImcId16.BROADCAST_ID.longValue() != id
                && ImcId16.ANNOUNCE.longValue() != id;
    }

    @Override
    public int compareTo(ImcId16 o) {
		//return (int) (longValue() - o.longValue());
        return (longValue() < o.longValue() ? -1 : (longValue() == o.longValue() ? 0 : 1));
	}

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if (obj instanceof ImcId16) {
            return longValue() == ((ImcId16) obj).longValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int)(id ^ (id >>> 32));
    }

    @Override
    public int intValue() {
        return (int) id;
    }

    @Override
    public long longValue() {
        return id;
    }

    @Override
    public float floatValue() {
        return id;
    }

    @Override
    public double doubleValue() {
        return id;
    }

    /**
     * return The id as a {@link Long}
     */
    @Override
    public String toString() {
        return toHexString();
    }

    public String toIntString() {
        return String.valueOf(id);
    }

    public String toHexString() {
        return "0x" + toPrettyString().replace(":", "");
    }
    
    /**
     * @return The id in the form of XX:XX.
     */
    public String toPrettyString() {
      String b1 = Long.toHexString((id&0x0000ff00)>>8);
      if (b1.length() == 1)
          b1 = "0"+b1;
      String b2 = Long.toHexString(id&0x000000ff);
      if (b2.length() == 1)
          b2 = "0"+b2;
      return b1+":"+b2;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setId(String id) throws NumberFormatException {
        this.id = parseImcId16(id);
    }

    /**
     * @param s
     * @return
     * @throws NumberFormatException
     */
    public static long parseImcId16(String s) throws NumberFormatException {
        if (s == null) {
            throw new NumberFormatException("null");
        }
        if (s.startsWith("0x")) {
            return Long.parseLong(s.replace("0x", ""), 16);
        }
        long id = 0;
        StringTokenizer st = new StringTokenizer(s, ":");
        if (st.countTokens() != 2) {
            try {
                return Long.parseLong(s);
            }
            catch (Exception e) {
                throw new NumberFormatException("Unable to parse ImcId16: '"+s+"' (ID must have the format xx:xx hexa values)");
            }
        }
        for (int i = 0; i < 2 ; i++) {
            try {
                int val = Integer.parseInt(st.nextToken(), 16);
                if (val < 0 || val > 255) {
                    //System.err.println("Unable to parse ImcId16: '"+s+"' (value "+val+" is invalid)");
                    throw new NumberFormatException("Unable to parse ImcId16: '"+s+"' (value "+val+" is invalid)");
                }
                id = id | ((long) val <<((1-i)*8));
            }
            catch (Exception e) {
                //System.err.println("Unable to parse ImcId16: '"+s+"' ("+e.getMessage()+")");
                throw new NumberFormatException("Unable to parse ImcId16: '"+s+"' ("+e.getMessage()+")");
            }
        }
        return id;
    }

    /**
     * @param s
     * @return
     * @throws NumberFormatException
     */
    public static ImcId16 valueOf(String s) throws NumberFormatException {
        return new ImcId16(parseImcId16(s));
    }

    public int getFirstByte() {
        return getByte(0);
    }

    public int getSecondByte() {
        return getByte(1);
    }


    public int getByte(int number) {
        int shift = (1-number)*8;
        int val = (intValue() & (0x000000ff << shift)) >> shift;

        if (val < 0)
            return 255;
        else
            return val;
    }
    
   /**
    * Get IMC ID value according to serialization type.
    * @param slzType the serialization type
    * @return the correct object encoding the value with the correct
    * Java serialization type (short, int or long)
    **/
    public Number getValueAs(String slzType){
        Number slzNumber;
        if(slzType.equals("uint8_t")
                || slzType.equals("int8_t")
                || slzType.equals("int16_t")) {
            slzNumber = (short) id;
        }
        else if(slzType.equals("int32_t")
             || slzType.equals("uint16_t")) {
          slzNumber = (int) id;
        }
        else {
           slzNumber = id;
        }
        return slzNumber;
    }

    /*public int setByte(int byteNumber, int byteVal) throws Exception {
        int shift = (3-byteNumber)*8;
        return (intValue() & (0x000000ff << shift)) >> shift;
    }
    */


    public static void main(String[] args) throws Exception {
        ImcId16 id = new ImcId16("ed:01");
        NeptusLog.pub().info("<###> "+id.intValue());
        NeptusLog.pub().info("<###> "+id.toPrettyString());

        NeptusLog.pub().info("<###> "+id.getFirstByte());
        NeptusLog.pub().info("<###> "+id.getSecondByte());

        
        ImcId16 id1 = new ImcId16(60910);
        NeptusLog.pub().info("<###> "+id1.intValue());
        NeptusLog.pub().info("<###> "+id1.toPrettyString());

        NeptusLog.pub().info("<###> "+id1.getFirstByte());
        NeptusLog.pub().info("<###> "+id1.getSecondByte());

        NeptusLog.pub().info("<###> "+id.equals(id1));

        NeptusLog.pub().info("<###> "+(id == id1));

        
        ImcId16 id2 = new ImcId16(334360910);
        NeptusLog.pub().info("<###> "+id2.intValue());
        NeptusLog.pub().info("<###> "+id2.toPrettyString());

        ImcId16 idC1 = new ImcId16("a0:01");
        ImcId16 idC2 = new ImcId16("a0:01");
        NeptusLog.pub().info("<###> "+idC1.intValue());
        NeptusLog.pub().info("<###> "+idC2.intValue());
        NeptusLog.pub().info("<###> "+idC1.equals(idC2));
        NeptusLog.pub().info("<###> "+(idC1 == idC2));
        
        ImcId16 id3 = new ImcId16("0xa001");
        NeptusLog.pub().info("<###> "+id3.intValue());
        NeptusLog.pub().info("<###> "+id3.toPrettyString());
    }
}
