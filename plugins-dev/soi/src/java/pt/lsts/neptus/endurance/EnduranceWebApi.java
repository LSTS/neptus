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
 * Author: zepinto
 * 09/01/2018
 */
package pt.lsts.neptus.endurance;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import pt.lsts.neptus.util.conf.GeneralPreferences;

public class EnduranceWebApi {

	private static final String SOI_URL_DEFAULT = GeneralPreferences.ripplesUrl + "/soi";
	
	private static String soiUrl = SOI_URL_DEFAULT;
	
	/**
     * @return the soiUrlDefault
     */
    public static String getSoiUrlDefault() {
        return SOI_URL_DEFAULT;
    }
    
	/**
     * @return the soiUrl
     */
    public static String getSoiUrl() {
        return soiUrl;
    }
    
    /**
     * @param soiUrl the soiUrl to set
     */
    public static void setSoiUrl(String soiUrl) {
        EnduranceWebApi.soiUrl = soiUrl;
    }

	public static Future<List<Asset>> getSoiState() {
		return execute(new Callable<List<Asset>>() {
			@Override
			public List<Asset> call() throws Exception {
				URL url = new URL(soiUrl);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String inputLine;
				StringBuffer content = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					content.append(inputLine);
				}
				in.close();
				conn.disconnect();
				if (content.length() > 2)
				    System.out.println("SoiState JSON :> " + content);
				return AssetState.parseStates(content.toString());
			}
		});
	}

	private static <T> Future<T> execute(Callable<T> call) {
		ExecutorService exec = Executors.newSingleThreadExecutor();
		Future<T> ret = exec.submit(call);
		exec.shutdown();
		return ret;
	}

	public static Future<Void> setAssetState(String assetName, AssetState state) {
		JsonObject json = new JsonObject();
		json.add("name", assetName);
		json.add("received", Json.parse(state.toString()));
		return postJson(soiUrl, json.toString());
	}	
	
	public static Future<Void> setAssetPlan(String assetName, Plan plan) {
		JsonObject json = new JsonObject();
		json.add("name", assetName);
		json.add("plan", Json.parse(plan.toString()));
		return postJson(soiUrl, json.toString());
	}

	private static Future<Void> postJson(final String url, final String json) {
		return execute(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				URL url_ = new URL(url);
				HttpURLConnection conn = (HttpURLConnection) url_.openConnection();
				byte[] data = json.getBytes(StandardCharsets.UTF_8);

				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				conn.setFixedLengthStreamingMode(data.length);
				conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

				OutputStream os = conn.getOutputStream();
				os.write(data);
				os.close();

				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String inputLine;
				StringBuffer content = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					content.append(inputLine);
				}
				in.close();
				conn.disconnect();
				System.out.println("PostJSON Result Code :>" + conn.getResponseCode());
				return null;
			}
		});
	}

	public static Future<Void> setAsset(Asset asset) {
		return postJson(soiUrl, asset.toString());
	}

	public static void main(String[] args) throws Exception {
	    //setSoiUrl("http://127.0.0.1:8080/soi");
		Asset asset = new Asset("lauv-xplore-1");
		asset.setState(AssetState.builder()
				.withLatitude(41)
				.withLongitude(-8)
				.withTimestamp(new Date())
				.build());
		
		EnduranceWebApi.setAsset(asset).get(1000, TimeUnit.MILLISECONDS);
		System.out.println(EnduranceWebApi.getSoiState().get(1000, TimeUnit.MILLISECONDS));
	}
}
