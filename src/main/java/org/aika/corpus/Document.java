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


import java.util.HashSet;

/**
 * The <code>Document</code> class represents a single document which may be either used as processing input or
 * training input. The <code>Document</code> class contains the actual text, the option lattice containing
 * all the possible interpretations of this document.
 *
 * @author Lukas Molzberger
 */
public class Document implements Comparable<Document> {
    public final int id = docIdCounter++;
    public static int docIdCounter = 0;

    private String content;

    public int optionIdCounter = 1;
    public int expandNodeIdCounter = 0;

    public Option bottom = new Option(this, -1, 0, 0);

    public ExpandNode root = ExpandNode.createInitialExpandNode(this);
    public ExpandNode selectedExpandNode = null;
    public Option selectedOption = null;
    public long selectedMark = -1;

    public Document(String content) {
        this.content = content;
    }


    public static Document create(String content) {
        return new Document(content);
    }


    public String getContent() {
        return content;
    }


    public int length() {
        return content.length();
    }


    public String toString() {
		return content;
	}


    public String getText(Range r) {
        return content.substring(Math.max(0, Math.min(r.begin, length())), Math.max(0, Math.min(r.end, length())));
    }


    public String conflictsToString() {
        HashSet<Option> conflicts = new HashSet<>();
        bottom.collectConflicts(conflicts, Option.visitedCounter++);

        StringBuilder sb = new StringBuilder();
        sb.append("Conflicts:\n");
        for(Option n: conflicts) {
            sb.append(n.conflicts.primaryToString());
        }
        sb.append("\n");
        return sb.toString();
    }


    public String selectedOptionsToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Selected Options:\n");
        sb.append(selectedOption.toString());
        sb.append("\n");
        return sb.toString();
    }


    public boolean contains(Range r) {
        return r.begin >= 0 && r.end <= length();
    }


    @Override
    public int compareTo(Document doc) {
        return Integer.compare(id, doc.id);
    }
}
