/*
 * Copyright (C) 2012- Peer internet solutions & Finalist IT Group
 * 
 * This file is part of mixare.
 * 
 * This program is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version. 
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details. 
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.mixare.plugin.twitterprocessor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mixare.lib.data.PluginDataProcessor;
import org.mixare.lib.marker.InitialMarkerData;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A data processor for twitter urls or data, Responsible for converting raw data (to json and then) to marker data.
 * @author A. Egal
 */
public class TwitterDataProcessor extends PluginDataProcessor{

	public static final int MAX_JSON_OBJECTS = 1000;
	
	@Override
	public String[] getUrlMatch() {
		String[] str = {"twitter"};
		return str;
	}

	@Override
	public String[] getDataMatch() {
		String[] str = {"twitter"};
		return str;
	}
	
	@Override
	public List<InitialMarkerData> load(String rawData, int taskId, int colour)
			throws JSONException {
		List<InitialMarkerData> markers = new ArrayList<InitialMarkerData>();
		JSONObject root = convertToJSON(rawData);
		JSONArray dataArray = root.getJSONArray("results");
		int top = Math.min(MAX_JSON_OBJECTS, dataArray.length());

		for (int i = 0; i < top; i++) {
			JSONObject jo = dataArray.getJSONObject(i);
			
			InitialMarkerData ma = null;
			if (jo.has("geo")) {
				Double lat = null, lon = null;
	
				if (!jo.isNull("geo")) {
					JSONObject geo = jo.getJSONObject("geo");
					JSONArray coordinates = geo.getJSONArray("coordinates");
					lat = Double.parseDouble(coordinates.getString(0));
					lon = Double.parseDouble(coordinates.getString(1));
				} else if (jo.has("location")) {
	
					// Regex pattern to match location information
					// from the location setting, like:
					// iPhone: 12.34,56.78
					// ÜT: 12.34,56.78
					// 12.34,56.78
	
					Pattern pattern = Pattern
							.compile("\\D*([0-9.]+),\\s?([0-9.]+)");
					Matcher matcher = pattern.matcher(jo.getString("location"));
	
					if (matcher.find()) {
						lat = Double.parseDouble(matcher.group(1));
						lon = Double.parseDouble(matcher.group(2));
					}
				}
				if (lat != null) {
					String user=jo.getString("from_user");
					String url="http://twitter.com/"+user;
					
					//no ID is needed, since identical tweet by identical user may be safely merged into one.
					ma = new InitialMarkerData(
              i,
							user+": "+jo.getString("text"),
							lat, 
							lon, 
							0d,
              url,
							taskId,
              colour,
              "");
					markers.add(ma);
				}
			}
		}
		return markers;
	}
	
}
