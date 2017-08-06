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

        InterprNode[] o = new InterprNode[5];
        for(int i = 0; i < o.length; i++) {
            o[i] = InterprNode.addPrimitive(doc);
        }

        InterprNode o12 = InterprNode.add(doc, false, o[1], o[2]);
        InterprNode o23 = InterprNode.add(doc, false, o[2], o[3]);

        InterprNode o123 = InterprNode.add(doc, false, o12, o[3]);
        InterprNode o012 = InterprNode.add(doc, false, o12, o[0]);
        InterprNode o234 = InterprNode.add(doc, false, o23, o[4]);
        InterprNode o01234 = InterprNode.add(doc, false, o012, o234);

        Assert.assertTrue(Arrays.asList(o01234.parents).contains(o123));
        Assert.assertTrue(Arrays.asList(o01234.parents).contains(o012));
        Assert.assertTrue(Arrays.asList(o01234.parents).contains(o234));

        InterprNode o5 = InterprNode.addPrimitive(doc);
    }


    @Test
    public void testAddInternal2() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa");

        InterprNode[] o = new InterprNode[5];
        for(int i = 0; i < o.length; i++) {
            o[i] = InterprNode.addPrimitive(doc);
        }

        InterprNode o04 = InterprNode.add(doc, false, o[0], o[4]);
        InterprNode o12 = InterprNode.add(doc, false, o[1], o[2]);

        InterprNode o123 = InterprNode.add(doc, false, o12, o[3]);
        InterprNode o0124 = InterprNode.add(doc, false, o12, o04);
        InterprNode o01234 = InterprNode.add(doc, false, o0124, o[3]);

        Assert.assertEquals(2, o01234.parents.length);
        Assert.assertTrue(Arrays.asList(o01234.parents).contains(o123));
        Assert.assertTrue(Arrays.asList(o01234.parents).contains(o0124));

        InterprNode o5 = InterprNode.addPrimitive(doc);
    }


    @Test
    public void testAddInternal3() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa");

        InterprNode o0 = InterprNode.addPrimitive(doc);
        InterprNode o1 = InterprNode.addPrimitive(doc);
        InterprNode o2 = InterprNode.addPrimitive(doc);
        InterprNode o3 = InterprNode.addPrimitive(doc);
        InterprNode o4 = InterprNode.addPrimitive(doc);
        InterprNode o5 = InterprNode.addPrimitive(doc);

        InterprNode o01 = InterprNode.add(doc, false, o0, o1);
        InterprNode o012 = InterprNode.add(doc, false, o01, o2);
        InterprNode o34 = InterprNode.add(doc, false, o3, o4);
        InterprNode o234 = InterprNode.add(doc, false, o2, o34);
        InterprNode o01234 = InterprNode.add(doc, false, o012, o234);

        InterprNode o12 = InterprNode.add(doc, false, o1, o2);
        InterprNode o23 = InterprNode.add(doc, false, o2, o3);
        InterprNode o123 = InterprNode.add(doc, false, o12, o23);

        Assert.assertTrue(Arrays.asList(o123.children).contains(o01234));

        Assert.assertEquals(6, doc.bottom.children.length);
    }


    @Test
    public void testAddInternal4() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa");

        InterprNode bottom = InterprNode.add(doc, false, doc.bottom, doc.bottom);

        Assert.assertNotNull(bottom);
        Assert.assertTrue(bottom.isBottom());
    }


    @Test
    public void testAddPrimitive() {
        Model m = new Model();
        Document doc = m.createDocument("Bla");

        for(int i = 0; i < 10; i++) {
            InterprNode n = InterprNode.addPrimitive(doc);
            System.out.println(n);
        }
    }


    @Test
    public void testMarkContains() {
        Model m = new Model();
        Document doc = m.createDocument("Bla");

        InterprNode[] n = new InterprNode[8];
        for(int i = 0; i < 8; i++) {
            n[i] = InterprNode.addPrimitive(doc);
        }

        InterprNode n12 = InterprNode.add(doc, false, n[1], n[2]);

        InterprNode n23 = InterprNode.add(doc, false, n[2], n[3]);
    }



    @Test
    public void testAdd() {
        Model m = new Model();
        Document doc = m.createDocument("Bla");

        InterprNode[] n = new InterprNode[4];
        for(int i = 0; i < 4; i++) {
            n[i] = InterprNode.addPrimitive(doc);
        }

        InterprNode n01 = InterprNode.add(doc, false, n[0], n[1]);
        InterprNode n23 = InterprNode.add(doc, false, n[2], n[3]);
        InterprNode n12 = InterprNode.add(doc, false, n[1], n[2]);
        InterprNode n13 = InterprNode.add(doc, false, n[1], n[3]);
        InterprNode n02 = InterprNode.add(doc, false, n[0], n[2]);
        InterprNode n03 = InterprNode.add(doc, false, n[0], n[3]);
        InterprNode n013 = InterprNode.add(doc, false, n[0], n13);
        InterprNode n012 = InterprNode.add(doc, false, n01, n12);
        n02 = InterprNode.add(doc, false, n[0], n[2]);
        n03 = InterprNode.add(doc, false, n[0], n[3]);
        n013 = InterprNode.add(doc, false, n[0], n13);


        System.out.println();
    }

}
