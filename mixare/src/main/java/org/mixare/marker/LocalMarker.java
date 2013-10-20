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
package org.mixare.marker;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DecimalFormat;

import org.mixare.MixView;
import org.mixare.data.convert.Elevation;
import org.mixare.lib.MixContextInterface;
import org.mixare.lib.MixStateInterface;
import org.mixare.lib.MixUtils;
import org.mixare.lib.gui.Label;
import org.mixare.lib.gui.PaintScreen;
import org.mixare.lib.gui.ScreenLine;
import org.mixare.lib.marker.Marker;
import org.mixare.lib.marker.draw.ParcelableProperty;
import org.mixare.lib.marker.draw.PrimitiveProperty;
import org.mixare.lib.reality.PhysicalPlace;
import org.mixare.lib.render.Camera;
import org.mixare.lib.render.MixVector;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.location.Location;

/**
 * The class represents a marker and contains its information. It draws the
 * marker itself and the corresponding label. All markers are specific markers
 * like SocialMarkers or NavigationMarkers, since this class is abstract
 */

public abstract class LocalMarker implements Marker {

	private String ID;
	protected String title;
	protected boolean underline = false;
	private String URL;
	protected PhysicalPlace mGeoLoc;
	/* distance from user to mGeoLoc in meters */
	protected double distance;
	protected double bearing;
	/* Marker's color */
	private int color;

	private boolean active;

	// Draw properties
	/* Marker's Visibility to user */
	protected boolean isVisible;
	// private boolean isLookingAt;
	// private boolean isNear;
	// private float deltaCenter;
	public MixVector cMarker = new MixVector();

	protected MixVector signMarker = new MixVector();

	protected MixVector locationVector = new MixVector();

	private MixVector origin = new MixVector(0, 0, 0);

	private MixVector upV = new MixVector(0, 1, 0);

	private ScreenLine pPt = new ScreenLine();

	public Label txtLab = new Label();
	
