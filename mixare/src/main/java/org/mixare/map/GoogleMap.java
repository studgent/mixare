/*
 * Copyright (C) 2010- Peer internet solutions
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

package org.mixare.map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.mixare.DataView;
import org.mixare.MixListView;
import org.mixare.MixView;
import org.mixare.R;
import org.mixare.lib.MixUtils;
import org.mixare.lib.marker.Marker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * This class creates the map view and its overlay. It also adds an overlay with
 * the markers to the map.
 */
public class GoogleMap extends SherlockGoogleMapActivity implements
    OnTouchListener, ActionBar.OnNavigationListener {

  private Drawable drawable;

  // private static List<Marker> markerList;
  private static DataView dataView;
  private static List<LatLng> walkingPath = new ArrayList<LatLng>();

  public static final String PREFS_NAME = "MixMapPrefs";

//	private MapFragment mapView;

  // static MixMap map;
  private static Context thisContext;
  private static TextView searchNotificationTxt;

  // the search keyword
  protected String searchKeyword = "";

  // Array which holds the available maps
  private String[] maps;

  /* Menu ID's */
  // Center my Position
  private static final int MENU_CENTER_POSITION_ID = Menu.FIRST;
  // Whether to display Satellite or Map
  private static final int MENU_CHANGE_MODE_ID = Menu.FIRST + 1;
  // Go to MixListView
  private static final int MENU_LIST_VIEW = Menu.FIRST + 2;
  // Go to AugmentedView
  private static final int MENU_CAMERA_VIEW = Menu.FIRST + 3;
  // Toggle show Path
  private static final int MENU_SHOW_PATH = Menu.FIRST + 4;

  /**
   * First Launched Method onCreate() Does: - initiate View - Retrieve markers
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.googlemap);

    dataView = MixView.getDataView();
    setMapContext(this);
    getMap().setMyLocationEnabled(true);
    getMap().setMapType(com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE);

    Intent intent = this.getIntent();
    searchKeyword = intent.getStringExtra("search");

    addPoints();
    addWalkingPath();

    // Set center of the Map to your position or a Position out of the
    // IntentExtras
    if (intent.getBooleanExtra("center", false)) {
        setCenterZoom(
            intent.getDoubleExtra("latitude", 0.0),
            intent.getDoubleExtra("longitude", 0.0),
        16);
    } else {
      setOwnLocationToCenter();
      setZoomLevelBasedOnRadius();
    }

    maps = getResources().getStringArray(R.array.maps);

    Context context = getSupportActionBar().getThemedContext();
    ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(
        context, R.array.maps, R.layout.sherlock_spinner_item);
    list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);

    getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
    getSupportActionBar().setDisplayShowTitleEnabled(false);
    getSupportActionBar().setSelectedNavigationItem(getOwnListPosition());
    getSupportActionBar().setListNavigationCallbacks(list, this);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    // if (dataView.isFrozen()) {
    // searchNotificationTxt = new TextView(this);
    // searchNotificationTxt.setWidth(MixView.getdWindow().getWidth());
    // searchNotificationTxt.setPadding(10, 2, 0, 0);
    // searchNotificationTxt.setText(getString(R.string.search_active_1)
    // + " " + DataSourceList.getDataSourcesStringList()
    // + getString(R.string.search_active_2));
    // searchNotificationTxt.setBackgroundColor(Color.DKGRAY);
    // searchNotificationTxt.setTextColor(Color.WHITE);
    // searchNotificationTxt.setOnTouchListener(this);
    // addContentView(searchNotificationTxt, new LayoutParams(
    // LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
    // }
  }

  private com.google.android.gms.maps.GoogleMap getMap() {
    return ((MapFragment) getFragmentManager().findFragmentById(R.id.googleMapsFragment)).getMap();
  }

  /**
   * Closes MapView Activity and returns to MixView with or without the
   * request to refresh the screen.
   *
   * @param doRefreshScreen True to refresh screen false not to
   */
  private void closeMapViewActivity(boolean doRefreshScreen) {
    Intent closeMapView = new Intent();
    closeMapView.putExtra("RefreshScreen", doRefreshScreen);
    setResult(RESULT_OK, closeMapView);
    finish();
  }

	/* ********* Operators ********** */

  /**
   * Gets the own position in maps Array
   *
   * @return The index in the maps array
   */
  private int getOwnListPosition() {
    for (int i = 0; i < maps.length; i++) {
      if (maps[i].equals(getString(R.string.map_menu_map_google))) {
        return i;
      }
    }

    return 0;
  }

  private void setCenter(double latitude, double longitude) {
    LatLng centerPoint = new LatLng((int) (latitude * 1E6),
        (int) (longitude * 1E6));
    getMap().animateCamera(CameraUpdateFactory.newLatLng(centerPoint));
  }

  private void setCenterZoom(double latitude, double longitude, int zoom) {
    LatLng centerPoint = new LatLng((int) (latitude * 1E6),
        (int) (longitude * 1E6));
    getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(centerPoint, zoom));
  }

  private void setZoomLevelBasedOnRadius() {
    float mapZoomLevel = (getDataView().getRadius() / 2f);
    mapZoomLevel = MixUtils
        .earthEquatorToZoomLevel((mapZoomLevel < 2f) ? 2f
            : mapZoomLevel);
    getMap().animateCamera(CameraUpdateFactory.zoomTo(mapZoomLevel));
  }

  private void setOwnLocationToCenter() {
    Location location = getOwnLocation();
    setCenter(location.getLatitude(), location.getLongitude());
  }

  private Collection<Marker> filterMarkers(String searchKeyword) {
    List<Marker> result = new ArrayList<Marker>();
    Set<Marker> allMarkers = dataView.getDataHandler().getMarkerList();

    if (searchKeyword == null || searchKeyword.isEmpty()) {
      return allMarkers;
    }
    for (Marker marker : allMarkers) {
      if (marker.getTitle().toLowerCase().contains(searchKeyword.toLowerCase())) {
        result.add(marker);
      }
    }
    return result;
  }

  private LatLng createLatLng(Marker marker) {
    return new LatLng(
        (int) (marker.getLatitude() * 1E6),
        (int) (marker.getLongitude() * 1E6));
  }

  private LatLng createLatLng(Location location) {
    return new LatLng(
        (int) (location.getLatitude() * 1E6),
        (int) (location.getLongitude() * 1E6));
  }

  private boolean isBlank(String text) {
    return text == null || text.trim().isEmpty();
  }


  private MarkerOptions createMarkerOptions(Marker marker) {
    Drawable icon = getResources().getDrawable(R.drawable.icon_map_link);
    Drawable iconWithoutLink = getResources().getDrawable(R.drawable.icon_map_nolink);
    MarkerOptions result = new MarkerOptions().title(marker.getTitle()).position(createLatLng(marker));
    if (!isBlank(marker.getURL())) {
      //TODO:Create different icons for markers with url and without
      //TODO:Create ontap behavior
    }
    return result;
  }

  private void addPoints() {
    for (Marker marker : filterMarkers(searchKeyword)) {
      getMap().addMarker(createMarkerOptions(marker));
    }
    LatLng startPoint = createLatLng(getOwnLocation());
    getMap().addMarker(new MarkerOptions().title("Your Position").position(startPoint));
  }

  private PolylineOptions createPolyline(List<LatLng> points) {
    PolylineOptions result = new PolylineOptions();
//    result.setDither(true);
//    mPaint.setColor();
//    mPaint.setStyle(Paint.Style.STROKE);
//    mPaint.setStrokeJoin(Paint.Join.ROUND);
//    mPaint.setStrokeCap(Paint.Cap.ROUND);
//    mPaint.setStrokeWidth(3);
    result.color(Color.BLUE).addAll(points);
    return result;

  }

  private void addWalkingPath() {
    if (isPathVisible()) {
      getMap().addPolyline(createPolyline(walkingPath));
    }
  }

  private void createListView() {
    if (dataView.getDataHandler().getMarkerCount() > 0) {
      Intent intent1 = new Intent(this, MixListView.class);
      intent1.setAction(Intent.ACTION_VIEW);
      startActivityForResult(intent1, 42);// TODO receive result if any!
    }
    /* if the list is empty */
    else {
      getDataView().getContext().getNotificationManager()
          .addNotification(getString(R.string.empty_list));
    }
  }

  private void togglePath() {
    final String property = "pathVisible";
    final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    SharedPreferences.Editor editor = settings.edit();
    boolean result = settings.getBoolean(property, true);
    editor.putBoolean(property, !result);
    editor.commit();
  }

	/* ********* Operator - Menu ***** */

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    /* define the first */
    int base = Menu.FIRST;

    // TODO: Get Strings out of values
    menu.add(MENU_CENTER_POSITION_ID, MENU_CENTER_POSITION_ID, Menu.NONE,
        "Center").setIcon(android.R.drawable.ic_menu_mylocation)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

    SubMenu subMenu1 = menu.addSubMenu("More");

    if (getMap().getMapType() == com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE) {
      subMenu1.add(MENU_CHANGE_MODE_ID, MENU_CHANGE_MODE_ID, Menu.NONE,
          "Change to Map");
    } else {
      subMenu1.add(MENU_CHANGE_MODE_ID, MENU_CHANGE_MODE_ID, Menu.NONE,
          "Change to Satellite");
    }

    subMenu1.add(MENU_LIST_VIEW, MENU_LIST_VIEW, Menu.NONE, "ListView");

    subMenu1.add(MENU_CAMERA_VIEW, MENU_CAMERA_VIEW, Menu.NONE,
        getString(R.string.map_menu_cam_mode));

    if (isPathVisible()) {
      subMenu1.add(MENU_SHOW_PATH, MENU_SHOW_PATH, Menu.NONE,
          getString(R.string.map_toggle_path_off));
    } else {
      subMenu1.add(base, base + 4, base + 4,
          getString(R.string.map_toggle_path_on));
    }

    MenuItem subMenu1Item = subMenu1.getItem();
    subMenu1Item.setIcon(R.drawable.abs__ic_menu_moreoverflow_holo_dark);
    subMenu1Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      // Actionbar icon pressed
      case android.R.id.home:
        finish();
        break;
		/* MapMode */
      case MENU_CHANGE_MODE_ID:
        if (getMap().getMapType() == com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE) {
          getMap().setMapType(com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID);
        } else {
          getMap().setMapType(com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE);
        }
        getSherlock().dispatchInvalidateOptionsMenu();
        break;
		/* go to users location */
      case MENU_CENTER_POSITION_ID:
        setOwnLocationToCenter();
        break;
		/* List View */
      case MENU_LIST_VIEW:
        createListView();
        // finish(); don't close map if list view created
        break;
		/* back to Camera View */
      case MENU_SHOW_PATH:
        togglePath();
        // refresh:
        startActivity(getIntent()); // what Activity are we launching?
        closeMapViewActivity(false);
        break;
      case MENU_CAMERA_VIEW:
        closeMapViewActivity(false);
        break;
      default:
        break;// do nothing

    }

    return true;
  }

	/* ************ Handlers ************ */

  /**
   * Gets fired when the selected item of the ListNavigation changes. This
   * method changes to the specified map. (Google Map/OSM)
   */
  @Override
  public boolean onNavigationItemSelected(int itemPosition, long itemId) {
    if (maps[itemPosition].equals(getString(R.string.map_menu_map_osm))) {
      MixMap.changeMap(MixMap.MAPS.OSM);
      Intent intent = new Intent(this, OsmMap.class);
      startActivity(intent);
      finish();
    }
    return true;
  }

  private void handleIntent(Intent intent) {
    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      // String query = intent.getStringExtra(SearchManager.QUERY);
      // doMixSearch(query);
      intent.setClass(this, MixListView.class);
      startActivity(intent);
    }
  }

  @Override
  public void onNewIntent(Intent intent) {
    setIntent(intent);
    handleIntent(intent);
  }

  /*
   * TODO Fix onTouch function MixMap (non-Javadoc)
   *
   * @see android.view.View.OnTouchListener#onTouch(android.view.View,
   * android.view.MotionEvent)
   */
  @Override
  public boolean onTouch(View v, MotionEvent event) {
    dataView.setFrozen(false);
    // dataView.getDataHandler().setMarkerList(originalMarkerList);

    searchNotificationTxt.setVisibility(View.INVISIBLE);
    searchNotificationTxt = null;
    finish();
    Intent intent1 = new Intent(this, GoogleMap.class);
    startActivityForResult(intent1, 42);

    return false;
  }

	/* ******* Getter and Setters ********** */

  /**
   * @return the Current Location of the user
   */
  private Location getOwnLocation() {
    return getDataView().getContext().getLocationFinder()
        .getCurrentLocation();
  }

  /**
   * Returns current DataView
   *
   * @return DataView current DataView
   */
  public DataView getDataView() {
    return dataView;
  }

  public void setMapContext(Context context) {
    thisContext = context;
  }

  public Context getMapContext() {
    return thisContext;
  }

  /**
   * Adds a position to the walking route.(This route will be drawn on the
   * map)
   */
  public static void addWalkingPathPosition(LatLng geoPoint) {
    walkingPath.add(geoPoint);
  }

  /**
   * Checks stored user preference
   *
   * @return boolean false if specified, true otherwise (default)
   */
  private boolean isPathVisible() {
    final String property = "pathVisible";
    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    return settings.getBoolean(property, true);
  }
}

