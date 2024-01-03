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
 * Author: Paulo Dias
 * 18/11/2023
 */
package pt.lsts.neptus.plugins.videoreader;

import io.humble.video.Decoder;
import io.humble.video.Demuxer;
import io.humble.video.DemuxerStream;
import io.humble.video.Global;
import io.humble.video.MediaDescriptor;
import io.humble.video.MediaPacket;
import io.humble.video.MediaPicture;
import io.humble.video.Rational;
import io.humble.video.awt.ImageFrame;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;
import pt.lsts.neptus.NeptusLog;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

class Player {
    private ExecutorService service;
    private final String id;
    private Function<BufferedImage, Void> updateImageFrameFunction;
    private Demuxer demuxer;
    private int numStreams = -1;
    private int videoStreamId = -1;
    private long streamStartTime = Global.NO_PTS;
    private Decoder videoDecoder = null;

    private ImageFrame.ImageComponent mOnscreenPicture;

    private String url;

    private boolean streamingActive = false;
    private boolean streamingFinished = false;
    private boolean stopRequest = false;

    public Player(String id, ExecutorService service) {
        this.service = service;
        this.id = id;
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

    public boolean start(String url, Function<BufferedImage, Void> updateImageFrameFunction) throws IOException, InterruptedException {
        this.updateImageFrameFunction = updateImageFrameFunction;
        this.url = url;

        NeptusLog.pub().warn("Connecting " + ":" + id + ":" + " to " + url);

        demuxer = Demuxer.make();
        demuxer.setReadRetryCount(3);
        demuxer.open(url, null, false, true, null, null);

        int numStreams = demuxer.getNumStreams();
        int videoStreamId = -1;
        long streamStartTime = Global.NO_PTS;
        Decoder videoDecoder = null;
        for(int i = 0; i < numStreams; i++) {
            final DemuxerStream stream = demuxer.getStream(i);
            streamStartTime = stream.getStartTime();
            final Decoder decoder = stream.getDecoder();
            if (decoder != null && decoder.getCodecType() == MediaDescriptor.Type.MEDIA_VIDEO) {
                videoStreamId = i;
                videoDecoder = decoder;
                // stop at the first one.
                break;
            }
        }
        if (videoStreamId == -1) {
            throw new RuntimeException("could not find video stream in container " + ":" + id + ":" + ": " + url);
        }

        videoDecoder.open(null, null);

        final MediaPicture picture = MediaPicture.make(
                videoDecoder.getWidth(),
                videoDecoder.getHeight(),
                videoDecoder.getPixelFormat());

        /* A converter object we'll use to convert the picture in the video to a BGR_24 format that Java Swing
          can work with. You can still access the data directly in the MediaPicture if you prefer, but this
          abstracts away from this demo most of that byte-conversion work. Go read the source code for the
          converters if you're a glutton for punishment.
         */
        final MediaPictureConverter converter =
                MediaPictureConverterFactory.createConverter(
                        MediaPictureConverterFactory.HUMBLE_BGR_24,
                        picture);

        // TODO

        // Calculate the time BEFORE we start playing.
        long systemStartTime = System.nanoTime();
        // Set units for the system time, which because we used System.nanoTime will be in nanoseconds.
        final Rational systemTimeBase = Rational.make(1, 1000000000);
        // All the MediaPicture objects decoded from the videoDecoder will share this timebase.
        final Rational streamTimebase = videoDecoder.getTimeBase();

        startLoop(videoStreamId, videoDecoder, picture, streamStartTime, converter, systemStartTime, systemTimeBase, streamTimebase);

        return true;
    }

    private void startLoop(int videoStreamId, Decoder videoDecoder, MediaPicture picture,
                           long streamStartTime, MediaPictureConverter converter, long systemStartTime,
                           Rational systemTimeBase, Rational streamTimebase) {
        NeptusLog.pub().warn("Streaming from " + ":" + id + ":" + " " + url);

        final MediaPacket packet = MediaPacket.make();
        streamingActive = true;
        service.execute(() -> {
            try {
                BufferedImage image = null;
                while (demuxer.read(packet) >= 0 && !stopRequest) {
                    /*
                     * Now we have a packet, let's see if it belongs to our video stream
                     */
                    if (packet.getStreamIndex() == videoStreamId) {
                        /*
                         * A packet can actually contain multiple sets of samples (or frames of samples
                         * in decoding speak).  So, we may need to call decode  multiple
                         * times at different offsets in the packet's data.  We capture that here.
                         */
                        int offset = 0;
                        int bytesRead = 0;
                        do {
                            bytesRead += videoDecoder.decode(picture, packet, offset);
                            if (picture.isComplete()) {
//                                image = displayVideoAtCorrectTime(streamStartTime, picture,
//                                        converter, image, window, systemStartTime, systemTimeBase,
//                                        streamTimebase);
                                image = adjustToImage(picture, converter, image);
                                if (updateImageFrameFunction != null) {
                                    updateImageFrameFunction.apply(image);
                                }
                            }
                            offset += bytesRead;
                        } while (offset < packet.getSize() && !stopRequest);
                    }
                }

                NeptusLog.pub().warn("Stopping"  + ":" + id + ":" +" streaming from " + url);
                // Some video decoders (especially advanced ones) will cache
                // video data before they begin decoding, so when you are done you need
                // to flush them. The convention to flush Encoders or Decoders in Humble Video
                // is to keep passing in null until incomplete samples or packets are returned.
                do {
                    videoDecoder.decode(picture, null, 0);
                    if (picture.isComplete()) {
//                        image = displayVideoAtCorrectTime(streamStartTime, picture, converter,
//                                image, window, systemStartTime, systemTimeBase, streamTimebase);
                        image = adjustToImage(picture, converter, image);
                        if (updateImageFrameFunction != null) {
                            updateImageFrameFunction.apply(image);
                        }
                    }
                } while (picture.isComplete());
            } catch (Exception e) {
                NeptusLog.pub().error(e.getMessage());
            } finally {
                streamingActive = false;
                // It is good practice to close demuxers when you're done to free
                // up file handles. Humble will EVENTUALLY detect if nothing else
                // references this demuxer and close it then, but get in the habit
                // of cleaning up after yourself, and your future girlfriend/boyfriend
                // will appreciate it.
                try {
                    demuxer.close();
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e.getMessage());
                }
                streamingFinished = true;
            }
            NeptusLog.pub().warn("Streaming "  + ":" + id + ":" + " stopped from " + url);
        });
    }

