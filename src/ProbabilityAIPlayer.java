import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by silasa on 10/27/16.
 */
public class ProbabilityAIPlayer extends Player {
    ArrayList<Integer> myShips = new ArrayList<>();
    private boolean isFistMove = true;
    private int[] enemyBoardSize;
    private Board enemyBoard;
    private Board enemyProbabilityBoard;

    ProbabilityAIPlayer(String name) {
        super(name);
    }

    protected void endGame() {
        if (gameLogic.getIsWinner(this)) {
            System.out.println("Smarty mc smartypants " + threadName + " does it again!");
        } else {
            System.out.println("HOW? How could i loose, " + threadName + "?! It's the RNG's fault!");
        }
    }

    protected void makeAMove() {
        if (isFistMove) {
            isFistMove = false;
            enemyBoardSize = gameLogic.getEnemyBoardSize(this);
            enemyBoard = new Board(enemyBoardSize[0]);
        }
        enemyProbabilityBoard = generateProbabilityBoard(enemyBoard);
        ArrayList<Position> possiblePositions = getMostProbablePositions(enemyProbabilityBoard);
        Position target = possiblePositions.get((int)Math.floor(Math.random()*possiblePositions.size()));
        int result = gameLogic.tryHitFrom(target, this);
        enemyBoard.setSegment(target, result);
    }

    private int getLargest(ArrayList<Integer> values) {
        int largest = 0;
        for (int size : values) {
            if (size > largest) {
                largest = size;
            }
        }
        return largest;
    }

