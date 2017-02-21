/*
 * TactileFeedback.java
 * Copyright (c) 2015
 * Author: Ionut Damian
 * *****************************************************
 * This file is part of the Logue project developed at the Lab for Human Centered Multimedia
 * of the University of Augsburg.
 *
 * The applications and libraries are free software; you can redistribute them and/or modify them
 * under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or any later version.
 *
 * The software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package hcm.logue.feedback.classes;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.security.InvalidParameterException;

import hcm.logue.feedback.BandComm;
import hcm.logue.feedback.Console;
import hcm.logue.feedback.events.Event;
import hcm.logue.feedback.events.TactileEvent;
import hcm.ssj.core.Cons;
import hcm.ssj.myo.Vibrate2Command;


/**
 * Created by Johnny on 01.12.2014.
 */
public class TactileFeedback extends Feedback
{
    enum Device
    {
        Myo,
        MsBand
    }

    Activity activity;

    boolean firstCall = true;
    Myo myo = null;
    BandComm msband = null;
    Vibrate2Command cmd = null;

    long lock = 0;
    byte intensityNew[] = null;

    Device deviceType = Device.Myo;

    public TactileFeedback(Activity activity)
    {
        this.activity = activity;
        type = Type.Tactile;
    }

    public void firstCall()
    {
        if(deviceType == Device.Myo) {
            Hub hub = Hub.getInstance();

            long time = SystemClock.elapsedRealtime();
            while (hub.getConnectedDevices().isEmpty() && SystemClock.elapsedRealtime() - time < Cons.WAIT_SENSOR_CONNECT) {
                try {
                    Thread.sleep(Cons.SLEEP_ON_IDLE);
                } catch (InterruptedException e) {
                }
            }

            if (hub.getConnectedDevices().isEmpty())
                throw new RuntimeException("device not found");

            Console.print("connected to Myo");

            myo = hub.getConnectedDevices().get(0);
            cmd = new Vibrate2Command(hub);
        }
        else if(deviceType == Device.MsBand)
        {
            msband = new BandComm(null);
        }


        firstCall = false;
    }

    @Override
    public boolean execute(Event event)
    {
        if(firstCall)
            firstCall();

        //update only if the global lock has passed
        if(System.currentTimeMillis() < lock)
        {
            Log.i(name, "ignoring event, lock active for another " + (lock - System.currentTimeMillis()) + "ms");
            return false;
        }

        TactileEvent ev = (TactileEvent) event;
        if(ev == lastEvent)
        {
            //check lock
            //only execute if enough time has passed since last execution of this instance
            if (ev.lockSelf == -1 || System.currentTimeMillis() - ev.lastExecutionTime < ev.lockSelf)
                return false;

            if(deviceType == Device.Myo) {
                if (ev.multiplier != 1) {
                    intensityNew = multiply(intensityNew, ev.multiplier);
                }

                Log.i(name, "vibration " + ev.duration[0] + "/" + (int) intensityNew[0]);
                cmd.vibrate(myo, ev.duration, intensityNew);
            }
            else if(deviceType == Device.MsBand) {
                Log.i(name, "vibration " + ev.vibrationType);
                msband.vibrate(ev.vibrationType);
            }

        }
        else
        {
            if(deviceType == Device.Myo) {
                Log.i(name, "vibration " +  ev.duration[0] + "/" + (int)ev.intensity[0]);
                cmd.vibrate(myo, ev.duration, ev.intensity);

                if(intensityNew == null)
                    intensityNew = new byte[ev.intensity.length];
                System.arraycopy(ev.intensity, 0, intensityNew, 0, ev.intensity.length);
            }
            else if(deviceType == Device.MsBand) {
                Log.i(name, "vibration " + ev.vibrationType);
                msband.vibrate(ev.vibrationType);
            }
        }

        //set lock
        if(ev.lock > 0)
            lock = System.currentTimeMillis() + (long) ev.lock;
        else
            lock = 0;

        return true;
    }

    public byte[] multiply(byte[] src, float mult)
    {
        byte dst[] = new byte[src.length];

        int val_int;
        for(int i = 0; i < src.length; ++i)
        {
            val_int = (int)((int)src[i] * mult);
            if(val_int > 255)
                val_int = 255;

            dst[i] = (byte)val_int;
        }

        return dst;
    }

    protected void load(XmlPullParser xml, final Context context)
    {
        try {
            xml.require(XmlPullParser.START_TAG, null, "class");

            String device_name = xml.getAttributeValue(null, "device");
            if (device_name != null) {
                deviceType = Device.valueOf(device_name);
            }
        }
        catch(IOException | XmlPullParserException | InvalidParameterException e)
        {
            Log.e(tag, "error parsing config file", e);
        }

        super.load(xml, context);
    }
}
