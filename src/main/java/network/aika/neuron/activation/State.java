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

import network.aika.Utils;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.search.Decision;

import static network.aika.neuron.activation.search.Decision.EXCLUDED;
import static network.aika.neuron.activation.search.Decision.SELECTED;


/**
 * A <code>State</code> object contains the activation value of an activation object that belongs to a neuron.
 * It furthermore contains a weight that is used to check the interpretations during the search for the best
 * interpretation.
 *
 * @author Lukas Molzberger
 */
public class State {
    public final double lb;
    public final double ub;

    public final Fired firedLatest;
    public final Fired firedEarliest;

    public final double net;
    public final double weight;

    public static final State ZERO = new State(0.0, 0.0, 0.0, null, null, 0.0);


    public State(double lb, double ub, double net, Fired firedLatest, Fired firedEarliest, double weight) {
        assert !Double.isNaN(lb);
        this.lb = lb;
        this.ub = ub;
        this.net = net;
        this.firedLatest = firedLatest;
        this.firedEarliest = firedEarliest;
        this.weight = weight;
    }


    public boolean equals(State s) {
        return lowerBoundEquals(s) && upperBoundEquals(s);
    }


    public boolean lowerBoundEquals(State s) {
        return Math.abs(lb - s.lb) <= INeuron.WEIGHT_TOLERANCE;
    }

    public boolean upperBoundEquals(State s) {
        return Math.abs(ub - s.ub) <= INeuron.WEIGHT_TOLERANCE;
    }


    public boolean equalsWithWeights(State s) {
        return equals(s) && Math.abs(weight - s.weight) <= INeuron.WEIGHT_TOLERANCE;
    }


    public Decision getPreferredDecision() {
        return lb > 0.0 ? SELECTED : EXCLUDED;
    }


    public String toString() {
        return "V:" + Utils.round(lb) + " UB:" + (ub == Double.MAX_VALUE ? "MAX" : Utils.round(ub)) + " Net:" + Utils.round(net) + " W:" + Utils.round(weight);
    }
}
