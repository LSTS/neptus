package pt.lsts.neptus.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.TimeUnit;

import pt.lsts.neptus.NeptusLog;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;

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
