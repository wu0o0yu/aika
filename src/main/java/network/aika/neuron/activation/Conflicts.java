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
package network.aika.neuron.activation;


import network.aika.neuron.INeuron;

import java.util.*;

/**
 * The class {@code Conflicts} handles between different interpretation options for a given text.
 *
 * @author Lukas Molzberger
 */
public class Conflicts {

    public SortedSet<Activation> primary = new TreeSet<>();
    public Set<Activation> secondary = new TreeSet<>();


    public static boolean isConflicting(Activation a, Activation b) {
        if(a.conflicts.secondary.contains(b)) return true;
        if(b.conflicts.secondary.contains(a)) return true;

        return false;
    }


    public static void linkConflicts(Activation act, long v, Linker.Direction dir) {
        for (Activation.SynapseActivation sa : (dir == Linker.Direction.INPUT ? act.neuronInputs.values() : act.neuronOutputs)) {
            if(sa.synapse.isNegative() && sa.synapse.key.isRecurrent) {
                Activation oAct = (dir == Linker.Direction.INPUT ? act : sa.output);
                Activation iAct = (dir == Linker.Direction.INPUT ? sa.input : act);

                addConflict(oAct, iAct, v);
            }
        }
    }


    private static void addConflict(Activation oAct, Activation iAct, long v) {
        if(oAct == iAct) {
            return;
        }

        if (iAct.getINeuron().type != INeuron.Type.INHIBITORY) {
            add(oAct, iAct);
        } else {
            for (Activation.SynapseActivation sa : iAct.neuronInputs.values()) {
                if(!sa.synapse.key.isRecurrent) {
                    addConflict(oAct, sa.input, v);
                }
            }
        }
    }


    public static Collection<Activation> getConflicting(Activation n) {
        ArrayList<Activation> conflicts = new ArrayList<>();
        Conflicts.collectConflicting(conflicts, n);
        return conflicts;
    }


    private static void collectConflicting(Collection<Activation> results, Activation n) {
        results.addAll(n.conflicts.primary);
        results.addAll(n.conflicts.secondary);
    }


    public static void add(Activation primary, Activation secondary) {
        primary.conflicts.primary.add(secondary);
        secondary.conflicts.secondary.add(primary);
    }


    public boolean hasConflicts() {
        return !primary.isEmpty() || !secondary.isEmpty();
    }
}
