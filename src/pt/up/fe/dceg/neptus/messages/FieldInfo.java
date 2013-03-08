// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 

package pt.up.fe.dceg.neptus.messages;

/**
 * Class to encode meta-information for a message field.
 * @author Eduardo Marques
 */

public class FieldInfo {
    /**
     * Field name
     */
    private String name;
    /**
     * Long field name
     */
    private String longName;
    /**
     * Type 
     */
    private String type;
    /**
     * Units
     */
    private String units;
    /**
     * Width of field (for serialization types like 'char')
     */
    private int width;

   /**
    * Flag indicating that field is part of "payload header"
    */
   private boolean payloadHeaderFlag;
        
    /**
     * Constructor, must be called with mandatory fields.
     * Optional fields may be set with other methods.
     * @param name field name
     * @param longName long field name
     * @param type field type
     */
    public FieldInfo(String name, String longName, String type){
        this.name = name;
        this.longName = longName;
        this.type = type;
        this.units = "None";
        this.width = -1;
        this.payloadHeaderFlag = false;
    }

    /**
     * Get the field's name.
     * @return The field's name.
     */
    public String getName(){ 
        return name;
    }
    /**
     * Get the field's long name.
     * @return The field's long name.
     */
    public String getLongName(){ 
        return longName;
    }
    /**
     * Get the field's type.
     * @return The field's long name.
     */
    public String getType(){ 
        return type; 
    }
    /**
     * Get the field's measurement unit.
     * @return The field's unit.
     */
    public String getUnits(){ 
        return units; 
    }

    /**
     * Get the payload header flag.
     * @return true or false.
     */
    public boolean getPayloadHeaderFlag(){ 
      return payloadHeaderFlag;
    }

    /**
     * Set the payload header flag.
     * @param enable true or false.
     */
    public void setPayloadHeaderFlag(boolean enable ){ 
      payloadHeaderFlag = enable;
    }
    /**
     * Set the field's measurement unit.
     * @param units The field's unit.
     */
    public void setUnits(String units){ 
        this.units = units;
    }

    /**
     * Get the field's width.
     * @return The field's width.
     */
    public int getWidth(){ 
        return width;
    }

    /**
     * Set the field's width.
     * @param width The field's width.
     */
    public void setWidth(int width){ 
        this.width = width;
    }
}
