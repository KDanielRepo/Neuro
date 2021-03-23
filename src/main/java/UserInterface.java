import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserInterface extends Application {

    private GameInstance[] instances;
    private Genetics genetics;
    private Mutex mutex;

    private Integer delay;
    private Integer geneticsDelay;
    private Integer index;
    private Integer generationIndex;
    private Integer prefToolboxItemSizeX;
    private Integer prefToolboxItemSizeY;
    private boolean paused;

    private BrainController brainController;
    private Integer[][] tilesValues;
    private HBox toolbox;
    private VBox  neuralNetworkToolbox;
    private VBox geneticsToolbox;
    private VBox gameViewToolbox;
    private ComboBox instanceSelector;
    private TextField instanceNumber;

    private Canvas gameArea;
    private GraphicsContext gameAreaGC;
    private int squareSize;

    //wizualizacja
    private GraphicsContext gc;
    private Stage neuralNetworkStage;
    private Canvas canvas;

    //debug menu
    private Stage debugStage;
    private GraphicsContext debugGc;
    private Canvas debugCanvas;
    private Canvas generalInfo;
    private GraphicsContext generalInfoGC;
    private boolean allFinished;
    private int finishedInstances;
    private ComboBox debugSelector;
    private VBox debugToolbox;

    //genetic menu
    private Stage geneticStage;
    private GraphicsContext geneticGc;
    private Canvas geneticsCanvas;

    private List<Integer> neuralNetworkStructure;
    private boolean neuralNetworkStageShow;
    private boolean geneticsStageShow;

    //Menus
    private MenuBar menuBar;
    private Menu fileMenu;
    private Menu gameMenu;
    private Menu neuralNetworkMenu;
    private Menu geneticsMenu;

    private Font font;
    private final static String STYLE = "-fx-font-size:20";

    //nn vu
    private MenuBar neuralNetworkMenuBar;
    private boolean showConnections;

    @Override
    public void start(Stage primaryStage) throws Exception {
        //OBIEKTY
        mutex = new Mutex();
        genetics = new Genetics();
        genetics.setUserInterface(this);
        genetics.getBest().setUserInterface(this);
        genetics.setMutex(mutex);
        geneticsDelay = 1000;

        font = new Font("Times New Roman",20);
        neuralNetworkStructure = new ArrayList<>();
        index = 0;
        generationIndex = 0;
        delay = 200;
        prefToolboxItemSizeX = 200;
        prefToolboxItemSizeY = 45;
        //WIDOK
        primaryStage.setMinWidth(1400);
        primaryStage.setMinHeight(900);
        primaryStage.setTitle("2048 solver");
        BorderPane borderPane = new BorderPane();
        Scene scene = new Scene(borderPane);

        gameArea = new Canvas(800, 800);
        gameAreaGC = gameArea.getGraphicsContext2D();
        squareSize = (int) gameArea.getWidth() / 4;

        prepareToolbox();
        createMenus();
        borderPane.setTop(menuBar);
        borderPane.setCenter(gameArea);
        borderPane.setRight(toolbox);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void prepareToolbox(){
        toolbox = new HBox();
        if(gameViewToolbox==null){
            prepareGameViewToolbox();
        }
        toolbox.getChildren().addAll(gameViewToolbox);
    }

    private void prepareGameViewToolbox(){
        gameViewToolbox = new VBox();
        Background background = new Background(new BackgroundFill(Color.LIGHTGRAY,CornerRadii.EMPTY, Insets.EMPTY));
        gameViewToolbox.setBackground(background);
        Label instanceNumberLabel = new Label("Number of instances");
        instanceNumberLabel.setPrefSize(prefToolboxItemSizeX, prefToolboxItemSizeY);
        instanceNumberLabel.setFont(font);
        instanceNumberLabel.setAlignment(Pos.CENTER);
        instanceNumber = new TextField();
        instanceNumber.setFont(font);
        instanceNumber.setPrefSize(prefToolboxItemSizeX, prefToolboxItemSizeY);
        Button createInstancesButton = new Button("Create instances");
        createInstancesButton.setStyle(STYLE);
        createInstancesButton.setPrefSize(prefToolboxItemSizeX, prefToolboxItemSizeY);
        createInstancesButton.setOnAction(e -> {
            createInstances();
            addInstanceSelection();
        });

        Button startInstancesButton = new Button("Start instances");
        startInstancesButton.setStyle(STYLE);
        startInstancesButton.setPrefSize(prefToolboxItemSizeX, prefToolboxItemSizeY);
        startInstancesButton.setOnAction(e -> {
            Arrays.stream(instances).forEach(i -> {
                i.start();
            });
            startInstancesButton.setDisable(true);
        });

        Button pauseButton = new Button("Pause all instances");
        pauseButton.setStyle(STYLE);
        pauseButton.setPrefSize(prefToolboxItemSizeX, prefToolboxItemSizeY);
        pauseButton.setOnAction(e -> {
            setPaused(!paused);
        });

        gameViewToolbox.getChildren().addAll(
                instanceNumberLabel,
                instanceNumber,
                createInstancesButton,
                startInstancesButton,
                pauseButton);
    }

    private void createMenus(){
        if(menuBar==null){
            menuBar = new MenuBar();
            createFileMenu();
            createNeuralNetworkMenu();
            createGeneticsMenu();
            createGameMenu();
            menuBar.getMenus().addAll(fileMenu,gameMenu,neuralNetworkMenu,geneticsMenu);
        }
    }
    private void createFileMenu(){
        if(fileMenu==null){
            fileMenu = new Menu("File");
            MenuItem exportItem = new MenuItem("Export best specimen");
            exportItem.setOnAction(e->{
                BrainExporter.exportBrain(genetics.getBest());
            });
            MenuItem importItem = new MenuItem("Import best specimen");
            importItem.setOnAction(e->{
                BrainExporter.importBrain(genetics.getBest());
            });
            fileMenu.getItems().addAll(exportItem,importItem);
        }
    }
    private void createNeuralNetworkMenu(){
        if(neuralNetworkMenu==null){
            neuralNetworkMenu = new Menu("Neural net");
            MenuItem visualisationItem = new MenuItem("Visualize neural network");
            visualisationItem.setOnAction(e->{
                if (neuralNetworkStage == null) {
                    createNeuralNetworkScene();
                }
                neuralNetworkStage.show();
            });
            MenuItem structureItem = new MenuItem("Create neural network structure");
            structureItem.setOnAction(e->{
                TextInputDialog structureDialog = new TextInputDialog();
                structureDialog.setTitle("Set neural network structure");
                structureDialog.setHeaderText("Insert number of nodes in given layers separated by commas (eg. 16,8,8,4): ");
                structureDialog.setContentText("Insert structure: ");
                Optional<String> result = structureDialog.showAndWait();
                result.ifPresent(structure->{
                    neuralNetworkStructure = Arrays.stream(structure.split(",")).map(Integer::parseInt).collect(Collectors.toList());
                    if(instances!=null){
                        Arrays.stream(instances).forEach(i->{
                            i.getBrainController().getBrain().setStructure(neuralNetworkStructure);
                            i.getBrainController().getBrain().createNN();
                        });
                    }
                    if(genetics.getBest()!=null){
                        genetics.getBest().setStructure(neuralNetworkStructure);
                        genetics.getBest().createNN();
                    }
                });
            });
            neuralNetworkMenu.getItems().addAll(visualisationItem,structureItem);
        }
    }
    private void createGeneticsMenu(){
        if(geneticsMenu==null){
            geneticsMenu = new Menu("Genetics");
            MenuItem visualisationItem = new MenuItem("Visualize genetic algorithm");
            visualisationItem.setOnAction(e->{
                if(geneticStage==null){
                    createGeneticAlgorithmScene();
                }
                geneticStage.show();
            });
            MenuItem geneticsDelayItem = new MenuItem("Genetic algorithm animation delay");
            geneticsDelayItem.setOnAction(e->{
                TextInputDialog geneticsDelayDialog = new TextInputDialog();
                geneticsDelayDialog.setTitle("Set GA visualisation delay");
                geneticsDelayDialog.setHeaderText("Insert how fast should the genetic algorithm animation be (In ms; Lower number equals faster animation): ");
                geneticsDelayDialog.setContentText("Insert delay: ");
                Optional<String> result = geneticsDelayDialog.showAndWait();
                result.ifPresent(delay->{
                    try {
                        setGeneticsDelay(Integer.parseInt(delay));
                    } catch (Exception ex) {
                        System.out.println("Podano niepoprawna wartość opóźnienia");
                    }
                });
            });
            MenuItem populationItem = new MenuItem("Set population size");
            populationItem.setOnAction(e->{
                TextInputDialog populationDialog = new TextInputDialog();
                populationDialog.setTitle("Set population size");
                populationDialog.setHeaderText("Insert how big should population be: ");
                populationDialog.setContentText("Insert size: ");
                Optional<String> result = populationDialog.showAndWait();
                result.ifPresent(size->genetics.setPopulationSize(Integer.parseInt(size)));
            });
            MenuItem generationItem = new MenuItem("Set generation number");
            generationItem.setOnAction(e->{
                TextInputDialog generationDialog = new TextInputDialog();
                generationDialog.setTitle("Set generation number");
                generationDialog.setHeaderText("Insert how many generations should be run: ");
                generationDialog.setContentText("Insert number: ");
                Optional<String> result = generationDialog.showAndWait();
                result.ifPresent(number->genetics.setGeneration(Integer.parseInt(number)));
            });
            geneticsMenu.getItems().addAll(visualisationItem,geneticsDelayItem,populationItem,generationItem);
        }
    }
    private void createGameMenu(){
        if(gameMenu==null){
            gameMenu = new Menu("General settings");
            MenuItem gameDelayItem = new MenuItem("Game animation delay");
            gameDelayItem.setOnAction(e->{
                TextInputDialog gameDelayDialog = new TextInputDialog();
                gameDelayDialog.setTitle("Set game delay");
                gameDelayDialog.setHeaderText("Insert how fast should the game animation be (In ms; Lower number equals faster animation): ");
                gameDelayDialog.setContentText("Insert delay: ");
                Optional<String> result = gameDelayDialog.showAndWait();
                result.ifPresent(delay->{
                    try {
                        setDelay(Integer.parseInt(delay));
                    } catch (Exception ex) {
                        System.out.println("Podano niepoprawna wartość opóźnienia");
                    }
                });
            });
            MenuItem noVisualsItem = new MenuItem("Disable game animation");
            noVisualsItem.setOnAction(e->{
                for (int i = 0; i < instances.length; i++) {
                    instances[i].setSelectedAsView(false);
                }
            });
            MenuItem debugMenuItem = new MenuItem("Open debug menu");
            debugMenuItem.setOnAction(e->{
                if (debugStage == null) {
                    createDebugScene();
                }
                debugStage.show();
            });
            gameMenu.getItems().addAll(gameDelayItem,noVisualsItem,debugMenuItem);
        }
    }

    private void createDebugScene() {
        debugStage = new Stage();
        debugCanvas = new Canvas(300, 600);
        debugGc = debugCanvas.getGraphicsContext2D();
        debugGc.setStroke(Color.BLACK);
        debugGc.setLineWidth(1f);

        generalInfo = new Canvas(200, 600);
        generalInfoGC = generalInfo.getGraphicsContext2D();

        BorderPane borderPane = new BorderPane();
        Scene scene = new Scene(borderPane);

        HBox hBox = new HBox();
        RadioButton[] radioButtons = new RadioButton[brainController.getBrain().getValues().length];
        ToggleGroup group = new ToggleGroup();
        for (int i = 0; i < radioButtons.length; i++) {
            RadioButton radioButton = new RadioButton("layer " + i);
            radioButton.setToggleGroup(group);
            hBox.getChildren().add(radioButton);
            radioButtons[i] = radioButton;
        }
        Button showSelectedWeights = new Button("show selected weights");
        /*showSelectedWeights.setOnAction(e -> {
            clearDebugMenuCanvas();
            for (int i = 0; i < radioButtons.length; i++) {
                if (radioButtons[i].isSelected()) {
                    int finalI = i;
                    AtomicInteger index = new AtomicInteger();
                    brainController.getBrain().getBrainMap().get(i).forEach(neuron -> {
                        neuron.getWeights().forEach(weight -> {
                            System.out.println(finalI);
                            if (finalI > 0) {
                                debugGc.strokeText("Waga nr. " + index + " to: " + weight.toString().substring(0, 6), debugCanvas.getWidth() / 2, debugCanvas.getHeight() / brainController.getBrain().getBrainMap().get(finalI).size() * index.get() + debugGc.getFont().getSize());
                            } else {
                                debugGc.strokeText("Waga nr. " + index + " to: " + weight.toString(), debugCanvas.getWidth() / 2, debugCanvas.getHeight() / brainController.getBrain().getBrainMap().get(finalI).size() * index.get() + debugGc.getFont().getSize());
                            }
                            index.getAndIncrement();
                        });
                    });
                }
            }
        });*/

        debugToolbox = new VBox();
        debugToolbox.getChildren().addAll(hBox, showSelectedWeights);
        borderPane.setRight(debugToolbox);
        borderPane.setCenter(debugCanvas);
        borderPane.setLeft(generalInfo);
        debugStage.setScene(scene);
        debugStage.setTitle("Debug menu");
        debugStage.show();
    }

    private void clearDebugMenuCanvas() {
        debugGc.clearRect(0, 0, debugCanvas.getWidth(), debugCanvas.getHeight());
        debugGc.setStroke(Color.BLACK);
        debugGc.setLineWidth(1f);
    }

    private void createNeuralNetworkScene() {
        neuralNetworkStage = new Stage();
        neuralNetworkStage.setTitle("Neural network visualization");
        neuralNetworkMenuBar = new MenuBar();
        canvas = new Canvas(1600, 1200);
        gc = canvas.getGraphicsContext2D();
        gc.setFont(font);

        Menu show = new Menu("Visualisation control");
        MenuItem connectionItem = new MenuItem("Hide/Show connections");
        connectionItem.setOnAction(e->{
            showConnections = !showConnections;
        });

        MenuItem printSum = new MenuItem("Show sum equation of neuron");
        printSum.setOnAction(e->{
            TextInputDialog printDialog = new TextInputDialog();
            printDialog.setTitle("Select neuron");
            printDialog.setHeaderText("Insert node number from which sum equation will be printed (starting from 0,0): ");
            printDialog.setContentText("Insert node number: ");
            Optional<String> result = printDialog.showAndWait();
            result.ifPresent(structure->{
                List<Integer> coords = Arrays.stream(structure.split(",")).map(Integer::parseInt).collect(Collectors.toList());
                //dokoncz wypisywanie
            });
        });

        show.getItems().addAll(connectionItem,printSum);
        neuralNetworkMenuBar.getMenus().addAll(show);
        ZoomableScrollPane scrollPane = new ZoomableScrollPane(canvas);
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(scrollPane);
        borderPane.setTop(neuralNetworkMenuBar);
        Scene neuralNetworkScene = new Scene(borderPane);
        neuralNetworkStage.setScene(neuralNetworkScene);
        neuralNetworkStage.setX(300);
        neuralNetworkStage.setY(300);
        neuralNetworkStage.getScene().getWindow().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST,this::neuralNetworkStageCloseEvent);
        neuralNetworkStage.getScene().getWindow().addEventHandler(WindowEvent.WINDOW_SHOWING,this::neuralNetworkStageShowEvent);
    }

    public void updateNeuralNetworkScene() {
        if (neuralNetworkStage != null && neuralNetworkStageShow) {
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2f);

            int divisionSize = brainController.getBrain().getValues()[0].length;
            int layer = brainController.getBrain().getValues().length;
            int neuronWidth = (int) canvas.getHeight() / divisionSize;
            int neuronHeight = (int) canvas.getHeight() / divisionSize;

            for (int i = 0; i < divisionSize; i++) {
                for (int k = 0; k < divisionSize; k++) {
                    //zaczyna sie przy kazdym wejsciu albo neuronie
                    //leci do kazdego nastepnego neuronu
                    // i to warstwa w sieci, j to numer danego neuronu w danej sieci
                    gc.setStroke(Color.BLACK);
                    if(i==0){
                        gc.strokeOval(canvas.getWidth() / layer * i, canvas.getHeight() / divisionSize * k, neuronWidth, neuronHeight);
                        gc.setFill(Color.LIGHTSTEELBLUE);
                        gc.fillOval(canvas.getWidth() / layer * i, canvas.getHeight() / divisionSize * k, neuronWidth, neuronHeight);
                    }
                    gc.setStroke(Color.RED);
                    if(showConnections){
                        gc.strokeLine(canvas.getWidth() / layer * 0 + neuronWidth, canvas.getHeight() / divisionSize * i + neuronHeight / 2, canvas.getWidth() / layer * 1 - neuronWidth, canvas.getHeight() / divisionSize * k + neuronHeight / 2);
                    }
                }
            }

            for (int i = 1; i < layer + 1; i++) {
                int layerSize = brainController.getBrain().getValues()[i-1].length;
                for (int j = 0; j < layerSize; j++) {
                    if (i == 1) {
                        gc.setStroke(Color.BLACK);
                        gc.strokeText(tilesValues[j / 4][j % 4].toString(), canvas.getWidth() / layer * (i - 1) + neuronWidth/4, canvas.getHeight() / divisionSize * j + neuronHeight / 2);
                    }
                    for (int k = 0; k < layerSize; k++) {
                        int layerSizeNext = layerSize;
                        if (i < layer) {
                            layerSizeNext = brainController.getBrain().getValues()[i].length;
                        }
                        gc.setStroke(Color.RED);
                        if (k < layerSizeNext && showConnections){
                            gc.strokeLine(canvas.getWidth() / layer * (i) + neuronWidth, canvas.getHeight() / divisionSize * j + neuronHeight / 2, canvas.getWidth() / layer * (i + 1) - neuronWidth, canvas.getHeight() / divisionSize * k + neuronHeight / 2);
                        }
                    }
                    gc.setStroke(Color.BLACK);
                    gc.strokeOval(canvas.getWidth() / layer * i - neuronWidth, canvas.getHeight() / divisionSize * j, neuronWidth, neuronHeight);
                    /*if(i==1){
                        gc.setFill(Color.LIGHTSTEELBLUE);
                        gc.fillOval(canvas.getWidth() / layer * i - neuronWidth, canvas.getHeight() / divisionSize * j, neuronWidth, neuronHeight);
                    }*/if(i==layer && brainController.getCurrentMove()!=null && brainController.getCurrentMove()==j){
                        gc.setFill(Color.GREEN);
                        gc.fillOval(canvas.getWidth() / layer * i - neuronWidth, canvas.getHeight() / divisionSize * j, neuronWidth, neuronHeight);
                    }else{
                        gc.setFill(Color.LIGHTGRAY);
                        gc.fillOval(canvas.getWidth() / layer * i - neuronWidth, canvas.getHeight() / divisionSize * j, neuronWidth, neuronHeight);
                    }
                    String value = Float.toString(brainController.getBrain().getOutputs()[i-1][j]);
                    if(value.length()>6){
                        value = value.substring(0,6);
                    }

                    gc.strokeText(value, canvas.getWidth() / layer * i - (neuronWidth/1.2), canvas.getHeight() / divisionSize * j + neuronHeight / 2);
                }
            }
        }
    }

    private void createGeneticAlgorithmScene() {
        geneticStage = new Stage();
        geneticStage.setTitle("Wizualizacja algorytmu genetycznego");
        if(genetics.getParent()!=null){
            int width = 60*genetics.getParent().getWeightCount();
            geneticsCanvas = new Canvas(width,800);
        }else{
            geneticsCanvas = new Canvas(1200,800);
        }
        geneticGc = geneticsCanvas.getGraphicsContext2D();
        geneticGc.setStroke(Color.BLACK);
        geneticGc.setLineWidth(1f);
        geneticGc.setFont(font);

        ZoomableScrollPane scrollPane = new ZoomableScrollPane(geneticsCanvas);
        scrollPane.setPrefSize(1200,800);
        Scene geneticAlgorithmScene = new Scene(scrollPane);
        geneticStage.setScene(geneticAlgorithmScene);
        geneticStage.setX(300);
        geneticStage.setY(300);
        geneticStage.getScene().getWindow().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST,this::geneticsStageCloseEvent);
        geneticStage.getScene().getWindow().addEventHandler(WindowEvent.WINDOW_SHOWING,this::geneticsStageShowEvent);
    }

    public void updateGeneticAlgorithmScene(){
        if(geneticStage!=null && genetics.isCrossing() && geneticsStageShow){
            if(genetics.getParent()!=null && geneticsCanvas.getWidth()<60*genetics.getParent().getWeightCount()){
                geneticsCanvas.setWidth(60*genetics.getParent().getWeightCount());
                geneticGc = geneticsCanvas.getGraphicsContext2D();
            }
            geneticGc.clearRect(0,0,geneticsCanvas.getWidth(),geneticsCanvas.getHeight());
            int startX = 60;
            int startY = (int)geneticsCanvas.getHeight()/10;
            int width = startX;
            int height = startY;

            int crossoverIndex = 0;

            for (int i = 0; i < genetics.getParent().getWeights().length; i++) {
                for (int j = 0; j < genetics.getParent().getWeights()[i].length; j++) {
                    geneticGc.setFill(Color.GREEN);
                    geneticGc.fillRect(startX,startY,width,height);
                    geneticGc.setFill(Color.BLACK);
                    geneticGc.strokeRect(startX,startY,width,height);

                    String value = Float.toString(genetics.getParent().getWeights()[i][j]);
                    String value2 = Float.toString(genetics.getSecondParent().getWeights()[i][j]);
                    String childText="";
                    String childText2="";
                    if(value.length()>5){
                        value = value.substring(0,5);
                    }
                    if(value2.length()>5){
                        value2 = value2.substring(0,5);
                    }
                    geneticGc.strokeText(value,startX,startY*1.5);
                    //
                    //rodzic 2
                    geneticGc.setFill(Color.DARKRED);
                    geneticGc.fillRect(startX,startY*7,width,height);
                    geneticGc.setFill(Color.BLACK);
                    geneticGc.strokeRect(startX,startY*7,width,height);
                    geneticGc.strokeText(value2,startX,startY*7.5);
                    //
                    if(crossoverIndex<genetics.getCrossoverPoint()){
                        geneticGc.setFill(Color.GREEN);
                        childText = value;
                    }else{
                        geneticGc.setFill(Color.DARKRED);
                        childText = value2;
                    }
                    //dziecko 1
                    geneticGc.fillRect(startX,startY*3,width,height);
                    geneticGc.setFill(Color.BLACK);
                    geneticGc.strokeRect(startX,startY*3,width,height);
                    geneticGc.strokeText(childText,startX,startY*3.5);
                    //
                    if(i<genetics.getCrossoverPoint()){
                        geneticGc.setFill(Color.DARKRED);
                        childText2 = value2;
                    }else{
                        geneticGc.setFill(Color.GREEN);
                        childText2 = value;
                    }
                    //dziecko 2
                    geneticGc.fillRect(startX,startY*5,width,height);
                    geneticGc.setFill(Color.BLACK);
                    geneticGc.strokeRect(startX,startY*5,width,height);
                    geneticGc.strokeText(childText2,startX,startY*5.5);
                    //
                    startX+=width;
                    crossoverIndex++;
                }
            }
        }
    }

    private void showSelectedInstance(int instance) {
        brainController = instances[instance].getBrainController();
        tilesValues = instances[instance].getValues();
        updateGameArea();
    }

    private void createInstances() {
        instances = new GameInstance[Integer.parseInt(instanceNumber.getText())];
        for (int i = 0; i < instances.length; i++) {
            GameInstance instance = new GameInstance();
            instance.getBrainController().setUserInterface(this);
            instance.setTries(0);
            instance.setGenetics(genetics);
            instance.setUserInterface(this);
            if(i>0){
                setIndex(getIndex()+1);
            }
            //instance.setIndex(getIndex());
            instances[i] = instance;
        }
    }

    //dokoncz i napraw
    private void addDebugSelector(){
        if(!debugToolbox.getChildren().contains(debugSelector)){
            List<String> names = new ArrayList<>();
            names.add("Instances");
            names.add("Debug");
            names.add("Neural Network");
            ObservableList<String> list = FXCollections.observableArrayList(names);
            debugSelector = new ComboBox(list);
            debugSelector.setPrefSize(150, 20);
            Button setSelectedButton = new Button("select debug menu");
            setSelectedButton.setPrefSize(150, 20);
            setSelectedButton.setOnAction(e -> {
                switch(debugSelector.getSelectionModel().getSelectedIndex()){
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                }
                for (int i = 0; i < instances.length; i++) {
                    instances[i].setSelectedAsView(false);
                }
                instances[instanceSelector.getSelectionModel().getSelectedIndex()].setSelectedAsView(true);
                showSelectedInstance(instanceSelector.getSelectionModel().getSelectedIndex());
            });
            toolbox.getChildren().addAll(instanceSelector, setSelectedButton);
        }
    }

    private void addInstanceSelection() {
        if (!gameViewToolbox.getChildren().contains(instanceSelector)) {
            List<String> names = new ArrayList<>();
            for (int i = 0; i < instances.length; i++) {
                names.add(instances[i].getName());
            }
            ObservableList<GameInstance> list = FXCollections.observableArrayList(instances);
            instanceSelector = new ComboBox(list);
            instanceSelector.setStyle(STYLE);
            instanceSelector.setPrefSize(prefToolboxItemSizeX, prefToolboxItemSizeY);
            Button setSelectedButton = new Button("select instance");
            setSelectedButton.setStyle(STYLE);
            setSelectedButton.setPrefSize(prefToolboxItemSizeX, prefToolboxItemSizeY);
            setSelectedButton.setOnAction(e -> {
                for (int i = 0; i < instances.length; i++) {
                    instances[i].setSelectedAsView(false);
                }
                instances[instanceSelector.getSelectionModel().getSelectedIndex()].setSelectedAsView(true);
                showSelectedInstance(instanceSelector.getSelectionModel().getSelectedIndex());
            });
            gameViewToolbox.getChildren().addAll(instanceSelector, setSelectedButton);
        }
    }

    public void updateGeneralInfo() {
        /*generalInfoGC.clearRect(0, 0, generalInfo.getWidth(), generalInfo.getHeight());
        generalInfoGC.setStroke(Color.BLACK);
        generalInfoGC.setLineWidth(1f);
        double height = generalInfoGC.getFont().getSize();
        generalInfoGC.strokeText("Lista bloków: ", 0, height);
        for (int i = 0; i < brainController.getBlocks().size(); i++) {
            generalInfoGC.strokeText(brainController.getBlocks().get(i).toString(), 0, height * (2 + i));
        }
        generalInfoGC.strokeText("Wartości na wyjściach: ", 0, height * 7);
        for (int i = 0; i < brainController.getBrain().getOutputLayer().size(); i++) {
            String pre = "";
            switch (i) {
                case 0:
                    pre = "góra: ";
                    break;
                case 1:
                    pre = "prawo: ";
                    break;
                case 2:
                    pre = "dół: ";
                    break;
                case 3:
                    pre = "lewo: ";
                    break;
            }
            String middle = Iterables.get(brainController.getBrain().getOutputLayer().values(), i).toString();
            generalInfoGC.strokeText(pre + middle, 0, height * (8 + i));
        }*/
    }

    public void updateGameArea() {
        gameAreaGC.clearRect(0, 0, gameArea.getWidth(), gameArea.getHeight());
        for (int i = 0; i < 5; i++) {
            gameAreaGC.setLineWidth(5f);
            gameAreaGC.setFill(Color.BLACK);
            gameAreaGC.strokeLine(i * squareSize, 0, i * squareSize, gameArea.getHeight());
            gameAreaGC.strokeLine(0, i * squareSize, gameArea.getWidth(), i * squareSize);
            for (int j = 0; j < 4; j++) {
                if (i < 4) {
                    if (tilesValues[i][j] != 0) {
                        gameAreaGC.setLineWidth(2f);
                        gameAreaGC.setFill(colorFromScore(tilesValues[i][j]));
                        gameAreaGC.fillRect((j * squareSize) + 2, (i * squareSize) + 2, squareSize - 4, squareSize - 4);
                        gameAreaGC.setFill(Color.BLACK);
                        gameAreaGC.setFont(new Font("Times New Roman",30));
                        gameAreaGC.strokeText(tilesValues[i][j].toString(), (j * squareSize) + squareSize / 2, (i * squareSize) + squareSize / 2);
                    }
                }
            }
        }
    }

    private Color colorFromScore(int score) {
        if (score != 0) {
            switch (score) {
                case 2:
                    return new Color(1, 0.89, 0.76, 1);
                case 4:
                    return new Color(1, 0.96, 0.83, 1);
                case 8:
                    return new Color(1, 0.85, 0.76, 1);
                case 16:
                    return new Color(0.91, 0.69, 0.56, 1);
                case 32:
                    return new Color(0.91, 0.75, 0.56, 1);
                case 64:
                    return new Color(1, 0.77, 0.76, 1);
                case 128:
                    return new Color(0.91, 0.58, 0.56, 1);
                case 256:
                    return new Color(0.75, 0.49, 0.34, 1);
                case 512:
                    return new Color(0.75, 0.37, 0.34, 1);
                case 1024:
                    return new Color(0.61, 0.22, 0.20, 1);
                default:
                    return new Color(0.44, 0.09, 0.16, 1);
            }
        }
        return Color.WHITE;
    }

    public void startGenetics() {
        genetics.run();
    }

    public static void main(String[] args) {
        launch(UserInterface.class, args);
    }



    private void neuralNetworkStageCloseEvent(WindowEvent windowEvent){
        neuralNetworkStageShow = false;
    }
    private void neuralNetworkStageShowEvent(WindowEvent windowEvent){
        neuralNetworkStageShow = true;
    }

    private void geneticsStageCloseEvent(WindowEvent windowEvent){
        geneticsStageShow = false;
    }
    private void geneticsStageShowEvent(WindowEvent windowEvent){
        geneticsStageShow = true;
    }


    public Integer getGenerationIndex() {
        try {
            mutex.lock();
            return generationIndex;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mutex.unlock();
        }
        return 0;
    }

    public void setGenerationIndex(Integer generationIndex) {
        try {
            mutex.lock();
            this.generationIndex = generationIndex;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mutex.unlock();
        }
    }

    public Integer getIndex() {
        try {
            /*if (index > genetics.getPopulation() / 2) {
                genetics.setGenerated(false);
            }*/
            mutex.lock();
            return index;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mutex.unlock();
        }
        return 0;
    }

    public void setIndex(Integer index) {
        try {
            mutex.lock();
            this.index = index;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mutex.unlock();
        }
    }

    public Integer getDelay() {
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public boolean isPaused() {
        try {
            mutex.lock();
            return paused;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mutex.unlock();
        }
        return false;
    }

    public void setPaused(boolean paused) {
        try {
            mutex.lock();
            this.paused = paused;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mutex.unlock();
        }
    }

    public boolean isAllFinished() {
        try {
            if (getFinishedInstances() == instances.length) {
                mutex.lock();
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mutex.unlock();
        }
        return false;
    }

    public int getFinishedInstances() {
        try {
            mutex.lock();
            return finishedInstances;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mutex.unlock();
        }
        return 0;
    }

    public void setFinishedInstances(int finishedInstances) {
        try {
            mutex.lock();
            this.finishedInstances = finishedInstances;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mutex.unlock();
        }
    }

    public void setTilesValues(Integer[][] tilesValues) {
        this.tilesValues = tilesValues;
    }

    public GameInstance[] getInstances() {
        return instances;
    }

    public Integer getGeneticsDelay() {
        return geneticsDelay;
    }

    public void setGeneticsDelay(Integer geneticsDelay) {
        this.geneticsDelay = geneticsDelay;
    }

    public List<Integer> getNeuralNetworkStructure() {
        return neuralNetworkStructure;
    }
}
