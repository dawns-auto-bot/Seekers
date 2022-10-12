package com.example.seekers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun StepCounter(){
    val context = LocalContext.current

    val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    if (stepCounterSensor == null) {
        // show toast message, if there is no sensor in the device
        Toast.makeText(context.applicationContext, "No sensor detected on this device", Toast.LENGTH_SHORT).show()
    }
    
    val steps = rememberSaveable {
        mutableStateOf(0)
    }

    var running by rememberSaveable{
        mutableStateOf(false)
    }

    var initialSteps by rememberSaveable{
        mutableStateOf(-2)
    }

    //https://www.geeksforgeeks.org/proximity-sensor-in-android-app-using-jetpack-compose/
    val sensorEventListener = object: SensorEventListener{
        override fun onSensorChanged(event: SensorEvent) {
            if(event.sensor == stepCounterSensor){
                if(running){
                    event.values.firstOrNull()?.toInt().let { newSteps ->
                        if (initialSteps == -1) {
                            initialSteps = newSteps!!
                        }
                        val currentSteps = newSteps?.minus(initialSteps)
                        if (currentSteps != null) {
                            steps.value = currentSteps
                        }
                    }
                }
            //steps.value = event.values[0]
            }
        }


        override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {
            Log.d("something", "something")
        }
    }
        Button(onClick = {
            running=true
            sensorManager.registerListener(
                sensorEventListener,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Toast.makeText(context, "Start", Toast.LENGTH_SHORT).show()
        }) {
            Text("Start")

        }
        Spacer(modifier = Modifier.height(50.dp))
        Button(onClick = {
            running=false
            sensorManager.unregisterListener(sensorEventListener)
            Toast.makeText(context, "Stop", Toast.LENGTH_SHORT).show()
            initialSteps = -2
        }) {
            Text("Stop")

        }
        Text(text = steps.value.toString())
}
