package network.aika.lattice;

import network.aika.Document;

import java.util.TreeSet;

public class NodeQueue {

    private Document doc;

    private final TreeSet<Node> queue = new TreeSet<>(
            (n1, n2) -> Node.compareRank(doc.getThreadId(), n1, n2)
    );

    private long queueIdCounter = 0;


    public NodeQueue(Document doc) {
        this.doc = doc;
    }


    public void add(Node n) {
        if(!n.isQueued(doc.getThreadId(), queueIdCounter++)) {
            queue.add(n);
        }
    }


    public void process() {
        while(!queue.isEmpty()) {
            Node n = queue.pollFirst();

            n.setNotQueued(doc.getThreadId());
            n.processChanges(doc);
        }
    }
}
