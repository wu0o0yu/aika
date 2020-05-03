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
package network.aika.neuron.inhibitory;


import network.aika.neuron.*;

import static network.aika.neuron.PatternScope.SAME_PATTERN;


/**
 *
 * @author Lukas Molzberger
 */
public class InhibitorySynapse extends Synapse<INeuron, InhibitoryNeuron> {

    public static byte type;

    public InhibitorySynapse() {
        super();
    }

    public InhibitorySynapse(Neuron input, Neuron output) {
        super(input, output);
    }

    @Override
    public void init(PatternScope patternScope, Boolean isRecurrent, Boolean isNegative, boolean propagate) {

    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public boolean isRecurrent() {
        return false;
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public PatternScope getPatternScope() {
        return SAME_PATTERN;
    }

    @Override
    public boolean isPropagate() {
        return true;
    }

    protected void addLinkInternal(INeuron in, INeuron out) {
        in.addOutputSynapse(this);
    }

    protected void removeLinkInternal(INeuron in, INeuron out) {
        in.removeOutputSynapse(this);
    }


    public static class Builder extends Synapse.Builder {
        protected SynapseFactory getSynapseFactory() {
            return (input, output) -> new InhibitorySynapse(input, output);
        }
    }
}
