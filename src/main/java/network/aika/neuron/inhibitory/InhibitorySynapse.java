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

/**
 *
 * @author Lukas Molzberger
 */
public class InhibitorySynapse extends Synapse<Neuron<?>, InhibitoryNeuron> {

    public static byte type;

    public InhibitorySynapse() {
        super();
    }

    public InhibitorySynapse(Neuron<?> input, InhibitoryNeuron output) {
        super(input, output);
        setPropagate(true);
    }

    @Override
    public byte getType() {
        return type;
    }

    public void setWeight(double weight) {
        super.setWeight(weight);
        input.getNeuron().setModified(true);
    }

    public void update(double weightDelta, boolean recurrent) {
        super.update(weightDelta, recurrent);
        input.getNeuron().setModified(true);
    }

    protected void link(Neuron in, Neuron out) {
        in.getLock().acquireWriteLock();
        in.addOutputSynapse(this);
        in.getLock().releaseWriteLock();
    }

    protected void unlink(Neuron in, Neuron out) {
        in.getLock().acquireWriteLock();
        in.removeOutputSynapse(this);
        in.getLock().releaseWriteLock();
    }
}
