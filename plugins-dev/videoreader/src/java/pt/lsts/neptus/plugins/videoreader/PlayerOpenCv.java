/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 18/11/2023
 */
package pt.lsts.neptus.plugins.videoreader;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.SearchOpenCv;
import pt.lsts.neptus.util.UtilCv;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

class PlayerOpenCv {
    private static boolean tryToLoadOpenCVLibs = false;
    private static boolean loadedOpenCVLibs = false;

    // Timeout for watchDogThread in milliseconds
    private static final int WATCH_DOG_TIMEOUT_MILLIS = 4000;
    private static final int WATCH_DOG_LOOP_THREAD_TIMEOUT_MILLIS = 10_000;

    private static final int MAX_NULL_FRAMES_FOR_RECONNECT = 10;

    private static final int DEFAULT_WIDTH_CONSOLE = 640;
    private static final int DEFAULT_HEIGHT_CONSOLE = 480;

    private ExecutorService service;
    private final String id;
    private Function<Dimension, Void> sizeChangeFunction;
    private VideoCapture capture;
    // Image resize
    private Mat matResize;
    // Image receive
    private Mat mat;

    private String url;
    private String infoSizeStream;

    private Function<BufferedImage, Void> updateImageFrameFunction;
    private AtomicInteger emptyFramesCounter = new AtomicInteger(0);
    private AtomicInteger threadsIdCounter = new AtomicInteger(0);

    private Scalar black = new Scalar(0);
    // Size of output frame
    private Dimension size = null;

    // Width size of image
    private int widthImgRec;
    // Height size of image
    private int heightImgRec;
    // Width size of Console
    private int widthConsole = DEFAULT_WIDTH_CONSOLE;
    // Height size of Console
    private int heightConsole = DEFAULT_HEIGHT_CONSOLE;
    // flag for state of neptus logo
    private boolean noVideoLogoState = false;
    // Scale factor of x pixel
    private float xScale;
    // Scale factor of y pixel
    private float yScale;

    private BufferedImage frameImage;

    // counter for frame tag ID
    private short frameTagID = 1;
    private AtomicLong captureLoopAtomicLongMillis = new AtomicLong(-1);

    private boolean histogramFlag = false;

    private boolean streamingActive = false;
    private boolean streamingFinished = false;
    private boolean stopRequest = false;

    public PlayerOpenCv(String id, ExecutorService service) {
        this.service = service;
        this.id = id;

        if (findOpenCV()) {
            NeptusLog.pub().info(I18n.text("OpenCv-4.x.x found."));
        }
    }

    void sizeChange(Dimension size) {
        updateSizeVariables(size);
        matResize = new Mat((int) size.height, (int) size.width, CvType.CV_8UC3);
    }

    public boolean isHistogramFlag() {
        return histogramFlag;
    }

    public void setHistogramFlag(boolean histogramFlag) {
        this.histogramFlag = histogramFlag;
    }

    private void updateSizeVariables(Dimension size) {
        widthConsole = size.width;
        heightConsole = size.height;
        xScale = (float) widthConsole / widthImgRec;
        yScale = (float) heightConsole / heightImgRec;
        this.size = size;
    }

    private static boolean findOpenCV() {
        if (tryToLoadOpenCVLibs) {
            return loadedOpenCVLibs;
        }

        tryToLoadOpenCVLibs = true;
        loadedOpenCVLibs = SearchOpenCv.searchJni();
        return loadedOpenCVLibs;
    }

    public String getId() {
        return id;
    }

    public boolean isStreamingActive() {
        return streamingActive;
    }

    public boolean isStreamingFinished() {
        return streamingFinished;
    }

    public boolean isStopRequest() {
        return stopRequest;
    }

    public void setStopRequest() {
        this.stopRequest = true;
        updateImageFrameFunction = null;
    }

