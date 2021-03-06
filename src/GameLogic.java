import java.util.ArrayList;

/**
 * Created by silasa on 10/4/16.
 */

public class GameLogic {
    //Singleton spooks
    private static GameLogic gameLogic = new GameLogic();
    private int boardSize = Config.getInstance().getBoardSize();

    private Board board1;
    private Board board2;

    private Player player1;
    private Player player2;

    private ArrayList<Integer> shipsToPlace1 = new ArrayList<>();
    private ArrayList<Integer> shipsToPlace2 = new ArrayList<>();

    private GameState gameState = GameState.PRESETUP;

    private int activePlayer = 0;
    private int hasWon = 0;

    public static GameLogic getInstance() {
        return gameLogic;
    }

    public void setup() {
        // load player and board, set gamestate
        board1 = new Board(boardSize);
        board2 = new Board(boardSize);
        player1 = new RealPlayer("Hooman");
        player1.start();
        player2 = new ProbabilityAIPlayer("Probability AI");
        player2.start();
        shipsToPlace1 = generateShipsFromBoardSize(board1.getBoardSizeX(), board1.getBoardSizeY());
        shipsToPlace2 = generateShipsFromBoardSize(board2.getBoardSizeX(), board2.getBoardSizeY());
        gameState = GameState.SETUP;
    }

    public ArrayList<Integer> generateShipsFromBoardSize(int maxX, int maxY) {
        ArrayList<Integer> ships = new ArrayList<>();
        int boardSize = maxX * maxY;
        double[] shipAmount = {0.02, 0.02, 0.01, 0.01, 0.001};// Amount of [n+1] ships per square
        int maxShipSize = 5;
        for (int i = 1; i < maxShipSize; i++) {
            int iShipsToPlace = (int) (shipAmount[i - 1] * boardSize);
            for (int j = 0; j < iShipsToPlace; j++) {
                ships.add(i);
            }
        }
        return ships;
    }

    public GameState getNextState(Player player) {
        if (gameState == GameState.PRESETUP) {
            return GameState.WAIT;
        } else if (gameState == GameState.PLAY) {
            if (player.equals(player1)) {
                if (activePlayer == 1) {
                    return GameState.PLAY;
                } else {
                    return GameState.WAIT;
                }
            } else {
                if (activePlayer == 2) {
                    return GameState.PLAY;
                } else {
                    return GameState.WAIT;
                }
            }
        } else return gameState;
    }

    public int tryHitFrom(Position position, Player player) {
        Board boardToHit = getOppositeBoard(player);
        Player hitPlayer = getOppositePlayer(player);
        int hit = boardToHit.tryHit(position);
        hitPlayer.hitByEnemy(position, hit);
        if (hit == Board.SHIP) {
            if (boardToHit.isCleared()) {
                gameState = GameState.GAMEOVER;
                if (player.equals(player1)) {
                    hasWon = 1;
                } else {
                    hasWon = 2;
                }
            }
        }
        switchActivePlayer();
        return hit;
        //TODO Find a way to make this independent of player count
    }

    private Board getOppositeBoard(Player player) {
        return getPlayerBoard(getOppositePlayer(player));
    }

    private Player getOppositePlayer(Player player) {
        if (player1.equals(player)) {
            return player2;
        } else {
            return player1;
        }
    }

    public void switchActivePlayer() {
        if (activePlayer == 1) {
            activePlayer = 2;
        } else {
            activePlayer = 1;
        }
    }

    public Board getPlayerBoard(Player player) {
        if (player.equals(player1)) {
            return board1;
        } else {
            return board2;
        }
    }

    public boolean getIsWinner(Player player) {
        if (player.equals(player1)) {
            return (hasWon == 1);
        } else if (player.equals(player2)) {
            return (hasWon == 2);
        }
        return false;
    }

    RealPlayer getRealPlayer() {
        if (player1 instanceof RealPlayer) {
            return (RealPlayer) player1;
        } else if (player2 instanceof RealPlayer) {
            return (RealPlayer) player2;
        } else {
            return null;
        }
    }

    public int[] getEnemyBoardSize(Player player) {
        return getOppositeBoard(player).getBoardSize();
    }

    public ArrayList<Position> getPlaceablePositions(Player player) {
        return getPlaceablePositions(getPlayerBoard(player));
    }

    public ArrayList<Position> getPlaceablePositions(Board board) {
        ArrayList<Position> positions = new ArrayList<>();
        for (int i = 0; i < board.getBoardSizeX(); i++) {
            for (int j = 0; j < board.getBoardSizeY(); j++) {
                if (board.getSegment(i, j) == Board.NOTHING) {
                    positions.add(new Position(i, j));
                }
            }
        }
        return positions;
    }

    public boolean placeShip(Position startPosition, Position endPosition, Player player) {
        return placeShip(startPosition, endPosition, getPlayerBoard(player));
    }

