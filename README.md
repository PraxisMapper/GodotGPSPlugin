# PraxisMapper Godot GPS Plugin
This plugin allows a Godot (4.2.1+) app to get the user's location via GPS. Does not depend on Google Play Services, so 
this is usable on phones that don't include Google Play Services.

## Contents
* Preconfigured source and config files to build the AAR files used by the plugin
* A drag-and-drop complete addon for any Godot 4.2.1+ project.(the addons folder at the root level)

## Godot Plugin Installation
Drag the addons folder from this repo into your Godot app's folder structure. 

## Building the Android plugin From Source
**Note:** [Android Studio](https://developer.android.com/studio) is the recommended IDE for
developing Godot Android plugins. 
You can install the latest version from https://developer.android.com/studio.

To use this template, log in to github and click the green "Use this template" button at the top 
of the repository page.
This will let you create a copy of this repository with a clean git history.

- In a terminal window, navigate to the project's root directory and run the following command:
```
./gradlew assemble
```
On successful completion of the build, the output files can be found in
[`plugin/demo/addons`](plugin/demo/addons)
-----
OR
-----
open the folder in Android Studio, and click Build/Make Project, and drag the .aar file from 
\plugin\build\outputs\aar to the \addons\PraxisMapperGPSPlugin folder of your Godot app.

# Usager (in PraxisMapper games)
This plugin is already configued and used in the PraxisMapper Godot Components. If you started your game from that template,
you have nothing else to do here.

# Usage (From Scratch)
* Drag and drop the \addons folder from this repo to your Godot Game
* * If you are compiling the library yourself, you'll want to copy your built files from \plugin\build\outputs\aar to \addons\PraxisMapperGPSPlugin afterwards
* Create an Android export template for your app, and ensure that the Coarse Location and Fine Locations permissions are checked.
* In Godot, click Project\Project Settings, then Plugins, and check 'Enable' in the row for PraxisMapperGPSPlugin
* Somewhere in your game, you will need to run code to load the plugin and activate similar to this:
```
var gps_provider

func _ready():
  #The rest of your startup code goes here as usual
  get_tree().on_request_permissions_result.connect(perm_check)
  
  #NOTE: OS.request_permissions() should be called from a button the user actively touches after being informed of 
  #what the button will enable.  This is placed in _ready() only to indicate this must be called, and how to structure
  #handling the 2 paths code can follow after calling it.

  var allowed = OS.request_permissions() 
  if allowed:
    enableGPS()

func permCheck(permName, wasGranted):
  if permName == "android.permission.ACCESS_FINE_LOCATION" and wasGranted == true:
    enableGPS()

func enableGPS():
  gps_provider = Engine.get_singleton("PraxisMapperGPSPlugin")
    if gps_provider != null:
      gps_provider.onLocationUpdates.connect(your_listener_function_here)
	    gps_provider.StartListening()
```

# Documentation

## StartListening()
Calling this method on the plugin tells it to start listening for and reporting location changes.
This is set to report no faster than every 500ms or 0.5 meters.

## signal onLocationUpdates(Dictionary)
The signal emitted by the plugin when a location update is detected.
Sends back a dictionary with the following keys:

latitude, longitude, accuracy, altitude, verticalAccuracyMeters, speed, time, bearing

NOTE: bearing will indicate the actual direction of movement, whereas the value from onHeadingChange 
reflects the direction the device is facing.

## signal onHeadingChange (int)
The signal emitted by the plugin to indicate the current heading.
0 is north, 90 is east, -90 is west, south is 180 or -180. Turning from south-east to south-west will result in the compass spinning the long way around.
This will fire approx. 5 times a second if the heading changes, will not emit if the value is the same as the previous one.
