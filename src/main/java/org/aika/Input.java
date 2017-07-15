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
package org.aika;

import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;


/**
 *
 * @author Lukas Molzberger
 */
public class Input implements Comparable<Input> {
    public boolean recurrent;
    public boolean optional;
    public Neuron neuron;
    public double weight;
    public double maxLowerWeightsSum = Double.MAX_VALUE;
    public double minInput;
    public boolean matchRange = true;
    public Synapse.RangeVisibility startVisibility = Synapse.RangeVisibility.MAX_OUTPUT;
    public Synapse.RangeVisibility endVisibility = Synapse.RangeVisibility.MAX_OUTPUT;
    public Synapse.RangeSignal startSignal = Synapse.RangeSignal.START;
    public Synapse.RangeSignal endSignal = Synapse.RangeSignal.END;

    public Integer relativeRid;
    public Integer absoluteRid;


    /**
     * If recurrent is set to true, then this input will describe an feedback loop.
     * The input neuron may depend on the output of this neuron.
     *
     * @param recurrent
     * @return
     */
    public Input setRecurrent(boolean recurrent) {
        this.recurrent = recurrent;
        return this;
    }

    /**
     * If optional is set to true, then this input is an optional part of a conjunction.
     * This is only used for the method createAndNeuron.
     *
     * @param optional
     * @return
     */
    public Input setOptional(boolean optional) {
        this.optional = optional;
        return this;
    }

    /**
     * Determines the input neuron.
     *
     * @param neuron
     * @return
     */
    public Input setNeuron(Neuron neuron) {
        assert neuron != null;
        this.neuron = neuron;
        return this;
    }

    /**
     * MaxLowerWeightsSum is the expected sum of all weights smaller then the current weight. It is
     * used as an hint to compute the boolean representation of this neuron.
     *
     * @param maxLowerWeightsSum
     * @return
     */
    public Input setMaxLowerWeightsSum(double maxLowerWeightsSum) {
        this.maxLowerWeightsSum = maxLowerWeightsSum;
        return this;
    }

    /**
     * The synapse weight of this input.
     *
     * @param weight
     * @return
     */
    public Input setWeight(Double weight) {
        this.weight = weight;
        return this;
    }

    /**
     * The minimum activation value that is required for this input. The minInput
     * value is used to compute the neurons bias.
     *
     * @param minInput
     * @return
     */
    public Input setMinInput(double minInput) {
        this.minInput = minInput;
        return this;
    }

    /**
     * If the absolute relational id (rid) not null, then it is required to match the rid of input activation.
     *
     * @param absoluteRid
     * @return
     */
    public Input setAbsoluteRid(Integer absoluteRid) {
        this.absoluteRid = absoluteRid;
        return this;
    }

    /**
     * The relative relational id (rid) determines the relative position of this inputs rid with respect to
     * other inputs of this neuron.
     *
     * @param relativeRid
     * @return
     */
    public Input setRelativeRid(Integer relativeRid) {
        this.relativeRid = relativeRid;
        return this;
    }

    /**
     * If set to true then the range of this inputs activation needs to match.
     *
     * @param matchRange
     * @return
     */
    public Input setMatchRange(boolean matchRange) {
        this.matchRange = matchRange;
        return this;
    }

    /**
     * Determines if this input is used to compute the range begin of the output activation.
     *
     * @param rv
     * @return
     */
    public Input setStartVisibility(Synapse.RangeVisibility rv) {
        this.startVisibility = rv;
        return this;
    }

    /**
     * Determines if this input is used to compute the range end of the output activation.
     *
     * @param rv
     * @return
     */
    public Input setEndVisibility(Synapse.RangeVisibility rv) {
        this.endVisibility = rv;
        return this;
    }


    public Input setStartSignal(Synapse.RangeSignal startSignal) {
        this.startSignal = startSignal;
        return this;
    }


    public Input setEndSignal(Synapse.RangeSignal endSignal) {
        this.endSignal = endSignal;
        return this;
    }



    @Override
    public int compareTo(Input in) {
        int r = Double.compare(weight, in.weight);
        if(r != 0) return r;
        r = Double.compare(minInput, in.minInput);
        if(r != 0) return r;
        r = Boolean.compare(optional, in.optional);
        if(r != 0) return r;
        r = neuron.compareTo(in.neuron);
        if(r != 0) return r;
        r = Boolean.compare(matchRange, in.matchRange);
        if(r != 0) return r;
        r = Utils.compareInteger(relativeRid, in.relativeRid);
        if (r != 0) return r;
        r = Utils.compareInteger(absoluteRid, in.absoluteRid);
        if (r != 0) return r;
        r = Utils.compareInteger(startVisibility.ordinal(), in.startVisibility.ordinal());
        if (r != 0) return r;
        r = Utils.compareInteger(endVisibility.ordinal(), in.endVisibility.ordinal());
        if (r != 0) return r;
        r = Utils.compareInteger(startSignal.ordinal(), in.startSignal.ordinal());
        if (r != 0) return r;
        r = Utils.compareInteger(endSignal.ordinal(), in.endSignal.ordinal());
        return r;
    }
}
