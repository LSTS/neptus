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

/**
 * Required functionality of a frame decoder.
 *
 * @author Ricardo Martins
 */
public interface FrameDecoder {
    /**
     * Tests if a given folder contains valid frames.
     *
     * @param folder folder of interest.
     * @return true if folder contains valid frames, false otherwise.
     */
    boolean folderContainsFrames(File folder);

    /**
     * Load all available frames in a given folder.
     *
     * @param folder folder of interest.
     */
    void load(File folder);

    /**
     * Retrieves the number of frames in this stream.
     *
     * @return number of frame in this stream.
     */
    int getFrameCount();

    /**
     * Retrieves the number of frames per second in this stream.
     *
     * @return frames per second in this stream.
     */
    int getFrameRate();

    /**
     * Retrieves the contents of the current frame.
     *
     * @return frame contents.
     */
    VideoFrame getCurrentFrame();

    /**
     * Sets the current frame to be the frame closest to a reference time.
     *
     * @param timeStamp reference time in millisecond since the Unix Epoch.
     */
    void seekToTime(long timeStamp);

    /**
     * Sets the current frame to a given frame number.
     *
     * @param frameNumber frame number.
     */
    void seekToFrame(int frameNumber);

    /**
     * Retrieves the number of a frame given a time stamp.
     *
     * @param time time in millisecond since the Unix Epoch.
     * @return frame number.
     */
    int getFrameNumberByTime(long time);

    /**
     * Returns true if this stream has more frames when traversing it in the forward direction. In other words, returns
     * true if next() would return an element rather than throwing an exception.
     *
     * @return true if the list iterator has more elements when traversing the list in the forward direction.
     */
    boolean hasNext();

    /**
     * Returns the next element in the stream and advances the cursor position. This method may be called repeatedly to
     * iterate through the stream, or intermixed with calls to previous() to go back and forth. Note that alternating
     * calls to next and previous will return the same element repeatedly.
     *
     * @return the next frame in the stream.
     */
    VideoFrame next();

    /**
     * Returns true if this stream has more frames when traversing it in the reverse direction. In other words, returns
     * true if previous() would return an element rather than throwing an exception.
     *
     * @return true if the list iterator has more elements when traversing the list in the reverse direction.
     */
    boolean hasPrevious();

    /**
     * Returns the previous frame in the stream and moves the cursor position backwards. This method may be called
     * repeatedly to iterate through the stream, or intermixed with calls to next() to go back and forth. Note that
     * alternating calls to next and previous will return the same element repeatedly.
     *
     * @return the previous frame in the stream.
     */
    VideoFrame previous();
}
