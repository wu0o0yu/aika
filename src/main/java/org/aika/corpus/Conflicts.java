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


import org.aika.Activation;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Lukas Molzberger
 */
public class Conflicts {

    public SortedMap<Key, Conflict> primary = new TreeMap<>();
    public Map<Key, Conflict> secondary = new TreeMap<>();


    static void collectDirectConflicting(Collection<InterprNode> results, InterprNode n) {
        for(Conflict c: n.conflicts.primary.values()) {
            results.add(c.secondary);
        }
        for(Conflict c: n.conflicts.secondary.values()) {
            results.add(c.primary);
        }
    }


    static void collectAllConflicting(Collection<InterprNode> results, InterprNode n, long v) {
        if(n.visitedCollectAllConflicting == v) return;
        n.visitedCollectAllConflicting = v;

        collectDirectConflicting(results, n);

        for(InterprNode pn: n.parents) {
            collectAllConflicting(results, pn, v);
        }

        // TODO: consider orInterprNodes.size() == 1
    }


    public static void add(Document doc, Activation act, InterprNode primary, InterprNode secondary) {
        Key ck = new Key(secondary, act);
        Conflict c = primary.conflicts.primary.get(ck);
        if(c == null) {
            c = new Conflict(act, primary, secondary, InterprNode.add(primary.doc, false, primary, secondary));

            primary.countRef();
            secondary.countRef();
            c.conflict.countRef();

            c.conflict.isConflict++;

            primary.conflicts.primary.put(ck, c);
            secondary.conflicts.secondary.put(new Key(primary, act), c);

            c.conflict.removeActivationsRecursiveStep(doc, c.conflict, InterprNode.visitedCounter++);
        }
    }


    public static void remove(Document doc, Activation act, InterprNode primary, InterprNode secondary) {
        Key ck = new Key(secondary, act);

        Conflict c = primary.conflicts.primary.get(ck);
        if(c == null) return;

        primary.conflicts.primary.remove(ck);
        secondary.conflicts.secondary.remove(new Key(primary, act));
        c.conflict.isConflict--;

        c.conflict.expandActivationsRecursiveStep(doc, c.conflict, InterprNode.visitedCounter++);

        removeInternal(c);
    }


    private static void removeInternal(Conflict c) {
        c.primary.releaseRef();
        c.secondary.releaseRef();
        c.conflict.releaseRef();
    }


    public void removeAll() {
        if(primary != null) {
            for(Conflict c: primary.values()) {
                c.secondary.conflicts.secondary.remove(c.primary);
                removeInternal(c);
            }
        }
        primary.clear();
        if(secondary != null) {
            for(Conflict c: secondary.values()) {
                c.primary.conflicts.primary.remove(c.secondary);
                removeInternal(c);
            }
        }
        secondary.clear();
    }


    public boolean hasConflicts() {
        return !primary.isEmpty() || !secondary.isEmpty();
    }


    public static class Conflict {
        public Activation act;
        public InterprNode primary;
        public InterprNode secondary;
        public InterprNode conflict;


        public Conflict(Activation act, InterprNode primary, InterprNode secondary, InterprNode conflict) {
            this.act = act;
            this.primary = primary;
            this.secondary = secondary;
            this.conflict = conflict;
        }
    }


    public static class Key implements Comparable<Key> {
        public InterprNode o;
        public Activation act;

        public Key(InterprNode o, Activation act) {
            this.o = o;
            this.act = act;
        }

        @Override
        public int compareTo(Key k) {
            int r = o.compareTo(k.o);
            if(r != 0) return r;
            return Activation.compare(act, k.act);
        }
    }
}