///**
// * Draws Items on the map.
// */
//class MixOverlay extends ItemizedOverlay<OverlayItem> {
//
//  private ArrayList<OverlayItem> overlayItems = new ArrayList<OverlayItem>();
//  private GoogleMap mixMap;
//
//  public MixOverlay(GoogleMap mixMap, Drawable marker) {
//    super(boundCenterBottom(marker));
//    // need to call populate here. See
//    // http://code.google.com/p/android/issues/detail?id=2035
//    populate();
//    this.mixMap = mixMap;
//  }
//
//  @Override
//  protected OverlayItem createItem(int i) {
//    return overlayItems.get(i);
//  }
//
//  @Override
//  public int size() {
//    return overlayItems.size();
//  }
//
//  @Override
//  protected boolean onTap(int index) {
//    String url = overlayItems.get(index).getSnippet();
//
//    try {
//      if (url != null && url.startsWith("webpage")) {
//        String newUrl = MixUtils.parseAction(url);
//        // Log.d("test", "open: " + newUrl);
//        mixMap.getDataView().getContext().getWebContentManager()
//            .loadWebPage(newUrl, mixMap.getMapContext());
//      } else {
//        OverlayItem item = overlayItems.get(index);
//        AlertDialog.Builder dialog = new AlertDialog.Builder(mixMap);
//        dialog.setTitle(item.getTitle());
//        dialog.setMessage(item.getSnippet());
//        dialog.show();
//        return true;
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//
//    return true;
//  }
//
//  public void addOverlay(OverlayItem overlay) {
//    overlayItems.add(overlay);
//    populate();
//  }
//}
//
///**
// * Draws a path(line) on the map.
// */
//class MixPath extends Overlay {
//
//  private List<LatLng> geoPoints;
//
//  public MixPath(List<LatLng> geoPoints) {
//    Log.i("MapActivity", geoPoints.toString());
//    this.geoPoints = geoPoints;
//  }
//
//  public void draw(Canvas canvas, MapView mapv, boolean shadow) {
//    super.draw(canvas, mapv, shadow);
//
//    if (geoPoints.size() <= 0) {
//      return;
//    }
//
//    Projection projection = mapv.getProjection();
//    Paint mPaint = new Paint();
//    mPaint.setDither(true);
//    mPaint.setColor(Color.BLUE);
//    mPaint.setStyle(Paint.Style.STROKE);
//    mPaint.setStrokeJoin(Paint.Join.ROUND);
//    mPaint.setStrokeCap(Paint.Cap.ROUND);
//    mPaint.setStrokeWidth(3);
//
//    final Path usrPath = new Path();
//
//    Point start = new Point();
//    projection.toPixels(geoPoints.get(0), start);
//    usrPath.moveTo(start.x, start.y);
//
//    for (LatLng gp : geoPoints) {
//      Point p = new Point();
//      projection.toPixels(gp, p);
//      usrPath.lineTo(p.x, p.y);
//    }
//
//    canvas.drawPath(usrPath, mPaint);
//  }
//}