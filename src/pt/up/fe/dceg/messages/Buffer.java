// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 


package pt.up.fe.dceg.messages;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * Buffer class for serialization.
 * @author Eduardo Marques
 */
public class Buffer {
    /**
     * Charset decoder.
     * For the moment ISO-8859-1 is assumed to be the only valid charset.
     * Note that Java uses UTF-8 by default and accents take 2 chars
     * in UTF-8.
     */
    private static CharsetDecoder csDec 
      = Charset.forName("ISO-8859-1").newDecoder();

    /**
     * Charset encoder, fixed to ISO-8859-1.
     */
    private static CharsetEncoder csEnc = Charset.forName("ISO-8859-1")
            .newEncoder();

    /**
     * Internal ByteBuffer instance, can be accessed by subclasses.
     */
    protected ByteBuffer buf;

    /**
     * Constructor.
     * @param data the source data
     */
    public Buffer(byte[] data){
      this(data, data.length);
    }

    /** 
     * Constructor.
     * @param lenght Length of buffer.
     */ 
    public Buffer(int length){
      this(new byte[length], length);
    }

    /**
     * Constructor from a data byte array.
     * @param data the source data
     * @param len the source data length
     */
    public Buffer(byte[] data, int len){
        buf = ByteBuffer.wrap(data, 0, len);
    }

    /**
     * Get data array.
     * @return the byte array 
     */
    public final byte[] getData(){
        return buf.array();
    } 

    /**
     * Get buffer position. 
     * @return buffer position.
     */
    public final int position(){
      return buf.position();
    }

    /**
     * Get number of available bytes in buffer.
     * @return number of available bytes.
     */
    public final int available(){
      return buf.remaining(); 
    }

    /**
     * Resize the buffer. 
     * @param newsz new buffer size
     */
    public final void resize(int newsz){
      int sz = buf.capacity();
      if(sz < newsz){
        byte[] data = new byte[newsz];
        ByteBuffer newb = ByteBuffer.wrap(data);
        newb.position(buf.position());
        newb.order(buf.order());
        System.arraycopy(buf.array(), 0, data, 0, sz);
        buf = newb;
      }
    }
    /**
     * Rewind buffer to starting position.
     */
    public final void rewind(){ 
      buf.rewind(); 
    }

    /**
     * Change byte order in use.
     * @param order the byte order to use (little endian or big endian)
     */
    public final void setByteOrder(ByteOrder order){
        buf.order(order);
    }

    /**
     * Set buffer position.
     * @param pos position
     */
    public void position(int pos){
      buf.position(pos);
    }

    /**
     * Advance buffer position forward or backward.
     * @param pos offset
     */
    public void advance(int off){
      buf.position(buf.position() + off); 
    }


