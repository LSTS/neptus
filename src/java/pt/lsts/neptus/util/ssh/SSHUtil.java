/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Dec 21, 2015
 */
package pt.lsts.neptus.util.ssh;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * @author zp
 *
 */
public class SSHUtil {

    /**
     * Execute a command on a remote SSH server
     * @param host The host where to connect
     * @param port The port where SSH is running
     * @param user The username to use
     * @param password The password to use for authentication
     * @param command The command to be executed remotely
     * @return A Future which will hold the result of the command invocation
     */
    public static Future<String> exec(String host, int port, String user, String password, String command) {
        return Executors.newSingleThreadExecutor().submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Session session = createSession(host, port, user, password);

                ChannelExec channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(command);
                channel.connect();

                BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String msg = null;

                while ((msg = in.readLine()) != null) {
                    sb.append(msg + "\n");
                }

                channel.disconnect();
                session.disconnect();
                return sb.toString();
            }
        });
    }

    /**
     * Retrieve a file from a remote SSH server
     * @param host The host where to connect
     * @param port The port where SSH is running
     * @param user The username to use
     * @param password The password to use for authentication
     * @param localFile Where to store locally the remote file contents
     * @param remoteFile The path to the remote file to be retrieved 
     * @return A Future which will be True on success
     */
    public static Future<Boolean> getFile(String host, int port, String user, String password, String remoteFile,
            File localFile) {
        return Executors.newSingleThreadExecutor().submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Session session = createSession(host, port, user, password);

                ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
                channel.connect();
                FileOutputStream fos = new FileOutputStream(localFile);
                channel.get(remoteFile, fos);
                fos.close();
                channel.disconnect();
                session.disconnect();
                return true;
            }
        });
    }

    /**
     * Send a local file to a remote SSH server
     * @param host The host where to connect
     * @param port The port where SSH is running
     * @param user The username to use
     * @param password The password to use for authentication
     * @param localFile The File to be uploaded
     * @param remoteFile The path to the file where to store remotely
     * @return A Future which will be True on success 
     */
    public static Future<Boolean> putFile(String host, int port, String user, String password, File localFile,
            String remoteFile) {
        return Executors.newSingleThreadExecutor().submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Session session = createSession(host, port, user, password);
                ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
                channel.connect();
                FileInputStream fis = new FileInputStream(localFile);
                channel.put(fis, remoteFile);
                fis.close();
                channel.disconnect();
                session.disconnect();
                return true;
            }
        });
    }
    
    private static Session createSession(String host, int port, String user, String password) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);
        session.setPassword(password);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        return session;
    }
}
