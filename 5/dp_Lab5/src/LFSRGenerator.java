import java.util.ArrayList;
import java.util.List;

class LFSRGenerator {
    private final List<Integer> register;
    private final List<Integer> taps;

    public LFSRGenerator(List<Integer> seed, List<Integer> taps) {
        this.register = new ArrayList<>(seed);
        this.taps = new ArrayList<>(taps);
    }

    public int generateNextBit() {
        int feedbackBit = calculateFeedbackBit();
        int outputBit = register.get(0);
        register.remove(0);
        register.add(feedbackBit);
        return outputBit;
    }

    private int calculateFeedbackBit() {
        int feedbackBit = 0;
        int tapsSize = taps.size();
        for (int i = 0; i < tapsSize; i++) {
            feedbackBit ^= register.get(taps.get(i) - 1);
        }
        return feedbackBit;
    }

    public List<Integer> generateSequence(int length) {
        List<Integer> sequence = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            int bit = generateNextBit();
            sequence.add(bit);
        }
        return sequence;
    }
}
