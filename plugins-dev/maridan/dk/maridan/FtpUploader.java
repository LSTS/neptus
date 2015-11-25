/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Nov 25, 2015
 */
package dk.maridan;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * Utility class for FTP uploading.
 * Example usage: 
<pre>
new FtpUploader("localhost", 21)
            .login("username", "password")
            .setRemoteFile("pub/myfile.txt")
            .upload("file contents")
            .disconnect();
</pre>
 * @author zp
 */
public class FtpUploader {

    private FTPClient client;
    private String remoteFileName = "file.txt";
    
    /**
     * Class constructor
     * @param hostname The FTP server host name
     * @param port The FTP server port
     * @throws Exception In case of connection errors with remote server
     */
    public FtpUploader(String hostname, int port) throws Exception {
        client = new FTPClient();
        client.connect(hostname, port);
        if (!FTPReply.isPositiveCompletion(client.getReplyCode()))
            throw new Exception("Error connecting to server: " + client.getReplyString());
    }
    
    /**
     * Login with user / password
     * @param username The username
     * @param password The password
     * @return Current instance for method chaining
     * @throws Exception In case login gives errors
     */
    public FtpUploader login(String username, String password) throws Exception {
        client.login(username, password);
        if (!FTPReply.isPositiveCompletion(client.getReplyCode()))
            throw new Exception("Error logging in: " + client.getReplyString());
        return this;
    }
    
    /**
     * Set the path of the remote file
     * @param pathname The absolute path of the remote file (including filename) 
     * @return Current instance for method chaining
     * @throws Exception In case the path does not exists or the user has no permissions
     */
    public FtpUploader setRemoteFile(String pathname) throws Exception {
        this.remoteFileName = pathname;
        int lastSlash = pathname.lastIndexOf("/");
        if (lastSlash != -1) {
            client.cwd(pathname.substring(0, lastSlash));
            if (!FTPReply.isPositiveCompletion(client.getReplyCode()))
                throw new Exception("Error changing working directory: " + client.getReplyString());
            remoteFileName = pathname.substring(lastSlash + 1);
        }
        return this;
    }
    
    /**
     * Upload file contents to a remote file
     * @param fileContents The contents to be written to the remote file
     * @return Current instance for method chaining
     * @throws Exception In case there is a connection error during the upload
     */
    public FtpUploader upload(InputStream fileContents) throws Exception {
        client.deleteFile(remoteFileName);
        client.storeFile(remoteFileName, fileContents);
        if (!FTPReply.isPositiveCompletion(client.getReplyCode()))
            throw new Exception("Error sending file contents: " + client.getReplyString());
        return this;
    }
    
    /**
     * Upload text file contents to remote file
     * @param contents The text contents to be stored as a String
     * @return Current instance for method chaining
     * @throws Exception In case there is a connection error during the upload
     */
    public FtpUploader upload(String contents) throws Exception {
        return upload(new ByteArrayInputStream(contents.getBytes()));        
    }
    
    /**
     * Disconnect from remote server
     * @throws Exception In case there is an error during disconnect
     */
    public void disconnect() throws Exception {
        client.disconnect();
    }
}
