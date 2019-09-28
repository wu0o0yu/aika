package network.aika.training;


public class Config {
    public double learnRate;

    public double metaThreshold;
    private double maturityThreshold;

    public Config setLearnRate(double learnRate) {
        this.learnRate = learnRate;
        return this;
    }


    public double getMetaThreshold() {
        return metaThreshold;
    }

    public Config setMetaThreshold(double metaThreshold) {
        this.metaThreshold = metaThreshold;
        return this;
    }

    public double getMaturityThreshold() {
        return maturityThreshold;
    }

    public Config setMaturityThreshold(double maturityThreshold) {
        this.maturityThreshold = maturityThreshold;
        return this;
    }
}