    public boolean placeShip(Position startPosition, Position endPosition, Board board) {
        //Check if can be placed, break if cant.
        if (!canBePlaced(startPosition, endPosition, board)) {
            return false;
        }
        if (startPosition.equals(endPosition)) {
            board.setSegment(startPosition, Board.SHIP);
        } else if (startPosition.getX() != endPosition.getX()) {
            int changePos = startPosition.getX() - endPosition.getX();
            if (changePos > 0) {
                for (int i = 0; i <= changePos; i++) {
                    board.setSegment(endPosition.getX() + i, startPosition.getY(), Board.SHIP);
                }
            } else {
                for (int i = 0; i >= changePos; i--) {
                    board.setSegment(endPosition.getX() + i, startPosition.getY(), Board.SHIP);
                }
            }
        } else if (startPosition.getY() != endPosition.getY()) {
            int changePos = startPosition.getY() - endPosition.getY();
            if (changePos > 0) {
                for (int i = 0; i <= changePos; i++) {
                    board.setSegment(startPosition.getX(), endPosition.getY() + i, Board.SHIP);
                }
            } else {
                for (int i = 0; i >= changePos; i--) {
                    board.setSegment(startPosition.getX(), endPosition.getY() + i, Board.SHIP);
                }
            }
        }
        //Pop placed ship from the proper list - Consider extracting method
        if (board.equals(board1)) {
            shipsToPlace1.remove(0);
        } else {
            shipsToPlace2.remove(0);
        }
        //Check if we are done placing ships
        checkIfSetupDone();
        return true;
    }

    public boolean canBePlaced(Position firstPos, Position lastPos, Board board) {
        //Check if within borders
        if (!isWithinBorders(firstPos, board)) return false;
        if (!isWithinBorders(lastPos, board)) return false;
        int nextRealPlacement = getNextPlacement(getPlayerFromBoard(board)) - 1;
        if (firstPos.getDistance(lastPos) > nextRealPlacement || firstPos.getDistance(lastPos) < nextRealPlacement) {
            return false;
        }
        //Check that pos are on a line
        if (!(firstPos.getX() == lastPos.getX() || firstPos.getY() == lastPos.getY())) {
            return false;
        }
        //Check if single block ship, and ok.
        if (firstPos.equals(lastPos) && board.getSegment(firstPos) == Board.NOTHING) {
            if (getNextPlacement(getPlayerFromBoard(board)) > 1) {
                return false;
            }
            return true;
        }
        //Check each position the ship is going to be on.
        else if (firstPos.getX() < lastPos.getX()) {
            int xLength = lastPos.getX() - firstPos.getX();
            for (int i = 0; i < xLength; i++) {
                if (board.getSegment(firstPos.getX() + i, firstPos.getY()) != Board.NOTHING) {
                    return false;
                }
            }
            return true;
        } else if (firstPos.getX() > lastPos.getX()) {
            int xLength = firstPos.getX() - lastPos.getX();
            for (int i = 0; i < xLength; i++) {
                if (board.getSegment(lastPos.getX() + i, firstPos.getY()) != Board.NOTHING) {
                    return false;
                }
            }
            return true;
        } else if (firstPos.getY() < lastPos.getY()) {
            int yLength = lastPos.getY() - firstPos.getY();
            for (int i = 0; i < yLength; i++) {
                if (board.getSegment(firstPos.getX(), firstPos.getY() + i) != Board.NOTHING) {
                    return false;
                }
            }
            return true;
        } else if (firstPos.getY() > lastPos.getY()) {
            int yLength = firstPos.getY() - lastPos.getY();
            for (int i = 0; i < yLength; i++) {
                if (board.getSegment(firstPos.getX(), lastPos.getY() + i) != Board.NOTHING) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void checkIfSetupDone() {
        if (shipsToPlace1.size() == 0 && shipsToPlace2.size() == 0) {
            gameState = GameState.PLAY;
            switchActivePlayer();
        }
    }

    private boolean isWithinBorders(Position position, Board board) {
        if (position.getX() < 0 || position.getX() > board.getBoardSizeX() - 1) {
            return false;
        }
        if (position.getY() < 0 || position.getY() > board.getBoardSizeY() - 1) {
            return false;
        }
        return true;
    }

    public int getNextPlacement(Player player) {
        if (player.equals(player1) && shipsToPlace1.size() != 0) {
            return shipsToPlace1.get(0);
        } else if (player.equals(player2) && shipsToPlace2.size() != 0) {
            return shipsToPlace2.get(0);
        } else return 0;
    }

    private Player getPlayerFromBoard(Board board) {
        if (board.equals(board1)) {
            return player1;
        }
        return player2;
    }

    public ArrayList<Position> getPossibleEndPositions(Board board, Position pos, int length) {
        return getPossibleEndPositions(board, pos.getX(), pos.getY(), length);
    }

    public ArrayList<Position> getPossibleEndPositions(Board board, int x, int y, int length) {
        length = length - 1;
        ArrayList<Position> possiblePositions = new ArrayList<>();

        if (canBePlaced(new Position(x, y), new Position(x + length, y), board)) {
            possiblePositions.add(new Position(x + length, y));
        }
        if (canBePlaced(new Position(x, y), new Position(x - length, y), board)) {
            possiblePositions.add(new Position(x - length, y));
        }
        if (canBePlaced(new Position(x, y), new Position(x, y + length), board)) {
            possiblePositions.add(new Position(x, y + length));
        }
        if (canBePlaced(new Position(x, y), new Position(x, y - length), board)) {
            possiblePositions.add(new Position(x, y - length));
        }
        return possiblePositions;
    }

    public ArrayList<Position> getPossibleEndPositions(Player player, Position pos, int length) {
        Board board = getPlayerBoard(player);
        return getPossibleEndPositions(board, pos.getX(), pos.getY(), length);
    }

    public int[] getPlayerBoardSize(Player player) {
        return getPlayerBoard(player).getBoardSize();
    }
}
