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
//       setUDPListener();
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

}