    /**
     * Takes the video picture and displays it at the right time.
     */
    private static BufferedImage displayVideoAtCorrectTime(long streamStartTime,
            final MediaPicture picture, final MediaPictureConverter converter,
            BufferedImage image, final ImageFrame window, long systemStartTime,
            final Rational systemTimeBase, final Rational streamTimebase)
            throws InterruptedException {
        long streamTimestamp = picture.getTimeStamp();
        // convert streamTimestamp into system units (i.e. nano-seconds)
        streamTimestamp = systemTimeBase.rescale(streamTimestamp-streamStartTime, streamTimebase);
        // get the current clock time, with our most accurate clock
        long systemTimestamp = System.nanoTime();
        // loop in a sleeping loop until we're within 1 ms of the time for that video frame.
        // a real video player needs to be much more sophisticated than this.
        while (streamTimestamp > (systemTimestamp - systemStartTime + 1000000)) {
            Thread.sleep(1);
            systemTimestamp = System.nanoTime();
        }
        // finally, convert the image from Humble format into Java images.
        image = converter.toImage(image, picture);

        // And ask the UI thread to repaint with the new image.
        window.setImage(image);
        return image;
    }

    private static BufferedImage adjustToImage(final MediaPicture picture,
             final MediaPictureConverter converter, BufferedImage image)
            throws InterruptedException {
        // finally, convert the image from Humble format into Java images.
        image = converter.toImage(image, picture);
        return image;
    }
}
