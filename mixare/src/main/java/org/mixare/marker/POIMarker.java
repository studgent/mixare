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

import java.text.DecimalFormat;

import org.mixare.lib.MixUtils;
import org.mixare.lib.gui.PaintScreen;

import android.graphics.Path;
import android.graphics.PointF;
import android.location.Location;

/**
 * This markers represent the points of interest. On the screen they appear as
 * circles, since this class inherits the draw method of the Marker.
 * 
 * @author hannes
 * 
 */
public class POIMarker extends LocalMarker {

	public static final int MAX_OBJECTS = 15;
	public static final int OSM_URL_MAX_OBJECTS = 5;
	private boolean isDirectionMarker = false;

	public POIMarker(String id, String title, double latitude,
			double longitude, double altitude, String URL, int type, int color) {
		super(id, title, latitude, longitude, altitude, URL, type, color);
	}

	public boolean isDirectionMarker() {
		return isDirectionMarker;
	}

	public void setIsDirectionMarker(boolean direction) {
		this.isDirectionMarker = direction;
	}

	@Override
	public void update(Location curGPSFix) {
		super.update(curGPSFix);
	}

	@Override
	public int getMaxObjects() {
		return MAX_OBJECTS;
	}

	@Override
	public void drawCircle(PaintScreen dw) {
		if (isVisible) {
			float maxHeight = dw.getHeight();
			// dw.setStrokeWidth(maxHeight / 100f);
			// dw.setFill(false);
			//
			// dw.setColor(getColor());

			// draw circle with radius depending on distance
			// 0.44 is approx. vertical fov in radians
			double angle = 2.0 * Math.atan2(10, distance);
			double radius = Math.max(
					Math.min(angle / 0.44 * maxHeight, maxHeight),
					maxHeight / 25f);

			/*
			 * distance 100 is the threshold to convert from circle to another
			 * shape
			 */
			if (distance < 100.0)
				otherShape(dw);
			else
				dw.paintCircle(getID() + "poi", cMarker.x, cMarker.y,
						(float) radius);

		}
	}

	@Override
	public void drawTextBlock(PaintScreen dw) {
		float maxHeight = Math.round(dw.getHeight() / 10f) + 1;
		// TODO: change textblock only when distance changes

		String textStr = "";

		if (!isDirectionMarker) {
			double d = distance;
			DecimalFormat df = new DecimalFormat("@#");
			textStr = getTitle() + "(" + MixUtils.formatDist((float) d) + ")";
			if (d < 1000.0) {
				textStr = getTitle() + " (" + df.format(d) + "m)";
			} else {
				d = d / 1000.0;
				textStr = getTitle() + " (" + df.format(d) + "km)";
			}
		} else {
			textStr = getTitle();
		}

		// if (isVisible) {
		// // based on the distance set the colour
		// if (distance < 100.0) {
		// textBlock.setBgColor(Color.argb(128, 52, 52, 52));
		// textBlock.setBorderColor(Color.rgb(255, 104, 91));
		// } else {
		// textBlock.setBgColor(Color.argb(128, 0, 0, 0));
		// textBlock.setBorderColor(Color.rgb(255, 255, 255));
		// }
		// //dw.setColor(DataSource.getColor(type));
		//
		float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y,
				signMarker.x, signMarker.y);
		// dw.setStrokeWidth(1f);
		// dw.setFill(true);
		if (isVisible) {
			dw.paintText3D(textStr, getURL(), new PointF(signMarker.x, signMarker.y
					+ maxHeight), currentAngle + 90);
		}
		// dw.paintObj(txtLab, signMarker.x - txtLab.getWidth() / 2,
		// signMarker.y + maxHeight, currentAngle + 90, 1);

	}

	public void otherShape(PaintScreen dw) {
		// This is to draw new shape, triangle
		float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y,
				signMarker.x, signMarker.y);
		float maxHeight = Math.round(dw.getHeight() / 10f) + 1;

		// dw.setColor(getColor());
		float radius = maxHeight / 1.5f;
		// dw.setStrokeWidth(dw.getHeight() / 100f);
		// dw.setFill(false);

		Path tri = new Path();
		float x = 0;
		float y = 0;
		tri.moveTo(x, y);
		tri.lineTo(x - radius, y - radius);
		tri.lineTo(x + radius, y - radius);

		tri.close();
		dw.paintTriangle(getID() + "triangle", signMarker.x, signMarker.y,
				radius);
		// dw.paintPath(tri, cMarker.x, cMarker.y, radius * 2, radius * 2,
		// currentAngle + 90, 1);
	}

}
