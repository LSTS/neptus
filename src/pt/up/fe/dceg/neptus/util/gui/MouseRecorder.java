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
 * Aug 8, 2012
 * $Id:: MouseRecorder.java 9615 2012-12-30 23:08:28Z pdias                     $:
 */
package pt.up.fe.dceg.neptus.util.gui;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

/**
 * @author zp
 */
public class MouseRecorder implements AWTEventListener {

    protected BufferedImage img = null;
    protected BufferedImage lastSnapshot = null;

    protected Point lastPoint = null;
    protected long lastTime = 0;
    protected Graphics2D pathGraphics = null;
    protected Dimension screenSize;
    protected Robot r;
    
    public MouseRecorder() {
        try {
            r = new Robot();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    }

    protected void createSnapshot(Point clicked, boolean leftClick) {

        BufferedImage image = r.createScreenCapture(new Rectangle(screenSize));

        if (clicked != null) {
            Graphics2D g = (Graphics2D) image.getGraphics();

            Color color = Color.orange;
            if (!leftClick) {
                color = Color.green;
            }
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
            g.fill(new Ellipse2D.Double(clicked.getX() - 8, clicked.getY() - 8 , 16, 16));
            g.setColor(color.darker().darker());
            g.draw(new Ellipse2D.Double(clicked.getX() - 8, clicked.getY() - 8, 16, 16));        
            g.drawImage(img, null, null);

            img = null;
        }

        saveSnapshot(image);
    }

    public void startRecording() {
        Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        createSnapshot(null, false);
    }

    public void stopRecording() {
        createSnapshot(null, false);
        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
    }


    @Override
    public void eventDispatched(AWTEvent event) {

        MouseEvent mouseEvent = (MouseEvent) event;
        boolean leftClick = mouseEvent.getButton() == MouseEvent.BUTTON1 ? true : false;
        Point point = mouseEvent.getLocationOnScreen();
        switch (event.getID()) {
            case MouseEvent.MOUSE_CLICKED:
                System.out.println(MouseEvent.BUTTON1 & event.getID());
                // desenhar click e passar event
                createSnapshot(point, leftClick);
                DelayedSnapshot timerTask = new DelayedSnapshot();
                timerTask.setP(point);
                timerTask.setLeftBtn(leftClick);
                new Timer("delayed snapshot").schedule(timerTask, 100);
                
                break;
            case MouseEvent.MOUSE_MOVED:
                drawPath(point);
                break;
            case MouseEvent.MOUSE_DRAGGED:
                drawPath(MouseInfo.getPointerInfo().getLocation());
                break;
            default:
                break;
        }
    }

    private void drawPath(Point p) {

        if (img == null) {
            img = new BufferedImage((int)screenSize.getWidth(), (int)screenSize.getHeight(), BufferedImage.TYPE_INT_ARGB);
            pathGraphics = (Graphics2D) img.getGraphics();
            pathGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        if (lastPoint != null) {
            pathGraphics.setColor(Color.black);
            pathGraphics.draw(new Line2D.Double(lastPoint, p));


            if (System.currentTimeMillis() - lastTime > 1000) {
                double width = (System.currentTimeMillis() - lastTime) / 1000.0;
                width = Math.min(width, 60);                
                pathGraphics.draw(new Ellipse2D.Double(p.getX()-width/2, p.getY()-width/2, width, width));
                pathGraphics.setColor(new Color(0,0,0,200));
                pathGraphics.fill(new Ellipse2D.Double(p.getX()-width/2, p.getY()-width/2, width, width));                
            }

        }

        lastPoint = p;
        lastTime = System.currentTimeMillis();
    }


    String fileName = "Snapshots_" + System.currentTimeMillis();
    File dir = new File("log/MouseRecorder" + fileName);


    private void saveSnapshot(BufferedImage image) {
        dir.mkdirs();

        try {
            String filename = System.currentTimeMillis()+".png";
            ImageIO.write(image, "PNG", new File(dir, filename));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private class DelayedSnapshot extends TimerTask {
        private Point p;
        private boolean isLeftBtn;


        /**
         * @param p the point where the mouse is
         */
        public void setP(Point p) {
            this.p = p;
        }


        /**
         * @param isLeftBtn true is the left mouse button was pressed
         */
        public void setLeftBtn(boolean isLeftBtn) {
            this.isLeftBtn = isLeftBtn;
        }

        @Override
        public void run() {
            createSnapshot(p, isLeftBtn);
        }

    }
}
