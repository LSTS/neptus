/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 15/10/2011
 */
package pt.lsts.neptus.plugins;

import java.awt.Dialog.ModalityType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import javax.swing.JDialog;

import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.WorldRenderPainter;
import pt.lsts.neptus.renderer2d.tiles.MapPainterProvider;
import pt.lsts.neptus.renderer2d.tiles.Tile;

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
    
    /**
     * @return true if is a base map and false for a layer. Base map is opaque and paints all 
     * the canvas while layers have a transparent background and may be used on top of base maps.
     */
    public boolean isBaseMapOrLayer() default true;
    
    /**
     * @return the layer priority. The higher the more on top it will be. This is only valid for 
     * layer maps, not base maps.
     */
    public short layerPriority() default 0;
}
