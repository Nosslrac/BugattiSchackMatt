package Game;

public class Position {
    char castlingRights;
    long[] posProps;
    int enPassantPawn, numCheckers;

    public Position(char castlingRights, long[] posProps, int enPassantPawn, int numCheckers) {
        this.castlingRights = castlingRights;
        this.enPassantPawn = enPassantPawn;
        this.numCheckers = numCheckers;
        this.posProps = new long[5];
        System.arraycopy(posProps, 0, this.posProps, 0, 5);
    }
}
