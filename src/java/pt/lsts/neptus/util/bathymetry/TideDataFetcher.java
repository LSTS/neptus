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
 * Author: zp
 * Dec 4, 2013
 */
package pt.lsts.neptus.util.bathymetry;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import pt.lsts.neptus.util.bathymetry.CachedData.TidePeak;

/**
 * @author zp
 *
 */
public class TideDataFetcher {

    public enum Harbor {
        Leixoes(12, 0),
        DouroCapitania(50, 12),
        DouroCrestuma(42, 12),
        
        Funchal(112, 0),
        
        PontaDelgada(211, 0),
        AngraDoHeroísmo(221, 0),
        Horta(231, 0),
        VilaDoPorto(245, 0),
        StaCruzDasFlores(241, 0),

        VianaDoCastelo(74, 0),
        PovoaDeVarzim(49, 74),
        Caminha(63, 74),
        Esposende(68, 74),
        VilaDoConde(69, 74),

        Aveiro(13, 0),
        
        FigueiraDaFoz(73, 0),
        
        Peniche(29, 0),
        Nazare(65, 29),

        Lisboa(16, 0),
        Seixal(39, 16),
        Alfeite(58, 16),
        Trafaria(35, 16),

        Cascais(15, 0),
        Setubal(20, 0),
        Sesimbra(28, 0),
        Sines(43, 0),
        Lagos(18, 0),
        Faro(19, 0),
        VilaRealSantoAntonio(21, 0),
        ;

        protected int id_prim, id_sec;

        public long id_prim() {
            return id_prim;
        }

        Harbor(int id_prim, int id_sec) {
            this.id_prim = id_prim;
            this.id_sec = id_sec;
        }
    }

    private static String fetchCookies() throws Exception {
        // fetch cookies
        URL url = new URL("http://www.hidrografico.pt/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        String cookies = "";

        String headerName = null;
        for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equals("Set-Cookie")) {
                String cookie = conn.getHeaderField(i);
                if (cookie.indexOf(";") != -1)
                    cookie = cookie.substring(0, cookie.indexOf(";"));
                if (cookies.isEmpty())
                    cookies = cookie;
                else
                    cookies += "; "+cookie;
            }
        }
        conn.disconnect();

        return cookies;
    }

    public static Vector<TidePeak> fetchData(String port, Date aroundDate) throws Exception {
        String cookies = fetchCookies();

        Harbor harbor = Harbor.valueOf(port);
        if (harbor == null)
            throw new Exception("Harbor is unknown: "+port);

        // fetch tide data
        URL url = new URL("http://www.hidrografico.pt/components/com_products/scripts/server/data_getportdetail.php");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection(); 

        String post = "codp="+harbor.id_prim+"&porcodp="+harbor.id_sec+"&epoch="+aroundDate.getTime()/1000+"&detail=1&display=0";
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Cookie", cookies);
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        conn.connect();
        wr.writeBytes(post);        
        wr.flush();
        wr.close();

        // parse html data
        org.jsoup.nodes.Document doc = Jsoup.parse(conn.getInputStream(), "UTF-8", "");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Vector<TidePeak> ret = new Vector<>();
        
        Elements elems = doc.getElementsByTag("tr");
        for (int i = 0; i < elems.size(); i++) {
            Vector<String> cells = new Vector<>();  
            for (org.jsoup.nodes.Element el : elems.get(i).children()) {
                cells.add(el.text().replaceAll("\\u00A0", ""));
            }
            if (cells.size() < 3)
                continue;
            String date = cells.firstElement().substring(5);
            String height = cells.get(cells.size()-2);
            try {
                ret.add(new TidePeak(sdf.parse(date), Double.parseDouble(height)));
            }
            catch (Exception e) {
                continue;
            }
        }
        
        return ret;
    }
}
