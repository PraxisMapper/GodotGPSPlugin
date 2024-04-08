package org.praxismapper.gps

import android.annotation.TargetApi
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Display
import android.view.View
import org.godotengine.godot.Dictionary
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

var gravity:FloatArray = floatArrayOf(0f,0f,0f)
var magnetic:FloatArray = floatArrayOf(0f,0f,0f)
var R:FloatArray = floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f)
var R2:FloatArray = floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f)
var I:FloatArray = floatArrayOf(0f,0f,0f,0f,0f,0f,0f,0f,0f)
var lastEmittedHeadingTime:Long = 0
var heading:Int = 0

class TestForegroundService: Service() {
    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

}

class GodotAndroidPlugin(godot: Godot): GodotPlugin(godot), LocationListener, SensorEventListener {

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

    //TODO: figure out what i can do with foreground services and FOREGROUND_SERVICE_LOCATION

    private lateinit var locman: LocationManager
    private var loc: Location? = null
    private lateinit var sensorman: SensorManager

    private val locationUpdateSignal =
        SignalInfo("onLocationUpdates", Dictionary::class.java)
    private val lastKnownLocationSignal =
        SignalInfo("onLastKnownLocation", Dictionary::class.java)
    private val errorSignal =
        SignalInfo("onLocationError", Int::class.javaObjectType, String::class.java)
    private val headingSignal =
        SignalInfo("onHeadingChange", Int::class.javaObjectType)

    @TargetApi(Build.VERSION_CODES.S)
    override fun onMainCreate(activity: Activity?): View? {
        Log.d("PraxisGPS", "Entered OnMainCreate")
        locman = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sensorman = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        var compassAccel = sensorman.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (compassAccel != null)
        {
            Log.d("PraxisGPS", "RotationVector found")
            sensorman.registerListener(this, compassAccel, SensorManager.SENSOR_DELAY_NORMAL, 1000000);
        }
        else
        {
            Log.d("PraxisGPS", "Rotation sensor NOT found")
        }

        var compassMagnetic = sensorman.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (compassMagnetic != null)
        {
            Log.d("PraxisGPS", "Magnetic sensor found")
            sensorman.registerListener(this, compassMagnetic, SensorManager.SENSOR_DELAY_NORMAL, 1000000);
        }
        else
        {
            Log.d("PraxisGPS", "Magnetic sensor NOT found")
        }
        return super.onMainCreate(activity)
    }

    override fun onSensorChanged(event: SensorEvent)
    {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = floatArrayOf(event.values[0], event.values[1], event.values[2])
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetic = event.values
            return
        }

        if (SensorManager.getRotationMatrix(R, I, gravity, magnetic))
        {
            var orientation = floatArrayOf(0f, 0f, 0f)
            SensorManager.getOrientation(R, orientation)
            //Some debounce math here so that we get some resistance to shaking
            heading = ((heading * 0.7) + (0.3 * Math.toDegrees(orientation[0].toDouble()).toInt())).toInt()

            if (event.timestamp - lastEmittedHeadingTime > 200000000) {
                emitSignal(headingSignal.name, heading);
                lastEmittedHeadingTime = event.timestamp
            }
        }
        else
        {
            Log.d("PraxisGPS", "No heading detectable - get rotation matrix failed")
        }
    }

    private fun getDisplayCompat(): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.display
        } else {
            @Suppress("DEPRECATION")
            (activity?.windowManager?.defaultDisplay)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //Disregard, not concerned with this on a compass
    }

    @TargetApi(Build.VERSION_CODES.O)
    @UsedByGodot
    fun StartListening(){
        Log.d("PraxisGPS", "StartListening called")
        this.runOnUiThread() {
            locman.requestLocationUpdates("gps", 500, 0.5f, this);
        }
    }

    override fun getPluginSignals(): Set<SignalInfo> {
        return setOf(locationUpdateSignal, lastKnownLocationSignal, errorSignal, headingSignal)
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
            emitSignal(locationUpdateSignal.name, locationDictionary)

    }

    private fun emitError(signalInfo: SignalInfo, errorCodes: ErrorCodes) {
        emitSignal(signalInfo.name, errorCodes.errorCode, errorCodes.message)
    }

}
