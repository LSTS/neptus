/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * Jun 4, 2010
 */
package pt.lsts.neptus.util.lsf.gz;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import pt.lsts.neptus.NeptusLog;

/**
 * An input stream which reads sequentially from multiple sources.
 * More information about this class is available from <a target="_top" href=
 * "http://ostermiller.org/utils/">ostermiller.org</a>.
 *
 * @author Stephen Ostermiller http://ostermiller.org/contact.pl?regarding=Java+Utilities
 * @since ostermillerutils 1.04.00
 */
public class ConcatInputStream extends InputStream {


    /**
     * Current index to inputStreamQueue
     *
     * @since ostermillerutils 1.04.01
     */
    private int inputStreamQueueIndex = 0;

    /**
     * Queue of inputStreams that have yet to be read from.
     *
     * @since ostermillerutils 1.04.01
     */
    private ArrayList<InputStream> inputStreamQueue = new ArrayList<InputStream>();

    /**
     * A cache of the current inputStream from the inputStreamQueue
     * to avoid unneeded access to the queue which must
     * be synchronized.
     *
     * @since ostermillerutils 1.04.01
     */
    private InputStream currentInputStream = null;

    /**
     * true iff the client may add more inputStreams.
     *
     * @since ostermillerutils 1.04.01
     */
    private boolean doneAddingInputStreams = false;

    /**
     * Causes the addInputStream method to throw IllegalStateException
     * and read() methods to return -1 (end of stream)
     * when there is no more available data.
     * <p>
     * Calling this method when this class is no longer accepting
     * more inputStreams has no effect.
     *
     * @since ostermillerutils 1.04.01
     */
    public void lastInputStreamAdded(){
        doneAddingInputStreams = true;
    }

    /**
     * Add the given inputStream to the queue of inputStreams from which to
     * concatenate data.
     *
     * @param in InputStream to add to the concatenation.
     * @throws IllegalStateException if more inputStreams can't be added because lastInputStreamAdded() has been called, close() has been called, or a constructor with inputStream parameters was used.
     *
     * @since ostermillerutils 1.04.01
     */
    public void addInputStream(InputStream in){
        synchronized(inputStreamQueue){
            if (in == null) throw new NullPointerException();
            if (closed) throw new IllegalStateException("ConcatInputStream has been closed");
            if (doneAddingInputStreams) throw new IllegalStateException("Cannot add more inputStreams - the last inputStream has already been added.");
            inputStreamQueue.add(in);
        }
    }

    /**
     * Add the given inputStream to the queue of inputStreams from which to
     * concatenate data.
     *
     * @param in InputStream to add to the concatenation.
     * @throws IllegalStateException if more inputStreams can't be added because lastInputStreamAdded() has been called, close() has been called, or a constructor with inputStream parameters was used.
     * @throws NullPointerException the array of inputStreams, or any of the contents is null.
     *
     * @since ostermillerutils 1.04.01
     */
    public void addInputStreams(InputStream[] in){
        for (InputStream element: in) {
            addInputStream(element);
        }
    }

    /**
     * Gets the current inputStream, looking at the next
     * one in the list if the current one is null.
     *
     * @since ostermillerutils 1.04.01
     */
    private InputStream getCurrentInputStream(){
        if (currentInputStream == null && inputStreamQueueIndex < inputStreamQueue.size()){
            synchronized(inputStreamQueue){
                // inputStream queue index is advanced only by the nextInputStream()
                // method.  Don't do it here.
                currentInputStream = inputStreamQueue.get(inputStreamQueueIndex);
            }
        }
        return currentInputStream;
    }

    /**
     * Indicate that we are done with the current inputStream and we should
     * advance to the next inputStream.
     *
     * @since ostermillerutils 1.04.01
     */
    private void advanceToNextInputStream(){
        currentInputStream = null;
        inputStreamQueueIndex++;
    }

    /**
     * True iff this the close() method has been called on this stream.
     *
     * @since ostermillerutils 1.04.00
     */
    private boolean closed = false;


