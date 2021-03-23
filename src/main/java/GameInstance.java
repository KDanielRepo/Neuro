import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
public class GameInstance extends Thread {
    private Tile[][] tiles;
    private Integer[][] values;
    private Integer score;
    private BrainController brainController;
    private GameState gameState;
    private Integer tries;
    private Genetics genetics;
    private UserInterface userInterface;
    //private Integer index;
    private Integer highest;
    private boolean selectedAsView;
    private boolean scoreAdded;
    private boolean checkingAvailableMoves;
    private int moves;

    public GameInstance() {
        brainController = new BrainController();
        tiles = new Tile[4][4];
        values = new Integer[4][4];
        gameState = GameState.GAME_START;
        score = 0;
        highest = 0;
        random();
        random();
        brainController.setCurrentInputs(getValuesFromTiles());
        moves = 0;
    }

    public void run() {
        try {
            gameState = GameState.GAME_RUNNING;
            while (gameState != GameState.GAME_OVER) {
                while (userInterface.isPaused()){
                    sleep(1);
                }
                brainController.setCurrentInputs(getValuesFromTiles());
                values = getValuesFromTiles();
                brainController.setCurrentMove(brainController.generateMoveWithoutBlocks());
                move(brainController.getCurrentMove());
                if (selectedAsView) {
                    userInterface.updateGameArea();
                    userInterface.updateNeuralNetworkScene();
                }
                sleep(userInterface.getDelay());
                if (gameState == GameState.GAME_OVER) {
                    restart();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restart() {
        brainController.getBrain().setMoves(moves);
        brainController.getBrain().setBestBlock(highest);
        brainController.getBrain().setScore(score);
        if(userInterface.getGenerationIndex()>0 && userInterface.getIndex()<genetics.getPopulationSize()){
            brainController.setBrain(genetics.getPopulation().get(userInterface.getIndex()));
        }else if(userInterface.getGenerationIndex()==0){
            genetics.getPopulation().add(brainController.getBrain());
            Brain brain = new Brain();
            brainController.setBrain(brain);
        }
        if(userInterface.getIndex()==genetics.getPopulationSize()-1 && userInterface.getGenerationIndex()==0){
            userInterface.setFinishedInstances(userInterface.getFinishedInstances() + 1);
            userInterface.startGenetics();
            while (!genetics.isGroupset() || !genetics.isGenerated()) {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            genetics.setGroupset(false);
            genetics.setGenerated(false);
            brainController.setBrain(genetics.getPopulation().get(userInterface.getIndex()));
        }else if(userInterface.getIndex()==genetics.getPopulationSize() && userInterface.getGenerationIndex()!=0){
            userInterface.setFinishedInstances(userInterface.getFinishedInstances() + 1);
            userInterface.startGenetics();
            while (!genetics.isGroupset() || !genetics.isGenerated()) {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            genetics.setGroupset(false);
            genetics.setGenerated(false);
            brainController.setBrain(genetics.getPopulation().get(userInterface.getIndex()));
        }
        //index = userInterface.getIndex();
        userInterface.setIndex(userInterface.getIndex()+1);
        tiles = new Tile[4][4];
        values = new Integer[4][4];
        values = getValuesFromTiles();
        gameState = GameState.GAME_RUNNING;
        score = 0;
        highest = 0;
        moves = 0;
        random();
        random();
        values = getValuesFromTiles();
        brainController.setCurrentInputs(getValuesFromTiles());
        brainController.setBlocks(new ArrayList<>());
        userInterface.setTilesValues(getValuesFromTiles());
        if (selectedAsView) {
            userInterface.updateGameArea();
        }
    }

    public void move(int move) {
        switch (move) {
            case 0:
                if(moveLeft()){
                    moveLeft();
                }else {
                    brainController.addBlock(move);
                }
                break;
            case 1:
                if(moveUp()){
                    moveUp();
                }else {
                    brainController.addBlock(move);
                }
                break;
            case 2:
                if(moveRight()){
                    moveRight();
                }else {
                    brainController.addBlock(move);
                }
                break;
            case 3:
                if(moveDown()){
                    moveDown();
                }else {
                    brainController.addBlock(move);
                }
                break;
        }
    }

    /*private void createTiles() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Tile tile = new Tile(0);
                tiles[i][j] = tile;
            }
        }
    }*/

    private Integer[][] getValuesFromTiles() {
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles.length; j++) {
                if(tiles[i][j]!=null){
                    values[i][j] = tiles[i][j].getValue();
                }else{
                    values[i][j]=0;
                }
                /*if (tiles[i][j].getValue() != 0) {
                    System.out.println("r: " + i + " c: " + j + " == " + tiles[i][j].getValue());
                }*/
                //System.out.println(tiles[i][j].getValue());
            }
        }
        return values;
    }

    private void random() {
        int pos = ThreadLocalRandom.current().nextInt(16);
        int row, col;
        do {
            pos = (pos + 1) % (16);
            row = pos / 4;
            col = pos % 4;
        } while (tiles[row][col] != null);

        int val = ThreadLocalRandom.current().nextInt(10) == 0 ? 4 : 2;
        tiles[row][col] = new Tile(val);
    }

    private void clearMerged() {
        for (Tile[] row : tiles) {
            for (Tile tile : row) {
                if (tile != null) {
                    tile.setMerged(false);
                }
            }
        }
    }

    private boolean movesAvailable() {
        checkingAvailableMoves = true;
        boolean hasMoves = moveUp() || moveDown() || moveLeft() || moveRight();
        if(!moveLeft()){
            brainController.addBlock(0);
        }
        if(!moveUp()){
            brainController.addBlock(1);
        }
        if(!moveRight()){
            brainController.addBlock(2);
        }
        if(!moveDown()){
            brainController.addBlock(3);
        }
        checkingAvailableMoves = false;
        return hasMoves;
    }

    private boolean move(int countDownFrom, int yIncr, int xIncr) {
        boolean moved = false;

        for (int i = 0; i < 16; i++) {
            int j = Math.abs(countDownFrom - i);

            int r = j / 4;
            int c = j % 4;

            if (tiles[r][c] == null)
                continue;

            int nextR = r + yIncr;
            int nextC = c + xIncr;

            while (nextR >= 0 && nextR < 4 && nextC >= 0 && nextC < 4) {

                Tile next = tiles[nextR][nextC];
                Tile curr = tiles[r][c];

                if (next == null) {

                    if (checkingAvailableMoves)
                        return true;

                    tiles[nextR][nextC] = curr;
                    tiles[r][c] = null;
                    r = nextR;
                    c = nextC;
                    nextR += yIncr;
                    nextC += xIncr;
                    moves++;
                    moved = true;

                } else if (next.canMergeWith(curr)) {

                    if (checkingAvailableMoves)
                        return true;

                    int value = next.mergeWith(curr);
                    if (value > highest){
                        highest = value;
                    }
                    score += value;
                    tiles[r][c] = null;
                    moves++;
                    moved = true;
                    break;
                } else
                    break;
            }
        }

        if (moved) {
            brainController.setBlocks(new ArrayList<>());
            if (highest < 2048) {
                clearMerged();
                random();
                if (!movesAvailable()) {
                    gameState = GameState.GAME_OVER;
                }
            } else if (highest == 2048){
                brainController.setCurrentInputs(getValuesFromTiles());
                values = getValuesFromTiles();
                userInterface.setPaused(true);
                System.out.println("*******************************");
                System.out.println("UDALO SIE!!!");
                System.out.println("Osobnik" +this.toString()+" UZYSKAL BLOK 2048");
                System.out.println("*******************************");
                while (userInterface.isPaused()) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            //gameState = GameState.GAME_WON;
        }

        return moved;
    }

    private boolean moveUp() {
        return move(0, -1, 0);
    }

    private boolean moveDown() {
        return move(15, 1, 0);
    }

    private boolean moveLeft() {
        return move(0, 0, -1);
    }

    private boolean moveRight() {
        return move(15, 0, 1);
    }
}
