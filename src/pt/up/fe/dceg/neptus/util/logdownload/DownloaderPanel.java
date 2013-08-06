/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 2009/09/12
 */
package pt.up.fe.dceg.neptus.util.logdownload;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.GlossPainter;
import org.jdesktop.swingx.painter.RectanglePainter;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.ftp.FtpDownloader;
import pt.up.fe.dceg.neptus.gui.MiniButton;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.MathMiscUtils;
import pt.up.fe.dceg.neptus.util.MovingAverage;
import pt.up.fe.dceg.neptus.util.StreamUtil;
import pt.up.fe.dceg.neptus.util.conf.GeneralPreferences;
import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;

/**
 * @author pdias
 *
 */
@SuppressWarnings({"serial","unused"})
public class DownloaderPanel extends JXPanel implements ActionListener {

    private static boolean debug = false;
    
	public static final ImageIcon ICON_STOP = ImageUtils.getIcon("images/stop.png");
	public static final ImageIcon ICON_DOWNLOAD = ImageUtils.getIcon("images/restart.png");

//    private static final Color COLOR_BACK = new Color(242, 251, 254);
//    private static final Color COLOR_FRONT = Color.GRAY;

    private static final Color COLOR_IDLE = new JXPanel().getBackground();
    private static final Color COLOR_DONE = new Color(140, 255, 170);
    private static final Color COLOR_NOT_DONE = new Color(255, 210, 140);
    private static final Color COLOR_TIMEOUT = new Color(173, 154, 79);
    private static final Color COLOR_ERROR = new Color(255, 100, 100);
    private static final Color COLOR_WORKING = new Color(190, 220, 240); // blue

	public static final String ACTION_STOP = "Stop";
	public static final String ACTION_DOWNLOAD = "Download";
	
	public static enum State {IDLE, WORKING, DONE, ERROR, NOT_DONE, TIMEOUT};
	
	private State state = State.IDLE;
	
	private DownloadStateListener stateListener = null;
	
	private boolean usePartialDownload = true;
	
	private String name = "";
	private String uri = "";
	private File outFile = null;

	private long startTimeMillis = -1;
	private long endTimeMillis = -1;
	
	private long downloadedSize = 0;
	private long fullSize = -1;

    private FtpDownloader client = null;
    private FTPFile ftpFile;
    
    private boolean isDirectory = false;
    
    private long doneFilesForDirectory = 0;
    
    private InputStream stream; // Generic stream
    private boolean stopping = false;

	//UI
	private JXLabel infoLabel = null;
	private JProgressBar progressBar = null;
	//private JXLabel progressLabel = null;
	private JXLabel msgLabel = null;
	private MiniButton stopButton = null;
	private MiniButton downloadButton = null;
	
	public DownloaderPanel() {
		initialize();
	}

	
	/**
	 * @param client
	 * @param name
	 * @param uri
	 */
	public DownloaderPanel(FtpDownloader client, FTPFile ftpFile, String uri, File outFile) {
	    this();
		this.client = client;
		this.ftpFile = ftpFile;
		this.name = ftpFile.getName();
		this.uri = uri;
		this.outFile = outFile;
		
		this.isDirectory = ftpFile.isDirectory();
		getInfoLabel().setText(name + " (" + uri + ")");
	}

