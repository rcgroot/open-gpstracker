/*
 * Copyright (C) 2010  Just Objects B.V.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opentraces.metatracker.activity;


import android.content.SharedPreferences;
import android.location.Location;
import org.opentraces.metatracker.Constants;

public class TrackingState
{
	public int loggingState;
	public boolean newRoadRating;
	public int roadRating;
	public float distance;
	public Track track = new Track();
	public Segment segment = new Segment();
	public Waypoint waypoint = new Waypoint();
	private SharedPreferences sharedPreferences;

	public TrackingState(SharedPreferences sharedPreferences) {
		this.sharedPreferences = sharedPreferences;
		reset();
	}
	
	public void load()
	{
		distance = sharedPreferences.getFloat("distance", 0.0f);

		track.load();
		waypoint.load();
	}

	public void save()
	{
		sharedPreferences.edit().putFloat("distance", distance).commit();
		track.save();
		waypoint.save();
	}

	public void reset()
	{
		loggingState = Constants.DOWN;
		distance = 0.0f;
		waypoint.reset();
		track.reset();
	}

	public class Track
	{
		public int id = -1;
		public String name = "NO TRACK";
		public long creationTime;

		public void load()
		{
			id = sharedPreferences.getInt("track.id", -1);
			name = sharedPreferences.getString("track.name", "NO TRACK");
		}

		public void save()
		{
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putInt("track.id", id);
			editor.putString("track.name", name);
			editor.commit();
		}

		public void reset()
		{
			name = "NO TRACK";
			id = -1;
		}
	}

	public static class Segment
	{
		public int id = -1;

	}

	public class Waypoint
	{
		public int id = -1;
		public Location location;
		public float speed;
		public float accuracy;
		public int count;

		public void load()
		{
			id = sharedPreferences.getInt("waypoint.id", -1);
			count = sharedPreferences.getInt("waypoint.count", 0);
		}

		public void save()
		{
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putInt("waypoint.id", id);
			editor.putInt("waypoint.count", count);
			editor.commit();
		}

		public void reset()
		{
			count = 0;
			id = -1;
			location = null;
		}
	}
}


