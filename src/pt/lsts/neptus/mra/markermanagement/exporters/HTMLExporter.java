/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Manuel Ribeiro
 * Feb 10, 2017
 */
package pt.lsts.neptus.mra.markermanagement.exporters;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import com.webfirmframework.wffweb.tag.html.Body;
import com.webfirmframework.wffweb.tag.html.Br;
import com.webfirmframework.wffweb.tag.html.H1;
import com.webfirmframework.wffweb.tag.html.H2;
import com.webfirmframework.wffweb.tag.html.Html;
import com.webfirmframework.wffweb.tag.html.P;
import com.webfirmframework.wffweb.tag.html.attribute.Align;
import com.webfirmframework.wffweb.tag.html.attribute.Alt;
import com.webfirmframework.wffweb.tag.html.attribute.ColSpan;
import com.webfirmframework.wffweb.tag.html.attribute.Src;
import com.webfirmframework.wffweb.tag.html.attribute.Width;
import com.webfirmframework.wffweb.tag.html.attribute.global.Style;
import com.webfirmframework.wffweb.tag.html.attributewff.CustomAttribute;
import com.webfirmframework.wffweb.tag.html.formatting.Strong;
import com.webfirmframework.wffweb.tag.html.html5.stylesandsemantics.Footer;
import com.webfirmframework.wffweb.tag.html.images.Img;
import com.webfirmframework.wffweb.tag.html.metainfo.Head;
import com.webfirmframework.wffweb.tag.html.stylesandsemantics.Span;
import com.webfirmframework.wffweb.tag.html.stylesandsemantics.StyleTag;
import com.webfirmframework.wffweb.tag.html.tables.TBody;
import com.webfirmframework.wffweb.tag.html.tables.THead;
import com.webfirmframework.wffweb.tag.html.tables.Table;
import com.webfirmframework.wffweb.tag.html.tables.Td;
import com.webfirmframework.wffweb.tag.html.tables.Tr;
import com.webfirmframework.wffweb.tag.htmlwff.CustomTag;
import com.webfirmframework.wffweb.tag.htmlwff.NoTag;

import pt.lsts.neptus.mra.markermanagement.LogMarkerItem;
import pt.lsts.neptus.mra.markermanagement.LogMarkerItem.Classification;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author Manuel Ribeiro
 *
 */
public class HTMLExporter {

