// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 

package pt.lsts.neptus.messages;

public class InvalidFieldValueException extends Exception 
{
  private static final long serialVersionUID = 3980287650427141196L;

  public InvalidFieldValueException(String message) 
  {
     super(message);
  }
    
  public InvalidFieldValueException(Exception cause)
  {
    super(cause);
  }
}
