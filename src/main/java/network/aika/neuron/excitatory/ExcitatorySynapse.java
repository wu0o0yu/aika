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
package network.aika.neuron.excitatory;

import network.aika.neuron.*;

/**
 *
 * @author Lukas Molzberger
 */
public class ExcitatorySynapse extends Synapse<Neuron<?>, ExcitatoryNeuron<?>> {

    public static byte type;

    public ExcitatorySynapse() {
        super();
    }

    public ExcitatorySynapse(Neuron<?> input, ExcitatoryNeuron<?> output) {
        super(input, output);
    }

    @Override
    public byte getType() {
        return type;
    }

    public void setWeight(double weight) {
        super.setWeight(weight);
        if(isPropagate()) {
            input.getNeuron().setModified(true);
        }
        output.getNeuron().setModified(true);
    }

    public void updateWeight(double weightDelta) {
        super.updateWeight(weightDelta);

        getOutput().updateDirectConjunctiveBias(weightDelta);

        if(isPropagate()) {
            input.getNeuron().setModified(true);
        }
        output.getNeuron().setModified(true);
    }

    protected void link(Neuron in, Neuron out) {
        if(isPropagate()) {
            boolean dir = in.getId() < out.getId();
            (dir ? in : out).getLock().acquireWriteLock();
            (dir ? out : in).getLock().acquireWriteLock();

            out.addInputSynapse(this);
            in.addOutputSynapse(this);

            (dir ? in : out).getLock().releaseWriteLock();
            (dir ? out : in).getLock().releaseWriteLock();
        } else {
            out.getLock().acquireWriteLock();
            out.addInputSynapse(this);
            out.getLock().releaseWriteLock();
        }
    }

    protected void unlink(Neuron in, Neuron out) {
        if(isPropagate()) {
            boolean dir = in.getId() < out.getId();
            (dir ? in : out).getLock().acquireWriteLock();
            (dir ? out : in).getLock().acquireWriteLock();

            out.removeInputSynapse(this);
            in.removeOutputSynapse(this);

            (dir ? in : out).getLock().releaseWriteLock();
            (dir ? out : in).getLock().releaseWriteLock();
        } else {
            out.getLock().acquireWriteLock();
            out.removeInputSynapse(this);
            out.getLock().releaseWriteLock();
        }
    }
}
