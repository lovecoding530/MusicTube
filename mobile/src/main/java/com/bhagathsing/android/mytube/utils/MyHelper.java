package com.bhagathsing.android.mytube.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Kangtle_R on 11/8/2017.
 */

public class MyHelper {
    public static int getSecondsFromISO(String isoString){
        final Pattern LENGTH_PATTERN = Pattern.compile("^PT(?:([0-9]+)H)?(?:([0-9]+)M)?(?:([0-9]+)S)?$", Pattern.CASE_INSENSITIVE);

        if (isoString == null) {
            return 0;
        }

        // Example: "PT2M58S" -- ISO-8601
        // Not really a compliant parser
        Matcher m = LENGTH_PATTERN.matcher(isoString);
        if (m.matches()) {
            String hr = m.group(1);
            String min = m.group(2);
            String sec = m.group(3);

            int duration = 0;
            if (hr != null)
                duration += Long.parseLong(hr) * 60 * 60;
            if (min != null)
                duration += Long.parseLong(min) * 60;
            if (sec != null)
                duration += Long.parseLong(sec);
            return duration; // Milliseconds
        }

        return 0;
    }
}
