// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 


package pt.up.fe.dceg.messages;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * FIFO message queue with non-blocking access.
 * 
 * It acts as a wrapper for java.util.concurrent.ConcurrentLinkedQueue
 * that uses an efficient "wait-free/lock-free" algorithm.
 *
 * @author Eduardo Marques
 */
public class Queue<T extends Message> {
    //! The actual queue.
    private final 
    ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<T>();

    //! Constructor.
    public Queue(){ }

    //! Add a message to the queue. 
    //! @param msg Message to queue.
    public void add(T msg){
      queue.add(msg);
    } 

    //! Get a message from the queue.
    //! @return a message, or null if queue is empty.
    public T remove(){ return queue.poll(); }
    
    //! Get size of the queue.
    //! Note that this operation is not performed in constant time 
    //! due to the behavior of java.util.concurrent.ConcurrentLinkedQueue.
    //! @return queue size.
    public int size(){ return queue.size(); }

    //! Check if queue is empty.
    //! Note that this operation is not performed in constant time 
    //! due to the behavior of java.util.concurrent.ConcurrentLinkedQueue.
    //! @return 'true' if queue is empty.
    public boolean empty(){ return size() == 0; }
}
