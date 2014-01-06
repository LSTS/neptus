/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.comm.ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.conf.ConfigFetch;


public class SSHScp
extends SSHExec
{
	
	public SSHScp(String vehicleId)
	{
		super(vehicleId);
	}

	
	
	public boolean execScpTo (String lfile, String rfile)
    {
		if (!prepareChannel())
		{
			sessionCleanup();
			return false;
		}

		if (!getStreams())
		{
			sessionCleanup();
			return false;
		}

		// exec 'scp -t rfile' remotely
		String command="scp -p -t " + rfile;
		setCommand(command);

		
		if (!connectChannel())
		{
			sessionCleanup();
			return false;
		}

		if (waitForAckInt(in) != 0) 
		{
			channelCleanup();
			sessionCleanup();
			return false;
		}

		// send "C0644 filesize filename", where filename should not include '/'
		long filesize=(new File(lfile)).length();
		command="C0644 "+filesize+" ";
		if(lfile.lastIndexOf('/') > 0){
			command+=lfile.substring(lfile.lastIndexOf('/')+1);
		}
		else if(lfile.lastIndexOf('\\') > 0){
			command+=lfile.substring(lfile.lastIndexOf('\\')+1);
		}
		else{
			command+=lfile;	
		}
		command+="\n";
		try {
			out.write(command.getBytes());
			out.flush();
		} catch (Exception e) {
		    NeptusLog.pub().error(e.getStackTrace());
			channelCleanup();
			sessionCleanup();
			return false;
		}		

		if (waitForAckInt(in) != 0)
		{
			channelCleanup();
			sessionCleanup();
			return false;
		}

		try {
			// send a content of lfile
			FileInputStream fis = new FileInputStream(lfile);
			byte[] buf = new byte[1024];
			while (true) {
				int len = fis.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len); //out.flush();
			}
			fis.close();
			fis = null;
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
		} catch (Exception e) {
		    NeptusLog.pub().error(e.getStackTrace());
			channelCleanup();
			sessionCleanup();
			return false;
		}		

		if (waitForAckInt(in) != 0)
		{
			channelCleanup();
			sessionCleanup();
			return false;
		}

		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		exitStatus = channel.getExitStatus();
        channel.disconnect();
        session.disconnect();
		return (exitStatus <= 0)?true:false;
    }


	public boolean execScpFrom (String rfile, String lfile)
    {
		String prefix = null;
		if (new File(lfile).isDirectory()) {
			prefix = lfile + File.separator;
		}

		if (!prepareChannel())
		{
			sessionCleanup();
			return false;
		}

		if (!getStreams())
		{
			sessionCleanup();
			return false;
		}

		// exec 'scp -t rfile' remotely
		String command="scp -f " + rfile;
		setCommand(command);

		
		if (!connectChannel())
		{
			sessionCleanup();
			return false;
		}
		/*if (!waitForAck(in))
		{
			channelCleanup();
			sessionCleanup();
			return false;
		}*/

		byte[] buf = new byte[1024];

		try {
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
		} catch (Exception e) {
		    NeptusLog.pub().error(e.getStackTrace());
			channelCleanup();
			sessionCleanup();
			return false;
		}
		
		try {
			while (true) {
				int c = checkAck(in);
				if (c != 'C') { //'C' = 67
					break;
				}
				// read '0644 '
				in.read(buf, 0, 5);

				long filesize = 0L;
				while (true) {
					if (in.read(buf, 0, 1) < 0) {
						// error
						break;
					}
					if (buf[0] == ' ')
						break;
					filesize = filesize * 10L + (long) (buf[0] - '0');
				}

				String file = null;
				for (int i = 0;; i++) {
					in.read(buf, i, 1);
					if (buf[i] == (byte) 0x0a) {
						file = new String(buf, 0, i);
						break;
					}
				}

				// NeptusLog.pub().info("<###>filesize="+filesize+", file="+file);

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();

				// read a content of lfile
				FileOutputStream fos = new FileOutputStream(
						prefix == null ? lfile : prefix + file);
				int foo;
				while (true) {
					if (buf.length < filesize)
						foo = buf.length;
					else
						foo = (int) filesize;
					foo = in.read(buf, 0, foo);
					if (foo < 0) {
						// error
						break;
					}
					fos.write(buf, 0, foo);
					filesize -= foo;
					if (filesize == 0L)
						break;
				}
				fos.close();
				fos = null;

				if (checkAck(in) != 0) 
				{
					channelCleanup();
					sessionCleanup();
					return false;
				}

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
			}
		} catch (Exception e) {
		    NeptusLog.pub().error(e.getStackTrace());
		}		
		exitStatus = channel.getExitStatus();
        channel.disconnect();
        session.disconnect();
		return (exitStatus <= 0)?true:false;
    }

	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		ConfigFetch.initialize();
		SSHScp scw = new SSHScp("lauv");
		boolean ret = scw.execScpTo("neptus-config.xml", "~/");
		NeptusLog.pub().info("<###> "+ret + " " + scw.getExitStatus());
		ret = scw.execScpFrom("~/testeHD.txt", "t.txt");
		NeptusLog.pub().info("<###> "+ret + " " + scw.getExitStatus());
	}

}
