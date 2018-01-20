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


import org.aika.Model;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 *
 * @author Lukas Molzberger
 */
public class InterpretationNodeLatticeTest {


    @Test
    public void testAddInternal1() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa");

        InterpretationNode[] o = new InterpretationNode[5];
        for(int i = 0; i < o.length; i++) {
            o[i] = InterpretationNode.addPrimitive(doc);
        }

        InterpretationNode o12 = InterpretationNode.add(doc, false, o[1], o[2]);
        InterpretationNode o23 = InterpretationNode.add(doc, false, o[2], o[3]);

        InterpretationNode o123 = InterpretationNode.add(doc, false, o12, o[3]);
        InterpretationNode o012 = InterpretationNode.add(doc, false, o12, o[0]);
        InterpretationNode o234 = InterpretationNode.add(doc, false, o23, o[4]);
        InterpretationNode o01234 = InterpretationNode.add(doc, false, o012, o234);

        Assert.assertTrue(Arrays.asList(o01234.parents).contains(o123));
        Assert.assertTrue(Arrays.asList(o01234.parents).contains(o012));
        Assert.assertTrue(Arrays.asList(o01234.parents).contains(o234));

        InterpretationNode o5 = InterpretationNode.addPrimitive(doc);
    }


    @Test
    public void testAddInternal2() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa");

        InterpretationNode[] o = new InterpretationNode[5];
        for(int i = 0; i < o.length; i++) {
            o[i] = InterpretationNode.addPrimitive(doc);
        }

        InterpretationNode o04 = InterpretationNode.add(doc, false, o[0], o[4]);
        InterpretationNode o12 = InterpretationNode.add(doc, false, o[1], o[2]);

        InterpretationNode o123 = InterpretationNode.add(doc, false, o12, o[3]);
        InterpretationNode o0124 = InterpretationNode.add(doc, false, o12, o04);
        InterpretationNode o01234 = InterpretationNode.add(doc, false, o0124, o[3]);

        Assert.assertEquals(2, o01234.parents.length);
        Assert.assertTrue(Arrays.asList(o01234.parents).contains(o123));
        Assert.assertTrue(Arrays.asList(o01234.parents).contains(o0124));

        InterpretationNode o5 = InterpretationNode.addPrimitive(doc);
    }


    @Test
    public void testAddInternal3() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa");

        InterpretationNode o0 = InterpretationNode.addPrimitive(doc);
        InterpretationNode o1 = InterpretationNode.addPrimitive(doc);
        InterpretationNode o2 = InterpretationNode.addPrimitive(doc);
        InterpretationNode o3 = InterpretationNode.addPrimitive(doc);
        InterpretationNode o4 = InterpretationNode.addPrimitive(doc);
        InterpretationNode o5 = InterpretationNode.addPrimitive(doc);

        InterpretationNode o01 = InterpretationNode.add(doc, false, o0, o1);
        InterpretationNode o012 = InterpretationNode.add(doc, false, o01, o2);
        InterpretationNode o34 = InterpretationNode.add(doc, false, o3, o4);
        InterpretationNode o234 = InterpretationNode.add(doc, false, o2, o34);
        InterpretationNode o01234 = InterpretationNode.add(doc, false, o012, o234);

        InterpretationNode o12 = InterpretationNode.add(doc, false, o1, o2);
        InterpretationNode o23 = InterpretationNode.add(doc, false, o2, o3);
        InterpretationNode o123 = InterpretationNode.add(doc, false, o12, o23);

        Assert.assertTrue(Arrays.asList(o123.children).contains(o01234));

        Assert.assertEquals(6, doc.bottom.children.length);
    }


    @Test
    public void testAddInternal4() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa");

        InterpretationNode bottom = InterpretationNode.add(doc, false, doc.bottom, doc.bottom);

        Assert.assertNotNull(bottom);
        Assert.assertTrue(bottom.isBottom());
    }


    @Test
    public void testAddPrimitive() {
        Model m = new Model();
        Document doc = m.createDocument("Bla");

        for(int i = 0; i < 10; i++) {
            InterpretationNode n = InterpretationNode.addPrimitive(doc);
            System.out.println(n);
        }
    }


    @Test
    public void testMarkContains() {
        Model m = new Model();
        Document doc = m.createDocument("Bla");

        InterpretationNode[] n = new InterpretationNode[8];
        for(int i = 0; i < 8; i++) {
            n[i] = InterpretationNode.addPrimitive(doc);
        }

        InterpretationNode n12 = InterpretationNode.add(doc, false, n[1], n[2]);

        InterpretationNode n23 = InterpretationNode.add(doc, false, n[2], n[3]);
    }



    @Test
    public void testAdd() {
        Model m = new Model();
        Document doc = m.createDocument("Bla");

        InterpretationNode[] n = new InterpretationNode[4];
        for(int i = 0; i < 4; i++) {
            n[i] = InterpretationNode.addPrimitive(doc);
        }

        InterpretationNode n01 = InterpretationNode.add(doc, false, n[0], n[1]);
        InterpretationNode n23 = InterpretationNode.add(doc, false, n[2], n[3]);
        InterpretationNode n12 = InterpretationNode.add(doc, false, n[1], n[2]);
        InterpretationNode n13 = InterpretationNode.add(doc, false, n[1], n[3]);
        InterpretationNode n02 = InterpretationNode.add(doc, false, n[0], n[2]);
        InterpretationNode n03 = InterpretationNode.add(doc, false, n[0], n[3]);
        InterpretationNode n013 = InterpretationNode.add(doc, false, n[0], n13);
        InterpretationNode n012 = InterpretationNode.add(doc, false, n01, n12);
        n02 = InterpretationNode.add(doc, false, n[0], n[2]);
        n03 = InterpretationNode.add(doc, false, n[0], n[3]);
        n013 = InterpretationNode.add(doc, false, n[0], n13);


        System.out.println();
    }

}
