// ---------------------------------------------------------------------------- 
//  Copyright (C) 2009 All Rights Reserved
//  Laboratório de Sistemas e Tecnologia Subaquática
//  Departamento de Engenharia Electrotécnica e de Computadores
//  Rua Dr. Roberto Frias, 4200-465 Porto, Portugal
//  http://whale.fe.up.pt
// ---------------------------------------------------------------------------- 


package pt.up.fe.dceg.messages;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A publish/subscribe message board.
 *
 * The PS board is thread-safe and lock-free upon message delivery 
 * by appropriate use of java.util.concurrent.ConcurrentHashMap and java.util.concurrent.ConcurrentLinkedList, and the Queue class.
 *
 * The board works under the following principles:
 * - subscribe(): allows callers to subscribe to messages 
 *   published to the board, by indicating the queue instance
 *   in which they intend to receive the messages.
 * - publish(): publishes a message to the board, causing 
 *   subscriber queues to be updated with a message.
 * 
 * The copy-on-delivery flag may be enabled so that messages are cloned 
 * on a per subscriber basis, so that each gets a distinct instance, 
 * upon delivery. 
 *
 * @author Eduardo Marques
 */
public final class Board<T extends Message> {

   /** 
    * Copy on delivery flag.
    */
   private final boolean copyOnDelivery;

   /**
    * Subscriptions per message.
    */
   private final
   ConcurrentHashMap<Class<? extends T>, ConcurrentLinkedQueue<Queue<T>>> 
     subscriptions = new ConcurrentHashMap<Class<? extends T>,ConcurrentLinkedQueue<Queue<T>>>();

   /**
    * Subscriptions for all messages.
    */
   private final 
   ConcurrentLinkedQueue<Queue<T>> subscriptionsA 
     = new ConcurrentLinkedQueue<Queue<T>>();

   /**
    * Constructor with no arguments.
    * Copy-on-deliverey message flag will be disabled by default.
    */
   public Board(){
     copyOnDelivery = false;
   }

   /**
    * Constructor with argument specifying copy-on-delivery setting.
    * @param enableCOD Set to 'true' to enable copy-on-delivery.
    */
   public Board(boolean enableCOD){
     copyOnDelivery = enableCOD;
   }

   /**
    * Subscribe to a particular type of message, indicating 
    * the queue for reception.
    * Note that all type-based subscriptions are synchronized, but
    * this fact that does not affect the core aspect of lock-free 
    * based message delivery, nor does it interrupt on-going deliveries.
    * @param clazz Class identifying message type. 
    * @param q Subscriber queue.
    */
   public final void subscribe(Class<? extends T> clazz, Queue<T> q){
     synchronized(subscriptions){
        ConcurrentLinkedQueue<Queue<T>> list = subscriptions.get(clazz);
        if(list == null){
          list = new ConcurrentLinkedQueue<Queue<T>>(); 
          subscriptions.put(clazz, list); 
        }
        list.add(q);
     }
   }

   /**
    * Subscribe to all messages published to the board,  indicating 
    * the queue for reception.
    * @param q Subscriber queue.
    */
   public final void subscribe(Queue<T> q){
     subscriptionsA.add(q); 
   }

   /** 
    * Indicate that the existing message queue should no longer be used
    * for general message subscription. 
    * If 'q' is not subscribing, request will be ignored without error.
    * @param q Queue.
    */
   public final void unsubscribe(Queue<T> q){
     subscriptionsA.remove(q);
   }

   /** 
    * Indicate that the existing message queue should no longer be used
    * for type-based message subscription. 
    * If 'q' is not subscribing to 'clazz' request will be ignored without error.
    * @param clazz Message class.
    * @param q Queue.
    */
   public final void unsubscribe(Class<? extends T> clazz, Queue<T> q){
     ConcurrentLinkedQueue<Queue<T>> list = subscriptions.get(clazz);
     if(list != null)
       list.remove(q);
   }

   /**
    * Publish a message onto the board.
    * All queues that subscribe to the type of the message will  
    * receive the message or a distinct copy of the message 
    * when copy-on-delivery is enabled.
    * @param msg Message to publish.
    */
   @SuppressWarnings("unchecked")
   public final void publish(T msg){
     ConcurrentLinkedQueue<Queue<T>> list = subscriptions.get(msg.getClass());

     // Type-based subscribers
     for(Queue<T> q : list){
       q.add(copyOnDelivery ? (T) msg.copy() : msg);
     } 

     // General subscribers
     for(Queue<T> q: subscriptionsA){
       q.add(copyOnDelivery ? (T) msg.copy() : msg);
     }
   }
}
