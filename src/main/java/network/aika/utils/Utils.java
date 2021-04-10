/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.aika.utils;

/**
 *
 * @author Lukas Molzberger
 */
public class Utils {

    public static double TOLERANCE = 0.001;

    public static boolean belowTolerance(double x) {
        return Math.abs(x) < TOLERANCE;
    }

    public static void checkTolerance(double x) {
        if(Math.abs(x) < TOLERANCE)
            throw new BelowToleranceThresholdException();
    }

    public static double round(double x) {
        return Math.round(x * 1000.0) / 1000.0;
    }

    public static String collapseText(String txt, int length) {
        if (txt.length() <= 2 * length) {
            return txt;
        } else {
            return txt.substring(0, length) + "..." + txt.substring(txt.length() - length);
        }
    }

    public static String addPadding(String s, int targetSize) {
        StringBuilder sb = new StringBuilder();
        sb.append(s);
        for(int i = s.length(); i < targetSize; i++) {
            sb.append(' ');
        }

        return sb.toString();
    }
}
