package network;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class DerDieDasTest {

    CharBasedTraining charBasedTrainings = new CharBasedTraining();

    @BeforeEach
    public void init() {
        charBasedTrainings.init();
    }

    @Test
    public void train() {
        String[] trainData = {"der", "die", "das"};
        for(int i = 0; i < 1000; i++) {
            charBasedTrainings.train(trainData[new Random().nextInt(3)]);
        }
    }
}
