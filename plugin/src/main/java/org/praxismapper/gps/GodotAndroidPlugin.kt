package org.praxismapper.gps

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.View
import org.godotengine.godot.Dictionary
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

class GodotAndroidPlugin(godot: Godot): GodotPlugin(godot), LocationListener {

    //override fun getPluginName() = BuildConfig.GODOT_PLUGIN_NAME
    var timeDelay: Long = 500
    var distMin: Float = 0.5f
    lateinit var lastLocation: Dictionary

    enum class ErrorCodes(val errorCode: Int, val message: String) {
        ACTIVITY_NOT_FOUND(100, "Godot Activity is null!"),
        LOCATION_UPDATES_NULL(
            101,
            "Location Updates object is null!"
        ),
        LAST_KNOWN_LOCATION_NULL(
            102,
            "Last Know Location object is null!"
        ),
        LOCATION_PERMISSION_MISSING(103, "Missing location permissions!");
    }

    //TODO: figure out what i can do with foreground services and FOREGROUND_SERVICE_LOCATION

    private lateinit var locman: LocationManager
    private var loc: Location? = null

    private val locationUpdateSignal =
        SignalInfo("onLocationUpdates", Dictionary::class.java)
    private val lastKnownLocationSignal =
        SignalInfo("onLastKnownLocation", Dictionary::class.java)
    private val errorSignal =
        SignalInfo("onLocationError", Int::class.javaObjectType, String::class.java)

    @TargetApi(Build.VERSION_CODES.S)
    override fun onMainCreate(activity: Activity?): View? {
        locman = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return super.onMainCreate(activity)
    }

    private fun getDisplayCompat(): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.display
        } else {
            @Suppress("DEPRECATION")
            (activity?.windowManager?.defaultDisplay)
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    @UsedByGodot
    fun StartListening(){
        Log.d("PraxisGPS", "StartListening called")
        this.runOnUiThread() {
            locman.requestLocationUpdates("gps", timeDelay, distMin, this);
        }
        var startLoc = locman.getLastKnownLocation("gps")
        if (startLoc != null)
        {
            Log.d("PraxisGPS", "Sending last known location")
            onLocationChanged(startLoc)
        }
        else
        {
            Log.d("PraxisGPS", "No last known location, not updating app")
        }
    }

    @UsedByGodot
    fun SetMinTimeMs(newTime: Long)
    {
        timeDelay = newTime
    }

    @UsedByGodot
    fun SetMinDistMeters(newDist: Float)
    {
        distMin = newDist
    }

    @UsedByGodot
    fun GetLastLocation(): Dictionary {
        return lastLocation
    }

    override fun getPluginSignals(): Set<SignalInfo> {
        return setOf(locationUpdateSignal, lastKnownLocationSignal, errorSignal)
    }

    override fun getPluginMethods(): List<String> {
        return listOf(
            "StartListening",
            "SetMinTimeMs",
            "SetMinDistMeters",
            "GetLastLocation"
        )
    }

    override fun getPluginName(): String {
        return "PraxisMapperGPSPlugin"
    }

    override fun onLocationChanged(location: Location) {
            val longitude = location.longitude.toFloat()
            val latitude = location.latitude.toFloat()
            val accuracy = location.accuracy
            var verticalAccuracyMeters = 0.0f
            val altitude = location.altitude.toFloat()
            val speed = location.speed
            val time = location.time
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                verticalAccuracyMeters = location.verticalAccuracyMeters
            }
            val bearing = location.bearing
            val locationDictionary = Dictionary()
            locationDictionary["longitude"] = longitude
            locationDictionary["latitude"] = latitude
            locationDictionary["accuracy"] = accuracy
            locationDictionary["verticalAccuracyMeters"] = verticalAccuracyMeters
            locationDictionary["altitude"] = altitude
            locationDictionary["speed"] = speed
            locationDictionary["time"] = time
            locationDictionary["bearing"] = bearing
            lastLocation = locationDictionary
            emitSignal(locationUpdateSignal.name, locationDictionary)

    }

    private fun emitError(signalInfo: SignalInfo, errorCodes: ErrorCodes) {
        emitSignal(signalInfo.name, errorCodes.errorCode, errorCodes.message)
    }
}
