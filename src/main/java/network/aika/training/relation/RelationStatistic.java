package network.aika.training.relation;

import network.aika.neuron.activation.Activation;
import network.aika.training.Sign;

public class RelationStatistic {

    public double weight;

    private double binaryFrequencyIPosOPos;
    private double frequencyIPosOPos;
    private double frequencyIPosONeg;
    private double frequencyINegOPos;
    private double frequencyINegONeg;
    public double N;
    private boolean needsFrequencyUpdate;

    private double countValueIPosOPos;
    private double countValueIPosONeg;
    private double countValueINegOPos;
    private double countValueINegONeg;
    private boolean needsCountUpdate;

    public int lastCount;


    public RelationStatistic() {
    }


    public RelationStatistic(double weight) {
        this.weight = weight;
    }


    public void initCountValues() {
        countValueIPosOPos = 0.0;
        countValueIPosONeg = 0.0;
        countValueINegOPos = 0.0;
        countValueINegONeg = 0.0;
        needsCountUpdate = true;
    }


    public void updateCountValue(double p, double inputValue, double lp, double outputValue) {
        if(!needsCountUpdate) {
            return;
        }
        needsCountUpdate = false;

        double optionProp = p * lp;

        countValueIPosOPos += (Sign.POS.getX(inputValue) * Sign.POS.getX(outputValue) * optionProp);
        countValueIPosONeg += (Sign.POS.getX(inputValue) * Sign.NEG.getX(outputValue) * optionProp);
        countValueINegOPos += (Sign.NEG.getX(inputValue) * Sign.POS.getX(outputValue) * optionProp);
        countValueINegONeg += (Sign.NEG.getX(inputValue) * Sign.NEG.getX(outputValue) * optionProp);

        needsFrequencyUpdate = true;
    }


    public void updateFrequencies(double alpha, Activation iAct, Activation oAct) {
        if(!needsFrequencyUpdate) {
            return;
        }
        needsFrequencyUpdate = false;
/*
        double stepsWithin;
        double stepsBefore;
        if(iAct != null) {
            int beginPos = StatUtil.getCurrentPos(iAct, BEGIN);
            int endPos = StatUtil.getCurrentPos(iAct, END);

            stepsWithin = endPos - beginPos;
            stepsBefore = beginPos - lastCount;
            lastCount = endPos;

            lengthSum += stepsWithin;
            numActs++;
        } else {
            stepsWithin = getAvgLength();

            int endPos = StatUtil.getCurrentPos(oAct, END);

            stepsBefore = (endPos - stepsWithin) - lastCount;
            lastCount = endPos;

        }

        frequencyIPosOPos = (stepsWithin * countValueIPosOPos) + (alpha * frequencyIPosOPos);
        frequencyIPosONeg = (stepsWithin * countValueIPosONeg) + (alpha * frequencyIPosONeg);
        frequencyINegOPos = (stepsWithin * countValueINegOPos) + (alpha * frequencyINegOPos);
        frequencyINegONeg = stepsBefore + (stepsWithin * countValueINegONeg) + (alpha * frequencyINegONeg);

        binaryFrequencyIPosOPos = (countValueIPosOPos > 0.0 ? 1.0 : 0.0) + (alpha * binaryFrequencyIPosOPos);

        N = stepsBefore + stepsWithin + (alpha * N);

        numCounts++;
        */
    }


    public void setFrequency(double fPP, double fPN, double fNP, double fNN) {
        frequencyIPosOPos = fPP;
        frequencyIPosONeg = fPN;
        frequencyINegOPos = fNP;
        frequencyINegONeg = fNN;
    }


    public RelationStatistic copy() {
        RelationStatistic rs = new RelationStatistic();
        rs.weight = weight;
        return rs;
    }


    public String toString() {
        return "" + weight;
    }
}
