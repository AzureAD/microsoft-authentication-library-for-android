// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.identity.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class DateTimeAdapter implements JsonDeserializer<Date>, JsonSerializer<Date> {

    private static final String TAG = "DateTimeAdapter";

    private final DateFormat mEnUsFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT,
            DateFormat.DEFAULT, Locale.US);

    private final DateFormat mLocalFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT,
            DateFormat.DEFAULT);

    private final DateFormat mISO8601Format = buildIso8601Format();

    private static DateFormat buildIso8601Format() {
        DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return iso8601Format;
    }

    /**
     * Default constructor for {@link DateTimeAdapter}.
     */
    public DateTimeAdapter() {
        // Default constructor, intentionally empty.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Date deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException {
        String jsonString = json.getAsString();

        // Datetime string is serialized with iso8601 format by default, should
        // always try to deserialize with iso8601.
        try {
            return mISO8601Format.parse(jsonString);
        } catch (final ParseException ignored) {
//            Logger.v(TAG, "Cannot parse with ISO8601, try again with local format.");
            throw new JsonParseException("Could not parse date: " + jsonString);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized JsonElement serialize(Date src, Type typeOfSrc,
            JsonSerializationContext context) {
        return new JsonPrimitive(mISO8601Format.format(src));
    }
}
