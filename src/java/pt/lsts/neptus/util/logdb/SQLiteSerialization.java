/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 2009/10/09
 */
package pt.lsts.neptus.util.logdb;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCMessageType;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 * 
 */
public class SQLiteSerialization {

	private static final String driver = "org.sqlite.JDBC";
	private static DateFormat day = new SimpleDateFormat("yyyyMMdd");
	private static DateFormat timeOfDay = new SimpleDateFormat("HHmmss");
	private Connection conn = null;
	private PreparedStatement newHeader, updateHeaderMsgId;
	private long commitTime = System.currentTimeMillis();
	private static SQLiteSerialization sessionDb = null;
	private String filename = null;
	
	public synchronized static SQLiteSerialization getDb() {
		if (sessionDb == null)
			sessionDb = connect();
		return sessionDb;
	}
	
	private static final String DB_TEMPLATE_FILENAME = "conf/sqlite_template.db";

	protected LinkedHashMap<Integer, PreparedStatement> messageInserts = new LinkedHashMap<Integer, PreparedStatement>();

	static {
		try {
			Class.forName(driver);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a new SQLite DB and connects to it
	 * @return A new SQLiteSerialization object, associated with a newly created DB
	 */
	public static SQLiteSerialization connect() {
		return connect(getNewDbFilename());
	}

	/**
	 * Connect to an existing DB
	 * @param filename The filename of the SQLite database data. If this file does not exist, a new DB is created in that file.
	 * @return A new SQLiteSerialization object, associated with the DB in the given filename
	 */
	public static SQLiteSerialization connect(String filename) {
		try {
			return new SQLiteSerialization(filename);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}		
	}

	
	/**
	 * Fetch the last message of some type
	 * @param messageType The IMC type of the message to be returned
	 * @return the message of the given type with highest timestamp in the DB 
	 * @throws SQLException
	 */
	public IMCMessage getLatestMessageOfType(int messageType) throws SQLException {
		IMCMessage msg = null;
		IMCMessageType type = new IMCMessage(messageType).getMessageType();
		
		String sql = "select * from "+type.getShortName()+" join (select * from IMC_MessageHeader where IMC_MessageType_id = "+messageType+
		" order by IMC_MessageHeader_timestamp desc limit 1) using (IMC_MessageHeader_id);";
		
		synchronized (conn) {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(sql);
			
			if(rs.next()) {
				try {
                    msg = IMCDefinition.getInstance().create(type.getShortName());
                }
                catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
				msg.setTimestamp(rs.getLong("IMC_MessageHeader_timestamp"));
				
				for (String fieldName : type.getFieldNames()) {
					msg.setValue(fieldName, extractValue(type.getFieldType(fieldName).getTypeName(), fieldName, rs));
				}												
			}
			rs.close();
		}
		
		return msg;
		//IMCMessage[] msgs = getLastN(messageType, 1);
		//return (msgs.length > 0)? msgs[0] : null;
	}
	
	/**
	 * Fetch the last message of some type
	 * @param messageType The abbreviated name of the message to be returned
	 * @return the message of the given type with highest timestamp in the DB 
	 * @throws SQLException
	 */
	public IMCMessage getLastMessageOfType(String messageAbbrev) throws SQLException {
		return getLastMessageOfType(IMCDefinition.getInstance().getMessageId(messageAbbrev));
	}
	
	/**
	 * Fetch the last message of some type
	 * @param messageType The IMC type of the message to be returned
	 * @return the message of the given type with highest timestamp in the DB 
	 * @throws SQLException
	 */
	public IMCMessage getLastMessageOfType(int messageType) throws SQLException {
		IMCMessageType type = new IMCMessage(messageType).getMessageType();
		IMCMessage msg = null;
		
		String sql = "select * from (select * from "+type.getShortName()+" order by rowid desc limit 1) join IMC_MessageHeader using (IMC_MessageHeader_id)";
		
		//String sql = "select * from (select * from IMC_MessageHeader order by IMC_MessageHeader_timestamp desc limit 1) join "+type.getShortName()+" using (IMC_MessageHeader_id)";
		
		synchronized (conn) {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(sql);
			
			if(rs.next()) {
				msg = new IMCMessage(type.getShortName());
				msg.setTimestamp(rs.getLong("IMC_MessageHeader_timestamp"));
				
				for (String fieldName : type.getFieldNames()) {
					msg.setValue(fieldName, extractValue(type.getFieldType(fieldName).getTypeName(), fieldName, rs));
				}												
			}
			rs.close();
		}
		
		return msg;
		//IMCMessage[] msgs = getLastN(messageType, 1);
		//return (msgs.length > 0)? msgs[0] : null;
	}
		
	/**
	 * Stores the given message in the Database
	 * @param msg The message to be serialized onto the DB
	 * @return The row id of the Header associated with this message 
	 * @throws SQLException 
	 */
	public long store(IMCMessage msg) throws SQLException {
		long href = storeHeader(msg.getHeader());

        PreparedStatement stmt = getMessageInsertStatement(msg.getMgid());

			if (stmt == null)
			return href;

		stmt.setLong(1, href); // header
		int i = 2;
		IMCMessageType messageType = msg.getMessageType();
		
		for (String fieldName : messageType.getFieldNames()) {
		    String fieldType = messageType.getFieldType(fieldName).getTypeName();
			if (fieldType.contains("int"))
				stmt.setLong(i, msg.getLong(fieldName));
			else if (fieldType.contains("fp"))
				stmt.setDouble(i, msg.getDouble(fieldName));
			else if (fieldType.equals("plaintext"))
				stmt.setString(i, msg.getString(fieldName));
			else if (fieldType.equals("rawdata")) {
				stmt.setBlob(i, new ByteArrayInputStream(msg.getRawData(fieldName)));
			} else if (fieldType.equals("message")) {
//				IMCMessage inline = msg.getMessage(fieldName);
				long mref = -1;
				//FIXME this is commented because generates a deadlock...
				//if (inline != null)
				//	mref = store(inline);
				stmt.setLong(i, mref);
			} else {
				NeptusLog.pub().error("Field type " + fieldType
						+ " not recognized");
			}
			i++;
		}
		long msg_ref = -1;
		
		synchronized (conn) {
			stmt.executeUpdate();
			ResultSet rs = stmt.getGeneratedKeys();
			msg_ref = rs.getLong(1);
			rs.close();			
		}
		updateHeaderMsgId.setLong(1, msg_ref);
		updateHeaderMsgId.setLong(2, href);
		
		synchronized (conn) {
			updateHeaderMsgId.executeUpdate();
		}
		
		if (System.currentTimeMillis() - commitTime > 1000)
			commit();
		return href;
	}

		
	/**
	 * Commit any uncommited transactions to the database
	 * @throws SQLException
	 */
	protected void commit() throws SQLException {
		commitTime = System.currentTimeMillis();
		
		synchronized (conn) {
			//NeptusLog.pub().info("<###>commiting db...");
			conn.commit();
		}		
	}

	private SQLiteSerialization(String filename) throws SQLException {

		if (!new File(filename).canRead()) {
			FileUtil.copyFile(DB_TEMPLATE_FILENAME, filename);
		}

		conn = DriverManager.getConnection("jdbc:sqlite:" + filename);
		conn.setAutoCommit(false);

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				if (conn != null) {
					try {
						System.out.print("closing db connection...");
						synchronized (conn) {
							conn.close();
						}
						NeptusLog.pub().info("<###>Done.");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}));

		synchronized (conn) {
			newHeader = 
				conn.prepareStatement("INSERT INTO IMC_MessageHeader (IMC_MessageHeader_timestamp,IMC_Specification_id,IMC_MessageType_id,IMC_MessageHeader_src,IMC_MessageHeader_dst) VALUES (?, ?, ?, ?, ?)");
			
			updateHeaderMsgId = 
				conn.prepareStatement("UPDATE IMC_MessageHeader set IMC_Message_id = ? where IMC_MessageHeader_id = ?;");	
		}		
		this.filename = filename;
	}
	
	private static String getNewDbFilename() {
		Date startDate = new Date();

		String filename = "log/db/" + day.format(startDate) + "/"
				+ timeOfDay.format(startDate);
		new File(filename).mkdirs();
		return filename + "/sqlite.db";
	}	

	protected PreparedStatement getMessageInsertStatement(int msgType)
			throws SQLException {

		if (messageInserts.containsKey(msgType))
			return messageInserts.get(msgType);

		IMCMessageType type = IMCDefinition.getInstance()
				.getType(msgType);

		String secondPart = ") values (?";

		String sql = "insert into " + type.getShortName()
				+ " (IMC_MessageHeader_id";
		for (String fieldName : type.getFieldNames()) {
			sql += ",`" + fieldName + "`";
			secondPart += ",?";
		}
		sql += secondPart + ");";

		PreparedStatement stmt = conn.prepareStatement(sql);
		messageInserts.put(msgType, stmt);
		return stmt;
	}


	
	public static Object extractValue(String imcType, String column, ResultSet rs) throws SQLException {
		if (imcType.contains("int"))
			return rs.getLong(column);
		else if (imcType.contains("fp"))
			return rs.getDouble(column);
		else if (imcType.equals("plaintext"))
			return rs.getString(column);
		else if (imcType.equals("rawdata")) {
			Blob b = rs.getBlob(column);
			return b.getBytes(1, (int)b.length());
		}
		else if (imcType.equals("message")) {
			//TODO testing
			return /* new IMCMessage(); */ null;
		} else {
			return rs.getObject(column);
		}
	}

	protected long storeHeader(IMCMessage header) throws SQLException {

		int src = header.getInteger("src");
		int dst = header.getInteger("src");
		
		newHeader.setDouble(1, (double) header.getTimestamp() / 1000.0);
		newHeader.setInt(2, 0);
		newHeader.setInt(3, header.getMgid());
		newHeader.setInt(4, src);
		newHeader.setInt(5, dst);

		synchronized (conn) {
			newHeader.executeUpdate();
		}
		ResultSet rs = newHeader.getGeneratedKeys();
		long l = rs.getLong(1);
		rs.close();
		if (System.currentTimeMillis()-commitTime>1000)
			commit();		
		return l;
	}
	
	/**
	 * @return the conn
	 */
	public Connection getConn() {
		return conn;
	}

	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	public static void main(String[] args) throws SQLException {
		ConfigFetch.initialize();
		final SQLiteSerialization ser = SQLiteSerialization.connect("myDb.db");

		NeptusLog.pub().info("<###> "+System.currentTimeMillis());
		
		LinkedHashMap<Double, Long> all = new LinkedHashMap<Double, Long>();
		
		synchronized (ser.conn) {
			Statement st = ser.conn.createStatement();
			ResultSet rs = st.executeQuery("select IMC_Message_id, IMC_MessageHeader_timestamp from IMC_MessageHeader where IMC_MessageType_id = 3");
			while(rs.next()) {
				all.put(rs.getDouble(2), rs.getLong(1));
			}
			rs.close();			
		}
		NeptusLog.pub().info("<###> "+all.size());
		NeptusLog.pub().info("<###> "+System.currentTimeMillis());
		//for (double d : all.keySet()) {
		//	NeptusLog.pub().info("<###> "+d+"="+all.get(d));
		//}
		//NeptusLog.pub().info("<###> "+all.size());
		
		IMCMessage msg;
		
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						
						Thread.sleep(1000);
						for (int i = 0; i < 10; i++) {
							IMCMessage m = ser.getLastMessageOfType(3);
							NeptusLog.pub().info("<###> "+m.getValue("x"));
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
		});
		t.start();
		
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						
						Thread.sleep(1202);
						for (int i = 0; i < 10; i++) {
							IMCMessage m = ser.getLastMessageOfType(3);
							NeptusLog.pub().info("<###> "+m.getValue("x"));
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
		});
		t2.start();
		for (int i = 100000; i > 0; i--) {
		    msg = null;
			try {
                msg = IMCDefinition.getInstance().create("EstimatedState", "x",i);
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
			System.out.flush();
			NeptusLog.pub().info("<###> "+i);
			ser.store(msg);		
		}

		System.exit(0);
	}
}
