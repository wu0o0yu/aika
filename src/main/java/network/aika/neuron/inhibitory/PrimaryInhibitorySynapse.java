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

import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Synapse;

/**
 *
 * @author Lukas Molzberger
 */
public class PrimaryInhibitorySynapse extends InhibitorySynapse {

    public static byte type;

    public PrimaryInhibitorySynapse() {
        super();
    }

    public PrimaryInhibitorySynapse(NeuronProvider input, NeuronProvider output) {
        super(input, output);
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public boolean isPropagate() {
        return true;
    }

    protected void link(Neuron in, Neuron out) {
        boolean dir = in.getId() < out.getId();
        (dir ? in : out).getLock().acquireWriteLock();
        (dir ? out : in).getLock().acquireWriteLock();

        out.addInputSynapse(this);
        in.addOutputSynapse(this);

        (dir ? in : out).getLock().releaseWriteLock();
        (dir ? out : in).getLock().releaseWriteLock();
    }

    protected void unlink(Neuron in, Neuron out) {
        boolean dir = in.getId() < out.getId();
        (dir ? in : out).getLock().acquireWriteLock();
        (dir ? out : in).getLock().acquireWriteLock();

        out.removeInputSynapse(this);
        in.removeOutputSynapse(this);

        (dir ? in : out).getLock().releaseWriteLock();
        (dir ? out : in).getLock().releaseWriteLock();
    }

    public static class Builder extends Synapse.Builder {
        @Override
        public Synapse createSynapse(NeuronProvider outputNeuron) {
            return new PrimaryInhibitorySynapse(inputNeuron, outputNeuron);
        }
    }
}
