// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 
package pt.up.fe.dceg.messages;

/**
 * Utility enumeration for validation errors.
 * @author Eduardo Marques
 */
public class Validation {
      public static final Validation MIN = new Validation("minimum","<");
      public static final Validation MAX = new Validation("maximum",">");
      public static final Validation VALUE = new Validation("value","!=");
      public static final Validation MAGIC = new Validation("magic","!=");
      public static final Validation CKSUM = new Validation("checksum","!=");
      public static final Validation MID = new Validation("message id","!=");
      public static final Validation SIZE = new Validation("available data size","<");
      
      private final String desc;
      private final String op;

      /** 
       * Constructor.
       */
      private Validation(String desc, String op){
          this.desc = desc; 
          this.op = op;
      }

     /**
      * Utility method to throw validation errors.
      */
      public void error
      (
        Object o,
        String fieldName, 
        Object v , 
        Object cmp 
      ) throws InvalidMessageException {
        throw new InvalidMessageException(
          String.format("%s.%s : invalid %s value (%s %s %s)",
                     o.getClass().getSimpleName(),
                     fieldName, desc, v, op, cmp));
      }
}
