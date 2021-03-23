import java.util.List;

public class ArithmeticUtils {
    public static int log2(float value){
        if (value == 0f) {
            return 0;
        }
        return (int)(Math.log(value) / Math.log(2) + 1e-10);
    }

    public static float normalize(float value, float[][] array) {
        return 1 - ((value - minimum(array)) / (maximum(array) - minimum(array)));
    }

    public static float normalize(float value, float[] array) {
        return 1 - ((value - minimum(array)) / (maximum(array) - minimum(array)));
    }

    public static float maximum(float[][] array){
        float max = 0;
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                if(max<array[i][j]){
                    max = array[i][j];
                }
            }
        }
        return max;
    }

    public static float maximum(float[] array){
        float max = 0;
        for (int i = 0; i < array.length; i++) {
            if(max<array[i]){
                max = array[i];
            }
        }
        return max;
    }

    public static float minimum(float[][] array){
        float min = 4096;
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                if(min>array[i][j]){
                    min = array[i][j];
                }
            }
        }
        return min;
    }

    public static float minimum(float[] array){
        float min = 4096;
        for (int i = 0; i < array.length; i++) {
            if(min>array[i]){
                min = array[i];
            }
        }
        return min;
    }

    public static double average(List<Integer> list){
        return list.stream().mapToDouble(Integer::intValue).sum()/list.size();
    }
    public static double averageDouble(List<Double> list){
        return list.stream().mapToDouble(Double::intValue).sum()/list.size();
    }
    public static int sum(List<Integer> list){
        return list.stream().mapToInt(Integer::intValue).sum();
    }
}