	/**
	 * 
	 */
	private void initialize() {
//	    try {
//            usePartialDownload = GeneralPreferences.getPropertyBoolean(GeneralPreferences.LOGS_DOWNLOADER_ENABLE_PARTIAL_DOWNLOAD);
//        }
//        catch (GeneralPreferencesException e) {
//            e.printStackTrace();
//            usePartialDownload = true;
//        }
	    usePartialDownload = GeneralPreferences.logsDownloaderEnablePartialDownload;
	    
		//this.setBackground(Color.WHITE);
		this.setPreferredSize(new Dimension(400, 75));
		//this.setBackgroundPainter(new CompoundPainter<JXPanel>(new GlossPainter()));
		updateBackColor(this.getBackground());
		
		this.setBorder(new EmptyBorder(10, 10, 10, 10));
        GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		
		layout.setAutoCreateGaps(false);
        layout.setAutoCreateContainerGaps(false);
        
        layout.setHorizontalGroup(
    		layout.createParallelGroup(GroupLayout.Alignment.CENTER)
    			.addComponent(getInfoLabel())
    			.addGroup(
    				layout.createSequentialGroup()
    					.addComponent(getProgressBar())
    					.addGap(5)
    					.addComponent(getDownloadButton(), (int) getProgressBar().getPreferredSize().getHeight(), (int) getProgressBar().getPreferredSize().getHeight(),(int) getProgressBar().getPreferredSize().getHeight())
    					.addComponent(getStopButton(), (int) getProgressBar().getPreferredSize().getHeight(), (int) getProgressBar().getPreferredSize().getHeight(),(int) getProgressBar().getPreferredSize().getHeight())
    			)
    			//.addComponent(getProgressLabel())
    			.addComponent(getMsgLabel())
		);

        layout.setVerticalGroup(
       		layout.createSequentialGroup()
    			.addComponent(getInfoLabel())
    			.addGroup(
    				layout.createParallelGroup(GroupLayout.Alignment.CENTER)
    					.addComponent(getProgressBar())
    					.addComponent(getDownloadButton())
    					.addComponent(getStopButton())
    			)
    			//.addComponent(getProgressLabel())
    			.addComponent(getMsgLabel())
		);
        
        layout.linkSize(SwingConstants.VERTICAL, getProgressBar(), getStopButton(),
        		getDownloadButton());
	}

