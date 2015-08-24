# Introduction #

Open GPS Tracker uses the Android resource strings method to translate text to match the locale of a device.

If no specific language is found the default (English) text strings are used. By translating these English texts to an other language and placing these in a special locale directory a new language is usable.

# Details #

## Download ##

The defaults language files are stored in XML. The newest are always available at:
  * http://open-gpstracker.googlecode.com/svn/trunk/OpenGPSTracker/trunk/application/res/values/strings.xml
  * http://open-gpstracker.googlecode.com/svn/trunk/OpenGPSTracker/trunk/application/res/values/array.xml
  * http://open-gpstracker.googlecode.com/svn/trunk/OpenGPSTracker/trunk/application/res/values/units.xml

## Translate ##

The translation is done by changing the text between the XML tags in the language files.

For instance the speed graph was a title
`<string name="graphtype_timespeed">Speed over time</string>` the Dutch variant would then be `<string name="graphtype_timespeed">Snelheid tegen tijd</string>`.

Some string elements are not for translation. These are numbers, names and other fixed values. These elements can be skipped and are recognizable by the `translatable="false"` or `type="raw"`.

## Upload / Submit ##

You can put the translation on the roadmap by creating a new issue on [issue tracker](http://code.google.com/p/open-gpstracker/issues/entry) and attaching the files there. Also just emailing the XML files will be okay too.