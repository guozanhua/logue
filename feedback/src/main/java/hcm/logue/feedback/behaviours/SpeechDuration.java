/*
 * Loudness.java
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

package hcm.logue.feedback.behaviours;

import android.content.Context;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import hcm.ssj.core.event.Event;

/**
 * Created by Johnny on 01.12.2014.
 */
public class SpeechDuration extends Behaviour
{

    float _dur = 0;
    boolean _shouldSum= false;

    @Override
    public float parseEvent(Event event)
    {
        if(_shouldSum)
            _dur += event.dur / 1000.0f;
        else
            _dur = event.dur / 1000.0f;

        return _dur;
    }

    public boolean checkEvent(Event event)
    {
        if (event.name.equalsIgnoreCase(_event)
            && event.sender.equalsIgnoreCase(_sender)
            && event.state == Event.State.COMPLETED)
            return true;

        return false;
    }

    @Override
    protected void load(XmlPullParser xml, Context context)
    {
        try
        {
            xml.require(XmlPullParser.START_TAG, null, "behaviour");
        }
        catch (XmlPullParserException | IOException e)
        {
            Log.e(_tag, "error parsing config file", e);
        }

        _shouldSum = Boolean.getBoolean(xml.getAttributeValue(null, "sum"));
        super.load(xml, context);
    }
}
