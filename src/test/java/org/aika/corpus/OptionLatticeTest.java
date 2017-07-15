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
public class OptionLatticeTest {


    @Test
    public void testAddInternal1() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa");

        Option[] o = new Option[5];
        for(int i = 0; i < o.length; i++) {
            o[i] = Option.addPrimitive(doc);
        }

        Option o12 = Option.add(doc, false, o[1], o[2]);
        Option o23 = Option.add(doc, false, o[2], o[3]);

        Option o123 = Option.add(doc, false, o12, o[3]);
        Option o012 = Option.add(doc, false, o12, o[0]);
        Option o234 = Option.add(doc, false, o23, o[4]);
        Option o01234 = Option.add(doc, false, o012, o234);

        Assert.assertTrue(Arrays.asList(o01234.parents).contains(o123));
        Assert.assertTrue(Arrays.asList(o01234.parents).contains(o012));
        Assert.assertTrue(Arrays.asList(o01234.parents).contains(o234));

        Option o5 = Option.addPrimitive(doc);
    }


    @Test
    public void testAddInternal2() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa");

        Option[] o = new Option[5];
        for(int i = 0; i < o.length; i++) {
            o[i] = Option.addPrimitive(doc);
        }

        Option o04 = Option.add(doc, false, o[0], o[4]);
        Option o12 = Option.add(doc, false, o[1], o[2]);

        Option o123 = Option.add(doc, false, o12, o[3]);
        Option o0124 = Option.add(doc, false, o12, o04);
        Option o01234 = Option.add(doc, false, o0124, o[3]);

        Assert.assertEquals(2, o01234.parents.length);
        Assert.assertTrue(Arrays.asList(o01234.parents).contains(o123));
        Assert.assertTrue(Arrays.asList(o01234.parents).contains(o0124));

        Option o5 = Option.addPrimitive(doc);
    }


    @Test
    public void testAddInternal3() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa");

        Option o0 = Option.addPrimitive(doc);
        Option o1 = Option.addPrimitive(doc);
        Option o2 = Option.addPrimitive(doc);
        Option o3 = Option.addPrimitive(doc);
        Option o4 = Option.addPrimitive(doc);
        Option o5 = Option.addPrimitive(doc);

        Option o01 = Option.add(doc, false, o0, o1);
        Option o012 = Option.add(doc, false, o01, o2);
        Option o34 = Option.add(doc, false, o3, o4);
        Option o234 = Option.add(doc, false, o2, o34);
        Option o01234 = Option.add(doc, false, o012, o234);

        Option o12 = Option.add(doc, false, o1, o2);
        Option o23 = Option.add(doc, false, o2, o3);
        Option o123 = Option.add(doc, false, o12, o23);

        Assert.assertTrue(Arrays.asList(o123.children).contains(o01234));

        Assert.assertEquals(6, doc.bottom.children.length);
    }


    @Test
    public void testAddInternal4() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa");

        Option bottom = Option.add(doc, false, doc.bottom, doc.bottom);

        Assert.assertNotNull(bottom);
        Assert.assertTrue(bottom.isBottom());
    }


    @Test
    public void testAddPrimitive() {
        Model m = new Model();
        Document doc = m.createDocument("Bla");

        for(int i = 0; i < 10; i++) {
            Option n = Option.addPrimitive(doc);
            System.out.println(n);
        }
    }


    @Test
    public void testMarkContains() {
        Model m = new Model();
        Document doc = m.createDocument("Bla");

        Option[] n = new Option[8];
        for(int i = 0; i < 8; i++) {
            n[i] = Option.addPrimitive(doc);
        }

        Option n12 = Option.add(doc, false, n[1], n[2]);

        Option n23 = Option.add(doc, false, n[2], n[3]);
    }



    @Test
    public void testAdd() {
        Model m = new Model();
        Document doc = m.createDocument("Bla");

        Option[] n = new Option[4];
        for(int i = 0; i < 4; i++) {
            n[i] = Option.addPrimitive(doc);
        }

        Option n01 = Option.add(doc, false, n[0], n[1]);
        Option n23 = Option.add(doc, false, n[2], n[3]);
        Option n12 = Option.add(doc, false, n[1], n[2]);
        Option n13 = Option.add(doc, false, n[1], n[3]);
        Option n02 = Option.add(doc, false, n[0], n[2]);
        Option n03 = Option.add(doc, false, n[0], n[3]);
        Option n013 = Option.add(doc, false, n[0], n13);
        Option n012 = Option.add(doc, false, n01, n12);
        n02 = Option.add(doc, false, n[0], n[2]);
        n03 = Option.add(doc, false, n[0], n[3]);
        n013 = Option.add(doc, false, n[0], n13);


        System.out.println();
    }

}
