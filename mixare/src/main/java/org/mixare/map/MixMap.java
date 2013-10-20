package org.mixare.map;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class MixMap extends Activity {

  private static final String TAG = MixMap.class.getSimpleName();

	private static String mixMapPrefs = "mixmap";
	private static String mapUsage = "mapUsage";
	private static SharedPreferences prefs;
	
	public enum MAPS {
		GOOGLE,
		OSM
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "Launch Map");
		super.onCreate(savedInstanceState);
		prefs = getSharedPreferences(mixMapPrefs, MODE_PRIVATE);
		Intent mapToLaunch;
		String map = prefs.getString(mapUsage, MAPS.GOOGLE.name());
		Log.d(TAG, map);
		if (map == MAPS.GOOGLE.name()) {
			Log.d(TAG, "Launch GoogleMaps");
			mapToLaunch = new Intent(this, GoogleMap.class);
		} else if (map == MAPS.OSM.name()){
			Log.d(TAG, "Launch OSM");
			mapToLaunch = new Intent(this, OsmMap.class);
		} else {
			Log.d(TAG, "Fallback");
			mapToLaunch = new Intent(this, GoogleMap.class);
			changeMap(MAPS.GOOGLE);
		}
		
		Intent intent = this.getIntent();
		if (intent.getBooleanExtra("center", false)) {
			mapToLaunch.putExtra("center", true);
			mapToLaunch.putExtra("latitude", intent.getDoubleExtra("latitude", 0.0));
			mapToLaunch.putExtra("longitude", intent.getDoubleExtra("longitude", 0.0));
		}
		
		startActivity(mapToLaunch);
		finish();
	}
	
	public static void changeMap(MAPS mapName) {
		Log.d("test", "Change map to: " + mapName.name());
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(mapUsage, mapName.name());
		editor.commit();
	}
}