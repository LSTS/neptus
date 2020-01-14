/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 2009/09/12
 */
package pt.lsts.neptus.util.logdownload;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.apache.commons.net.ftp.FTPFile;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.GlossPainter;
import org.jdesktop.swingx.painter.RectanglePainter;

import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.ftp.FtpDownloader;
import pt.lsts.neptus.gui.MiniButton;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.MovingAverage;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.concurrency.QueueWorkTickets;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author pdias
 *
 */
@SuppressWarnings({"serial","unused"})
public class DownloaderPanel extends JXPanel implements ActionListener {

    private static final int DELAY_START_ON_TIMEOUT = 8000;
    private static final int DELAY_START_ON_QUEUE = 1000;

    private static boolean debug = false;
    
	public static final ImageIcon ICON_STOP = ImageUtils.getIcon("images/stop.png");
	public static final ImageIcon ICON_DOWNLOAD = ImageUtils.getIcon("images/restart.png");

//    private static final Color COLOR_BACK = new Color(242, 251, 254);
//    private static final Color COLOR_FRONT = Color.GRAY;

    private static final Color COLOR_IDLE = new JXPanel().getBackground();
    private static final Color COLOR_DONE = new Color(140, 255, 170); // greennish
    private static final Color COLOR_NOT_DONE = new Color(255, 210, 140); // beigeish
    private static final Color COLOR_TIMEOUT = new Color(173, 154, 79); // brownnish
    private static final Color COLOR_QUEUED = new Color(255, 240, 245); // LavenderBlush
    private static final Color COLOR_ERROR = new Color(255, 100, 100); // redish
    private static final Color COLOR_WORKING = new Color(190, 220, 240); // blueish

	public static final String ACTION_STOP = "Stop";
	public static final String ACTION_DOWNLOAD = "Download";
	
	public static enum State {IDLE, WORKING, DONE, ERROR, NOT_DONE, TIMEOUT, QUEUED};
	
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
    private HashMap<String, FTPFile> directoryContentsList = new LinkedHashMap<>();
    
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

	//Background Painter Stuff
    private RectanglePainter rectPainter;
    private CompoundPainter<JXPanel> compoundBackPainter;
    
    // Executer for periodic tasks
    private ScheduledThreadPoolExecutor threadScheduledPool;
    
    private QueueWorkTickets<DownloaderPanel> queueWorkTickets;

	public DownloaderPanel() {
		initialize();
	}
	
	/**
	 * @param client
	 * @param id
	 * @param uri
	 */
    public DownloaderPanel(FtpDownloader client, FTPFile ftpFile, String uri, File outFile,
            ScheduledThreadPoolExecutor threadScheduledPool, QueueWorkTickets<DownloaderPanel> queueWorkTickets) {
        this(client, ftpFile, uri, outFile, null, threadScheduledPool, queueWorkTickets);
    }

    /**
     * @param client
     * @param ftpFile
     * @param uri
     * @param outFile
     * @param directoryContentsList
     */
    public DownloaderPanel(FtpDownloader client, FTPFile ftpFile, String uri, File outFile,
            HashMap<String, FTPFile> directoryContentsList, ScheduledThreadPoolExecutor threadScheduledPool,
            QueueWorkTickets<DownloaderPanel> queueWorkTickets) {
	    this();
		this.client = client;
		this.ftpFile = ftpFile;
		this.name = ftpFile.getName();
		this.uri = uri;
		this.outFile = outFile;
		
		this.threadScheduledPool = threadScheduledPool;
		this.queueWorkTickets = queueWorkTickets;
		
		this.isDirectory = ftpFile.isDirectory();
		
		if (this.isDirectory) {
		    if (directoryContentsList != null && !directoryContentsList.isEmpty()) {
		        this.directoryContentsList.putAll(directoryContentsList);
		    }
		}
		
		updateInfoLabel();
	}

    /**
     * @param uri
     */
    private void updateInfoLabel() {
        String prefixTxt = "";
        String txt = name + " (" + uri + ")";
        switch (getState()) {
            case ERROR:
                prefixTxt = I18n.text("Error");
                break;
            case NOT_DONE:
                prefixTxt = I18n.text("Incomplete");
                break;
            case TIMEOUT:
                prefixTxt = I18n.text("Timeout");
                break;
            case QUEUED:
                prefixTxt = I18n.text("Queued");
                break;
            default:
                break;
        }
        txt = prefixTxt + (prefixTxt.isEmpty() ? "" : " :: ") + txt;
        getInfoLabel().setText(txt);
    }

