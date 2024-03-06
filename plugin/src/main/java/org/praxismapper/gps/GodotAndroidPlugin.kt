package org.praxismapper.gps

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.ArraySet
import android.util.Log
import android.view.View
import org.godotengine.godot.Dictionary
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot


class GodotAndroidPlugin(godot: Godot): GodotPlugin(godot), LocationListener {

    //override fun getPluginName() = BuildConfig.GODOT_PLUGIN_NAME

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
        Log.d("PraxisGPS", "Entered OnMainCreate")
        locman = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return super.onMainCreate(activity)
    }

    @UsedByGodot
    fun StartListening(){
        Log.d("PraxisGPS", "StartListening called")
        this.runOnUiThread() {
            locman.requestLocationUpdates("gps", 500, 0.5f, this);
        }
    }

    override fun getPluginSignals(): Set<SignalInfo> {
        return setOf(locationUpdateSignal, lastKnownLocationSignal, errorSignal)
    }

    override fun getPluginMethods(): List<String> {
        return listOf(
            "StartListening"
        )
    }

    override fun getPluginName(): String {
        return "PraxisMapperGPSPlugin"
    }

    override fun onLocationChanged(location: Location) {
            Log.d("PraxisGPS", "Location Changed")
            val longitude = location.longitude.toFloat()
            val latitude = location.latitude.toFloat()
            val accuracy = location.accuracy
            var verticalAccuracyMeters = 0.0f
            val altitude = location.altitude.toFloat()
            val speed = location.speed
            val time = location.time.toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                verticalAccuracyMeters = location.verticalAccuracyMeters
            }
            val locationDictionary = Dictionary()
            locationDictionary["longitude"] = longitude
            locationDictionary["latitude"] = latitude
            locationDictionary["accuracy"] = accuracy
            locationDictionary["verticalAccuracyMeters"] = verticalAccuracyMeters
            locationDictionary["altitude"] = altitude
            locationDictionary["speed"] = speed
            locationDictionary["time"] = time
            emitSignal(locationUpdateSignal.name, locationDictionary)

    }

    private fun emitError(signalInfo: SignalInfo, errorCodes: ErrorCodes) {
        emitSignal(signalInfo.name, errorCodes.errorCode, errorCodes.message)
    }

}