    /**
     * Create a new input stream that can dynamically accept new sources.
     * <p>
     * New sources should be added using the addInputStream() method.
     * When all sources have been added the lastInputStreamAdded() should
     * be called so that read methods can return -1 (end of stream).
     * <p>
     * Adding new sources may by interleaved with read calls.
     *
     * @since ostermillerutils 1.04.01
     */
    public ConcatInputStream(){
        // Empty constructor
    }

    /**
     * Create a new InputStream with one source.
     *
     * @param in InputStream to use as a source.
     *
     * @throws NullPointerException if in is null
     *
     * @since ostermillerutils 1.04.00
     */
    public ConcatInputStream(InputStream in){
        addInputStream(in);
        lastInputStreamAdded();
    }

    /**
     * Create a new InputStream with two sources.
     *
     * @param in1 first InputStream to use as a source.
     * @param in2 second InputStream to use as a source.
     *
     * @throws NullPointerException if either source is null.
     *
     * @since ostermillerutils 1.04.00
     */
    public ConcatInputStream(InputStream in1, InputStream in2){
        addInputStream(in1);
        addInputStream(in2);
        lastInputStreamAdded();
    }

    /**
     * Create a new InputStream with an arbitrary number of sources.
     *
     * @param in InputStreams to use as a sources.
     *
     * @throws NullPointerException if the input array on any element is null.
     *
     * @since ostermillerutils 1.04.00
     */
    public ConcatInputStream(InputStream[] in){
        addInputStreams(in);
        lastInputStreamAdded();
    }