    public boolean start(String url, Function<BufferedImage, Void> updateImageFrameFunction) throws Exception {
        this.updateImageFrameFunction = updateImageFrameFunction;
        this.url = url;

        NeptusLog.pub().warn("Connecting " + ":" + id + ":" + " to " + url);

        // just to initialize size vars
        updateSizeVariables(new Dimension(widthConsole, heightConsole));

        // Create Buffer (type MAT) for Image receive
        mat = new Mat(heightImgRec, widthImgRec, CvType.CV_8UC3);
        capture = new VideoCapture();
        capture.setExceptionMode(true);

        try {
            NeptusLog.pub().info("Video Stream from IPCam capturing - tid::" + id);
            boolean res = capture.open(url);
            if (false && !res) {
                if (capture.isOpened()) {
                    capture.release();
                }

                setStopRequest();
                streamingFinished = true;
                return false;
            }
        } catch (Exception | Error e) {
            NeptusLog.pub().error("Video Stream from IPCam open error - tid::" + id +
                    " :: " + e.getMessage());

            setStopRequest();
            streamingFinished = true;
            return false;
        }

        if (capture != null && capture.isOpened()) {
            streamingActive = true;
            NeptusLog.pub().info("Video Stream from IPCam is captured - tid::" + id);
            //startWatchDog();
            emptyFramesCounter.set(0);
        }

        startLoop();

        return true;
    }

    private void startLoop() {
        NeptusLog.pub().warn("Streaming from " + ":" + id + ":" + " " + url);
        long startTime = System.currentTimeMillis();
        streamingActive = true;

         //resetWatchDog(4000);

        service.execute(() -> {
            try {
                BufferedImage image = null;
                while (/*watchDog.isAlive() &&*/ streamingActive && capture != null && capture.isOpened() && !stopRequest) {
                    try {
                        boolean ret = capture.read(mat);
                        if (stopRequest) {
                            NeptusLog.pub().warn("Streaming exiting connection tid::" + id + " by stop request");
                            break;
                        }

                        if (ret) {
                            //resetWatchDog(4_000);
                        } else {
                            break;
                        }
                    } catch (Exception | Error e) {
                        NeptusLog.pub().debug(e.getMessage());
                        break;
                    }

                    long stopTime = System.currentTimeMillis();
                    if ((stopTime - startTime) != 0) {
                        infoSizeStream = String.format("Size(%d x %d) | Scale(%.2f x %.2f) | FPS:%d |\t\t\t",
                                mat.cols(), mat.rows(), xScale, yScale, (int) (1000 / (stopTime - startTime)));
                    }

                    if (mat.empty()) {
                        NeptusLog.pub().warn(I18n.text("ERROR capturing, empty img of IPCam - tid::" + id));
                        //repaint();
                        emptyFramesCounter.incrementAndGet();
                        continue;
                    }

                    emptyFramesCounter.set(0);

                    xScale = (float) widthConsole / mat.cols();
                    yScale = (float) heightConsole / mat.rows();
                    Imgproc.resize(mat, matResize, new Size(widthConsole, heightConsole));
                    // Convert Mat to BufferedImage
                    frameImage = UtilCv.matToBufferedImage(matResize);
                    // Display image in JFrame
                    if (histogramFlag) {
//                        if (saveSnapshot) {
//                            UtilCv.saveSnapshot(UtilCv.addText(UtilCv.histogramCv(frameImage),
//                                            I18n.text("Histogram - On"), VideoReader.LABEL_WHITE_COLOR,
//                                            frameImage.getWidth() - 5, 20),
//                                    String.format(logDir + "/snapshotImage"));
//                            saveSnapshot = false;
//                        }
                        if (updateImageFrameFunction != null) {
                            updateImageFrameFunction.apply(UtilCv.addText(UtilCv.histogramCv(frameImage),
                                    I18n.text("Histogram - On"),
                                    VideoReader.LABEL_WHITE_COLOR, frameImage.getWidth() - 5, 20));
                        }
                    }
                    else {

//                        if (saveSnapshot) {
//                            UtilCv.saveSnapshot(offlineImage,
//                                    String.format(logDir + "/snapshotImage"));
//                            saveSnapshot = false;
//                        }
                        if (updateImageFrameFunction != null) {
                            updateImageFrameFunction.apply(frameImage);
                        }
                    }
                }
            } catch (Exception e) {
                NeptusLog.pub().error(e.getMessage());
                e.printStackTrace();
            } finally {
                streamingActive = false;
                try {
                    if (capture != null && capture.isOpened()) {
                        capture.release();
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e.getMessage());
                }
                streamingFinished = true;
            }
            NeptusLog.pub().warn("Streaming "  + ":" + id + ":" + " stopped from " + url);
        });
    }
}
