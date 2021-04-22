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
package network.aika.neuron.activation.scopes;

import network.aika.neuron.activation.direction.Direction;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class Scope {
    private String label;

    private Set<Transition> inputs = new TreeSet<>(getComparator(OUTPUT));
    private Set<Transition> outputs = new TreeSet<>(getComparator(INPUT));

    private static Comparator<Transition> getComparator(Direction dir) {
        return Comparator.<Transition, String>comparing(t -> t.getType().getSimpleName())
                        .thenComparing(t -> dir.getScope(t).getLabel())
                        .thenComparingInt(t -> t.isTarget() ? 1 : 0);
    }

    public Scope(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public Set<Transition> getInputs() {
        return inputs;
    }

    public Set<Transition> getOutputs() {
        return outputs;
    }

    public String toString() {
        return label;
    }
}
