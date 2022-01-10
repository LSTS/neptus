/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Created in 26/02/2011
 */
package pt.lsts.neptus.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

/**
 * v1.6 Encoder formats: BarcodeFormat.EAN_8, BarcodeFormat.EAN_13,
 * BarcodeFormat.QR_CODE, BarcodeFormat.CODE_39, BarcodeFormat.CODE_128,
 * BarcodeFormat.ITF.
 * 
 * @author pdias
 * 
 */
public class BarCodesUtil {

    /**
     * @param contents
     * @param format
     * @param width
     * @param height
     * @param hints
     * @return
     * @throws WriterException
     * @throws Exception
     */
    private static BitMatrix createBarCodeBitMatrix(String contents,
            BarcodeFormat format, int width, int height,
            Hashtable<EncodeHintType, String> hints) throws WriterException,
            Exception {
        Writer writer = new MultiFormatWriter();
        BitMatrix matrix = writer
                .encode(contents, format, width, height, hints);
        return matrix;
    }

    /**
     * @param contents
     * @param format
     * @param width
     * @param height
     * @param hints
     * @return
     * @throws WriterException
     * @throws Exception
     */
    public static BufferedImage createBarCodeImage(String contents,
            BarcodeFormat format, int width, int height,
            Hashtable<EncodeHintType, String> hints) throws WriterException,
            Exception {
        BitMatrix matrix = createBarCodeBitMatrix(contents, format, width,
                height, hints);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        return image;
    }

    /**
     * @param contents
     * @param format
     * @param width
     * @param height
     * @return
     * @throws WriterException
     * @throws Exception
     */
    public static BufferedImage createBarCodeImage(String contents,
            BarcodeFormat format, int width, int height)
                    throws WriterException, Exception {
        return createBarCodeImage(contents, format, width, height, null);
    }

    /**
     * @param contents
     * @param width
     * @param height
     * @return
     * @throws WriterException
     * @throws Exception
     */
    public static BufferedImage createQRCodeImage(String contents, int width,
            int height) throws WriterException, Exception {
        return createBarCodeImage(contents, BarcodeFormat.QR_CODE, width,
                height, null);
    }

    /**
     * @param contents
     * @param format
     * @param width
     * @param height
     * @param hints
     * @param imageFormat
     *            see {@link ImageIO}
     * @param file
     * @return
     * @throws WriterException
     * @throws Exception
     */
    public static boolean createBarCodeImageToFile(String contents,
            BarcodeFormat format, int width, int height,
            Hashtable<EncodeHintType, String> hints, String imageFormat,
            File file) throws WriterException, Exception {
        BitMatrix matrix = createBarCodeBitMatrix(contents, format, width,
                height, hints);
        MatrixToImageWriter.writeToPath(matrix, imageFormat, file.toPath());
        return true;
    }

    /**
     * @param contents
     * @param format
     * @param width
     * @param height
     * @param imageFormat
     *            see {@link ImageIO}
     * @param file
     * @return
     * @throws WriterException
     * @throws Exception
     */
    public static boolean createBarCodeImageToFile(String contents,
            BarcodeFormat format, int width, int height, String imageFormat,
            File file) throws WriterException, Exception {
        return createBarCodeImageToFile(contents, format, width, height, null,
                imageFormat, file);
    }

    /**
     * @param contents
     * @param width
     * @param height
     * @param imageFormat see {@link ImageIO}
     * @param file
     * @return
     * @throws WriterException
     * @throws Exception
     */
    public static boolean createQRCodeImageToFile(String contents, int width,
            int height, String imageFormat, File file) throws WriterException,
            Exception {
        return createBarCodeImageToFile(contents, BarcodeFormat.QR_CODE, width,
                height, null, imageFormat, file);
    }

    /**
     * @param contents
     * @param format
     * @param width
     * @param height
     * @param hints
     * @param imageFormat see {@link ImageIO}
     * @param stream
     * @return
     * @throws WriterException
     * @throws Exception
     */
    public static boolean createBarCodeImageToStream(String contents,
            BarcodeFormat format, int width, int height,
            Hashtable<EncodeHintType, String> hints, String imageFormat,
            OutputStream stream) throws WriterException, Exception {
        BitMatrix matrix = createBarCodeBitMatrix(contents, format, width,
                height, hints);
        MatrixToImageWriter.writeToStream(matrix, imageFormat, stream);
        return true;
    }

    /**
     * @param contents
     * @param format
     * @param width
     * @param height
     * @param imageFormat see {@link ImageIO}
     * @param stream
     * @return
     * @throws WriterException
     * @throws Exception
     */
    public static boolean createBarCodeImageToStream(String contents,
            BarcodeFormat format, int width, int height, String imageFormat,
            OutputStream stream) throws WriterException, Exception {
        return createBarCodeImageToStream(contents, format, width, height,
                null, imageFormat, stream);
    }

    /**
     * @param contents
     * @param width
     * @param height
     * @param imageFormat see {@link ImageIO}
     * @param stream
     * @return
     * @throws WriterException
     * @throws Exception
     */
    public static boolean createQRCodeImageToStream(String contents, int width,
            int height, String imageFormat, OutputStream stream)
                    throws WriterException, Exception {
        return createBarCodeImageToStream(contents, BarcodeFormat.QR_CODE,
                width, height, null, imageFormat, stream);
    }

    /**
     * @param image
     * @param hints
     * @return
     * @throws NotFoundException
     */
    public static Result decodeBarCode(BufferedImage image,
            Hashtable<DecodeHintType, String> hints) throws NotFoundException {
        // convert the image to a binary bitmap source
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        // decode the barcode
        MultiFormatReader reader = new MultiFormatReader();

        Result result = reader.decode(bitmap, hints);
        return result;
    }

    /**
     * @param image
     * @return
     * @throws NotFoundException
     */
    public static Result decodeBarCode(BufferedImage image)
            throws NotFoundException {
        return decodeBarCode(image, null);
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        BarcodeFormat[] formats = new BarcodeFormat[] { BarcodeFormat.QR_CODE,
                BarcodeFormat.EAN_8, BarcodeFormat.EAN_13,
                BarcodeFormat.CODE_39, BarcodeFormat.CODE_128,
                BarcodeFormat.ITF };
        String data = "http://whale.fe.up.pt/neptus/";
        File fx;

        for (BarcodeFormat format : formats) {
            String dx = data;
            if (format == BarcodeFormat.EAN_8)
                dx = "12345678";
            else if (format == BarcodeFormat.EAN_13)
                dx = "1234567890123";
            else if (format == BarcodeFormat.CODE_39)
                dx = "123456789012345678901234567890123456789";
            else if (format == BarcodeFormat.ITF)
                dx = "http://www.lsts.pt/neptus/";
            try {
                System.out.print(format + ": ");
                fx = new File("test-" + format + ".png");
                createBarCodeImageToFile(dx, format, 200, 200, "png", fx);
                Result result = decodeBarCode(ImageIO.read(fx));
                System.out.println(dx + " == " + result.getText() + " >>> " + (dx.equals(result.getText())));
            }
            catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }
}
