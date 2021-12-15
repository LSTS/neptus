package pt.lsts.neptus.comm.transports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.CommUtil;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.comm.transports.DeliveryListener;
import pt.lsts.neptus.comm.transports.DeliveryListener.ResultEnum;
import pt.lsts.neptus.comm.transports.ImcDtlsTransport;
import pt.lsts.neptus.comm.transports.udp.UDPMessageListener;
import pt.lsts.neptus.comm.transports.udp.UDPNotification;
import pt.lsts.neptus.comm.transports.dtls.DTLSTransport;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageInfoImpl;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.util.conf.ConfigFetch;

public class ImcDtlsTransport {

   private LinkedHashSet<MessageListener<MessageInfo, IMCMessage>> listeners = new LinkedHashSet<MessageListener<MessageInfo, IMCMessage>>();
   private IMCDefinition imcDefinition;

   private DTLSTransport dtlsTransport = null;
   private String inetAddress = null;


   public ImcDtlsTransport(IMCDefinition imcDefinition, String inetAddress) {
       this.imcDefinition = imcDefinition;
       this.inetAddress = inetAddress;
       getDtlsTransport();
       setUDPListener();
   }

    /**
     *
     */
    private void setUDPListener() {
        getDtlsTransport().addListener(new UDPMessageListener() {
            @Override
            public void onUDPMessageNotification(UDPNotification req) {
                //ByteUtil.dumpAsHex(req.getBuffer(),System.out);
//					long start = System.nanoTime();
                IMCMessage msg;

//                if(req.getBuffer().length == 0){
//                    return;
//                }

                try {
                    msg = imcDefinition.parseMessage(req.getBuffer());
                }
                catch (IOException e) {
                    NeptusLog.pub().warn(e.getMessage()+" while unpacking message sent from " + req.getAddress().getHostString());
                    return;
                }

                MessageInfo info = new MessageInfoImpl();
                info.setPublisher(req.getAddress().getAddress().getHostAddress());
                info.setPublisherInetAddress(req.getAddress().getAddress().getHostAddress());
                info.setPublisherPort(req.getAddress().getPort());
                info.setTimeReceivedNanos(req.getTimeMillis() * (long)1E6);
                info.setTimeSentNanos((long)msg.getTimestamp() * (long)1E9);
                info.setProperty(MessageInfo.TRANSPORT_MSG_KEY, "UDP");
                for (MessageListener<MessageInfo, IMCMessage> lst : listeners) {
                    try {
                        //req.getMessage().dump(System.err);
                        lst.onMessage(info , msg);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                    }
                    catch (Error e) {
                        NeptusLog.pub().error(e);
                    }
                }
            }
        });
    }


    /**
     * @return the dtlsTransport
     */
    public DTLSTransport getDtlsTransport() {
        if (dtlsTransport == null) {
            dtlsTransport = new DTLSTransport(1, inetAddress);
        }
        return dtlsTransport;
    }

    /**
     * @param listener
     * @return
     */
    public boolean addListener(MessageListener<MessageInfo, IMCMessage> listener) {
        boolean ret = false;
        synchronized (listeners) {
            ret = listeners.add(listener);
        }
        return ret;
    }

    /**
     * @param listener
     * @return
     */
    public boolean removeListener(
            MessageListener<MessageInfo, IMCMessage> listener) {
        boolean ret = false;
        synchronized (listeners) {
            ret = listeners.remove(listener);
        }
        return ret;
    }

    /**
     * @param message
     */
    public boolean sendMessage(IMCMessage message) {
        return sendMessage(message, null);
    }

    public boolean sendMessage(final IMCMessage message,
                               final MessageDeliveryListener deliveryListener) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IMCOutputStream imcOs = new IMCOutputStream(baos);

        try {
            message.serialize(imcOs);
            DeliveryListener listener = null;
            if (deliveryListener != null) {
                listener = new DeliveryListener() {
                    @Override
                    public void deliveryResult(ResultEnum result, Exception error) {
                        switch (result) {
                            case Success:
                                deliveryListener.deliveryUncertain(message, new Exception("Message delivered via UDP"));
                                break;
                            case Error:
                                deliveryListener.deliveryError(message, error);
                                break;
                            case TimeOut:
                                deliveryListener.deliveryTimeOut(message);
                                break;
                            case Unreacheable:
                                deliveryListener.deliveryUnreacheable(message);
                                break;
                            default:
                                deliveryListener.deliveryError(message, new Exception("Delivery "
                                        + ResultEnum.UnFinished));
                                break;
                        }
                    }
                };
            }
            boolean ret = getDtlsTransport().sendMessage(baos.toByteArray(), listener);
//            message.dump(System.err);
//            if (message.getAbbrev().equalsIgnoreCase("LblConfig")) {
//                NeptusLog.pub().info("<###> sissssssssssss" + baos.toByteArray().length);
//                ByteUtil.dumpAsHex(message.getAbbrev(), baos.toByteArray(), System.out);
//            }
            if (!ret) {
                if (deliveryListener != null) {
                    deliveryListener.deliveryError(message, new Exception("Delivery "
                            + ResultEnum.UnFinished + " due to closing transport!"));
                }
            }
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            NeptusLog.pub().error(e);
            if (deliveryListener != null) {
                deliveryListener.deliveryError(message, e);
            }
            return false;
        }
    }

}
