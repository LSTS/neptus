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
import java.lang.reflect.InvocationTargetException;

/**
 * Frame decoder factory.
 *
 * @author Ricardo Martins
 */
public class FrameDecoderFactory {
    private static final Class<?>[] decoderClasses = {
      FrameDecoderMotionJPEG.class,
      //FrameDecoderJPEG.class,
    };

    /**
     * Creates a decoder object suitable to decode the frames of a given folder.
     * Upon creation the decoder object will analyze the available frames and might
     * scan files and load frames to memory.
     *
     * @param folder folder of interest.
     * @return decoder object or null if the folder does not contain decodable frames.
     */
    public static FrameDecoder createDecoder(File folder) {
        FrameDecoder frameStream = getFirstDecoder(folder);
        if (frameStream != null)
            frameStream.load(folder);

        return frameStream;
    }

    /**
     * Tests if a folder contains valid frames and therefore can be decoded.
     *
     * @param folder folder of interest.
     * @return true if folder contains valid frames.
     */
    public static boolean isDecodable(File folder) {
        return getFirstDecoder(folder) != null;
    }

    /**
     * Retrieves the first frame decoder that can successfully decode the frames in a given folder.
     *
     * @param folder folder of interest.
     * @return decoder object if the folder contains supported frames, null otherwise.
     */
    private static FrameDecoder getFirstDecoder(File folder) {
        for (Class<?> decoderClass : decoderClasses) {
            try {
                FrameDecoder decoder = (FrameDecoder) decoderClass.getDeclaredConstructor().newInstance();
                if (decoder.folderContainsFrames(folder)) {
                    return decoder;
                }
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
