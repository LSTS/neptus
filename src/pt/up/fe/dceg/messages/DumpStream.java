// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 
package pt.up.fe.dceg.messages;
import java.io.PrintStream;

/**
 * Dump Stream.
 * @author Eduardo Marques
 */
public class DumpStream {
  private final PrintStream ps;  
  public DumpStream(){
    this(System.out);
  }

  public DumpStream(PrintStream ps){
    this.ps = ps;
  }
  
  public synchronized void dump(Message m){
    dump(m, 0); 
    ps.print("\r\n");
  }
  private void dump(Message m, int indent_level){
    String[] fields = m.fields();
    Object[] values = m.values();
    int n = fields.length;
    indent(ps, indent_level);
    ps.printf("%s {\r\n", m.name());
    indent_level+=2;
    for(int i=0;i<n;i++){
      Object o = values[i];
      indent(ps, indent_level);
      ps.printf("%s = ", fields[i]);
      if(o == null){
        ps.print("<null>");
      }else if(o instanceof Message){
         ps.print("\r\n");
         dump((Message) o, indent_level+2);
      }else if(o instanceof byte[]){
        ps.print(Utils.toHexString((byte[]) o));
      }else if(o instanceof String){
        ps.print('\"');
        ps.print(Utils.escape((String)o));
        ps.print('\"');
      }else{
        ps.print(o);
      }
      ps.print("\r\n");
    }
    indent_level -= 2; 
    indent(ps, indent_level);
    ps.print("}");
  }

  private void indent(PrintStream ps, int indent_level){
    for(int i=0;i<indent_level;i++)
      ps.print(' ');
  }
}
