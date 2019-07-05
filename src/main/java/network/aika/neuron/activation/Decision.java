package network.aika.neuron.activation;

public enum Decision {
    SELECTED(
            'S',
            sn -> sn.selectedChild,
            sn -> sn.excludedChild
    ),
    EXCLUDED(
            'E',
            sn -> sn.excludedChild,
            sn -> sn.selectedChild
    ),
    UNKNOWN('U',
            sn -> null,
            sn -> null
    );

    char s;
    ChildNode childNode;
    ChildNode invertedChildNode;

    Decision(char s, ChildNode cn, ChildNode icn) {
        this.s = s;
        this.childNode = cn;
        this.invertedChildNode = icn;
    }

    public SearchNode getChild(SearchNode sn) {
        return childNode.getChild(sn);
    }

    public SearchNode getInvertedChild(SearchNode sn) {
        return invertedChildNode.getChild(sn);
    }

    interface ChildNode {
        SearchNode getChild(SearchNode sn);
    }
}