    /**
     * Swap byte order in use.
     */
    public final void swapByteOrder(){
      if(buf.order() == ByteOrder.LITTLE_ENDIAN)
        buf.order(ByteOrder.BIG_ENDIAN);
      else
        buf.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Get byte order in use (big endian or little endian).
     * @return byte order in use.
     **/
    public final ByteOrder getByteOrder(){
        return buf.order();
    }

    /**
     * Read a int8_t field.
     * @return value read from buffer.
     */
    public short readINT8() 
    {
        return buf.get();
    }

    /**
     * Read a uint8_t field.
     * @return value read from buffer.
     */
    public short readUINT8()  
    {
        byte b = buf.get();

        return (b >= 0 ? 
               (short) b : 
               (short) (Constants.MAX_UINT8 + 1 + (short) b));
    }

    /**
     * Read a int16_t field.
     * @return value read from buffer.
     */
    public short readINT16() 
    {
        return buf.getShort();
    }

    /**
     * Read a uint16_t field.
     * @return value read from buffer.
     */
    public int readUINT16()  
    {
        return buf.getChar();
    }

    /**
     * Read a int32_t field.
     * @return value read from buffer.
     */
    public int readINT32() 
    {
        return buf.getInt(); 
    }
    /**
     * Read a uint32_t field.
     * @return value read from buffer.
     */
    public long readUINT32()  
    {
        int i32 = buf.getInt();
        return i32 >= 0 ? 
          (long) i32 : (long) (Constants.MAX_UINT32 + 1) + (long) i32;
    }

    /**
     * Read a int64_t field.
     * @return value read from buffer.
     */
    public long readINT64() 
    {
        return buf.getLong(); 
    }

    /**
     * Read a uint64_t field.
     * Known issue: 
     * current implementation handles it the same way as int64_t for now.
     * Use of "big int" classes is avoided, at least for now.
     * @return value read from buffer.
     */
    public long readUINT64()  
    {
      return buf.getLong();
    }
    /**
     * Read a fp32_t value.
     * @return value read from buffer.
     */
    public float readFP32()  
    {
      return buf.getFloat();
    }
    /**
     * Read a fp64_t value.
     * @return value read from buffer.
     */
    public double readFP64()  
    {
        return buf.getDouble();
    }
    /**
     * Read a rawdata value.
     * @return value read from buffer.
     */
    public byte[] readRawData()  
    {
      int len = readUINT16();
      byte[] data = null;
      if(len > 0) buf.get(data = new byte[len], 0, len);
      return data;
    }

    /**
     * Read a string value.
     * @return value read from buffer.
     */
    private String readString(int length)
    {
      int l;
      byte[] bdata = new byte[length];
      buf.get(bdata);

      for(l = 0;l<length && bdata[l] != 0;l++) {}

      if (l == 0) return ""; //Empty String

      CharBuffer cb = CharBuffer.wrap(new char[l]);
      csDec.decode(ByteBuffer.wrap(bdata,0,l), cb, false);
      cb.rewind();
      return cb.toString();
    }
    /**
     * Read a plaintext value.
     * @return value read from buffer.
     */
    public String readPlainText()  
    {
      int length = readUINT16();
      return length>0 ? readString(length) : "";
    }

    /**
     * Read an inline message.
     * @param protocol protocol handle.
     * @param parent message in scope.
     * @return value read from buffer.
     */
    public <T extends Message> 
    T readMessage(Protocol<T> protocol, T parent) 
    throws InvalidMessageException {
      T msg = null;
      int id = readUINT16();

      if(id != Constants.NULL_SERIAL_ID){
        msg = protocol.newMessage(id);
        protocol.copyAttributes(parent, msg);
        msg.unserialize(this);
      }
      return msg;
    }

    /**
     * Write a int8_t value.
     * @param i8 value to write.
     */
    public void writeINT8(short i8) 
    {
      buf.put((byte)(i8&0x00FF));
    }
    /**
     * Write a uint8_t value.
     * @param u8 value to write.
     */
    public void writeUINT8(short u8) 
    {
      buf.put((byte)(u8 & 0x00FF));
    }

    /**
     * Write a int16_t value.
     * @param i16 value to write.
     */
    public void writeINT16(short i16) 
    {
      buf.putShort(i16);
    }

    /**
     * Write a uint16_t value.
     * @param u16 value to write.
     */
    public void writeUINT16(int u16)  
    {
      buf.putChar((char)u16);
    }

    /**
     * Write a int32_t value.
     * @param i32 value to write.
     */
    public void writeINT32(int i32) 
    {
      buf.putInt(i32);
    }
    /**
     * Write a uint32_t value.
     * @param u32 value to write.
     */
    public void writeUINT32(long u32)  
    {
      buf.putInt((int)u32 & 0xFFFFFFFF);
    }

    /**
     * Write a int64_t value.
     * @param i64 value to write.
     */
    public void writeINT64(long i64) 
    {
      buf.putLong(i64);
    }

    /**
     * Write a uint64_t value.
     * Note that currently uint64_t values are handled the same way as int64_t, 
     * so there is no true support for uint64_t.
     * @param u64 value to write.
     */
    public void writeUINT64(long u64)  
    {
      buf.putLong(u64);
    }

    /**
     * Write a fp32_t value.
     * @param f32 value to write.
     */
    public void writeFP32(float f32)  
    {
      buf.putFloat(f32);
    }

    /**
     * Write a fp64_t value.
     * @param f64 value to write.
     */
    public void writeFP64(double f64)  
    {
      buf.putDouble(f64);
    }
    
    /**
     * Write a rawdata value.
     * @param data value to write.
     */
    public void writeRawData(byte[] data) 
    {
      if(data == null)
      {
        writeUINT16(0);
      }
      else 
      {
        writeUINT16(data.length);
        buf.put(data);
      }
    }

    /**
     * Write a string.
     * @param text value to write.
     */
    private void writeString(String text)
    {
      csEnc.encode(CharBuffer.wrap(text), buf, false );    
    }

    /**
     * Write a plaintext value.
     * @param text value to write.
     */
    public void writePlainText(String text)  
    {
      int length = text == null ? 0 : text.length();
      writeUINT16(length);
      if(length > 0) writeString(text);
    }

    /** 
     * Write an inline message.
     * @param msg message to write.
     */
    public void writeMessage(Message msg) throws InvalidMessageException {
      if(msg != null){
        writeUINT16(msg.serialId());
        msg.serialize(this);
      } else {
        writeUINT16(Constants.NULL_SERIAL_ID);
      }
    }
}
