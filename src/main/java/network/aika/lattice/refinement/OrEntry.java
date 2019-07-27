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
package network.aika.lattice.refinement;

import network.aika.Model;
import network.aika.Provider;
import network.aika.Writable;
import network.aika.lattice.Node;
import network.aika.lattice.OrNode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.TreeMap;

/**
 *
 * @author Lukas Molzberger
 */
public class OrEntry implements Comparable<OrEntry>, Writable {
    public int[] synapseIds;
    public TreeMap<Integer, Integer> revSynapseIds = new TreeMap<>();
    public Provider<? extends Node> parent;
    public Provider<OrNode> child;

    private OrEntry() {}

    public OrEntry(int[] synapseIds, Provider<? extends Node> parent, Provider<OrNode> child) {
        this.synapseIds = synapseIds;
        for(int ofs = 0; ofs < synapseIds.length; ofs++) {
            revSynapseIds.put(synapseIds[ofs], ofs);
        }

        this.parent = parent;
        this.child = child;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(synapseIds.length);
        for(int i = 0; i < synapseIds.length; i++) {
            Integer ofs = synapseIds[i];
            out.writeBoolean(ofs != null);
            out.writeInt(ofs);
        }
        out.writeInt(parent.getId());
        out.writeInt(child.getId());
    }

    public static OrEntry read(DataInput in, Model m)  throws IOException {
        OrEntry rv = new OrEntry();
        rv.readFields(in, m);
        return rv;
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        int l = in.readInt();
        synapseIds = new int[l];
        for(int i = 0; i < l; i++) {
            if(in.readBoolean()) {
                Integer ofs = in.readInt();
                synapseIds[i] = ofs;
                revSynapseIds.put(ofs, i);
            }
        }
        parent = m.lookupNodeProvider(in.readInt());
        child = m.lookupNodeProvider(in.readInt());
    }


    @Override
    public int compareTo(OrEntry oe) {
        int r = child.compareTo(oe.child);
        if(r != 0) return r;

        r = parent.compareTo(oe.parent);
        if(r != 0) return r;

        r = Integer.compare(synapseIds.length, oe.synapseIds.length);
        if(r != 0) return r;

        for(int i = 0; i < synapseIds.length; i++) {
            r = Integer.compare(synapseIds[i], oe.synapseIds[i]);
            if(r != 0) return r;
        }
        return 0;
    }
}

