/*
 * Copyright (c) 2004-2016 OceanScan - Marine Systems & Technology, Lda.
 * Polo do Mar do UPTEC, Avenida da Liberdade, 4450-718 Matosinhos, Portugal
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
 */
package pt.lsts.neptus.plugins.mjpeg;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.apache.commons.io.FileUtils;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.mjpeg.containers.avi.MjpegFile;

/**
 * Decoder for video streams encoded as AVI/MJPEG files.
 *
 * @author Ricardo Martins
 */
public class FrameDecoderMotionJPEG implements FrameDecoder {
    /** Valid AVI/MJPEG file extensions. */
    private static final String[] validExtensions = {"mjpg"};
    /** Default frame rate. */
    private static final int DEFAULT_FRAME_RATE = 5;
    /** Folder of interest. */
    private File folder;
    /** List of AVI encoded Motion JPEG files. */
    private final ArrayList<MjpegFile> fileList = new ArrayList<>();
    /** Current position in the frame stream. */
    private final Cursor cursor = new Cursor();
    /** Total number of frames in stream collection. */
    private int frameCount = 0;
    /** Average frame rate. */
    private int averageFrameRate;

    @Override
    public boolean folderContainsFrames(File folder) {
        return FileUtils.iterateFiles(folder, validExtensions, true).hasNext();
    }

    @Override
    public void load(File folder) {
        this.folder = folder;
        createFileList();
    }

    @Override
    public int getFrameCount() {
        return frameCount;
    }

    @Override
    public int getFrameRate() {
        return averageFrameRate;
    }

    @Override
    public VideoFrame getCurrentFrame() {
        return createVideoFrame(cursor.fileNumber, cursor.frameNumber, cursor.globalFrameNumber);
    }

    @Override
    public void seekToTime(long timeStamp) {
        Cursor cursor = findFrameByTime(timeStamp);
        setCursor(cursor);
    }

    @Override
    public void seekToFrame(int frameNumber) {
        Cursor cursor = findFrameByNumber(frameNumber);
        setCursor(cursor);
    }

    @Override
    public int getFrameNumberByTime(long timeStamp) {
        Cursor cursor = findFrameByTime(timeStamp);
        return cursor.globalFrameNumber;
    }

    @Override
    public boolean hasNext() {
        return cursor.globalFrameNumber < (getFrameCount() - 1);
    }

    @Override
    public VideoFrame next() {
        VideoFrame videoFrame = createVideoFrame(cursor.fileNumber, cursor.frameNumber, cursor.globalFrameNumber);
        int newFileFrame = cursor.frameNumber + 1;
        int newFileNumber = cursor.fileNumber;

        if (newFileFrame == fileList.get(cursor.fileNumber).getFrameCount()) {
            if (newFileNumber < fileList.size() - 1) {
                newFileNumber = cursor.fileNumber + 1;
                newFileFrame = 0;
            }
            else {
                System.err.format("ERROR: ups\n");
            }
        }

        cursor.set(cursor.globalFrameNumber + 1, newFileNumber, newFileFrame, fileList.get(newFileFrame).getFrameTime(newFileFrame));

        return videoFrame;
    }

    @Override
    public boolean hasPrevious() {
        return (cursor.fileNumber > 0 || cursor.frameNumber > 0);
    }

    @Override
    public VideoFrame previous() {
        return createVideoFrame(cursor.fileNumber, cursor.frameNumber, cursor.globalFrameNumber);
    }

    private void createFileList() {
        Collection<File> validFileCollection = FileUtils.listFiles(folder, validExtensions, true);
        File[] validFiles = validFileCollection.toArray(new File[validFileCollection.size()]);
        Arrays.sort(validFiles);

        double frameRateAccumulator = 0;

        for (File file : validFiles) {
            try {
                MjpegFile mjpegFile = new MjpegFile(file.getAbsoluteFile());
                NeptusLog.pub().info(String.format(Locale.US, "added '%s' with %d frames", file.getName(), mjpegFile.getFrameCount()));
                frameCount = Long.valueOf(frameCount + mjpegFile.getFrameCount()).intValue();
                fileList.add(mjpegFile);
                frameRateAccumulator += mjpegFile.getFrameRate();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        averageFrameRate = (int)Math.round(frameRateAccumulator / validFiles.length);
        if (averageFrameRate <= 0) {
            NeptusLog.pub().warn("invalid frame rate, using default");
            averageFrameRate = DEFAULT_FRAME_RATE; 
        }
    }

    private VideoFrame createVideoFrame(int fileNumber, int frameNumber, int globalFrameNumber) {
        VideoFrame videoFrame = new VideoFrame();
        videoFrame.setNumber(globalFrameNumber);
        videoFrame.setTimeStamp(fileList.get(fileNumber).getFrameTime(frameNumber));
        videoFrame.setImage(fileList.get(fileNumber).getFrameImage(frameNumber));
        return videoFrame;
    }

    private void setCursor(Cursor cursor) {
        if (cursor == null)
            return;

        this.cursor.set(cursor);
        this.cursor.timeStamp = fileList.get(cursor.fileNumber).getFrameTime(cursor.frameNumber);
    }

    private Cursor findFrameByNumber(int frameNumber) {
        if (frameNumber >= (frameCount - 1))
            return null;

        Cursor cursor = null;
        int globalFrameNumber = frameNumber;

        int fileNumber = 0;
        for (MjpegFile file : fileList) {
            if (frameNumber > file.getFrameCount() - 1) {
                frameNumber -= file.getFrameCount();
            }
            else {
                cursor = new Cursor(globalFrameNumber, fileNumber, frameNumber, file.getFrameTime(frameNumber));
                break;
            }

            ++fileNumber;
        }

        return cursor;
    }

    private Cursor findFrameByTime(long timeStamp) {
        long minimumDelta = Long.MAX_VALUE;
        Cursor minimumCursor = new Cursor();
        int fileIndex = 0;
        int globalFrameNumber = 0;

        for (MjpegFile file : fileList) {
            for (int i = 0; i < file.getFrameCount(); ++i) {
                long delta = Math.abs(file.getFrameTime(i) - timeStamp);
                if (delta < minimumDelta) {
                    minimumDelta = delta;
                    minimumCursor.set(globalFrameNumber, fileIndex, i, file.getFrameTime(i));
                }
                else {
                    break;
                }

                ++globalFrameNumber;
            }

            ++fileIndex;
        }

        return minimumCursor;
    }

    private class Cursor {
        int globalFrameNumber = 0;
        int fileNumber = 0;
        int frameNumber = 0;
        long timeStamp = -1;

        public Cursor() {
        }

        public Cursor(int globalFrameNumber, int fileNumber, int frameNumber, long timeStamp) {
            set(globalFrameNumber, fileNumber, frameNumber, timeStamp);
        }

        public void set(Cursor cursor) {
            this.globalFrameNumber = cursor.globalFrameNumber;
            this.fileNumber = cursor.fileNumber;
            this.frameNumber = cursor.frameNumber;
            this.timeStamp = cursor.timeStamp;
        }

        public void set(int globalFrameNumber, int fileNumber, int frameNumber, long timeStamp) {
            this.globalFrameNumber = globalFrameNumber;
            this.fileNumber = fileNumber;
            this.frameNumber = frameNumber;
            this.timeStamp = timeStamp;
        }
    }
}
