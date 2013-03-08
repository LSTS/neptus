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
 * Base class for messages.
 * @author Eduardo Marques
 */
public abstract class Message implements Cloneable {
	
    /**
     * Get message class flags.
     * @return Flag bitmask.
     */
    public abstract int flags();

    /**
     * Print out message contents.  
     * @param ps print stream to use for print-out.
     */
    public void dump(PrintStream ps){
      new DumpStream(ps).dump(this);
    }

    /**
     * Get sequence id.
     * Unless overriden by concrete implementations, this method 
     * throws an exception of type FeatureNotSupportedException.
     * @return value of sequence id.
     */
    public long sequenceId() throws FeatureNotSupportedException {
      throw new FeatureNotSupportedException("sequence ids are not supported");
    }
    /**
     * Set sequence id for message.
     * Unless overriden by concrete implementations, this method 
     * throws an exception of type FeatureNotSupportedException.
     * @param id sequence id
     */
    public void setSequenceId(long id) throws FeatureNotSupportedException {
      throw new FeatureNotSupportedException("sequence ids are not supported");
    }

    /**
     * Get message timestamp. 
     * Unless overriden by concrete implementations, this method 
     * throws an exception of type FeatureNotSupportedException.
     * @return message timestamp
     */
    public double timestamp() throws FeatureNotSupportedException {
      throw new FeatureNotSupportedException("timestamps are not supported");
    }

    /**
     * Set message timestamp. 
     * Unless overriden by concrete implementations, this method 
     * throws an exception of type FeatureNotSupportedException.
     * @param t message timestamp
     */
    public void setTimestamp(double t) throws FeatureNotSupportedException {
      throw new FeatureNotSupportedException("timestamps are not supported");
    }
    
    /**
     * Abstract method to validate messages.
     * The validate() method throws InvalidMessageException when
     * the message is invalid.
     */
    public abstract void validate() throws InvalidMessageException;

    /**
     * Get serialization id for message.
     * @return serialization id for message 
     */
    public abstract int serialId();

    /**
     * Get serialization size for message.
     * @return serialization size for message 
     */
    public abstract int serialSize();

    /**
     * Serialize message payload.
     * @param buffer Output buffer
     * @throws InvalidMessageException if the message is not valid
     */
    public abstract void serialize(Buffer buffer) 
    throws InvalidMessageException;

    /**
     * Unserialize message payload.
     * @param buffer Input buffer
     * @throws InvalidMessageException if the unserialized message is not valid
     */
    public abstract void unserialize(Buffer buffer) 
    throws InvalidMessageException;

    /**
     * Get field names.
     * @return array of field abbreviated names in order of definition.
     */
    public abstract String[] fields();

    /**
     * Get field values.
     * @return array of field values in order of definition.
     */
    public abstract Object[] values();

    /**
     * Get array of field values in definition order and timestamp, if any,
     * relative to Epoch time supplied as argument.     
     * @param basetime Base Epoch time.
     * @return array of field values.
     */
    public abstract Object[] values(double basetime);

    /**
     * Get field types.
     * @return array of field types in order of definition.
     */
    public abstract Object[] types();

    /**
    /**
     * Get abbreviated name of message.
     * @return Abbreviated name of message.
     */
    public final String name() {
        return getClass().getSimpleName();
    }

    /**
     * Get long descriptive name of message.
     * @return Long name of message.
     */
    public abstract String longName();

    /**
     * copy() implementation. This is equivalent to calling
     * (Message) msg.clone() on an object msg of type message.
     * but without having to handle CloneNotSupportedException. 
     * 
     * If CloneNotSupportedException is thrown by clone, a RuntimeException
     * with CloneNotSupportedException as cause is thrown instead.
     *
     * This is a final method. Sub-classes should override clone() to 
     * provide deep-copy semantics if they contain 'rawdata' fields.
     *
     * @return copy of the message
     */
    public final Message copy() {
        try {
            return (Message) clone();
        } 
        catch(CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}