	/**
	 * 
	 */
	private void initialize() {
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

	/**
	 * @return the rectPainter
	 */
	private RectanglePainter getRectPainter() {
		if (rectPainter == null) {
	        rectPainter = new RectanglePainter(5, 5, 5, 5, 10, 10);
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
     * @return the uri
     */
    public String getUri() {
        return uri;
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
			updateBackColor(COLOR_WORKING);
			getStopButton().setEnabled(true);
			getDownloadButton().setEnabled(false);
		}
		else if (state == State.ERROR) {
			updateBackColor(COLOR_ERROR);
			getStopButton().setEnabled(false);
			getDownloadButton().setEnabled(true);
		}
		else if (state == State.DONE) {
			updateBackColor(COLOR_DONE);
			getStopButton().setEnabled(false);
			getDownloadButton().setEnabled(true);
		}
		else if (state == State.NOT_DONE) {
			updateBackColor(COLOR_NOT_DONE);
			getStopButton().setEnabled(false);
			getDownloadButton().setEnabled(true);
		}
        else if (state == State.TIMEOUT) {
            updateBackColor(COLOR_TIMEOUT);
            getStopButton().setEnabled(true);
            getDownloadButton().setEnabled(true);
        }
        else if (state == State.QUEUED) {
            updateBackColor(COLOR_QUEUED);
            getStopButton().setEnabled(true);
            getDownloadButton().setEnabled(true);
        }
		else {
			updateBackColor(COLOR_IDLE);
			getStopButton().setEnabled(false);
			getDownloadButton().setEnabled(true);
		}
		
		updateInfoLabel();
		
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

	private void setStateQueued() {
	    setState(State.QUEUED);
	}   

	/**
	 * Only warns if a state change happens. 
	 * @param newState
	 * @param oldState
	 */
	private void warnStateListener(DownloaderPanel.State newState, DownloaderPanel.State oldState) {
		if (stateListener != null && newState != oldState)
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

	public boolean removeStateChangeListener() {
		this.stateListener = null;
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

	/**
	 * Called to start the download
	 */
	public void actionDownload () {
		new Thread() {
			@Override
			public void run() {
				doDownload();
			}
		}.start();
	}
	
	/**
	 * Called to stop the download
	 */
	public void actionStop() {
		new Thread() {
			@Override
			public void run() {
				doStop();
			}
		}.start();
	}

	/**
	 * Called to stop the download and invalidate the component by removing the FTP client.
	 */
	public void actionStopAndInvalidate() {
	    new Thread() {
	        @Override
	        public void run() {
	            doStopAndInvalidate();
	        }
	    }.start();
	}

	private boolean doDownload() {
        if (getState() == State.QUEUED) {
            if (queueWorkTickets.isLeased(this)) {
                return doDownloadWorker();
            }
            else if (queueWorkTickets.isQueued(this)) {
                System.out.println("Is not leased but is queued! " + name);
                return true;
            }
            else if (GeneralPreferences.logsNumberSimultaneousDownloadsControl) {
                return askForLease();
            }
            else {
                return doDownloadWorker();
            }
        }
        else {
            if (GeneralPreferences.logsNumberSimultaneousDownloadsControl)
                return askForLease();
            else
                return doDownloadWorker();
        }
	}

    /**
     * @return
     */
    private boolean askForLease() {
        setStateQueued();
        Future<Boolean> future = queueWorkTickets.leaseAndWait(this, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (DownloaderPanel.this.getState() == DownloaderPanel.State.QUEUED) {
                    NeptusLog.pub().debug("callable download for " + getName());
                    boolean ret = doDownloadWorker();
                    queueWorkTickets.release(DownloaderPanel.this); // We need to release the lock
                    return ret;
                }
                queueWorkTickets.release(DownloaderPanel.this); // If we are not using the lease, just release it
                return true;
            }
        });
        return true;
    }

    private boolean doDownloadWorker() {
        if (getState() == State.WORKING)
            return false;

        State prevState = getState();
        setStateWorking();
        stopping = false;

        String basePath = outFile.getParentFile().getParentFile().getParentFile().getAbsolutePath();

        NeptusLog.pub().debug(DownloaderPanel.class.getSimpleName() + " :: " + "Downloading"
                + (isDirectory ? " directory" : "")
                + " '" + name + "' from '" + uri + "' to " + outFile.getAbsolutePath());

        if (!client.isConnected()) {
            try {
                client.renewClient();
            }
            catch (Exception e) {
                e.printStackTrace();
                setStateError();
                return false;
            }
        }
        
        long begByte = 0;
        if (usePartialDownload && prevState != State.DONE && outFile.exists() && outFile.isFile() && !isDirectory) {
            begByte = outFile.length();
            if (begByte > ftpFile.getSize())
                begByte = 0;
            NeptusLog.pub().warn(DownloaderPanel.class.getSimpleName() + " :: " + "!begin byte: " + begByte);
        }
        setStateWorking();
        
        boolean isOnTimeout = false;
        
        if (debug) {
            NeptusLog.pub().info("<###>URI: " + uri);
        }
        
        try {
            // For directory
            HashMap<String, FTPFile> fileList = directoryContentsList; // client.listDirectory("/" + uri);
            long contentLengh = ftpFile.getSize();
            final long listSize = fileList.size();

            getProgressBar().setValue(0);
            getProgressBar().setString(begByte == 0 ? I18n.text("Starting...") : I18n.text("Resuming..."));
            getMsgLabel().setText("");
            startTimeMillis = System.currentTimeMillis();
            getInfoLabel().setText(name + " (" + uri + ")");
            
            if (!isDirectory) {
                if (begByte > 0) {
                    downloadedSize = begByte;
                    client.getClient().setRestartOffset(begByte);
                    System.out.println(DownloaderPanel.class.getSimpleName() + " :: " + "using resume");
                    begByte = client.getClient().getRestartOffset();
                }
                else {
                    downloadedSize = 0;
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
                
                FilterDownloadDataMonitor ioS = new FilterDownloadDataMonitor(stream, threadScheduledPool);
                boolean streamRes = StreamUtil.copyStreamToFile(ioS, outFile, begByte == 0 ? false : true);
                outFile.setLastModified(Math.max(ftpFile.getTimestamp().getTimeInMillis(), 0));
                
                if (debug) {
                    NeptusLog.pub().info("<###>To receive / received: " + (begByte > 0 ? fullSize - begByte: fullSize) + "/" + downloadedSize);
                }
                else {
                    NeptusLog.pub().debug("To receive / received: " + (begByte > 0 ? fullSize - begByte: fullSize) + "/" + downloadedSize);
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
            else { // isDirectory
                getProgressBar().setString(I18n.text("Starting... ") + 
                        (listSize >= 0 ? I18n.textf("%number files", MathMiscUtils.parseToEngineeringRadix2Notation(fullSize,1)) : "unknown files"));
                
                threadScheduledPool.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        getProgressBar().setValue((int) ((doneFilesForDirectory / (float)listSize) * 100));
                        getProgressBar().setString(doneFilesForDirectory + " out of " + listSize);
                        
                        if (state != State.WORKING) {
                            threadScheduledPool.remove(this);
                            threadScheduledPool.purge();
                        }
                    }
                }, 10, 100, TimeUnit.MILLISECONDS);

                doneFilesForDirectory = 0;
                for(String key : fileList.keySet()) {
                    try {
                        if(stopping)
                            break;
                        
                        File out = new File(basePath + "/" + key);
                        
                        if(out.exists() && fileList.get(key).getSize() == out.length()) {
                            doneFilesForDirectory++;
                            NeptusLog.pub().debug("File for folder already synchronized: " + doneFilesForDirectory + " | "+ key);
                            continue;
                        }
                        
                        stream = client.getClient().retrieveFileStream(new String(key.getBytes(), "ISO-8859-1"));
                        
                        out.getParentFile().mkdirs();
                        try {
                            out.createNewFile();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                        
                        boolean streamRes = StreamUtil.copyStreamToFile(stream, out, false);
                        out.setLastModified(fileList.get(key).getTimestamp().getTimeInMillis());
                        if (client.getClient() != null) {
                            client.getClient().completePendingCommand();
                        }
                        if (streamRes)
                            doneFilesForDirectory++;
                    }
                    catch (Exception e) {
                        if (!stopping)
                            e.printStackTrace();
                    }
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
        }
        catch (Exception ex) {
            if (!stopping)
                NeptusLog.pub().warn(ex, ex);
            
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
            queueWorkTickets.release(this);
            try {
                if (client.isConnected())
                    client.getClient().disconnect();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (isOnTimeout) {
            final Runnable command = new Runnable() {
                @Override
                public void run() {
                    if (DownloaderPanel.this.getState() == DownloaderPanel.State.TIMEOUT)
                        doDownload();
                }
            };
            threadScheduledPool.schedule(new Runnable() {
                @Override
                public void run() {
                    new Thread(command, DownloaderPanel.class.getSimpleName() +  " :: On Timeout Retry Launcher for '" + name + "'").start();
                }
            }, DELAY_START_ON_TIMEOUT, TimeUnit.MILLISECONDS);
        }
        
        return true;
    }

	private void doStop(boolean invalidate) {
	    stopping = true;
		try {
		    if (stream != null)
		    stream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (client != null && client.isConnected())
                client.getClient().disconnect();
            if (invalidate)
                client = null;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (getState() != State.DONE)
            setStateNotDone();
		
		if (getState() == State.TIMEOUT || getState() == State.QUEUED)
		    setStateNotDone();
		
		if (invalidate)
		    stateListener = null;
		
		queueWorkTickets.release(this);
	}

    protected void doStop() {
        doStop(false);
    }
	
	protected void doStopAndInvalidate() {
        doStop(true);
    }
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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


	/**
	 * For use internally
	 * @author pdias
	 */
	private class FilterDownloadDataMonitor extends FilterInputStream {

        private static final int MAX_TIME_MINUTES_LEFT_TO_SHOW = 180;

		private long prec = 0;
		
		private long timeC = -1;
		private long btimer = 0;
		
		private ScheduledThreadPoolExecutor threadScheduledPool = null;
        private Runnable ttask = null;
        private ScheduledFuture<?> taskFuture = null;
        
        private MovingAverage movingAverage = new MovingAverage((short) 25);
		
		/**
		 * @param in
		 */
		public FilterDownloadDataMonitor(InputStream in, ScheduledThreadPoolExecutor threadScheduledPool) {
			super(in);
			this.threadScheduledPool = threadScheduledPool;
		}

		public void stopDisplayUpdate() {
			if (ttask != null) {
				taskFuture.cancel(true);
			}
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
		
		/*This one just calls "read(byte[] b, int off, int len)",
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
					taskFuture.cancel(true);
					ttask = null;
					updateProgressInfo();
				}
			}
			else {
				if (ttask == null) {
					ttask = getTimerTask();
					taskFuture = threadScheduledPool.schedule(ttask, 150, TimeUnit.MILLISECONDS);
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
			getProgressBar().setString(I18n.textf("%downloadedSize of %fullSize %dataRate - %remainingSize remaining", 
			        MathMiscUtils.parseToEngineeringRadix2Notation(downloadedSize, 1) + "B",
			        MathMiscUtils.parseToEngineeringRadix2Notation(fullSize, 1) + "B",
			        ((bps < 0) ? "" : " @"+MathMiscUtils.parseToEngineeringRadix2Notation(movingAverage.mean(), 1) + "B/s"),
			        getTimeLeft(movingAverage.mean())));
		}
		
		/**
		 * @param bps
		 * @return
		 */
		private String getTimeLeft(double bps) {
			long leftB = fullSize - downloadedSize;
			double tLeft = leftB / bps;
			long maxS = MAX_TIME_MINUTES_LEFT_TO_SHOW * 60; // 10min
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
					ttask = null;
					if (downloadedSize >= fullSize)
						return;
					updateProgressInfo();
				}
			};
		}
	}

    public static void main(String[] args) {
		GuiUtils.setLookAndFeel();
		//GuiUtils.setSystemLookAndFeel();

		DownloaderPanel dpn = new DownloaderPanel();
		dpn.getProgressBar().setString("3452bytes (15.9KB/s),00:00:07s left");
		//GuiUtils.testFrame(dpn);
	}
}
