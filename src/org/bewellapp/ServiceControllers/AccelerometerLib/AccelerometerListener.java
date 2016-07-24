package org.bewellapp.ServiceControllers.AccelerometerLib;

public interface AccelerometerListener {

        public void onAccelerationChanged(long timestamp, float x, float y, float z);
        
        public void onShake(float force);
        
}