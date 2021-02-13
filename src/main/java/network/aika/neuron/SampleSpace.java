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
package network.aika.neuron;

import network.aika.Model;
import network.aika.utils.Writable;
import network.aika.neuron.activation.Reference;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The <a href="https://en.wikipedia.org/wiki/Sample_space}">Sample Space</a> keeps track of the number of
 * training instances a certain neuron or synapse has encountered. The Sample Space is used
 * to convert the counted frequencies to probabilities.
 *
 * @author Lukas Molzberger
 */
public class SampleSpace implements Writable {

    private double N = 0;
    private Integer lastPos;

    public double getN() {
        return N;
    }

    public void setN(int N) {
        this.N = N;
    }

    public Integer getLastPos() {
        return lastPos;
    }

    public void setLastPos(int lastPos) {
        this.lastPos = lastPos;
    }

    public void update(Model m, Reference ref) {
        int n = 0;

        if(lastPos != null) {
            n = getAbsoluteBegin(m, ref) - lastPos;
        }
        assert n >= 0;

        N += 1 + n / ref.length();

        Integer newPos = getAbsoluteEnd(m, ref);
        assert lastPos == null || newPos > lastPos;

        lastPos = newPos;
    }

    public int getAbsoluteBegin(Model m, Reference ref) {
        return m.getN() + (ref != null ? ref.getBegin() : 0);
    }

    public int getAbsoluteEnd(Model m, Reference ref) {
        return m.getN() + (ref != null ? ref.getEnd() : 0);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(N);
        out.writeBoolean(lastPos != null);
        if(lastPos != null) {
            out.writeInt(lastPos);
        }
    }

    public static SampleSpace read(DataInput in, Model m) throws IOException {
        SampleSpace sampleSpace = new SampleSpace();
        sampleSpace.readFields(in, m);
        return sampleSpace;
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        N = in.readDouble();
        if(in.readBoolean()) {
            lastPos = in.readInt();
        }
    }

    public String toString() {
        return "N:" + N + " lastPos:" + lastPos;
    }
}
