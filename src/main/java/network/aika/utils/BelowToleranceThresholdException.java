package network.aika.utils;

import network.aika.neuron.activation.Element;

public class BelowToleranceThresholdException extends RuntimeException {
    private Element element;

    public BelowToleranceThresholdException(Element e) {
        element = e;
    }

    public Element getElement() {
        return element;
    }
}
