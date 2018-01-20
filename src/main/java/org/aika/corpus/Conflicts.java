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
package org.aika.corpus;


import org.aika.lattice.NodeActivation;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The class {@code Conflicts} handles between different interpretation options for a given text.
 *
 * @author Lukas Molzberger
 */
public class Conflicts {

    public SortedMap<Key, Conflict> primary = new TreeMap<>();
    public Map<Key, Conflict> secondary = new TreeMap<>();


    public static void collectConflicting(Collection<InterpretationNode> results, InterpretationNode n, long v) {
        assert n.primId >= 0;
        n.conflicts.primary.values().forEach(c -> c.secondary.collectPrimitiveNodes(results, v));
        n.conflicts.secondary.values().forEach(c -> results.add(c.primary));
    }


    public static void add(NodeActivation act, InterpretationNode primary, InterpretationNode secondary) {
        Key ck = new Key(secondary, act);
        Conflict c = primary.conflicts.primary.get(ck);
        if(c == null) {
            c = new Conflict(act, primary, secondary, InterpretationNode.add(primary.doc, false, primary, secondary));

            c.conflict.isConflict++;

            primary.conflicts.primary.put(ck, c);
            secondary.conflicts.secondary.put(new Key(primary, act), c);
        }
    }


    public boolean hasConflicts() {
        return !primary.isEmpty() || !secondary.isEmpty();
    }


    public static class Conflict {
        public NodeActivation act;
        public InterpretationNode primary;
        public InterpretationNode secondary;
        public InterpretationNode conflict;


        public Conflict(NodeActivation act, InterpretationNode primary, InterpretationNode secondary, InterpretationNode conflict) {
            this.act = act;
            this.primary = primary;
            this.secondary = secondary;
            this.conflict = conflict;
        }
    }


    public static class Key implements Comparable<Key> {
        public InterpretationNode o;
        public NodeActivation act;

        public Key(InterpretationNode o, NodeActivation act) {
            this.o = o;
            this.act = act;
        }

        @Override
        public int compareTo(Key k) {
            int r = o.compareTo(k.o);
            if(r != 0) return r;
            return NodeActivation.compare(act, k.act);
        }
    }
}
