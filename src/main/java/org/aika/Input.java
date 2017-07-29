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
import org.aika.neuron.Synapse.RangeSignal;
import org.aika.neuron.Synapse.RangeMatch;


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
    public RangeMatch startRangeMatch = RangeMatch.NONE;
    public RangeMatch endRangeMatch = RangeMatch.NONE;
    public boolean startRangeOutput;
    public boolean endRangeOutput;
    public RangeSignal startSignal = Synapse.RangeSignal.START;
    public RangeSignal endSignal = Synapse.RangeSignal.END;

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
     * value is used to compute the neurons bias. It is only applied in the createAndNeuron method and only affects inputs with a positive weight and the optional flag set to false.
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
     * @return
     */
    public Input setStartRangeMatch(RangeMatch rm) {
        assert !startRangeOutput || rm == RangeMatch.EQUALS;
        this.startRangeMatch = rm;
        return this;
    }

    public Input setEndRangeMatch(RangeMatch rm) {
        assert !endRangeOutput || rm == RangeMatch.EQUALS;
        this.endRangeMatch = rm;
        return this;
    }


    public enum RangeRelation {
        EQUALS,
        CONTAINS,
        CONTAINED_IN,
        NONE
    }


    public Input setRangeMatch(RangeRelation rr) {
        switch(rr) {
            case EQUALS:
                setStartRangeMatch(RangeMatch.EQUALS);
                setEndRangeMatch(RangeMatch.EQUALS);
                break;
            case CONTAINS:
                setStartRangeMatch(RangeMatch.LESS_THAN);
                setEndRangeMatch(RangeMatch.GREATER_THAN);
                break;
            case CONTAINED_IN:
                setStartRangeMatch(RangeMatch.GREATER_THAN);
                setEndRangeMatch(RangeMatch.LESS_THAN);
                break;
            default:
                setStartRangeMatch(RangeMatch.NONE);
                setEndRangeMatch(RangeMatch.NONE);
        }
        return this;
    }


    /**
     * Determines if this input is used to compute the range of the output activation.
     *
     * @param ro
     * @return
     */
    public Input setRangeOutput(boolean ro) {
        setStartRangeOutput(ro);
        setEndRangeOutput(ro);
        return this;
    }


    public Input setStartRangeOutput(boolean ro) {
        this.startRangeOutput = ro;
        if(ro) {
            setStartRangeMatch(RangeMatch.EQUALS);
        }
        return this;
    }


    public Input setEndRangeOutput(boolean ro) {
        this.endRangeOutput = ro;
        if(ro) {
            setEndRangeMatch(RangeMatch.EQUALS);
        }
        return this;
    }


    public Input setStartRangeSignal(RangeSignal startSignal) {
        this.startSignal = startSignal;
        return this;
    }


    public Input setEndRangeSignal(RangeSignal endSignal) {
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
        r = startRangeMatch.compareTo(in.startRangeMatch);
        if(r != 0) return r;
        r = endRangeMatch.compareTo(in.endRangeMatch);
        if(r != 0) return r;
        r = Utils.compareInteger(relativeRid, in.relativeRid);
        if (r != 0) return r;
        r = Utils.compareInteger(absoluteRid, in.absoluteRid);
        if (r != 0) return r;
        r = Boolean.compare(startRangeOutput, in.startRangeOutput);
        if (r != 0) return r;
        r = Boolean.compare(endRangeOutput, in.endRangeOutput);
        if (r != 0) return r;
        r = Utils.compareInteger(startSignal.ordinal(), in.startSignal.ordinal());
        if (r != 0) return r;
        r = Utils.compareInteger(endSignal.ordinal(), in.endSignal.ordinal());
        return r;
    }
}
