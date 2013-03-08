// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 

package pt.up.fe.dceg.messages;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

/**
 * Message protocol abstract class.
 * Each message protocol as specified by the LSTS XML specification format
 * and generated through XSLT has its own. 
 * It defines factory methods to instantiate messages, message headers 
 * and footer, plus some global meta-information facilities.
 * @author Eduardo Marques
 */
public abstract class Protocol<T extends Message> {

    /**
     * Get protocol name.
     * @return the protocol id.
     */
    public abstract String name(); 

    /**
     * Get protocol version.
     * @return the protocol version identifier.
     */
    public abstract String version();

    /**
     * Message creation by serial id.
     * @param id Message serial id
     * @return new message instance 
     * @throws InvalidMessageException when the id supplied is invalid
     */
    public abstract T newMessage(int id) throws InvalidMessageException;


    /**
     * Message creation by name.
     * @param name Message name
     * @return new message instance 
     * @throws InvalidMessageException when the name supplied is invalid
     */
    public abstract T newMessage(String name)throws InvalidMessageException;


    /**
     * Get message name corresponding to an id.
     * @param id message id 
     * @return (abbreviated) message name
     */
    public abstract String messageName(int id) throws InvalidMessageException;
      
    /**
     * Get message id corresponding to a name.
     * @param name (abbreviated) message name
     * @return message id 
     */
    public abstract int messageId(String name) throws InvalidMessageException;

    /**
     * Method to retrieve all classes for the message facility.
     * @return an array of references to all the classes ordered by their
     * definition in the XML config file.
     */
    public abstract Collection<Class<? extends T>> getMessageClasses();

    /**
     * Method to retrieve all class names for the message facility.
     * @return an array of references to all the classes ordered by their
     * definition in the XML config file.
     */
    public abstract Collection<String> getMessageNames();

    /**
     * Method to retrieve the number of messages defined for the message facility.
     * @return the message count 
     */
    public abstract int getMessageCount();

    /**
     * Copy general attributes from one message to another.
     * This is normally used in conjunction inline message serialization
     * or "payload header" fields.
     * On exit the message instance 'dst' should have the same 
     * general attributes as 'src'.
     * @param src source message.
     * @param dst source message.
     */
    public abstract void copyAttributes(T src, T dst);

    /**
     * Get total serialization size for a message, including any header and footer required.
     * @param m Message.
     * @return Number of bytes required for the full serialization of m. 
     */
    public abstract int serializationSize(T m);

    /**
     * Main serialization method to be implemented by an instance.
     * @param m message to serialize
     * @param b serialization buffer
     */ 
    public abstract void 
    serialize(T m, Buffer b) throws InvalidMessageException;

    /**
     * Serialize a message onto an array of bytes.
     * @param m message to serialize
     * @param b serialization buffer 
     */ 
    public final void 
    serialize(T m, byte[] data) throws InvalidMessageException {
      serialize(m, new Buffer(data));
    }

    /**
     * Get serialized form of message.
     * @param m message to serialize
     * @return message serialized onto an array of bytes
     */ 
    byte[] serialize(T m) throws InvalidMessageException {
      byte[] b = new byte[serializationSize(m)];
      serialize(m, new Buffer(b));
      return b;
    }

    /**
     * Serialize a message onto a stream.
     * @param os output stream
     */
    public final void serialize(T m, OutputStream os) 
    throws InvalidMessageException, IOException {
      os.write(serialize(m));
    }

    /**
     * Unserialize a message from a stream.
     * @param is input stream
     */
    public abstract T unserialize(InputStream is) throws InvalidMessageException, IOException;
 
    /**
     * Main unserialization method to be implemented by an instance.
     * @param m message (null if message type is not fixed)
     * @param b serialization buffer
     * @return message unserialized (equal to m if m is not null on entry)
     */ 
    public abstract T 
    unserialize(T m, Buffer b) throws InvalidMessageException;

    public final void unserialize(T m, byte[] b) throws InvalidMessageException {
      unserialize(m, new Buffer(b));
    }

    public final T unserialize(byte[] b) throws InvalidMessageException {
      return unserialize(null, new Buffer(b));
    }

    public final T unserialize(Buffer b) throws InvalidMessageException {
      return unserialize(null, b);
    }
}
