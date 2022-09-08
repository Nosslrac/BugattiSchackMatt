package Game;

public class Position {
    char castlingRights;
    long[] posProps, boardMasks;
    int enPassantPawn, numCheckers;

    public Position(long[] posProps, long[] boardMaps, int enPassantPawn, int numCheckers, char castlingRights) {
        this.castlingRights = castlingRights;
        this.enPassantPawn = enPassantPawn;
        this.numCheckers = numCheckers;
        this.posProps = new long[5];
        this.boardMasks = new long[3];
        System.arraycopy(posProps, 0, this.posProps, 0, 5);
        System.arraycopy(boardMaps, 0, this.boardMasks, 0, 3);
    }
}
