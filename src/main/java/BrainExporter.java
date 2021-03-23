import lombok.Data;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class BrainExporter {
    public static void importBrain(Brain brain){
        try {
            JFileChooser jFileChooser = new JFileChooser();
            FileNameExtensionFilter fileNameExtensionFilter = new FileNameExtensionFilter("Texts","txt");
            jFileChooser.setCurrentDirectory(new File("C:\\Users\\Daniel\\IdeaProjects\\Test"));
            jFileChooser.setFileFilter(fileNameExtensionFilter);
            int ret = jFileChooser.showOpenDialog(null);
            if (ret == JFileChooser.APPROVE_OPTION) {
                File file = jFileChooser.getSelectedFile();
                Scanner scanner = new Scanner(file);
                AtomicInteger indexX = new AtomicInteger(0);
                AtomicInteger indexY = new AtomicInteger(0);
                Arrays.stream(scanner.nextLine().split("\\|")).forEach(line->{
                    Arrays.stream(line.split(";")).forEach(flo->{
                        brain.getWeights()[indexX.get()][indexY.get()] = Float.parseFloat(flo);
                        indexY.getAndIncrement();
                    });
                    indexY.set(0);
                    indexX.getAndIncrement();
                });
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void exportBrain(Brain brain){
        try {
            String exportString = "";
            for (int i = 0; i < brain.getWeights().length; i++) {
                for (int j = 0; j < brain.getWeights()[i].length; j++) {
                    exportString = exportString.concat(brain.getWeights()[i][j]+";");
                }
                exportString = exportString.concat("|");
            }
            File file = new File("./" + brain.toString()+".txt");
            if(!file.exists()){
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(exportString);
            fileWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