	//Background Painter Stuff
	private RectanglePainter rectPainter;
	private CompoundPainter<JXPanel> compoundBackPainter;
	/**
	 * @return the rectPainter
	 */
	private RectanglePainter getRectPainter() {
		if (rectPainter == null) {
	        rectPainter = new RectanglePainter(5,5,5,5, 10,10);
	        rectPainter.setFillPaint(COLOR_IDLE);
	        rectPainter.setBorderPaint(COLOR_IDLE.darker().darker().darker());
	        rectPainter.setStyle(RectanglePainter.Style.BOTH);
	        rectPainter.setBorderWidth(2);
	        rectPainter.setAntialiasing(true);//RectanglePainter.Antialiasing.On);
		}
		return rectPainter;
	}
	/**
	 * @return the compoundBackPainter
	 */
	private CompoundPainter<JXPanel> getCompoundBackPainter() {
		compoundBackPainter = new CompoundPainter<JXPanel>(
					//new MattePainter(Color.BLACK),
					getRectPainter(), new GlossPainter());
		return compoundBackPainter;
	}
	/**
	 * @param color
	 */
	private void updateBackColor(Color color) {
		getRectPainter().setFillPaint(color);
		getRectPainter().setBorderPaint(color.darker().darker().darker());

		this.setBackgroundPainter(getCompoundBackPainter());
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#toString()
	 */
	@Override
	public String toString() {
		return name;
	}
	
	/* (non-Javadoc)
	 * @see java.awt.Component#getName()
	 */
	@Override
	public String getName() {
		return name;
	}
	
	/**
	 * @return the infoLabel
	 */
	private JXLabel getInfoLabel() {
		if (infoLabel == null) {
			infoLabel = new JXLabel(I18n.text("Downloading..."));
		}
		return infoLabel;
	}

	/**
	 * @return the msgLabel
	 */
	private JXLabel getMsgLabel() {
		if (msgLabel == null) {
			msgLabel = new JXLabel("");
		}
		return msgLabel;
	}
	
	/**
	 * @return the progressBar
	 */
	private JProgressBar getProgressBar() {
		if (progressBar == null) {
			progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
			progressBar.setIndeterminate(false);
			progressBar.setStringPainted(true);
			//progressBar.setBackground(COLOR_BACK);
			//progressBar.setForeground(COLOR_FRONT);
		}
		return progressBar;
	}
	
	/**
	 * @return the miniButton
	 */
	private MiniButton getStopButton() {
		if (stopButton == null) {
			stopButton = new MiniButton();
			stopButton.setIcon(ICON_STOP);
			stopButton.addActionListener(this);
			stopButton.setActionCommand(ACTION_STOP);
			stopButton.setEnabled(false);
		}
		return stopButton;
	}

	/**
	 * @return the miniButton
	 */
	private MiniButton getDownloadButton() {
		if (downloadButton == null) {
			downloadButton = new MiniButton();
			downloadButton.setIcon(ICON_DOWNLOAD);
			downloadButton.addActionListener(this);
			downloadButton.setActionCommand(ACTION_DOWNLOAD);
			downloadButton.setEnabled(true);
		}
		return downloadButton;
	}

	/**
	 * @return the state
	 */
	public State getState() {
		return state;
	}

	/**
	 * @param state
	 */
	private void setState(State state) {
		State oldState = this.state;
		this.state = state;
		if (state == State.WORKING) {
			//this.setBackground(COLOR_WORKING);
			updateBackColor(COLOR_WORKING);
			getStopButton().setEnabled(true);
			getDownloadButton().setEnabled(false);
		}
		else if (state == State.ERROR) {
			//this.setBackground(COLOR_ERROR);
			updateBackColor(COLOR_ERROR);
			getStopButton().setEnabled(false);
			getDownloadButton().setEnabled(true);
		}
		else if (state == State.DONE) {
			//this.setBackground(COLOR_DONE);
			updateBackColor(COLOR_DONE);
			getStopButton().setEnabled(false);
			getDownloadButton().setEnabled(true);
		}
		else if (state == State.NOT_DONE) {
			//this.setBackground(COLOR_NOT_DONE);
			updateBackColor(COLOR_NOT_DONE);
			getStopButton().setEnabled(false);
			getDownloadButton().setEnabled(true);
		}
        else if (state == State.TIMEOUT) {
            updateBackColor(COLOR_TIMEOUT);
            getStopButton().setEnabled(true);
            getDownloadButton().setEnabled(true);
        }
		else {
			//this.setBackground(COLOR_IDLE);
			updateBackColor(COLOR_IDLE);
			getStopButton().setEnabled(false);
			getDownloadButton().setEnabled(true);
		}
		
		warnStateListener(this.state, oldState);
	}
	
	private void setStateIdle() {
		setState(State.IDLE);
	}

	private void setStateWorking() {
		setState(State.WORKING);
	}

	private void setStateError() {
		setState(State.ERROR);
	}

	private void setStateDone() {
		setState(State.DONE);
	}

	private void setStateNotDone() {
		setState(State.NOT_DONE);
	}

	private void setStateTimeout() {
        setState(State.TIMEOUT);
    }	
	
	/**
	 * @param newState
	 * @param oldState
	 */
	private void warnStateListener(DownloaderPanel.State newState,
			DownloaderPanel.State oldState) {
		if (stateListener != null)
			stateListener.downloaderStateChange(newState, oldState);
	}
	
	/**
	 * @param stateListener Only one is supported.
	 * @return
	 */
	public boolean addStateChangeListener(DownloadStateListener stateListener) {
		this.stateListener = stateListener;
		return true;
	}

	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		if (ACTION_STOP.equals(action)) {
			doStop();
		}
		else if (ACTION_DOWNLOAD.equals(action)) {
			AsyncWorker.post(new AsyncTask() {
				@Override
				public Object run() throws Exception {
					doDownload();
					return null;
				}
				@Override
				public void finish() {
					
				}
			});
		}
	}

	//FIXME
	public void actionDownload () {
		new Thread() {
			@Override
			public void run() {
				if(!isDirectory)
				    doDownload();
				else
				    doDownloadDirectory();
			}
		}.start();
	}
	
	/**
	 * 
	 */
	public void actionStop() {
		new Thread() {
			@Override
			public void run() {
				doStop();
			}
		}.start();
	}

