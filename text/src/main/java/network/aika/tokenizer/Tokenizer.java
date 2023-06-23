package network.aika.tokenizer;

import network.aika.parser.Context;

public interface Tokenizer {

    void tokenize(String text, Context context, TokenConsumer tokenConsumer);

}
