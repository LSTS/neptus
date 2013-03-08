/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 15/10/2011
 * $Id:: MapTileProvider.java 9615 2012-12-30 23:08:28Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.plugins;

import java.awt.Dialog.ModalityType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import javax.swing.JDialog;

import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.renderer2d.WorldRenderPainter;
import pt.up.fe.dceg.neptus.renderer2d.tiles.MapPainterProvider;
import pt.up.fe.dceg.neptus.renderer2d.tiles.Tile;

/**
 * This annotation will flag the {@link WorldRenderPainter} that is a map 
 * provider. The provider may use one of the two options.
 * One option is to extend {@link Tile} and the tiles will be managed by 
 * the {@link WorldRenderPainter}.
 * The second option is to implement {@link MapPainterProvider} and will 
 * be essentially a {@link Renderer2DPainter} with no managed by the
 * {@link WorldRenderPainter} except if it should paint or not.
 * <br>
 * <br>
 * If no public static method <code>getMaxLevelOfDetail()</code> returning 
 * the max level of detail to use the {@link WorldRenderPainter#MAX_LEVEL_OF_DETAIL}
 * will be used.
 * <br>
 * <br>
 * For {@link Tile}s a public static method <code>getTilesMap()</code> returning
 * <code>{@link Map}&lt;String, {@link Tile}&gt;</code> is needed.
 * <br>
 * <br>
 * Also for {@link Tile}s a <br><code>public static void clearDiskCache() {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Tile.clearDiskCache(tileClassId);<br>
 * }</code> MUST be created.
 * <br>
 * <br>
 * Optionally a public static method <code>isFetchableOrGenerated</code> returning a
 * boolean. <code>true</code> if the tiles are fetch outside and allow all tiles to
 * be fetch at ounce for the current level of detail ({@link StateRenderer2D#getLevelOfDetail()})
 * plus 2 more levels up.
 * <br>
 * <br>
 * Additionally if you use {@link NeptusProperty} and {@link #usePropertiesOrCustomOptionsDialog()}
 * to <code>true</code> the {@link WorldRenderPainter} will try to call a public static method
 * <code>staticPropertiesChanged()</code> in order for you to save the properties to disc or
 * adjust the tile behavior.
 * 
 * In the case of {@link #usePropertiesOrCustomOptionsDialog()} equals to false, that is because
 * the map provider will be using a custom options dialog. In this case a method with signature
 * <code>public JDialog getOptionsDialog(JDialog parent)</code> or 
 * <code>public JDialog getOptionsDialog(JDialog parent, StateRenderer2D renderer)</code> will
 * need to be implemented. This will be only called once and reused. The dispose will be called
 * at the end when the {@link WorldRenderPainter} dies.
 * 
 * @author Paulo Dias
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MapTileProvider {
    /**
     * @return the unique name identifier.
     */
    public String name();

    /**
     * This will make the properties {@link NeptusProperty} to be automatically
     * gathered into a dialog for user edition or it will call for 
     * <code>getOptionsDialog(JDialog parent)</code> (expecting a {@link JDialog} 
     * in return).
     * @return 
     */
    public boolean usePropertiesOrCustomOptionsDialog() default true;

    /**
     * If true will not use {@link ModalityType#DOCUMENT_MODAL}.
     */
    public boolean makeCustomOptionsDialogIndependent() default false;
}