	public LocalMarker(final String id, String title, final double latitude,
			double longitude, final double altitude, final String link,
			int type, final int color) {
		super();

		this.active = false;
		this.title = title;
		this.mGeoLoc = (new PhysicalPlace(latitude, longitude, altitude));
		if (link != null && link.length() > 0) {
			try {
				this.URL = ("webpage:" + URLDecoder.decode(link, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.underline = true;
		}
		this.color = color;
		this.ID = id + "##" + type + "##" + title;
	}

	private void cCMarker(MixVector originalPoint, Camera viewCam, float addX,
			float addY) {

		// Temp properties
		final MixVector tmpa = new MixVector(originalPoint);
		final MixVector tmpc = new MixVector(upV);
		tmpa.add(locationVector); // 3
		tmpc.add(locationVector); // 3
		tmpa.sub(viewCam.lco); // 4
		tmpc.sub(viewCam.lco); // 4
		tmpa.prod(viewCam.transform); // 5
		tmpc.prod(viewCam.transform); // 5

		final MixVector tmpb = new MixVector();
		viewCam.projectPoint(tmpa, tmpb, addX, addY); // 6
		cMarker.set(tmpb); // 7
		viewCam.projectPoint(tmpc, tmpb, addX, addY); // 6
		signMarker.set(tmpb); // 7
	}

	/**
	 * Checks if Marker is within Z angle of Camera. It sets the visibility upon
	 * that.
	 */
	private void calcV() {
		isVisible = false;
		// isLookingAt = false;
		// deltaCenter = Float.MAX_VALUE;

		if (cMarker.z < -1f) {
			isVisible = true;
		}
	}

	public void update(Location curGPSFix) {
		// Checks if programm should get Altitude from
		// http://api.geonames.org/astergdem
		String type = this.getClass().getName();
		if (POIMarker.class.getName() == type) {
			// Set direction Marker to user height
			if (((POIMarker) this).isDirectionMarker()) {
				getmGeoLoc().setAltitude(curGPSFix.getAltitude());
			}
		} else if (type == NavigationMarker.class.getName()) {
			getmGeoLoc().setAltitude(curGPSFix.getAltitude());
		} else if (type != NavigationMarker.class.getName()) {
			if (this.getURL() != null && this.getmGeoLoc().getAltitude() == 0.0) {
				this.getmGeoLoc().setAltitude(
						Double.valueOf(Elevation.getElevation().calcElevation(
								curGPSFix.getLatitude(),
								curGPSFix.getLongitude())));
			}
		}

		// compute the relative position vector from user position to POI
		// location
		PhysicalPlace.convLocToVec(curGPSFix, getmGeoLoc(), locationVector);
	}

	public void calcPaint(Camera viewCam, float addX, float addY) {
		cCMarker(origin, viewCam, addX, addY);
		calcV();
	}

	public void draw(PaintScreen dw) {
		drawCircle(dw);
		if (MixView.drawTextBlock) {
			drawTextBlock(dw);
		}
	}

	public void drawCircle(PaintScreen dw) {

		if (isVisible) {
			float maxHeight = Math.round(dw.getHeight() / 10f) + 1;
			// 0.44 is approx. vertical fov in radians
			double angle = 2.0 * Math.atan2(10, distance);
			double radius = Math.max(
					Math.min(angle / 0.44 * maxHeight, maxHeight),
					maxHeight / 25f);

			dw.paintCircle(ID + "poi", cMarker.x, cMarker.y, (float) radius);
		}
	}

	public void drawTextBlock(PaintScreen dw) {
		// TODO: grandezza cerchi e trasparenza
		float maxHeight = Math.round(dw.getHeight() / 10f) + 1;

		// TODO: change textblock only when distance changes
		String textStr = "";

		double d = distance;
		DecimalFormat df = new DecimalFormat("@#");
		if (d < 1000.0) {
			textStr = getTitle() + " (" + df.format(d) + "m)";
		} else {
			d = d / 1000.0;
			textStr = getTitle() + " (" + df.format(d) + "km)";
		}

		if (isVisible) {

			// dw.setColor(DataSource.getColor(type));

			float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y,
					signMarker.x, signMarker.y);
			dw.paintText3D(textStr, getURL(), new PointF(signMarker.x, signMarker.y
					+ maxHeight), currentAngle + 90);
		}

	}

	/* ****** Getters / setters ********* */

	public String getTitle() {
		return title;
	}

	public String getURL() {
		return URL;
	}

	public double getLatitude() {
		return getmGeoLoc().getLatitude();
	}

	public double getLongitude() {
		return getmGeoLoc().getLongitude();
	}

	public double getAltitude() {
		return getmGeoLoc().getAltitude();
	}

	public MixVector getLocationVector() {
		return locationVector;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public double getBearing() {
		return bearing;
	}

	public void setBearing(double bearing) {
		this.bearing = bearing;
	}

	public void setAltitude(double altitude) {
		getmGeoLoc().setAltitude(altitude);
	}

	public String getID() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}

	public int compareTo(Marker another) {

		Marker leftPm = this;
		Marker rightPm = another;

		return Double.compare(leftPm.getDistance(), rightPm.getDistance());

	}

	@Override
	public boolean equals(Object marker) {
		return this.ID.equals(((Marker) marker).getID());
	}

	@Override
	public int hashCode() {
		return this.ID.hashCode();
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	abstract public int getMaxObjects();

	// abstract maybe!!
	public void setImage(Bitmap image) {
	}

	// Abstract!!
	public Bitmap getImage() {
		return null;
	}

	// get Color for OpenStreetMap based on the URL number
	public int getColor() {
		return color;
	}

	@Override
	public void setTxtLab(Label txtLab) {
		this.txtLab = txtLab;
	}

	@Override
	public Label getTxtLab() {
		return txtLab;
	}

	public void setExtras(String name, PrimitiveProperty primitiveProperty) {
		// nothing to add
	}

	public void setExtras(String name, ParcelableProperty parcelableProperty) {
		// nothing to add
	}

	/**
	 * @param String
	 *            the title to set
	 */
	protected void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the underline
	 */
	protected boolean isUnderline() {
		return underline;
	}

	/**
	 * @param boolean the underline to set
	 */
	protected void setUnderline(boolean underline) {
		this.underline = underline;
	}

	/**
	 * @param String
	 *            the uRL to set
	 */
	protected void setURL(String uRL) {
		URL = uRL;
	}

	/**
	 * @return the mGeoLoc
	 */
	protected PhysicalPlace getmGeoLoc() {
		return mGeoLoc;
	}

	/**
	 * @param PhysicalPlace
	 *            the mGeoLoc to set
	 */
	protected void setmGeoLoc(PhysicalPlace mGeoLoc) {
		this.mGeoLoc = mGeoLoc;
	}
}
