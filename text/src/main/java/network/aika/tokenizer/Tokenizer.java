package network.aika.tokenizer;

public interface Tokenizer {

    void tokenize(String text, TokenizerContext context, TokenConsumer tokenConsumer);

}