    @SuppressWarnings("serial")
    public static Html createHTML(ArrayList<LogMarkerItem> toExportList) {

        final int totalRows = toExportList.size();

        final Align align5 = new Align("center");
        final ColSpan colSpan12 = new ColSpan("2");
        final Style style2 = new Style("margin-left: auto; margin-right: auto; height: 239px;");
        final Style style10 = new Style("width:577px;height:550px;");
        final Style style11 = new Style("white-space:nowrap;");
        final Style style14 = new Style("text-align: left;");
        final Style whiteStyle = new Style("color: white;");
        final Width width3 = new Width("600");
        final CustomAttribute customAttribute4 = new CustomAttribute("bgcolor", "#4A7FCF");
        final CustomAttribute customAttribute7 = new CustomAttribute("bgcolor", "#E6EEFF");

        Html html = new Html(null) {{
            new Head(this) {{
                new StyleTag(this) {{
                    new NoTag(this, "table {\r\n"
                            + "border-collapse: collapse;\r\n"
                            + "}\r\n"
                            + "\r\n"
                            + "td, th {\r\n    "
                            + "border: 1px solid black;\r\n"
                            + "padding: 3px;\r\n"
                            + "}\r\n\r\n"
                            + "@media print {\r\n"
                            + "footer {"
                            + "page-break-after: always;"
                            + "}\r\n}");
                }};
            }};
            new Body(this) {{
                new H1(this, new CustomAttribute("style", "color: #5e9ca0; text-align: center;")) {{
                    new NoTag(this, "Markers List");
                }};
                for (int i = 0; i < totalRows; i++) {
                    NumberFormat nf = GuiUtils.getNeptusDecimalFormat();
                    DecimalFormat df2 = (DecimalFormat)nf;
                    df2.applyPattern("###.##");
                    String lbl = toExportList.get(i).getLabel();
                    String ts = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(toExportList.get(i).getTimestamp());
                    String loc = toExportList.get(i).getLocation().toString();
                    double depth = Double.valueOf(df2.format(toExportList.get(i).getDepth()));
                    String classif = toExportList.get(i).getClassification().toString();
                    String annot = toExportList.get(i).getAnnotation();
                    String img = toExportList.get(i).getSidescanImgPath().replaceAll("^/+", "");

                    new Footer(this) {{
                        new Table(this, style2, width3) {{
                            new THead(this) {{
                                new Tr(this, customAttribute4) {{
                                    new Td(this, align5) {{
                                        new CustomTag("font", this, whiteStyle) {{
                                            new NoTag(this, "Image");
                                        }};
                                    }};
                                    new Td(this, align5) {{
                                        new CustomTag("font", this, whiteStyle) {{
                                            new NoTag(this, "Details");
                                        }};
                                    }};
                                }};
                            }};
                            new TBody(this) {{
                                new Tr(this, customAttribute7) {{
                                    new Td(this) {{
                                        new Img(this,
                                                new Src(img),
                                                new Alt(lbl),
                                                style10);
                                    }};
                                    new Td(this, style11) {{
                                        new P(this) {{
                                            new Strong(this) {{
                                                new NoTag(this, "Label:");
                                            }};
                                        }};
                                        new P(this) {{
                                            new NoTag(this, lbl);
                                        }};
                                        new P(this) {{
                                            new Strong(this) {{
                                                new NoTag(this, "Date:");
                                            }};
                                        }};
                                        new P(this) {{
                                            new NoTag(this, ts);
                                        }};
                                        new P(this) {{
                                            new Strong(this) {{
                                                new NoTag(this, "Location:");
                                            }};
                                        }};
                                        new P(this) {{
                                            new NoTag(this, loc);
                                        }};
                                        new P(this) {{
                                            new Strong(this) {{
                                                new NoTag(this, "Depth:");
                                            }};
                                        }};
                                        new P(this) {{
                                            new NoTag(this, Double.toString(depth) + " m");
                                        }};
                                    }};
                                }};
                                new Tr(this) {{
                                    new Td(this, colSpan12, align5) {{
                                        new P(this) {{
                                            new Strong(this) {{
                                                new NoTag(this, "Classification:");
                                            }};
                                        }};
                                        new H2(this) {{
                                            new Span(this, generateColors(classif)) {{
                                                new Strong(this) {{
                                                    new NoTag(this, classif);
                                                }};
                                            }};
                                        }};
                                    }};
                                }};
                                new Tr(this) {{
                                    new Td(this, colSpan12, align5) {{
                                        new P(this) {{
                                            new Strong(this) {{
                                                new NoTag(this, "Annotation:");
                                            }};
                                        }};
                                        new P(this, style14) {{
                                            new NoTag(this, annot);
                                        }};
                                    }};
                                }};
                            }};
                        }};
                    }};
                    new Br(this);
                    new Br(this);
                }
            }};
        }};

        return html;

    }

    private static Style generateColors(String classification) {
        Style style = new Style();
        Classification temp = Classification.valueOf(classification);

        switch (temp) {
            case UNDEFINED: 
                style.addCssProperties("color: BlueViolet;");
                break;
            case CABLE:
                style.addCssProperties("color: Chocolate;");
                break;
            case NONE:
                style.addCssProperties("color: Brown;");
                break;
            case PIPE:
                style.addCssProperties("color: Crimson;");
                break;
            case ROCK:
                style.addCssProperties("color: DarkSalmon;");
                break;
            case UNKNOWN:
                style.addCssProperties("color: MediumOrchid;");
                break;
            case WRECK:
                style.addCssProperties("color: DarkSeaGreen;");
                break;
            default:
                break;
        }

        return style;
    }

    public static void saveHTML(Html createHTML, String fileName) {

        try {
            createHTML.toOutputStream(new FileOutputStream(fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
