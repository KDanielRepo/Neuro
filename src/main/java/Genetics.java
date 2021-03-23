import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Getter
@Setter
public class Genetics extends Thread {
    private List<Brain> population;
    private List<Brain> matingPool;
    private int convergence;
    private Brain best;
    private int populationSize;
    private int generation;
    private Mutex mutex;
    private boolean groupSet;
    private boolean generated;
    private int currentGeneration;
    private double globalFitness;
    private double averageFitness;
    private List<Integer> scores;
    private int parentIndex;
    private int secondParentIndex;
    private Brain parent;
    private Brain secondParent;
    private Brain child;
    private Brain secondChild;
    private boolean crossing;
    private UserInterface userInterface;
    private int crossoverPoint;
    private float mutationChance;
    private float crossoverChance;

    public Genetics() {
        population = new ArrayList<>();
        matingPool = new ArrayList<>();
        scores = new ArrayList<>();
        best = new Brain();
        convergence = 0;
        populationSize = 500;
        generation = 15000;
        groupSet = false;
        currentGeneration = 0;
        parentIndex = 0;
        secondParentIndex = 0;
        mutationChance = 0.01f;
        crossoverChance = 0.70f;
    }

    public Genetics(int populationSize) {
        this.populationSize = populationSize;
    }

    public void run(){
        if (userInterface.getIndex()+1 >= populationSize && userInterface.isAllFinished()) {
            System.out.println("----------------------------");
            System.out.println("Koniec generacji :" + userInterface.getGenerationIndex());
            getScores();
            getAverageFitness();
            calculateGlobalFitness();
            calculateRFitness();
            createOffspring();
            resetPcPool();
            System.out.println(ZonedDateTime.now());
            userInterface.setIndex(0);
            userInterface.setGenerationIndex(userInterface.getGenerationIndex() + 1);
            setCurrentGeneration(userInterface.getGenerationIndex());
            userInterface.setFinishedInstances(0);
            System.out.println("---------------------------");
        }
    }

    public void rouletteSelection(){
        for (int i = 0; i < population.size(); i++) {
            double sum = 0D;
            double random = ThreadLocalRandom.current().nextDouble()*globalFitness;
            Collections.shuffle(population);
            boolean picked = false;
            while (!picked){
                for (int j = 0; j < population.size(); j++) {
                    sum+=population.get(j).getFitness();
                    if((random<sum) && (matingPool.size()<population.size())){
                        matingPool.add(population.get(j));
                        picked = true;
                        break;
                    }
                }
            }
        }
    }

    public void adjustMatingPool(){
        if(matingPool.size()%2!=0){
            matingPool.add(population.get(ThreadLocalRandom.current().nextInt(0,populationSize)));
        }
        if(!matingPool.contains(best)){
            matingPool.remove(ThreadLocalRandom.current().nextInt(0, matingPool.size()));
            matingPool.add(best);
        }
        for (Brain brain : matingPool) {
            population.remove(brain);
        }
        while (population.size()+matingPool.size()>populationSize){
            population.remove(ThreadLocalRandom.current().nextInt(0,population.size()));
        }
    }

    public void createOffspring(){
        getFittest();
        //ustawianie PC kazdego osobnika
        //population.forEach(brain -> brain.setMatingProbability(ThreadLocalRandom.current().nextDouble() * brain.getFitness()));
        //wybieranie ruletka
        rouletteSelection();
        adjustMatingPool();
        while (matingPool.size()!=0){
            parentIndex = ThreadLocalRandom.current().nextInt(0, matingPool.size());
            secondParentIndex = ThreadLocalRandom.current().nextInt(0, matingPool.size());
            while (parentIndex == secondParentIndex){
                parentIndex = ThreadLocalRandom.current().nextInt(0, matingPool.size());
                secondParentIndex = ThreadLocalRandom.current().nextInt(0, matingPool.size());
            }
            parent = matingPool.get(parentIndex);
            secondParent = matingPool.get(secondParentIndex);
            cross();
        }
        setGroupset(true);
        setGenerated(true);
    }

