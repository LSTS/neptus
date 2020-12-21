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
 * Author: José Pinto ?
 * 
 */
package pt.lsts.neptus.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.TimeUnit;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;

import pt.lsts.neptus.NeptusLog;

public class VideoCreator {

	private int width, height;
	IMediaWriter writer;
	
	public VideoCreator(File output, int width, int height)
			throws Exception {
		this.width = width;
		this.height = height;
		writer = ToolFactory.makeWriter(output.getAbsolutePath());
		writer.addVideoStream(0, 0, width, height);
	}

	public BufferedImage convertToType(BufferedImage sourceImage, int targetType) {
		BufferedImage image;
		if (sourceImage.getType() == targetType
				&& sourceImage.getWidth() == width
				&& sourceImage.getHeight() == height)
			image = sourceImage;
		else {
			image = new BufferedImage(width,
					height, targetType);
			image.getGraphics().drawImage(
					sourceImage.getScaledInstance(width, height,
							BufferedImage.SCALE_SMOOTH), 0, 0, null);
		}

		return image;
	}

	long firstTimeStamp = -1;
	
	public void addFrame(BufferedImage frame, long time) {
		BufferedImage worksWithXugglerBufferedImage = convertToType(frame,
				BufferedImage.TYPE_3BYTE_BGR);
		if (firstTimeStamp == -1)
			firstTimeStamp = time;
		
		writer.encodeVideo(0, worksWithXugglerBufferedImage, time - firstTimeStamp, TimeUnit.MILLISECONDS);
	}

	public void closeStreams() {
		try {
            writer.close();
        }
        catch (Exception | Error e) {
            NeptusLog.pub().warn("Error closing " + VideoCreator.class.getSimpleName() + " stream.", e);
        }
	}
}
