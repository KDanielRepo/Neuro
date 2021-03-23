import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
public class Brain {
    private float[][] values;
    private float[][] outputs;
    private float[][] weights;

    private int score;
    private double fitness;
    private int bestBlock;
    private int weightCount;

    private final float lambda = 1.0507f;
    private final float alpha = 1.6732f;
    private List<Integer> structure;
    private UserInterface userInterface;
    private int moves;

    public Brain() {
        if(userInterface==null || userInterface.getNeuralNetworkStructure().isEmpty()){
            structure = Arrays.asList(16,8,4);
        }else{
            structure = userInterface.getNeuralNetworkStructure();
        }
        createNN();
        weightCount = 0;
    }

    public void createNN() {
        values = new float[structure.size()][];
        weights = new float[structure.size()][];
        outputs = new float[structure.size()][];
        for (int i = 0; i < structure.size(); i++) {
            float[] a = new float[structure.get(i)];
            float[] c = new float[structure.get(i)];
            values[i] = a;
            outputs[i] = c;
        }
        for (int i = 0; i < structure.size(); i++) {
            for (int j = 0; j < structure.get(i); j++) {
                if (i == 0) {
                    float[] b = new float[structure.get(i)];
                    weights[i] = b;
                } else {
                    float[] b = new float[structure.get(i) * structure.get(i - 1)];
                    weights[i] = b;
                }
            }
        }

        for (int i = 0; i < structure.size(); i++) {
            for (int j = 0; j < structure.get(i); j++) {
                if (i == 0) {
                    weights[i][j] = 1f;
                } else {
                    for (int k = 0; k < weights[i].length; k++) {
                        weights[i][k] = ThreadLocalRandom.current().nextFloat();
                    }
                }
                values[i][j] = 0f;
            }
        }
        initOutputs();
        createOutputs();
    }

    public void initOutputs() {
        for (int i = 0; i < outputs.length; i++) {
            Arrays.fill(outputs[i], 0f);
        }
    }

    public void createOutputs() {
        for (int i = 0; i < outputs.length; i++) {
            for (int j = 0; j < outputs[i].length; j++) {
                outputs[i][j] = sum(i,j);
            }
        }
    }

    public void setInputValues(List<Float> inputs) {
        for (int i = 0; i < values[0].length; i++) {
            values[0][i] = inputs.get(i);
        }
        activation();
    }

    public void activation() {
        for (int i = 0; i < outputs.length; i++) {
            for (int j = 0; j < outputs[i].length; j++) {
                float sum = sum(i,j);
                if (sum < 0) {
                    outputs[i][j] = ((float) ((alpha * Math.exp(sum) - alpha) * lambda));
                } else {
                    outputs[i][j] = (sum * lambda);
                }
            }
        }
    }

    public float sum(int x, int y) {
        float sum = 0f;
        int weightIndex = y * (weights[x].length / outputs[x].length);
        if (x == 0) {
            sum = ArithmeticUtils.log2(values[x][y]) * weights[x][y];
        }else{
            for (int k = 0; k < weights[x].length / outputs[x].length; k++) {
                sum += outputs[x - 1][k] * weights[x][weightIndex];
                weightIndex++;
            }
        }
        return sum;
    }

    public float[] getOutputValues() {
        return outputs[outputs.length - 1];
    }

    public int getWeightCount() {
        if (weightCount == 0) {
            for (int i = 0; i < weights.length; i++) {
                for (int j = 0; j < weights[i].length; j++) {
                    weightCount++;
                }
            }
        }
        return weightCount;
    }

}