    public void cross(){
        try{
            Float chance = ThreadLocalRandom.current().nextFloat();
            if(chance<crossoverChance || parent.equals(best) || secondParent.equals(best)) {
                child = new Brain();
                secondChild = new Brain();
                crossing = true;
                crossoverPoint = ThreadLocalRandom.current().nextInt(
                        (parent.getWeightCount() / 2) - (int) (parent.getWeightCount() * 0.3),
                        (parent.getWeightCount() / 2) + (int) (parent.getWeightCount() * 0.3));
                int crossoverTemp = crossoverPoint;
                for (int i = 0; i < parent.getWeights().length; i++) {
                    for (int j = 0; j < parent.getWeights()[i].length; j++) {
                        if (crossoverTemp >= 0) {
                            child.getWeights()[i][j] = parent.getWeights()[i][j];
                            secondChild.getWeights()[i][j] = secondParent.getWeights()[i][j];
                        } else {
                            child.getWeights()[i][j] = secondParent.getWeights()[i][j];
                            secondChild.getWeights()[i][j] = parent.getWeights()[i][j];
                        }
                        crossoverTemp--;
                    }
                }
                userInterface.updateGeneticAlgorithmScene();
                sleep(userInterface.getGeneticsDelay());
                mutate(child);
                mutate(secondChild);
                population.add(child);
                population.add(secondChild);
                matingPool.remove(parent);
                matingPool.remove(secondParent);
                crossing = false;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void resetPcPool() {
        matingPool = new ArrayList<>();
    }

    public void mutate(Brain brain) {
        for (int i = 1; i < brain.getWeights().length; i++) {
            for (int j = 0; j <brain.getWeights()[i].length; j++) {
                Float chance = ThreadLocalRandom.current().nextFloat();
                if(chance<mutationChance){
                    brain.getWeights()[i][j] = ThreadLocalRandom.current().nextFloat();
                }
            }
        }
    }

    public void getScores(){
        scores = new ArrayList<>();
        scores = population.stream().map(brain -> brain.getScore()*(brain.getMoves()/10)).collect(Collectors.toList());
    }


    public void getAverageFitness() {
        averageFitness = ArithmeticUtils.average(scores);
    }

    public void calculateGlobalFitness() {
        globalFitness = ArithmeticUtils.sum(scores);
    }

    public void calculateRFitness() {
        population.forEach(brain -> brain.setFitness((brain.getScore()*(brain.getMoves()/10))));
    }

    public void getFittest() {
        List<Brain> sorted = getPopulation()
                .stream()
                .sorted(Comparator.comparing(Brain::getScore).reversed())
                .collect(Collectors.toList());
        if(sorted.get(0).getScore() > best.getScore()){
            best = sorted.get(0);
            for (int i = 0; i < best.getWeights().length; i++) {
                for (int j = 0; j < best.getWeights()[i].length; j++) {
                    best.getWeights()[i][j] = sorted.get(0).getWeights()[i][j];
                }
            }
            convergence=0;
        }else{
            convergence++;
            if(convergence>100){
                System.out.println("Convergance!");
                for (int i = population.size()-1; i >= population.size()-population.size()/5; i--) {
                    Brain brain = new Brain();
                    population.set(i,brain);
                }
                convergence=0;
            }
        }
        System.out.println("Najlepszy wynik to: "+best.getScore());
        System.out.println("Najwyzszy block to: "+best.getBestBlock());
        System.out.println("Sredni wynik to: "+averageFitness);
    }

    public List<Brain> getPopulation() {
        try {
            mutex.lock();
            return population;
        } catch (Exception e) {

        } finally {
            mutex.unlock();
        }
        return null;
    }

    public int getGeneration() {
        try {
            mutex.lock();
            return generation;
        } catch (Exception e) {

        } finally {
            mutex.unlock();
        }
        return 0;
    }

    public void setGeneration(int generation) {
        try {
            mutex.lock();
            this.generation = generation;
        } catch (Exception e) {

        } finally {
            mutex.unlock();
        }
    }


    public boolean isGroupset() {
        try {
            mutex.lock();
            return groupSet;
        } catch (Exception e) {

        } finally {
            mutex.unlock();
        }
        return false;
    }

    public void setGroupset(boolean groupSet) {
        try {
            mutex.lock();
            this.groupSet = groupSet;
        } catch (Exception e) {

        } finally {
            mutex.unlock();
        }
    }

    public boolean isGenerated() {
        try {
            mutex.lock();
            return generated;
        } catch (Exception e) {

        } finally {
            mutex.unlock();
        }
        return false;
    }

    public void setGenerated(boolean generated) {
        try {
            mutex.lock();
            this.generated = generated;
        } catch (Exception e) {

        } finally {
            mutex.unlock();
        }
    }

    public Brain getParent(){
        return parent;
    }

    public Brain getSecondParent() {
        return secondParent;
    }

    public Brain getChild() {
        return child;
    }

    public Brain getSecondChild() {
        return secondChild;
    }

    public boolean isCrossing() {
        return crossing;
    }

    public void setUserInterface(UserInterface userInterface) {
        this.userInterface = userInterface;
    }

    public int getCrossoverPoint() {
        return crossoverPoint;
    }
}