	protected boolean doDownload() {
	    if(isDirectory)
            return doDownloadDirectory();
	    
	    
	    System.out.println("Downloading '" + name + "' from '" + uri + "' to " + outFile.getAbsolutePath());
		if (getState() == State.WORKING)
			return false;
		
		if (!client.getClient().isConnected()) {
		    try {
                client.renewClient();
            }
            catch (Exception e) {
                e.printStackTrace();
                setStateError();
            }
		}
		
		State prevState = getState();
		long begByte = 0;
		if (usePartialDownload && prevState != State.DONE && outFile.exists() && outFile.isFile() && !isDirectory) {
		    begByte = outFile.length();
		    System.out.println("!begin byte: " + begByte);
		}
		setStateWorking();
		
		boolean isOnTimeout = false;
		
		try {
			getProgressBar().setValue(0);
			getProgressBar().setString(begByte == 0 ? I18n.text("Starting...") : I18n.text("Resuming..."));
			getMsgLabel().setText("");
			startTimeMillis = System.currentTimeMillis();
			getInfoLabel().setText(name + " (" + uri + ")");
			
			if (begByte > 0) {
                downloadedSize = begByte;
                client.getClient().setRestartOffset(begByte);
                System.out.println("using resume");
            }
			
			// System.out.println("FTP Client is connected " + client.getClient().isConnected());
			stream = client.getClient().retrieveFileStream(new String(uri.getBytes(), "ISO-8859-1"));

			fullSize = ftpFile.getSize();

			outFile.getParentFile().mkdirs();
			
			try {
				outFile.createNewFile();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}

			FilterDownloadDataMonitor ioS = new FilterDownloadDataMonitor(stream);
			boolean streamRes = StreamUtil.copyStreamToFile(ioS, outFile, begByte == 0 ? false : true);

			if (debug) {
			    NeptusLog.pub().info("<###>To receive / received: " + (begByte > 0 ? fullSize - begByte: fullSize) + "/" + downloadedSize);
			}
			
			endTimeMillis = System.currentTimeMillis();
			ioS.stopDisplayUpdate();

			if (streamRes  && fullSize == downloadedSize) {
                getProgressBar().setString(I18n.textf("%fullSize done (in %time) @%dataRate", 
                        MathMiscUtils.parseToEngineeringRadix2Notation(fullSize, 1) + "B",
                        DateTimeUtil.milliSecondsToFormatedString(endTimeMillis - startTimeMillis),
                        (MathMiscUtils.parseToEngineeringRadix2Notation((begByte > 0 ? downloadedSize - begByte: downloadedSize)
                                / ((endTimeMillis - startTimeMillis) / 1000.0), 1)) + "B/s"));
			}
			else {
			    getProgressBar().setString(I18n.textf("%fullSize partially [%partialSize] done (in %time) @%dataRate", 
			            MathMiscUtils.parseToEngineeringRadix2Notation(fullSize, 1) + "B",
			            MathMiscUtils.parseToEngineeringRadix2Notation(downloadedSize, 1)+ "B",
			            DateTimeUtil.milliSecondsToFormatedString(endTimeMillis - startTimeMillis),
			            (MathMiscUtils.parseToEngineeringRadix2Notation((begByte > 0 ? downloadedSize - begByte: downloadedSize)
			                    / ((endTimeMillis - startTimeMillis) / 1000.0), 1)) + "B/s"));
			}
			if(streamRes && fullSize == downloadedSize) {
				setStateDone();
				// downloadButton.setEnabled(false); //FIXME pdias 20130805 For now disable redownload because the get stream above from client cames null
				// client.getClient().disconnect();
				getMsgLabel().setText(I18n.textf("Saved in '%filePath'", outFile.getAbsolutePath()));
			}
			else {
				setStateNotDone();
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		    if (ex.getMessage() != null && ex.getMessage().startsWith("Timeout waiting for connection")) {
		        isOnTimeout = true;
                getProgressBar().setString(" " + I18n.text("Error:") + " " + ex.getMessage());
                setStateTimeout();
		    }
		    else {
                getProgressBar().setString(
                        I18n.text("Error:") + " "
                                + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
		        setStateError();
		    }
		}
		finally {
            try {
                client.getClient().disconnect();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
		}
		if (isOnTimeout) {
		    new Thread() {
	            @Override
	            public void run() {
	                try { Thread.sleep(8000); } catch (InterruptedException e) { }
	                if (DownloaderPanel.this.getState() == DownloaderPanel.State.TIMEOUT)
	                    doDownload();
	            }
	        }.start();
		}
		return true;
	}
	
	protected boolean doDownloadDirectory() {
	    System.out.println("DOWNLOADING DIRECTORY");
	    System.out.println("Downloading " + name + " " + uri + " " + outFile.getAbsolutePath());

	    String basePath = outFile.getParentFile().getParentFile().getParentFile().getAbsolutePath();
	    
        // Get file list for directory 
        if (getState() == State.WORKING)
            return false;
        
        if (!client.getClient().isConnected()) {
            try {
                client.renewClient();
            }
            catch (Exception e) {
                e.printStackTrace();
                setStateError();
            }
        }

        State prevState = getState();
        
        setStateWorking();
        stopping = false;
        
        boolean isOnTimeout = false;

        if (debug) {
            NeptusLog.pub().info("<###>URI: " + uri);
        }
        
        try {
            LinkedHashMap<String, FTPFile> fileList = client.listDirectory("/" + uri);
            
            System.out.println("Number of FTPFiles in folder: " + fileList.size());

            getProgressBar().setValue(0);
            getProgressBar().setString(I18n.text("Starting..."));
            getMsgLabel().setText("");
            startTimeMillis = System.currentTimeMillis();
            getInfoLabel().setText(name + " (" + uri + ")");
            
            long contentLengh = ftpFile.getSize();
            final long listSize = fileList.size();
            
            getProgressBar().setString(I18n.text("Starting... ") + 
                    (listSize >= 0 ? I18n.textf("%number files", MathMiscUtils.parseToEngineeringRadix2Notation(fullSize,1)) : "unknown files"));
            //msgPanel.writeMessageText("["+MathMiscUtils.parseToEngineeringNotation(cSize,1)+" bytes] ...");

            final Timer t = new Timer(DownloaderPanel.class.getSimpleName() + " :: progress for files for directory " + basePath);
            t.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    getProgressBar().setValue((int) ((doneFilesForDirectory / (float)listSize) * 100));
                    getProgressBar().setString(doneFilesForDirectory + " out of " + listSize);
                    
                    if (state != State.WORKING)
                        t.cancel();
                }
            }, 0, 100);
            
            doneFilesForDirectory = 0;
            for(String key : fileList.keySet()) {
                if(stopping)
                    break;
                
                File out = new File(basePath + "/" + key);
                
                if(out.exists() && fileList.get(key).getSize() == out.length()) {
                    doneFilesForDirectory++;
                    continue;
                }
                
                // stream = client.getClient().retrieveFileStream(key);
                stream = client.getClient().retrieveFileStream(new String(key.getBytes(), "ISO-8859-1"));
                
                out.getParentFile().mkdirs();
                try {
                    out.createNewFile();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                
                boolean streamRes = StreamUtil.copyStreamToFile(stream, out, false);
                client.getClient().completePendingCommand();
                doneFilesForDirectory++;
            }
            
            endTimeMillis = System.currentTimeMillis();
            
            if (doneFilesForDirectory == listSize) {
                getProgressBar().setString(I18n.textf("%listSize files done (in %time) @%dataRate", 
                        listSize,
                        DateTimeUtil.milliSecondsToFormatedString(endTimeMillis - startTimeMillis),
                        (MathMiscUtils.parseToEngineeringRadix2Notation(downloadedSize
                                / ((endTimeMillis - startTimeMillis) / 1000.0), 1)) + "B/s"));
                getMsgLabel().setText(I18n.textf("Saved in '%filePath'", outFile.getAbsolutePath()));
                setStateDone();
                // client.getClient().disconnect();
            }
            else { 
                setStateNotDone();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            if (ex.getMessage() != null && ex.getMessage().startsWith("Timeout waiting for connection")) {
                isOnTimeout = true;
                getProgressBar().setString(" " + I18n.text("Error:") + " " + ex.getMessage());
                setStateTimeout();
            }
            else {
                getProgressBar().setString(
                        I18n.text("Error:") + " "
                                + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
                setStateError();
            }
        }
        finally {
            try {
                client.getClient().disconnect();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (isOnTimeout) {
            new Thread() {
                @Override
                public void run() {
                    try { Thread.sleep(8000); } catch (InterruptedException e) { }
                    if (DownloaderPanel.this.getState() == DownloaderPanel.State.TIMEOUT)
                        doDownloadDirectory();
                }
            }.start();
        }
        return true;
    }
	
	protected void doStop() {
		try {
		    stopping = true;
		    stream.close();
//            client.getClient().disconnect();
            setStateNotDone();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
		
		if (getState() == State.TIMEOUT)
		    setStateNotDone();
	}
	
	@Override
	public boolean equals(Object obj) {
	    if(this == obj)
	        return true;
	    
	    if(!(obj instanceof DownloaderPanel))
	        return false;
	    
		DownloaderPanel cmp = (DownloaderPanel) obj;
		return uri.equals(cmp.uri);

	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return name.hashCode();
	}


	private class FilterDownloadDataMonitor extends FilterInputStream {

		//private DownloaderPanel downloadPanel = null;
		
		//private long fullSize = -1;
		//private long bytesRead = 0;
		private long prec = 0;
		
		private long timeC = -1;
		private long btimer = 0;
		
		private Timer timer = new Timer(DownloaderPanel.class.getName() + " "
				+ DownloaderPanel.this.hashCode());
        private TimerTask ttask = null, ttaskIdle = null;
        
        private MovingAverage movingAverage = new MovingAverage((short) 25);
		
		/**
		 * @param in
		 */
		public FilterDownloadDataMonitor(InputStream in) {//, DownloaderPanel downloadPanel, long fullSize) {
			super(in);
			//this.downloadPanel = downloadPanel;
			//this.fullSize = fullSize;
//			downloadedSize = 0;
		}

		public void stopDisplayUpdate() {
			if (ttask != null)
				ttask.cancel();
            prec = (long) ((double) downloadedSize / (double) fullSize * 100.0);
			getProgressBar().setValue((int) prec);
		}
		
		/* (non-Javadoc)
		 * @see java.io.FilterInputStream#read()
		 */
		@Override
		public int read() throws IOException {
			int tmp = super.read();
            downloadedSize += (tmp == -1) ? 0 : 1;
			if (tmp != -1)
				updateValueInMessagePanel();
			return tmp;
		}
		
		/*This one justs calls "read(byte[] b, int off, int len)",
		 * so no need to implemented
		 */
		//public int read(byte[] b) throws IOException {
		
		/* (non-Javadoc)
		 * @see java.io.FilterInputStream#read(byte[], int, int)
		 */
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int tmp = super.read(b, off, len);
            downloadedSize += (tmp == -1) ? 0 : tmp;
			if (tmp != -1)
				updateValueInMessagePanel();
			return tmp;
		}
		
		private void updateValueInMessagePanel() {
			if (downloadedSize >= fullSize) {
				if (ttask != null) {
					ttask.cancel();
					ttask = null;
					updateProgressInfo();
				}
//				if (ttaskIdle != null) {
//					ttaskIdle.cancel();
//					ttaskIdle = null;
//				}
			}
			else {
				if (ttask == null) {
					ttask = getTimerTask();
					timer.schedule(ttask, 150);
				}
			}
				
		}
		
		private void updateProgressInfo () {
			
			double bps = -1;
			if (timeC != -1) {
				long ct =  System.currentTimeMillis();
				long deltaT = ct - timeC;
				long deltaB = downloadedSize - btimer;
				if (deltaT > 1000.0) {
					timeC = ct;
					btimer = downloadedSize;
				}
				
				bps = deltaB/(deltaT/1000.0);
				movingAverage.update(bps);
			}
			else {
				timeC = System.currentTimeMillis();
				btimer = downloadedSize;
				movingAverage.clear();
			}
			prec = (long) ((double)downloadedSize/(double)fullSize*100.0);
			getProgressBar().setValue((int) prec);
			//"346652bytes (95.9KB/s),00:00:07s left"
			//getProgressLabel().setText(
			getProgressBar().setString(I18n.textf("%downloadedSize of %fullSize %dataRate - %remainingSize remaining", 
			        MathMiscUtils.parseToEngineeringRadix2Notation(downloadedSize, 1) + "B",
			        MathMiscUtils.parseToEngineeringRadix2Notation(fullSize, 1) + "B",
			        ((bps < 0) ? "" : " @"+MathMiscUtils.parseToEngineeringRadix2Notation(movingAverage.mean(), 1) + "B/s"),
			        getTimeLeft(movingAverage.mean())));
			
//			NeptusLog.pub().info("<###> "+movingAverage);
		}
		
		/**
		 * @param bps
		 * @return
		 */
		private String getTimeLeft(double bps) {
			long leftB = fullSize - downloadedSize;
			double tLeft = leftB / bps;
			long maxS = 10 * 60; // 10min
			if (tLeft < maxS)
				return DateTimeUtil.milliSecondsToFormatedString(
						(long) (MathMiscUtils.round(tLeft,1) * 1000.0));
			else
				return "+" + DateTimeUtil.milliSecondsToFormatedString(
						maxS * 1000);
		}

		private TimerTask getTimerTask() {
			return new TimerTask() {
				
				@Override
				public void run() {
//					NeptusLog.pub().info("<###>............ "+DateTimeUtil.dateTimeFormater.format(new Date(System.currentTimeMillis())));
//					System.out.flush();
//					if (ttaskIdle != null) {
//						ttaskIdle.cancel();
//						ttaskIdle = null;
//					}
					ttask = null;
					if (downloadedSize >= fullSize)
						return;
					updateProgressInfo();
					
//					ttaskIdle = getTimerTaskIdle();
//					timer.schedule(ttaskIdle, 998, 1000);
				}
			};
		}
		
        private TimerTask getTimerTaskIdle() {
			return new TimerTask() {
				@Override
				public void run() {
//					NeptusLog.pub().info("<###>     ------------S "+DateTimeUtil.dateTimeFormater.format(new Date(System.currentTimeMillis())));
//					System.out.flush();
					updateValueInMessagePanel();
					//updateProgressInfo();
					ttaskIdle = null;
				}
			};
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GuiUtils.setLookAndFeel();
		//GuiUtils.setSystemLookAndFeel();

		SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(
                new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        schemeRegistry.register(
                new Scheme("https", 443, PlainSocketFactory.getSocketFactory()));
        PoolingClientConnectionManager httpConnectionManager = new PoolingClientConnectionManager(
                schemeRegistry);
        httpConnectionManager.setMaxTotal(200);
        httpConnectionManager.setDefaultMaxPerRoute(5);

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 30000);
        HttpClient client = new DefaultHttpClient(httpConnectionManager, params);
		
        HttpRequestBase head = new HttpGet("http://127.0.0.1:8080/dune/logs/download/20120428/092009_idle/Data.lsf");
        head.addHeader("Range", "bytes=10-19");
//        206
//        Server: DUNE/2.0.0
//        Content-Length: 201284
//        Cache-Control: max-age=1, must-revalidate
//        Last-Modified: Sat, 28 Apr 2012 10:33:22 GMT
//        Expires: Sat, 28 Apr 2012 10:33:22 GMT
//        Accept-Ranges: bytes
//        Content-Range: bytes 10-201294/201295
        HttpResponse iGetResultCode;
        try {
            iGetResultCode = client.execute(head);
            NeptusLog.pub().info("<###> "+iGetResultCode.getStatusLine().getStatusCode());
            for (Header header : iGetResultCode.getAllHeaders()) {
                NeptusLog.pub().info("<###> "+header.toString());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
		DownloaderPanel dpn = new DownloaderPanel();
		dpn.getProgressBar().setString("3452bytes (15.9KB/s),00:00:07s left");
		//GuiUtils.testFrame(dpn);

//        final DownloaderPanel dpn0 = new DownloaderPanel(client, "Data.lsf",
//                "http://127.0.0.1:8080/dune/logs/download/20120428/092009_idle/Data.lsf", new File("d:/zztest/Data.lsf"));
//
//        final DownloaderPanel dpn01 = new DownloaderPanel(client, "IMC.xml",
//                "http://127.0.0.1:8080/dune/logs/download/20120428/092009_idle/IMC.xml", new File("d:/zztest/IMC.xml"));
//
//        final DownloaderPanel dpn02 = new DownloaderPanel(client, "rfc2616.html",
//                "http://tools.ietf.org/html/rfc2616", new File("d:/zztest/rfc2616.html"));
//        
//        final DownloaderPanel dpn03 = new DownloaderPanel(client, "REC-xml-20081126.html",
//                "http://www.w3.org/TR/2008/REC-xml-20081126/", new File("d:/zztest/REC-xml-20081126.html"));
//        
//
//		final DownloaderPanel dpn1 = new DownloaderPanel(client, "crystal.tar.gz",
//				"http://127.0.0.1:8080/images/crystal.tar.gz", new File(
//				"d:/zztest/crystal.tar.gz"));
//		//dpn1.getProgressLabel().setText("346652bytes (95.9KB/s),00:00:07s left");
//
//
//		final DownloaderPanel dpn2 = new DownloaderPanel(client, "list.xml",
//				"http://localhost:8080/dune/logs/list.xml", new File(
//						"d:/zztest/list.xml"));
//		//dpn2.getProgressLabel().setText("346652bytes (95.9KB/s),00:00:07s left");
//
//		final DownloaderPanel dpn3 = new DownloaderPanel(client, "list.html",
//				"http://127.0.0.1:8080/dune/logssss/20090922/134555/asdasasdasdasdasdasasdasdasdasdasadadimages/list.html", new File(
//						"d:/zztest/list.html"));
//
//		final DownloaderPanel dpn4 = new DownloaderPanel(client, "list.html",
//				"http://127.0.0.1:8080/images/list.html", new File(
//						"d:/zztest/list.html"));
//
//		final DownloaderPanel dpn5 = new DownloaderPanel(client, "PAPER_MAST2008-NetworkedOperations-final.pdf",
//				"http://whale.fe.up.pt/Papers/2008/PAPER_MAST2008-NetworkedOperations-final.pdf", new File(
//						"d:/zztest/PAPER_MAST2008-NetworkedOperations-final.pdf"));
//
//        final DownloaderPanel dpn6 = new DownloaderPanel(client, "Blender 2.57",
//                "http://download.blender.org/release//Blender2.57/blender-2.57-windows32.exe",
//                new File("d:/zztest/blender-2.57-windows32.exe"));
//		
//        final DownloaderPanel dpn7 = new DownloaderPanel(client, "testBig",
//                "http://localhost:8080/dune/logs/download/20130202/174447_idle/testBig.raw",
//                new File("/tmp/testBig.raw"));
//            
//		JPanel jph = new JPanel();
//		jph.setLayout(new BoxLayout(jph, BoxLayout.Y_AXIS));
//		
//		JScrollPane sp = new JScrollPane();
//		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
//		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
//		sp.setBorder(BorderFactory.createEmptyBorder(3,5,3,5));
//		sp.setPreferredSize(new Dimension(800,600));
//		sp.setViewportView(jph);
//		
//		jph.add(dpn);
//        jph.add(dpn0);
//        jph.add(dpn01);
//        jph.add(dpn02);
//        jph.add(dpn03);
//        jph.add(dpn1);
//		jph.add(dpn2);
//		jph.add(dpn3);
//		jph.add(dpn4);
//		jph.add(dpn5);
//		jph.add(dpn6);
//		jph.add(dpn7);
//		
//		//jph.add(sp);
//		
//		GuiUtils.testFrame(sp, "Download Test", 800, 600);
//		
//		new Thread() {
//			@Override
//			public void run() {
//				dpn1.doDownload();
//			}
//		}.start();
//
//		new Thread() {
//			@Override
//			public void run() {
//				dpn2.doDownload();
//			}
//		}.start();
//
//		new Thread() {
//			@Override
//			public void run() {
//				dpn3.doDownload();
//			}
//		}.start();
	}
}
