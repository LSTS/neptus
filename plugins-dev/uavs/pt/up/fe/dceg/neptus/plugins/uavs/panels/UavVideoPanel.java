/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by canasta
 * 22 de Mai de 2012
 */
package pt.up.fe.dceg.neptus.plugins.uavs.panels;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.uavs.UavPaintersBag;
import pt.up.fe.dceg.neptus.plugins.uavs.interfaces.IUavPainter;
import pt.up.fe.dceg.neptus.plugins.uavs.painters.background.UavFramePainter;
import pt.up.fe.dceg.neptus.plugins.uavs.painters.foreground.UavCameraOrientationPainter;

/**
 * @author sergioferreira
 * @version 1.0
 * @category UavPanel
 */
@PluginDescription(name="Uav Video Panel", icon="pt/up/fe/dceg/neptus/plugins/uavs/wbutt.png", author="sergioferreira")
public class UavVideoPanel extends SimpleSubPanel implements NeptusMessageListener, MouseListener, MouseMotionListener, MouseWheelListener{

    private static final long serialVersionUID = 1L;
    private byte[] frameBytes;
    private BufferedImage frameImage;
    
    public static final float PAN_RIGHT = 1.0f;
    public static final float PAN_LEFT = -1.0f;
    
    public static final float TILT_UP = -1.0f;
    public static final float TILT_DOWN = 1.0f;
    
    public static final float ZOOM_IN = 1.0f;
    public static final float ZOOM_OUT = -1.0f;
    
    public static final float STOP = 0.0f;
    
    //different layers to be painted on top of the panel's draw area
    private UavPaintersBag layers;

    //arguments passed to each layer in the painting phase, in order to provide them with necessary data to allow rendering
    private Hashtable<String,Object> args;

    public UavVideoPanel(ConsoleLayout console){  
        super(console);    
        
        //clears all the unused initializations of the standard SimpleSubPanel
        removeAll();        
    }
    
    //Layers
    private void setLayers(UavPaintersBag layers) {
        this.layers = layers;
    }
    
    public UavPaintersBag getLayers() {
        return layers;
    }

    //Args
    private void setArgs(Hashtable<String, Object> args) {
        this.args = args;
    }
    
    //FrameImage
    public void setFrameImage(BufferedImage frameImage) {
        this.frameImage = frameImage;
    }

    public BufferedImage getFrameImage() {
        return frameImage;
    }

    @Override
    public void cleanSubPanel() {        
    }
    
