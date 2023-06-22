package network.aika.tokenizer;

import network.aika.elements.neurons.TokenNeuron;

public interface TokenConsumer {

    void processToken(TokenNeuron n, Integer pos, int begin, int end);

}
