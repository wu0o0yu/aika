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

import org.aika.corpus.Range.Operator;
import org.aika.corpus.Range.Mapping;
import org.aika.neuron.Synapse;


/**
 * The {@code Input} class is just a helper class which is used to initialize a neuron. Most of the parameters of this class
 * will be mapped to a input synapse for this neuron.
 *
 * @author Lukas Molzberger
 */
public class Input implements Comparable<Input> {
    boolean recurrent;
    Neuron neuron;
    float weight;
    double biasDelta;

    Operator startRangeMatch = Operator.NONE;
    Operator endRangeMatch = Operator.NONE;
    boolean startRangeOutput;
    boolean endRangeOutput;
    Mapping startMapping = Mapping.START;
    Mapping endMapping = Mapping.END;

    Integer relativeRid;
    Integer absoluteRid;


    /**
     * The property <code>recurrent</code> specifies if input is a recurrent feedback link. Recurrent
     * feedback links can be either negative or positive depending on the weight of the synapse. Recurrent feedback links
     * kind of allow to use future information as inputs of a current neuron. Aika allows this by making assumptions about
     * the recurrent input neuron. The class <code>SearchNode</code> modifies these assumptions until the best interpretation
     * for this document is found.
     *
     * @param recurrent
     * @return
     */
    public Input setRecurrent(boolean recurrent) {
        this.recurrent = recurrent;
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
     * The synapse weight of this input.
     *
     * @param weight
     * @return
     */
    public Input setWeight(float weight) {
        this.weight = weight;
        return this;
    }

    /**
     * The bias delta of this input that will later on be added to the neurons bias.
     *
     * @param biasDelta
     * @return
     */
    public Input setBiasDelta(double biasDelta) {
        this.biasDelta = biasDelta;
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
     * If set to true then the range begin of this inputs activation needs to match.
     *
     * @return
     */
    public Input setStartRangeMatch(Operator rm) {
        this.startRangeMatch = rm;
        return this;
    }

    /**
     * If set to true then the range end of this inputs activation needs to match.
     *
     * @return
     */
    public Input setEndRangeMatch(Operator rm) {
        this.endRangeMatch = rm;
        return this;
    }


    public enum RangeRelation {
        EQUALS,
        CONTAINS,
        CONTAINED_IN,
        NONE
    }


    /**
     * <code>setRangeMatch</code> is just a convenience function to call both <code>setStartRangeMatch</code> and <code>setEndRangeMatch</code> at the same time.
     *
     * @param rr
     * @return
     */
    public Input setRangeMatch(RangeRelation rr) {
        switch(rr) {
            case EQUALS:
                setStartRangeMatch(Operator.EQUALS);
                setEndRangeMatch(Operator.EQUALS);
                break;
            case CONTAINS:
                setStartRangeMatch(Operator.LESS_THAN_EQUAL);
                setEndRangeMatch(Operator.GREATER_THAN_EQUAL);
                break;
            case CONTAINED_IN:
                setStartRangeMatch(Operator.GREATER_THAN_EQUAL);
                setEndRangeMatch(Operator.LESS_THAN_EQUAL);
                break;
            default:
                setStartRangeMatch(Operator.NONE);
                setEndRangeMatch(Operator.NONE);
        }
        return this;
    }


    /**
     * <code>setRangeOutput</code> is just a convenience function to call <code>setStartRangeOutput</code> and <code>setEndRangeOutput</code> at the same time.
     *
     * @param ro
     * @return
     */
    public Input setRangeOutput(boolean ro) {
        setStartRangeOutput(ro);
        setEndRangeOutput(ro);
        return this;
    }


    /**
     * Determines if this input is used to compute the range start of the output activation.
     *
     * @param ro
     * @return
     */
    public Input setStartRangeOutput(boolean ro) {
        this.startRangeOutput = ro;
        return this;
    }


    /**
     * Determines if this input is used to compute the range end of the output activation.
     *
     * @param ro
     * @return
     */
    public Input setEndRangeOutput(boolean ro) {
        this.endRangeOutput = ro;
        return this;
    }


    /**
     * Using this method the range end of the input activation might be mapped to the range begin of the
     * output activation. By default the range begin of the input activation is mapped to the range begin
     * of the output activation.
     *
     * @param startMapping
     * @return
     */
    public Input setStartRangeMapping(Mapping startMapping) {
        this.startMapping = startMapping;
        return this;
    }


    /**
     * Using this method the range begin of the input activation might be mapped to the range end of the
     * output activation. By default the range end of the input activation is mapped to the range end
     * of the output activation.
     *
     * @param endMapping
     * @return
     */
    public Input setEndRangeMapping(Mapping endMapping) {
        this.endMapping = endMapping;
        return this;
    }


    Synapse getSynapse(Neuron outputNeuron) {
        Synapse s = new Synapse(
                neuron.m,
                neuron,
                outputNeuron,
                new Synapse.Key(
                        recurrent,
                        relativeRid,
                        absoluteRid,
                        startRangeMatch,
                        startMapping,
                        startRangeOutput,
                        endRangeMatch,
                        endMapping,
                        endRangeOutput
                )
        );

        Synapse os = outputNeuron.get().inputSynapses.get(s);
        if(os != null) return os;

        os = neuron.get().outputSynapses.get(s);
        if(os != null) return os;

        return s;
    }


    @Override
    public int compareTo(Input in) {
        int r = Double.compare(weight, in.weight);
        if(r != 0) return r;
        r = Double.compare(biasDelta, in.biasDelta);
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
        r = Utils.compareInteger(startMapping.ordinal(), in.startMapping.ordinal());
        if (r != 0) return r;
        r = Utils.compareInteger(endMapping.ordinal(), in.endMapping.ordinal());
        return r;
    }
}
