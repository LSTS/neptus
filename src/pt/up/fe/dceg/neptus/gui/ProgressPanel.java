/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Dec 21, 2012
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

/**
 * 
 * @author jqcorreia
 *
 */
public class ProgressPanel extends JPanel{
  
    private static final long serialVersionUID = 1L;
    double progress;
    
    @Override
    public Component add(Component comp) {
        if(comp instanceof JComponent) {
            ((JComponent)comp).setOpaque(false);
        }
        return super.add(comp);
    }
    public ProgressPanel() {
        
    }
    
    public void setProgress(double progress) {
        this.progress = progress;
        setToolTipText(progress + "");
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.red);
        g2d.fillRect(0, 0, (int) (getWidth() * (progress / 100)), getHeight() - 5);
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        ProgressPanel pp = new ProgressPanel();
        
        pp.setProgress(75);
        frame.setLayout(new MigLayout());
        frame.setSize(400, 200);
        
        frame.add(pp, "w 100%, h 100%");
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
