/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.util.comm.ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;


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

				// System.out.println("filesize="+filesize+", file="+file);

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
			// TODO: handle exception
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
		System.out.println(ret + " " + scw.getExitStatus());
		ret = scw.execScpFrom("~/testeHD.txt", "t.txt");
		System.out.println(ret + " " + scw.getExitStatus());
	}

}
