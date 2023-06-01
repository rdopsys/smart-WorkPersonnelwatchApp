package com.dotsoft.smartsonia;

import java.text.DecimalFormat;

/**
 * BEACON MODEL CLASS
 */
public class BeaconModel{
    public int majorId;
    public double distance;
    public String distanceFormatted;

    BeaconModel(int majorId,double distance){
        this.majorId = majorId;
        this.distance = distance;
        // get only 2 num after . (1.23 not 1.234567)
        DecimalFormat df = new DecimalFormat("0.00");
        distanceFormatted = df.format(distance);
    }
}