    private Board generateProbabilityBoard(Board board) {
        ArrayList<Integer> cShips = findShips(board);
        ArrayList<Integer> missingShips = removeFrom(cShips,myShips);
        int largestShip = getLargest(missingShips);
        int width = board.getBoardSizeX();
        int height = board.getBoardSizeY();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                board.setSegment(i,j,0);
                if (board.getSegment(i,j)==Board.NOTHING){
                    for (int k=0;k<largestShip;k++){ //+x
                        if (board.getSegment(i+k,j)==Board.HIT_NOTHING){
                            break;
                        }else if (board.getSegment(i+k,j)==Board.NOTHING){
                            int fieldValue = Collections.frequency(missingShips,new Integer(k+1)) + board.getSegment(i,j);
                            board.setSegment(i,j,fieldValue);
                        }else if (board.getSegment(i+k,j)==Board.SHIP || board.getSegment(i+k,j)==Board.HIT_SHIP){
                            int modValue = 2; //TODO Make dependent on ship orientation.
                            int fieldValue = modValue*Collections.frequency(missingShips,new Integer(k+1)) + board.getSegment(i,j);
                            board.setSegment(i,j,fieldValue);
                        }
                    }for (int k=0;k<largestShip;k++){ //+y
                        if (board.getSegment(i,j+k)==Board.HIT_NOTHING){
                            break;
                        }else if (board.getSegment(i,j+k)==Board.NOTHING){
                            int fieldValue = Collections.frequency(missingShips,new Integer(k+1)) + board.getSegment(i,j);
                            board.setSegment(i,j,fieldValue);
                        }else if (board.getSegment(i,j+k)==Board.SHIP || board.getSegment(i,j+k)==Board.HIT_SHIP){
                            int modValue = 2; //TODO Make dependent on ship orientation.
                            int fieldValue = modValue*Collections.frequency(missingShips,new Integer(k+1)) + board.getSegment(i,j);
                            board.setSegment(i,j,fieldValue);
                        }
                    }for (int k=0;k<largestShip;k++){ //-x
                        if (board.getSegment(i-k,j)==Board.HIT_NOTHING){
                            break;
                        }else if (board.getSegment(i-k,j)==Board.NOTHING){
                            int fieldValue = Collections.frequency(missingShips,new Integer(k+1)) + board.getSegment(i,j);
                            board.setSegment(i,j,fieldValue);
                        }else if (board.getSegment(i-k,j)==Board.SHIP || board.getSegment(i-k,j)==Board.HIT_SHIP){
                            int modValue = 2; //TODO Make dependent on ship orientation.
                            int fieldValue = modValue*Collections.frequency(missingShips,new Integer(k+1)) + board.getSegment(i,j);
                            board.setSegment(i,j,fieldValue);
                        }
                    }for (int k=0;k<largestShip;k++){ //-y
                        if (board.getSegment(i,j-k)==Board.HIT_NOTHING){
                            break;
                        }else if (board.getSegment(i,j-k)==Board.NOTHING){
                            int fieldValue = Collections.frequency(missingShips,new Integer(k+1)) + board.getSegment(i,j);
                            board.setSegment(i,j,fieldValue);
                        }else if (board.getSegment(i,j-k)==Board.SHIP || board.getSegment(i,j-k)==Board.HIT_SHIP){
                            int modValue = 2; //TODO Make dependent on ship orientation.
                            int fieldValue = modValue*Collections.frequency(missingShips,new Integer(k+1)) + board.getSegment(i,j);
                            board.setSegment(i,j,fieldValue);
                        }
                    }
                }
            }
        }
        return board;
    }

    private ArrayList<Integer> removeFrom(ArrayList<Integer> firstList, ArrayList<Integer> removeList) {
        for (int i=0; i<firstList.size(); i++){
            for (int j=0; j<removeList.size();j++){
                if (firstList.get(i).equals(removeList.get(j))){
                    firstList.remove(i);
                    removeList.remove(j);
                    break;
                }
            }
        }
        return firstList;
    }

    private ArrayList<Integer> findShips(Board board) {
        ArrayList<Integer> cShips = new ArrayList();
        int width = board.getBoardSizeX();
        int height = board.getBoardSizeY();
        int[][] usedPos = new int[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (usedPos[i][j] != 1) {
                    if (board.getSegment(i, j) == Board.SHIP) {
                        int cShip = 1;
                        if (board.getSegment(i + 1, j) == Board.SHIP) {
                            int add = 0;
                            while (true) {
                                if (board.getSegment(i + add, j) != Board.SHIP) {
                                    break;
                                }
                                usedPos[i + add][j] = 1;
                                add += 1;
                                cShip = add + 1;
                            }
                        } else if (board.getSegment(i, j + 1) == Board.SHIP) {
                            int add = 0;
                            while (true) {
                                if (board.getSegment(i, j + add) != Board.SHIP) {
                                    break;
                                }
                                usedPos[i][j+add] = 1;
                                add += 1;
                                cShip = add + 1;
                            }
                        }
                        cShips.add(cShip);
                    }
                }
            }
        }
        return cShips;
    }

    private ArrayList<Position> getMostProbablePositions(Board board) {
        int largestProb = 0;
        ArrayList<Position> positions = new ArrayList<>();
        int width = board.getBoardSizeX();
        int height = board.getBoardSizeY();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (board.getSegment(i, j) > largestProb) {
                    positions.clear();
                    positions.add(new Position(i, j));
                } else if (board.getSegment(i, j) == largestProb) {
                    positions.add(new Position(i, j));
                }
            }
        }
        return positions;
    }

    protected void placeAShip() {
        while (true) {
            //Keep going until a successful placement.
            int shipLength = gameLogic.getNextPlacement(this);
            if (shipLength == 0) {
                break;
            }
            ArrayList<Position> placeablePositions = gameLogic.getPlaceablePositions(this);
            Position chosenPos = placeablePositions.get((int) (Math.random() * placeablePositions.size()));
            Position chosenEndPos = chosenPos;
            if (shipLength != 1) {
                ArrayList<Position> possibleEndPositions = gameLogic.getPossibleEndPositions(this, chosenPos, shipLength);
                if (possibleEndPositions.size() == 0) {
                    continue;
                }
                chosenEndPos = possibleEndPositions.get((int) (Math.random() * possibleEndPositions.size()));
            }
            if (gameLogic.placeShip(chosenPos, chosenEndPos, this)) {
                myShips.add(shipLength);
                break;
            }
        }

    }

    @Override
    public void hitByEnemy(Position hitPosition, int hitType) {
        //Don rly do anyting
    }


}