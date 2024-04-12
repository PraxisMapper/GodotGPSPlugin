package org.praxismapper.gps

import android.annotation.TargetApi
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
import androidx.core.app.NotificationCompat
import org.godotengine.godot.Dictionary
import org.godotengine.godot.Godot
import org.godotengine.godot.GodotLib
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

class TestForegroundService: Service() {

    @TargetApi(Build.VERSION_CODES.Q)
    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    @TargetApi(Build.VERSION_CODES.Q)
    override fun onCreate(){
        Log.d("PraxisForeground", "Service called, activating foreground")
        val channel = NotificationChannel(
            "864",
            "PraxisForegroundChannel",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "PraxisMapper channel for foreground service notification"

        var notificationManager = getSystemService<NotificationManager>(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        var note = NotificationCompat.Builder(this, "864").setSmallIcon(R.drawable.ic_notify_icon).build()
        note.category = Notification.CATEGORY_NAVIGATION
        startForeground(864, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        Log.d("PraxisForeground", "Call completed")
    }
}

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
        Log.d("PraxisGPS", "Entered OnMainCreate")
        //NOTE: these might already exist on the godot object passed in to the constructor.
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
            locman.requestLocationUpdates("gps", 500, 0.5f, this);
        }

        //Moved this here so it'll only get called once there's STUFF that exists.
        Log.d("PraxisGPS", "create foreground service")
        var intent = Intent(activity, TestForegroundService::class.java)
        intent.putExtra("name", "Praxis Foreground Test")
        activity?.startForegroundService(intent)
        Log.d("PraxisGPS", "foreground service call started")
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
