package Game;

public class Position {
    char castlingRights;
    long blackAttack, whiteAttack, checkerMask,pinnedMask, blockerMask;
    int enPassantPawn, numCheckers;

    public Position(char castlingRights, long blackAttack, long whiteAttack,
                    long checkerMask, long blockerMask, long pinnedMask, int enPassantPawn, int numCheckers) {
        this.castlingRights = castlingRights;
        this.blackAttack = blackAttack;
        this.whiteAttack = whiteAttack;
        this.checkerMask = checkerMask;
        this.pinnedMask = pinnedMask;
        this.blockerMask = blockerMask;
        this.enPassantPawn = enPassantPawn;
        this.numCheckers = numCheckers;
    }
}
