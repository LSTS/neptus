/*
 * Copyright (c) 2004-2016 OceanScan - Marine Systems & Technology Lda.
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import pt.lsts.neptus.NeptusLog;

/**
 * Decoder for video streams encoded and separate JPEG files.
 *
 * @author Ricardo Martins
 */
public class FrameDecoderJPEG implements FrameDecoder {
    /** Name of the sub-folder that contains the JPEG files. */
    private static final String subFolder = "Photos";
    /** Valid JPEG file extensions. */
    private static final String[] validExtensions = {"jpg"};
    /** List of image files, sorted by name. */
    private final List<File> fileList = new ArrayList<>();
    /** Iterator to the current frame (cursor). */
    private ListIterator<File> fileListIterator = null;
    /** Folder of interest and parent of subFolder. */
    private File folder;
    /** Computed frame rate. */
    private int frameRate;

    private static long getTimeStamp(File file) {
        String fileName = file.getName();
        String time = fileName.substring(0, fileName.lastIndexOf("."));
        return (long)(Double.parseDouble(time) * 1000);
    }

    private static VideoFrame createVideoFrame(File imageFile) {
        VideoFrame videoFrame = new VideoFrame();
        videoFrame.setTimeStamp(getTimeStamp(imageFile));

        try {
            videoFrame.setImage(ImageIO.read(imageFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return videoFrame;
    }

    @Override
    public boolean folderContainsFrames(File folder) {
        folder = getBaseFolder(folder);
        if (!folder.isDirectory())
            return false;

        return FileUtils.iterateFiles(folder, validExtensions, true).hasNext();
    }

    @Override
    public void load(File folder) {
        this.folder = folder;
        createFileList();
        computeFrameRate();

        // FIXME: test empty list.
        fileListIterator = fileList.listIterator();
    }

    @Override
    public int getFrameCount() {
        return fileList.size();
    }

    @Override
    public int getFrameRate() {
        return 4;
    }

    @Override
    public VideoFrame getCurrentFrame() {
        int index = fileListIterator.nextIndex();
        return createVideoFrame(fileList.get(index));
    }

    @Override
    public void seekToTime(long timeStamp) {
        int index = findFrameByTime(timeStamp);
        fileListIterator = fileList.listIterator(index);
    }

    @Override
    public void seekToFrame(int frameNumber) {
        fileListIterator = fileList.listIterator(frameNumber);
    }

    @Override
    public int getFrameNumberByTime(long time) {
        return 0;
    }

    @Override
    public boolean hasNext() {
        return fileListIterator.hasNext();
    }

    @Override
    public VideoFrame next() {
        return createVideoFrame(fileListIterator.next());
    }

    @Override
    public boolean hasPrevious() {
        return fileListIterator.hasPrevious();
    }

    @Override
    public VideoFrame previous() {
        return createVideoFrame(fileListIterator.previous());
    }

    private File getBaseFolder(File folder) {
        return new File(folder, subFolder);
    }

    private void createFileList() {
        NeptusLog.pub().info("loading frames from " + folder.getAbsolutePath());
        fileList.addAll(FileUtils.listFiles(getBaseFolder(folder), validExtensions, true));
        Collections.sort(fileList);
        NeptusLog.pub().info(String.format(Locale.US, "loaded %d frames", fileList.size()));
    }

    private void computeFrameRate() {
        long deltaAccumulator = 0;
        long lastTime = -1;

        for (File file: fileList) {
            long timeStamp = getTimeStamp(file);

            if (lastTime < 0) {
                lastTime = timeStamp;
            } else {
                deltaAccumulator += timeStamp - lastTime;
                lastTime = timeStamp;
            }
        }

        double averageFrameRate = 1000.0 / (deltaAccumulator / (getFrameCount() - 1));
        frameRate = (int)Math.round(averageFrameRate);
        NeptusLog.pub().info(String.format(Locale.US, "average frame rate %d (%.1f)", frameRate, averageFrameRate));
    }

    private int findFrameByTime(long timeStamp) {
        long minimumDelta = Long.MAX_VALUE;
        int minimumDeltaIndex = 0;
        int index = 0;

        for (File file: fileList) {
            long delta = Math.abs(getTimeStamp(file) - timeStamp);
            if (delta < minimumDelta) {
                minimumDelta = delta;
                minimumDeltaIndex = index;
            } else {
                break;
            }
            ++index;
        }

        return minimumDeltaIndex;
    }
}
