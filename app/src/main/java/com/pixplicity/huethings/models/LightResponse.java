package com.pixplicity.huethings.models;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

/*
{
  "state":{
     "on":true,
     "bri":253,
     "hue":32722,
     "sat":173,
     "effect":"none",
     "xy":[
        0.0000,
        0.0000
     ],
     "alert":"none",
     "colormode":"hs",
     "reachable":false
  },
  "type":"Color light",
  "name":"Witte kastje",
  "modelid":"LLC001",
  "manufacturername":"Philips",
  "uniqueid":"00:17:88:01:00:0a:5b:3c-0b",
  "swversion":"2.0.0.5206"
}
 */
public class LightResponse {

    @SerializedName("state")
    public State state;

    @SerializedName("type")
    public String type;

    @SerializedName("name")
    public String name;

    @SerializedName("modelid")
    public String modelid;

    @SerializedName("manufacturername")
    public String manufacturername;

    @SerializedName("uniqueid")
    public String uniqueid;

    @SerializedName("swversion")
    public String swversion;

    public static class State {

        @SerializedName("on")
        public boolean on;

        @SerializedName("bri")
        public int bri;

        @SerializedName("hue")
        public int hue;

        @SerializedName("sat")
        public int sat;

        @SerializedName("effect")
        public String effect;

        @SerializedName("alert")
        public String alert;

        @SerializedName("colormode")
        public String colormode;

        @SerializedName("xy")
        public float[] xy;

        @SerializedName("reachable")
        public boolean reachable;

        @Override
        public String toString() {
            return "State{" +
                    "on=" + on +
                    ", bri=" + bri +
                    ", hue=" + hue +
                    ", sat=" + sat +
                    ", effect='" + effect + '\'' +
                    ", alert='" + alert + '\'' +
                    ", colormode='" + colormode + '\'' +
                    ", xy=" + Arrays.toString(xy) +
                    ", reachable=" + reachable +
                    '}';
        }

    }

}
