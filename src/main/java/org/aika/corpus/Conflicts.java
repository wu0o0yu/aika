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
import org.aika.Iteration;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


public class Conflicts {

    public SortedMap<Key, Conflict> primary = new TreeMap<>();
    public Map<Key, Conflict> secondary = new TreeMap<>();


    public static void collectDirectConflicting(Collection<Option> results, Option n) {
        for(Conflict c: n.conflicts.primary.values()) {
            results.add(c.secondary);
        }
        for(Conflict c: n.conflicts.secondary.values()) {
            results.add(c.primary);
        }
    }


    public static void collectAllConflicting(Collection<Option> results, Option n, long v) {
        if(n.visitedCollectAllConflicting == v) return;
        n.visitedCollectAllConflicting = v;

        collectDirectConflicting(results, n);

        for(Option pn: n.parents) {
            collectAllConflicting(results, pn, v);
        }

        // TODO: consider orOptions.size() == 1
    }


    public static void add(Iteration t, Activation act, Option primary, Option secondary) {
        Key ck = new Key(secondary, act);
        Conflict c = primary.conflicts.primary.get(ck);
        if(c == null) {
            c = new Conflict(act, primary, secondary, Option.add(primary.doc, false, primary, secondary));

            primary.countRef();
            secondary.countRef();
            c.conflict.countRef();

            c.conflict.isConflict++;

            primary.conflicts.primary.put(ck, c);
            secondary.conflicts.secondary.put(new Key(primary, act), c);

            c.conflict.removeActivationsRecursiveStep(t, c.conflict, Option.visitedCounter++);
        }
    }


    public static void remove(Iteration t, Activation act, Option primary, Option secondary) {
        Key ck = new Key(secondary, act);

        Conflict c = primary.conflicts.primary.get(ck);
        if(c == null) return;

        primary.conflicts.primary.remove(ck);
        secondary.conflicts.secondary.remove(new Key(primary, act));
        c.conflict.isConflict--;

        c.conflict.expandActivationsRecursiveStep(t, c.conflict, Option.visitedCounter++);

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


    public String primaryToString() {
        StringBuilder sb = new StringBuilder();
        for (Conflict c: primary.values()) {
            sb.append(c.act.toString());
            sb.append(" : ");
            sb.append(c.primary.toString());
            sb.append(" : ");
            sb.append(c.secondary.toString());
            sb.append("\n");
        }
        return sb.toString();
    }


    public static class Conflict {
        public Activation act;
        public Option primary;
        public Option secondary;
        public Option conflict;


        public Conflict(Activation act, Option primary, Option secondary, Option conflict) {
            this.act = act;
            this.primary = primary;
            this.secondary = secondary;
            this.conflict = conflict;
        }
    }


    public static class Key implements Comparable<Key> {
        public Option o;
        public Activation act;

        public Key(Option o, Activation act) {
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
