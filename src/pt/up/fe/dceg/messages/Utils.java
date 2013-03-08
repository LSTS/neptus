// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 
package pt.up.fe.dceg.messages;

/**
 * Class with misc. utility methods.
 * @author Eduardo Marques
 */
public class Utils {

  /** 
   * Convert a byte array to an hex string.
   * @param data array of bytes
   * @return hex string
   */
  public static String toHexString(byte[] data){
    StringBuffer sb = new StringBuffer(data.length * 2);
    for(byte b : data){
      int d = (int) b;
      if(d<0) d += 256;
      char d1 = (char) (d >> 4);
      char d2 = (char)(d & 0x0F);
      d1 += (d1<10) ? '0' : '7'; // Note: '7' = 'A' - 10
      d2 += (d2<10) ? '0' : '7';
      sb.append(d1); 
      sb.append(d2); 
    }
    return sb.toString();
  }

  /**
   * Convert string to "escaped" form.
   * @param string to "escape"
   * @return "escape" of s
   */
  public static String escape(String s){
    int l = s.length();
    StringBuffer sb = new StringBuffer(l);
    for(int j=0;j<l;j++){
      char c = s.charAt(j);
      boolean escape=true;
      switch(c){
        case '\n': c='n'; break;
        case '\t': c='t'; break;
        case '\r': c='r'; break;
        case '\b': c='b'; break;
        case '\f': c='f'; break;
        case '\\': c='\\'; break;
        case '\"': c='\"'; break;
        default: escape = false;
      }
      if(escape) sb.append('\\');
      sb.append(c);
    }
    return sb.toString();
  }
}
