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
package org.aika.training;


import org.aika.Input;
import org.aika.Neuron;
import org.aika.neuron.Synapse;

/**
 *
 * @author Lukas Molzberger
 */
public class MetaInput extends Input {

    public double metaWeight;
    public double metaBias;
    public boolean metaRelativeRid;

    public MetaInput setMetaWeight(double metaWeight) {
        this.metaWeight = metaWeight;
        return this;
    }

    public MetaInput setMetaBias(double metaBias) {
        this.metaBias = metaBias;
        return this;
    }

    public MetaInput setMetaRelativeRid(boolean metaRelativeRid) {
        this.metaRelativeRid = metaRelativeRid;
        return this;
    }


    protected Synapse getSynapse(Neuron outputNeuron) {
        Synapse s = super.getSynapse(outputNeuron);

        MetaSynapse ss = new MetaSynapse();
        ss.metaWeight = metaWeight;
        ss.metaBias = metaBias;
        ss.metaRelativeRid = metaRelativeRid;
        s.meta = ss;
        return s;
    }
}
