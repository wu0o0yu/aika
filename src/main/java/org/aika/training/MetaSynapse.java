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


import org.aika.Model;
import org.aika.Neuron;
import org.aika.neuron.Synapse;
import org.aika.Writable;
import org.aika.neuron.Synapse;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 *
 * @author Lukas Molzberger
 */
public class MetaSynapse implements Writable {

    public double metaWeight;
    public double metaBias;
    public boolean metaRelativeRid;


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(metaWeight);
        out.writeDouble(metaBias);
        out.writeBoolean(metaRelativeRid);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        metaWeight = in.readDouble();
        metaBias = in.readDouble();
        metaRelativeRid = in.readBoolean();
    }


    /**
     *
     * @author Lukas Molzberger
     */
    public static class Builder extends Synapse.Builder {

        public double metaWeight;
        public double metaBias;
        public boolean metaRelativeRid;

        public Builder setMetaWeight(double metaWeight) {
            this.metaWeight = metaWeight;
            return this;
        }

        public Builder setMetaBias(double metaBias) {
            this.metaBias = metaBias;
            return this;
        }

        public Builder setMetaRelativeRid(boolean metaRelativeRid) {
            this.metaRelativeRid = metaRelativeRid;
            return this;
        }


        public Synapse getSynapse(Neuron outputNeuron) {
            Synapse s = super.getSynapse(outputNeuron);

            MetaSynapse ss = new MetaSynapse();
            ss.metaWeight = metaWeight;
            ss.metaBias = metaBias;
            ss.metaRelativeRid = metaRelativeRid;
            s.meta = ss;
            return s;
        }
    }

}
