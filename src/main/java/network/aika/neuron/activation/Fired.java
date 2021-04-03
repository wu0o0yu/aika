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
package network.aika.neuron.activation;

import java.util.Comparator;

/**
 *
 * @author Lukas Molzberger
 */
public class Fired implements Comparable<Fired> {

    public static Fired NOT_FIRED = new Fired(Integer.MAX_VALUE, Integer.MAX_VALUE);

    public static final Comparator<Fired> COMPARATOR = Comparator
            .<Fired>comparingInt(f -> f.inputTimestamp)
            .thenComparing(f -> f.fired);

    public static final Comparator<Fired> COMPARATOR_NF_FIRST = Comparator
            .<Fired>comparingInt(f -> f == NOT_FIRED ? 0 : 1)
            .thenComparing(f -> f.inputTimestamp)
            .thenComparing(f -> f.fired);

    private final int inputTimestamp;
    private final int fired;

    public Fired(int inputTimestamp, int fired) {
        this.inputTimestamp = inputTimestamp;
        this.fired = fired;
    }

    public int getInputTimestamp() {
        return inputTimestamp;
    }

    public int getFired() {
        return fired;
    }

    public String toString() {
        if(this == NOT_FIRED)
            return "[NOT_FIRED]";

        return "[" + inputTimestamp + "," + fired + "]";
    }

    @Override
    public int compareTo(Fired f) {
        return COMPARATOR_NF_FIRST.compare(this, f);
    }
}
