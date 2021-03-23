import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class BrainController {
    private Brain brain;
    private List<Integer> blocks;
    private Integer currentMove;
    private UserInterface userInterface;

    public BrainController() {
        brain = new Brain();
        blocks = new ArrayList<>();
        //brain.createDefaultBrain();
    }

    public Integer generateMove(){
        Map<Integer, Float> map = softmax();
        Float value = Float.parseFloat(map
                .values()
                .stream()
                .max(Comparator.naturalOrder())
                .toString()
                .substring(8)
                .replaceAll("[\\[\\]]",""));

        return Integer.parseInt(map
                .entrySet()
                .stream()
                .filter(entry->value.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .toString()
                .substring(8)
                .replaceAll("[\\[\\]]",""));
    }

    public Integer generateMoveWithoutBlocks(){
        Map<Integer, Float> map = softmax();
        blocks.forEach(map::remove);
        Float value = Float.parseFloat(map
                .values()
                .stream()
                .max(Comparator.naturalOrder())
                .toString()
                .substring(8)
                .replaceAll("[\\[\\]]",""));

        return Integer.parseInt(map
                .entrySet()
                .stream()
                .filter(entry->value.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .toString()
                .substring(8)
                .replaceAll("[\\[\\]]",""));
    }

    public Map<Integer,Float> softmax(){
        Map<Integer,Float> probabilities = new HashMap<>();
        int sum = 0;
        for (int i = 0; i < brain.getOutputValues().length; i++) {
            //System.out.println(ArithmeticUtils.normalize(brain.getOutputValues()[i],brain.getOutputValues()));
            //sum+= Math.exp(ArithmeticUtils.normalize(brain.getOutputValues()[i],brain.getOutputValues()));
            probabilities.put(i,brain.getOutputValues()[i]);
        }
        /*for (int i = 0; i < brain.getOutputValues().length; i++) {

            //Float sum = Arrays.stream(brain.getOutputValues()).map(Math::exp).reduce(0d,Double::sum).floatValue();
            Float value = ArithmeticUtils.normalize(brain.getOutputValues()[i],brain.getOutputValues());
            Float probability =  value/sum;
            probabilities.put(i,probability);
        }*/
        return probabilities;
    }

    public void setCurrentInputs(Integer[][] matrix) {
        List<Float> list = new ArrayList<>();
        for (Integer[] i : matrix) {
            for (Integer j : i) {
                list.add(j.floatValue());
            }
        }
        brain.setInputValues(list);
    }

    public void addBlock(int block) {
        blocks.add(block);
    }
    public void clearBlocks(){
        blocks = new ArrayList<>();
    }

    public boolean isNotBlocked() {
        return blocks.size() == 0;
    }

}
