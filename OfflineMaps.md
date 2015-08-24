# Introduction #

By using the osmdroid library to render OSM maps the method for off-line maps described at [osmdroid](http://code.google.com/p/osmdroid/wiki/MobileAtlasCreator) is also applicable to Open GPS Tracker.

# Approach #

The approach is to use the [Mobile Atlas Creator](http://mobac.sourceforge.net/) to bundle all the maps you will need into a ZIP file and store that on you Android device.

# Steps #

## Mobile Atlas Creator ##

First download, install and run [Mobile Atlas Creator](http://mobac.sourceforge.net/).

## Selection ##

On the map of Mobile Atlas Creator select a part of the world that you wish to make available off-line. Choose a section as small as possible that still fits your needs. This speeds up downloading and reduces the file size.

## Map source ##

Choose **Openstreetmap Mapnik** or **Openstreemap Cyclemap** depending on which OSM overlay you will be using off-line in Open GPS Tracker.

## Zoom levels ##

Choose the zoom levels you will be needing off-line. Level 18 is zoomed in all the way and level 0 is zoomed out all the way. The number of tiles that will be downloaded will vary depending on the number of zoom levels you select.

## Atlas content ##

All the preparations to specify a bundle of map data need to be confirmed by the **Add selection** button.

## Atlas settings ##

It is important to set the Format in Atlas settings to **Osmdroid ZIP**. This is the format that Open GPS Tracker can use through its osmdroid library.

## Saved profiles ##

In the profiles part of the screen you can name and create ZIP file by clicking **Save** and **Create atlas**.

## Install ##

Copy the resulting zip to you Android device SD-card. Place the file in the osmdroid folder. It is likely that one is already created. You can recognize that folder by the tiles subfolder. Be sure to place the ZIP next to that tiles folder.

![http://open-gpstracker.googlecode.com/svn/wiki/images/tile_location.png](http://open-gpstracker.googlecode.com/svn/wiki/images/tile_location.png)