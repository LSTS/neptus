package pt.lsts.neptus.plugins.uavparameters.connection;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_statustext;

/**
 * Provides support for mavlink connection via TCP.
 */
public class TcpConnection {

	private static int CONNECTION_TIMEOUT = 20 * 1000;
	private static int CONNECTION_NUM_OF_RETRIES = 3;
	private Socket socket;
	private BufferedOutputStream mavOut;
	private BufferedInputStream mavIn;

	private String addr;
	private int port;

	public TcpConnection(String ip_addr, int port) {
		this.addr = ip_addr;
		this.port = port;
	}
	
	public void openConnection() throws IOException {
		getTCPStream();
	}

	public int readDataBlock(byte[] buffer) throws IOException {
		return mavIn.read(buffer);
	}

	public void sendBuffer(byte[] buffer) throws IOException {
		if (mavOut != null) {
			mavOut.write(buffer);
			mavOut.flush();
		}
	}

	public String getAddress() {
		return addr;
	}

	public int getPort() {
		return port;
	}

	public void closeConnection() throws IOException {
		if (socket != null)
			socket.close();
		
		System.out.println("Disconnected!");
	}

	private void getTCPStream() throws IOException {
		InetAddress serverAddr = InetAddress.getByName(addr);
		socket = new Socket();
		socket.connect(new InetSocketAddress(serverAddr, port), CONNECTION_TIMEOUT);

		mavOut = new BufferedOutputStream((socket.getOutputStream()));
		mavIn = new BufferedInputStream(socket.getInputStream());
		
		if (socket.isConnected())
			System.out.println("Connected!");
	}
	
	public static void main(String argv[]) throws Exception {

        TcpConnection con = new TcpConnection("10.0.20.125", 9999);
        con.openConnection();

        byte[] buf = new byte[1024];

        boolean run = true;
        while (run) {
            MAVLinkPacket packet;
            Parser parser = new Parser();

            int a = con.readDataBlock(buf);

            for (byte c : buf) {
                packet = parser.mavlink_parse_char(c & 0xFF);
                if(packet != null){
                    //System.out.println(packet.msgid + " " + packet.sysid);
                    MAVLinkMessage msg = (MAVLinkMessage) packet.unpack();
                    System.out.println(msg);
                    if (msg != null) {
                        if (msg.msgid == msg_statustext.MAVLINK_MSG_ID_STATUSTEXT) {
                            System.out.println(msg.toString());
                            msg_statustext status = (msg_statustext) msg;
                            System.out.println(status.toString());
                        }
                    }
                }  
            }
        }
    }
}