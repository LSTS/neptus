/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Dec 19, 2012
 */
package pt.up.fe.dceg.neptus.plugins.sidescan;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import org.imgscalr.Scalr;

import pt.up.fe.dceg.neptus.util.GuiUtils;

import com.jhlabs.image.TransformFilter;

/**
 * @author zp
 *
 */
public class SlantRangeImageFilter extends TransformFilter {

    protected double height, range;
    protected double imgWidth;
    
    public SlantRangeImageFilter(double height, double range, int imgWidth) {
        this.imgWidth = imgWidth;
        this.height = height * 2;
        this.range = range;
        
    }
    
    @Override
    protected void transformInverse(int x, int y, float[] out) {
        
        // r*r = x*x + h*h <=> r = sqrt(x*x+h*h)
        
        double h = height * (imgWidth/(range*2));
        double d = Math.abs(imgWidth / 2 - x);
        
        out[1] = y;
        if (x < imgWidth/2)
            out[0] = (float) (imgWidth/2-Math.sqrt(d*d+h*h));
        else
            out[0] = (float) (imgWidth/2+Math.sqrt(d*d+h*h));
    }
    
    
    public void setHeight(double height) {
        this.height = height * 2;
    }
    
    public static void main(String[] args) throws Exception {
        BufferedImage original = ImageIO.read(new File("/home/jqcorreia/Downloads/sidescan_example.png"));
        JTabbedPane tabs = new JTabbedPane();
        BufferedImage result = Scalr.apply(original, new SlantRangeImageFilter(5, 30, original.getWidth()));
        tabs.add("Original", new JLabel(new ImageIcon(original)));
        tabs.add("Processed", new JLabel(new ImageIcon(result)));
        
        GuiUtils.testFrame(tabs);
        
    }
}
