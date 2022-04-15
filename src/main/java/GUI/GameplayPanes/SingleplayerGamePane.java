package GUI.GameplayPanes;

import GUI.CustomButton;
import GUI.GameMode;
import GUI.GamePane;
import GUI.MenuPanes.MainMenuPane;
import engine.Engine;
import engine.MoveResult;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

import java.util.concurrent.*;

public class SingleplayerGamePane extends MultiplayerGamePane {
    private final boolean WHITE_IS_BOTTOM;
    private final double difficulty;
    private final ConfidenceBar CONFIDENCE_BAR;
    public SingleplayerGamePane(boolean WHITE_IS_BOTTOM, double difficulty, long startingTime) {
        super(WHITE_IS_BOTTOM, GameMode.SOLO,startingTime);
        this.WHITE_IS_BOTTOM = WHITE_IS_BOTTOM;
        this.difficulty=difficulty;
        CustomButton undoButton= new CustomButton(heightProperty().divide(13),"UndoArrow.png");

        CustomButton redoButton= new CustomButton(heightProperty().divide(13),"RedoArrow.png");
        mainPane.setSpacing(9);

        CONFIDENCE_BAR= new ConfidenceBar(heightProperty(), WHITE_IS_BOTTOM);

        mainPane.getChildren().add(0,CONFIDENCE_BAR);

        HBox undoRedoPane = new HBox();
        undoRedoPane.setSpacing(50);
        undoRedoPane.setAlignment(Pos.CENTER);
        undoRedoPane.getChildren().addAll(undoButton,redoButton);
        moveHistory.getChildren().add(undoRedoPane);
        moveHistory.prefSizePropertyBind(heightProperty().divide(1.1));


        undoButton.setOnAction(e->chessBoardPane.undo());
        redoButton.setOnAction(e->chessBoardPane.redo());

        Engine.setDifficulty(difficulty);
        startEngine();
    }
    private void startEngine() {
        ExecutorService engineThread= Executors.newSingleThreadExecutor();
        engineThread.execute(()->{
            while(!chessBoardPane.isReceivingMove) {
                Thread.onSpinWait();
            }
            Future<MoveResult> engineMove = Engine.getBestMove(chessBoardPane.internalBoard, WHITE_IS_BOTTOM ?blackRemainingTime : whiteRemainingTime);
            MoveResult bestMove = null;
            try {
                bestMove = engineMove.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                return;
            }catch (CancellationException ignored){

            }
            while (chessBoardPane.engineIsPaused) {
                Thread.onSpinWait();
                if(engineMove.isCancelled()) {
                    System.out.println("this ran");
                    chessBoardPane.engineIsPaused=false;
                    engineThread.shutdownNow();
                    startEngine();
                    return;
                }
            }
            CONFIDENCE_BAR.setPercentage(WHITE_IS_BOTTOM ?bestMove.confidence:1-bestMove.confidence);//FIXME
            MoveResult finalBestMove = bestMove;
            Platform.runLater(()-> {
                chessBoardPane.isReceivingMove=false;
                chessBoardPane.animateMovePiece(finalBestMove.move);
                startEngine();
            });
        });
    }

    @Override
    public GamePane nextMenu2() {//Rematch
        return new SingleplayerGamePane(WHITE_IS_BOTTOM,difficulty,startingTime);
    }
    @Override
    public GamePane previousMenu() {//Main Menu
        return new MainMenuPane();
    }


}