    @Override
    public void initSubPanel() {
        
        setLayers(new UavPaintersBag());
        setArgs(new Hashtable<String,Object>());
        
        //should load a pre-set image
        setFrameImage((BufferedImage) this.createImage(this.getWidth(), this.getHeight()));
        
        //sets up all the layers used by the panel
        addLayer("Frame Painter", 1, new UavFramePainter(), 0);
        addLayer("Camera Orientation Painter", 2, new UavCameraOrientationPainter(), 0);
        
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);
    }
    
    //------Specific Methods------//

    /**
     * Method adds a new painter layer to the UavPainterBag
     * 
     * @param Name 
     *          Name of the painter to be added to the UavPainterBag
     * @param Priority 
     *          Drawing priority, the higher the number, the higher the priority. 
     * @param Layer 
     *          UavPainter responsible for painting the specific layer
     * @param CacheMillis 
     *          Refresh rate in mili-seconds     *          
     * @return Void
     */
    public void addLayer(String name, Integer priority, IUavPainter layer, int cacheMillis) {
        this.layers.addPainter(name, layer, priority, cacheMillis);
    }
    
    @Override
    public String[] getObservedMessages() {
        return new String[]{"CompressedImage","EulerAngles","GpsFix"};
    }

    @Override
    public void messageArrived(IMCMessage message) {
 
        if(message.getAbbrev().equals("CompressedImage")){
            frameBytes = message.getRawData("data");

            // convert byte array back to BufferedImage
            InputStream in = new ByteArrayInputStream(frameBytes);
            
            try {
                setFrameImage(ImageIO.read(in));
            }
            catch (IOException e) {
                e.printStackTrace();
            }            
        }      
        else if(message.getAbbrev().equals("EulerAngles")){
            args.put("camera roll", Math.toDegrees(message.getDouble("roll")));
            args.put("camera pitch", Math.toDegrees(message.getDouble("pitch")));
        }
        
        repaint();      
    }
    
    /**
     * Method adds a new painter layer to the UavPainterBag
     * 
     * @param Name 
     *          Name of the painter to be added to the UavPainterBag
     * @param Priority 
     *          Drawing priority, the higher the number, the higher the priority. 
     * @param Layer 
     *          UavPainter responsible for painting the specific layer
     * @param CacheMillis 
     *          Refresh rate in mili-seconds     *          
     * @return Void
     */
    private void prepareArgs() {
               
        //this method should detect the active profile and the existence of video feed, if no feed is detected them a pre-set message should be shown
        if(frameImage != null){
            args.put("image", frameImage);
            args.put("image rotation", 180);
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
                        
        prepareArgs();
        
        synchronized (layers) {            
            for (IUavPainter layer : layers.getPostRenderPainters()) {
                Graphics2D gNew = (Graphics2D)g.create();
                layer.paint(gNew, this.getWidth(), this.getHeight(), args);
                gNew.dispose();
            }
        }
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if(e.getPoint().x < (this.getWidth()/2)+20 && 
                e.getPoint().x > (this.getWidth()/2)-20 && 
                e.getPoint().y < (this.getHeight()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Tilt="+TILT_DOWN));
        }
        else if(e.getPoint().x < (this.getWidth()/2)+20 && 
                e.getPoint().x > (this.getWidth()/2)-20 && 
                e.getPoint().y > (this.getHeight()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Tilt="+TILT_UP));
        }
        else if(e.getPoint().y < (this.getHeight()/2)+20 && 
                e.getPoint().y > (this.getHeight()/2)-20 && 
                e.getPoint().x < (this.getWidth()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+PAN_RIGHT));
        }
        else if(e.getPoint().y < (this.getHeight()/2)+20 && 
                e.getPoint().y > (this.getHeight()/2)-20 && 
                e.getPoint().x > (this.getWidth()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+PAN_LEFT));
        }
        else if(e.getPoint().x < (this.getWidth()/2) && e.getPoint().y < (this.getHeight()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+PAN_RIGHT+";Tilt="+TILT_DOWN));
        }
        else if(e.getPoint().x < (this.getWidth()/2) && e.getPoint().y > (this.getHeight()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+PAN_RIGHT+";Tilt="+TILT_UP));
        }
        else if(e.getPoint().x > (this.getWidth()/2) && e.getPoint().y < (this.getHeight()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+PAN_LEFT+";Tilt="+TILT_DOWN));
        }
        else if(e.getPoint().x > (this.getWidth()/2) && e.getPoint().y > (this.getHeight()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+PAN_LEFT+";Tilt="+TILT_UP));
        }        
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+STOP+";Tilt="+STOP));        
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if(e.getWheelRotation() > 0)
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Zoom="+ZOOM_OUT));
        else if(e.getWheelRotation() < 0)
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Zoom="+ZOOM_IN));    
        
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(500);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Zoom="+STOP)); 
            };
        }.start();
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if(e.getPoint().x < (this.getWidth()/2)+20 && 
                e.getPoint().x > (this.getWidth()/2)-20 && 
                e.getPoint().y < (this.getHeight()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Tilt="+TILT_DOWN));
        }
        else if(e.getPoint().x < (this.getWidth()/2)+20 && 
                e.getPoint().x > (this.getWidth()/2)-20 && 
                e.getPoint().y > (this.getHeight()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Tilt="+TILT_UP));
        }
        else if(e.getPoint().y < (this.getHeight()/2)+20 && 
                e.getPoint().y > (this.getHeight()/2)-20 && 
                e.getPoint().x < (this.getWidth()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+PAN_RIGHT));
        }
        else if(e.getPoint().y < (this.getHeight()/2)+20 && 
                e.getPoint().y > (this.getHeight()/2)-20 && 
                e.getPoint().x > (this.getWidth()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+PAN_LEFT));
        }
        else if(e.getPoint().x < (this.getWidth()/2) && e.getPoint().y < (this.getHeight()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+PAN_RIGHT+";Tilt="+TILT_DOWN));
        }
        else if(e.getPoint().x < (this.getWidth()/2) && e.getPoint().y > (this.getHeight()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+PAN_RIGHT+";Tilt="+TILT_UP));
        }
        else if(e.getPoint().x > (this.getWidth()/2) && e.getPoint().y < (this.getHeight()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+PAN_LEFT+";Tilt="+TILT_DOWN));
        }
        else if(e.getPoint().x > (this.getWidth()/2) && e.getPoint().y > (this.getHeight()/2)){
            send(IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+PAN_LEFT+";Tilt="+TILT_UP));
        }        
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        // TODO Auto-generated method stub
    }
}