    /**
     * Reads the next byte of data from the underlying streams. The value byte is
     * returned as an int in the range 0 to 255. If no byte is available because
     * the end of the stream has been reached, the value -1 is returned. This method
     * blocks until input data is available, the end of the stream is detected, or
     * an exception is thrown.
     * <p>
     * If this class in not done accepting inputstreams and the end of the last known
     * stream is reached, this method will block forever unless another thread
     * adds an inputstream or interrupts.
     *
     * @return the next byte of data, or -1 if the end of the stream is reached.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override public int read() throws IOException {
        if (closed) throw new IOException("InputStream closed");
        int r = -1;
        while (r == -1){
            InputStream in = getCurrentInputStream();
            if (in == null){
                if (doneAddingInputStreams) return -1;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException iox){
                    throw new IOException("Interrupted");
                }
            } else {
                r = in.read();
                if (r == -1) advanceToNextInputStream();
            }
        }
        return r;
    }

    /**
     * Reads some number of bytes from the underlying streams and stores them into
     * the buffer array b. The number of bytes actually read is returned as an
     * integer. This method blocks until input data is available, end of file is
     * detected, or an exception is thrown.
     * <p>
     * If the length of b is zero,
     * then no bytes are read and 0 is returned; otherwise, there is an attempt
     * to read at least one byte.
     * <p>
     * The read(b) method for class InputStream has the same effect as:<br>
     * read(b, 0, b.length)
     * <p>
     * If this class in not done accepting inputstreams and the end of the last known
     * stream is reached, this method will block forever unless another thread
     * adds an inputstream or interrupts.
     *
     * @param b - Destination buffer
     * @return The number of bytes read, or -1 if the end of the stream has been reached
     *
     * @throws IOException - If an I/O error occurs
     * @throws NullPointerException - If b is null.
     *
     * @since ostermillerutils 1.04.00
     */
    @Override public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads up to length bytes of data from the underlying streams into an array of bytes.
     * An attempt is made to read as many as length bytes, but a smaller number may be read,
     * possibly zero. The number of bytes actually read is returned as an integer.
     * <p>
     * If length is zero,
     * then no bytes are read and 0 is returned; otherwise, there is an attempt
     * to read at least one byte.
     * <p>
     * This method blocks until input data is available
     * <p>
     * If this class in not done accepting inputstreams and the end of the last known
     * stream is reached, this method will block forever unless another thread
     * adds an inputstream or interrupts.
     *
     * @param b Destination buffer
     * @param off Offset at which to start storing bytes
     * @param len Maximum number of bytes to read
     * @return The number of bytes read, or -1 if the end of the stream has been reached
     *
     * @throws IOException - If an I/O error occurs
     * @throws NullPointerException - If b is null.
     * @throws IndexOutOfBoundsException - if length or offset are not possible.
     */
    @Override public int read(byte[] b, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > b.length) throw new IllegalArgumentException();
        if (closed) throw new IOException("InputStream closed");
        int r = -1;
        while (r == -1){
            InputStream in = getCurrentInputStream();
            if (in == null){
                if (doneAddingInputStreams) return -1;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException iox){
                    throw new IOException("Interrupted");
                }
            } else {
                r = in.read(b, off, len);
                if (r == -1) advanceToNextInputStream();
            }
        }
        return r;
    }

    /**
     * Skips over and discards n bytes of data from this input stream. The skip method
     * may, for a variety of reasons, end up skipping over some smaller number of bytes,
     * possibly 0. This may result from any of a number of conditions; reaching end of
     * file before n bytes have been skipped is only one possibility. The actual number
     * of bytes skipped is returned. If n is negative, no bytes are skipped.
     * <p>
     * If this class in not done accepting inputstreams and the end of the last known
     * stream is reached, this method will block forever unless another thread
     * adds an inputstream or interrupts.
     *
     * @param n he number of characters to skip
     * @return The number of characters actually skipped
     *
     * @throws IOException If an I/O error occurs
     *
     * @since ostermillerutils 1.04.00
     */
    @Override public long skip(long n) throws IOException {
        if (closed) throw new IOException("InputStream closed");
        if (n <= 0) return 0;
        long s = -1;
        while (s <= 0){
            InputStream in = getCurrentInputStream();
            if (in == null){
                if (doneAddingInputStreams) return 0;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException iox){
                    throw new IOException("Interrupted");
                }
            } else {
                s = in.skip(n);
                // When nothing was skipped it is a bit of a puzzle.
                // The most common cause is that the end of the underlying
                // stream was reached.  In which case calling skip on it
                // will always return zero.  If somebody were calling skip
                // until it skipped everything they needed, there would
                // be an infinite loop if we were to return zero here.
                // If we get zero, let us try to read one character so
                // we can see if we are at the end of the stream.  If so,
                // we will move to the next.
                if (s <= 0) {
                    // read() will advance to the next stream for us, so don't do it again
                    s = ((read()==-1)?-1:1);
                }
            }

        }
        return s;
    }

    /**
     * Returns the number of bytes that can be read (or skipped over) from this input
     * stream without blocking by the next caller of a method for this input stream.
     * The next caller might be the same thread or or another thread.
     *
     * @throws IOException If an I/O error occurs
     *
     * @since ostermillerutils 1.04.00
     */
    @Override public int available() throws IOException {
        if (closed) throw new IOException("InputStream closed");
        InputStream in = getCurrentInputStream();
        if (in == null) return 0;
        return in.available();
    }

    /**
     * Closes this input stream and releases any system resources associated with the stream.
     *
     * @since ostermillerutils 1.04.00
     */
    @Override public void close() throws IOException {
        if (closed) return;
        for (Object element: inputStreamQueue) {
            ((InputStream)element).close();
        }
        closed = true;
    }

    /**
     * Mark not supported
     *
     * @since ostermillerutils 1.04.00
     */
    @Override public void mark(int readlimit){
        // Mark not supported -- do nothing
    }

    /**
     * Reset not supported.
     *
     * @throws IOException because reset is not supported.
     *
     * @since ostermillerutils 1.04.00
     */
    @Override public void reset() throws IOException {
        throw new IOException("Reset not supported");
    }

    /**
     * Does not support mark.
     *
     * @return false
     *
     * @since ostermillerutils 1.04.00
     */
    @Override public boolean markSupported(){
        return false;
    }

    public static void main(String[] args)  throws Exception {
        ConcatInputStream cis = new ConcatInputStream();
        cis.addInputStream(new FileInputStream("/home/zp/Desktop/mra-cmap/160924_motor_limit_half/Cache.lsf"));
        cis.addInputStream(new FileInputStream("/home/zp/Desktop/mra-cmap/160924_motor_limit_half/Data.lsf"));
        cis.lastInputStreamAdded();

        byte[] buff = new byte[255];

        while(cis.read(buff, 0, 255) != -1)
            NeptusLog.pub().info("<###>read");
        NeptusLog.pub().info("<###>finito");
        cis.close();
    }
}
