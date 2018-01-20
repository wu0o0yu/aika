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
package org.aika.lattice;


import org.aika.Utils;
import org.aika.corpus.Document;
import org.aika.corpus.InterpretationNode;
import org.aika.corpus.Range;

import java.util.*;


public class NodeActivation<T extends Node> implements Comparable<NodeActivation> {

    public final int id;

    public final Key<T> key;

    public final Document doc;

    public long visited = -1;

    public TreeMap<Key, NodeActivation<?>> inputs = new TreeMap<>();
    public TreeMap<Key, NodeActivation<?>> outputs = new TreeMap<>();


    public NodeActivation(int id, Document doc, Key<T> key) {
        this.id = id;
        this.doc = doc;
        this.key = key;
    }


    public NodeActivation(int id, Document doc, T n, Range pos, Integer rid, InterpretationNode o) {
        this.id = id;
        this.doc = doc;
        key = new Key<>(n, pos, rid, o);
    }


    public void link(Collection<NodeActivation> inputActs) {
        for(NodeActivation iAct: inputActs) {
            inputs.put(iAct.key, iAct);
            iAct.outputs.put(key, this);
        }
    }


    public String toString(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ACT ");
        sb.append(",(");
        sb.append(key.range);
        sb.append("),");
        sb.append(doc.getContent().substring(Math.max(0, key.range.begin - 3), Math.min(doc.length(), key.range.end + 3)));
        sb.append(",");
        sb.append(key.node);
        sb.append(">");
        return sb.toString();
    }


    @Override
    public int compareTo(NodeActivation act) {
        return key.compareTo(act.key);
    }


    public static int compare(NodeActivation a, NodeActivation b) {
        if(a == b) return 0;
        if(a == null && b != null) return -1;
        if(a != null && b == null) return 1;
        return a.compareTo(b);
    }


    public static final class Key<T extends Node> implements Comparable<Key> {
        public final T node;
        public final Range range;
        public final Integer rid;
        public final InterpretationNode interpretation;


        public Key(T node, Range range, Integer rid, InterpretationNode interpretation) {
            this.node = node;
            this.range = range;
            this.rid = rid;
            this.interpretation = interpretation;
        }


        @Override
        public int compareTo(Key k) {
            int x = node.compareTo(k.node);
            if(x != 0) return x;
            x = Range.compare(range, k.range, false);
            if(x != 0) return x;
            x = Utils.compareInteger(rid, k.rid);
            if(x != 0) return x;
            return interpretation.compareTo(k.interpretation);
        }


        public String toString() {
            return (node != null ? node.getNeuronLabel() : "") + " " + range + " " + rid + " " + interpretation;
        }
    }
}
