package Game;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Board {
    private final String startPos = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq";
    private final String test1 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -";
    private final String illegalEP = "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10";
    private final String castling = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -";
    private final String enPassantTest = "8/3p4/8/K3P2r/8/8/7k/8 b -";
    public long boardMask;
    public long whiteMask;
    public long blackMask;
    private long whiteAttack;
    private long blackAttack;
    private char castlingRights;
    private long checkerMask;
    private long blockerMask;
    private int enPassantPawn;
    private long pinnedMask;
    private int numCheckers;
    private int currPiece;
    private final long[] positionProps;
    private final long[] bitMaps;
    private int moveSound;

    private boolean whiteToMove = true;
    //currentPosition.enPassant: bits 0-7 pseudoPawn, bits 8-15 realPawn
    private int depth;
    private final Stack<Position> prevPositions;
    private final Stack<Character> prevMoves;
    private final Stack<Integer> captured;
    private int promotion = 3; //Default queen

    public Board(){
        bitMaps = new long[12];
        positionProps = new long[1];
        prevPositions = new Stack<>();
        prevMoves = new Stack<>();
        captured = new Stack<>();
        castlingRights = 0;
        depth = 0; //Initial depth
        enPassantPawn = 0xFF; //Pawns can't be on square 255
        castlingRights = 0;
        fenToArray(test1);
        whiteMask = bitMaps[0] | bitMaps[1] | bitMaps[2] | bitMaps[3] | bitMaps[4] | bitMaps[5];
        blackMask = bitMaps[6] | bitMaps[7] | bitMaps[8] | bitMaps[9] | bitMaps[10] | bitMaps[11];
        boardMask = whiteMask | blackMask;
        updateAttacks();
    }



    public int getPiece(int pos){
        long check = 1L << pos;
        currPiece = -1;
        for(int i = 0; i < 12; i++){
            if((check & bitMaps[i]) == check)
                currPiece = i;
        }
        return currPiece;
    }

    public List<Character> getLegalMoves(int origin){
        if(currPiece < 0)
            return null;
        List<Character> res = getAllMoves(numCheckers > 0);
        List<Character> pMoves = new ArrayList<>();
        for(char move : res){
            if(Move.getFrom(move) == origin)
                pMoves.add(move);
        }
        return pMoves;
    }

    private void findDissim(){
        String[] rokkChess = new String[]{
        "e1g1: 40",
        "e1c1: 40",
        "e1d1: 40",
        "e1f1: 40",
        "d5d6: 38",
        "a2a3: 41",
        "b2b3: 39",
        "f2f3: 40",
        "g2g3: 39",
        "a2a4: 42",
        "f2f4: 40",
        "g2g4: 40",
        "d5e6: 43",
        "g2h3: 39",
        "e5d7: 42",
        "e5f7: 41",
        "e5c6: 38",
        "e5g6: 40",
        "e5c4: 39",
        "e5g4: 42",
        "e5d3: 40",
        "e5f3: 41",
        "c3b5: 36",
        "c3a4: 39",
        "c3b1: 39",
        "c3d1: 39",
        "a1b1: 40",
        "a1c1: 40",
        "a1d1: 40",
        "h1f1: 40",
        "h1g1: 40",
        "d2h6: 40",
        "d2g5: 40",
        "d2f4: 40",
        "d2e3: 40",
        "d2c1: 40",
        "e2a6: 33",
        "e2b5: 36",
        "e2h5: 42",
        "e2c4: 38",
        "e2g4: 42",
        "e2d3: 39",
        "e2f3: 41",
        "e2d1: 41",
        "e2f1: 41"};
        String[] stockfish = new String[]{"a2a3: 41",
        "b2b3: 39",
        "f2f3: 40",
        "g2g3: 39",
        "d5d6: 38",
        "a2a4: 41",
        "f2f4: 40",
        "g2g4: 40",
        "g2h3: 39",
        "d5e6: 43",
        "c3b1: 39",
        "c3d1: 39",
        "c3a4: 39",
        "c3b5: 36",
        "e5d3: 40",
        "e5f3: 41",
        "e5c4: 39",
        "e5g4: 42",
        "e5c6: 38",
        "e5g6: 40",
        "e5d7: 42",
        "e5f7: 41",
        "d2c1: 40",
        "d2e3: 40",
        "d2f4: 40",
        "d2g5: 40",
        "d2h6: 40",
        "e2d1: 41",
        "e2f1: 41",
        "e2d3: 39",
        "e2f3: 41",
        "e2c4: 38",
        "e2g4: 42",
        "e2b5: 36",
        "e2h5: 42",
        "e2a6: 33",
        "a1b1: 40",
        "a1c1: 40",
        "a1d1: 40",
        "h1f1: 40",
        "h1g1: 40",
        "e1d1: 40",
        "e1f1: 40",
        "e1g1: 40",
        "e1c1: 40"};
        for(String s : stockfish){
            boolean found = false;
            for(String x : rokkChess){
                if (s.equals(x)) {
                    found = true;
                    break;
                }
            }
            if(!found)
                System.out.println(s);
        }
    }

    public boolean placePiece(int origin, int dest){
        if(origin == dest)
            return false;
        if(currPiece < 6 != whiteToMove)
            return false;
        //Moves move bitboard for relevant piece
        List <Character> moves = getLegalMoves(origin);
        moveSound = 0;
        for(char move : moves){
            int to = Move.getTo(move);
            if (to == dest){
                if(Move.getFlags(move) > 7)
                    makeMove((char) (move | promotion << 12));
                else
                    makeMove(move);
                return true;
            }
        }
        return false;
    }

    public void setPromotion(int promo){
        promotion = promo;
    }

    private void enPassant(int pushedPawn){
        //White double push
        if(whiteToMove){
            //If no black pawns present on 5th rank then no en passant possible
            if((bitMaps[7] & 0xFF00000000L) == 0)
                return;
            long sliders = bitMaps[3] | bitMaps[5];
            //If any of the slider occupy the fifth rank then we need to check for pin
            if((sliders & 0xFF00000000L) > 0) {
                long slideAttack = Attacks.straightAttack(boardMask, sliders, true);
                long kingAttack = Attacks.straightAttack(boardMask, bitMaps[6], true);
                long pawnPos = 1L << pushedPawn;
                //If either the king or sliders see the en passant pawn then a possible pin is there
                if((slideAttack & pawnPos) != 0){
                    //If the king sees the piece next to the pawn then there is no en passant available
                    if((kingAttack & (pawnPos >> 1)) != 0 || (kingAttack & (pawnPos << 1)) != 0){
                        return;
                    }
                }
                else if((kingAttack & pawnPos) != 0){
                    //If the slider sees pawn next to pushed pawn then no en passant available
                    if((slideAttack & (pawnPos >> 1)) != 0 || (slideAttack & (pawnPos << 1)) != 0){
                        return;
                    }
                }
            }
        }
        else{
            //If no white pawns present on 4th rank then no en passant possible
            if((bitMaps[1] & 0xFF000000L) == 0)
                return;
            long sliders = bitMaps[9] | bitMaps[11];
            //If any of the slider occupy the forth rank then we need to check for pin
            if((sliders & 0xFF000000L) > 0) {
                long slideAttack = Attacks.straightAttack(boardMask, sliders, true);
                long kingAttack = Attacks.straightAttack(boardMask, bitMaps[0], true);
                long pawnPos = 1L << pushedPawn;
                //If either the king or sliders see the en passant pawn then a possible pin is there
                if((slideAttack & pawnPos) != 0){
                    //If the king sees the piece next to the pawn then there is no en passant available
                    if((kingAttack & (pawnPos >> 1)) != 0 || (kingAttack & (pawnPos << 1)) != 0){
                        return;
                    }
                }
                else if((kingAttack & pawnPos) != 0){
                    //If the slider sees pawn next to pushed pawn then no en passant available
                    if((slideAttack & (pawnPos >> 1)) != 0 || (slideAttack & (pawnPos << 1)) != 0){
                        return;
                    }
                }
            }
        }
        int pseudo = whiteToMove ? pushedPawn + 8 : pushedPawn - 8;
        //Stores both real pawn and pseudoPawn
        enPassantPawn =  (pushedPawn << 8) | pseudo;
    }

    private void castle(int dest){
        switch (dest){
            case 2 -> {
                long set = 1L << 3;
                long reset = ~(1L);
                blackMask &= reset;
                bitMaps[9] &= reset;
                blackMask |= set;
                bitMaps[9] |= set;
            }
            case 6 -> {
                long set = 1L << 5;
                long reset = ~(1L << 7);
                blackMask &= reset;
                bitMaps[9] &= reset;
                blackMask |= set;
                bitMaps[9] |= set;
            }
            case 58 -> {
                long set = 1L << 59;
                long reset = ~(1L << 56);
                whiteMask &= reset;
                bitMaps[3] &= reset;
                whiteMask |= set;
                bitMaps[3] |= set;
            }
            case 62 -> {
                long set = 1L << 61;
                long reset = ~(1L << 63);
                whiteMask &= reset;
                bitMaps[3] &= reset;
                whiteMask |= set;
                bitMaps[3] |= set;
            }
        }
    }

    private void unCastle(int kingPos){
        switch (kingPos){
            case 2 -> {
                long reset = ~(1L << 3);
                long set = 1L;
                blackMask &= reset;
                bitMaps[9] &= reset;
                blackMask |= set;
                bitMaps[9] |= set;
            }
            case 6 -> {
                long reset = ~(1L << 5);
                long set = 1L << 7;
                blackMask &= reset;
                bitMaps[9] &= reset;
                blackMask |= set;
                bitMaps[9] |= set;
            }
            case 58 -> {
                long reset = ~(1L << 59);
                long set = 1L << 56;
                whiteMask &= reset;
                bitMaps[3] &= reset;
                whiteMask |= set;
                bitMaps[3] |= set;
            }
            case 62 -> {
                long reset = ~(1L << 61);
                long set = 1L << 63;
                whiteMask &= reset;
                bitMaps[3] &= reset;
                whiteMask |= set;
                bitMaps[3] |= set;
            }
        }
    }

    private void rookMoveOrCap(int from){
        //Reset correct castling flag
        switch (from){
            case 0 -> castlingRights &= 0b0111;
            case 7 -> castlingRights &= 0b1011;
            case 56 -> castlingRights &= 0b1101;
            case 63 -> castlingRights &= 0b1110;
        }
    }

    private void makeMove(char move){
        prevPositions.push(new Position(castlingRights, blackAttack, whiteAttack, checkerMask,
                blockerMask, pinnedMask, enPassantPawn, numCheckers));
        prevMoves.push(move);
        int from = Move.getFrom(move);
        int to = Move.getTo(move);
        long fromSq = 1L << from;
        long toSq = 1L << to;
        int flags = Move.getFlags(move);
        int start = whiteToMove ? 0 : 6;
        int moverID = findMask(fromSq, start);
        bitMaps[moverID] &= ~fromSq; // Clear mover from square
        if(whiteToMove){
            //Capture
            if((flags & 0x4) != 0){
                if((flags & 0x9) == 0x1){//Ep capture
                    long realPawn = ~(1L << (enPassantPawn >> 8));
                    bitMaps[7] &= realPawn;
                    blackMask &= realPawn;
                }
                else{//Regular capture
                    int capID = findMask(toSq, 6);
                    if(capID == 9)
                        rookMoveOrCap(to);
                    bitMaps[capID] &= ~toSq;
                    blackMask &= ~toSq;
                    captured.push(capID);
                }
                moveSound = 2;
            }
            whiteMask &= ~fromSq;
            whiteMask |= toSq;
        }
        else{
            //Capture
            if((flags & 0x4) != 0){
                if((flags & 0x9) == 1){//Ep capture
                    long realPawn = ~(1L << (enPassantPawn >> 8));
                    bitMaps[1] &= realPawn;
                    whiteMask &= realPawn;
                }
                else{//Regular capture
                    int capID = findMask(toSq, 0);
                    if(capID == 3)
                        rookMoveOrCap(to);
                    bitMaps[capID] &= ~toSq;
                    whiteMask &= ~toSq;
                    captured.push(capID);
                }
                moveSound = 2;
            }
            blackMask &= ~fromSq;
            blackMask |= toSq;
        }

        //Pawn double push
        if(flags == 1){
            enPassant(to);
        }
        else{
           enPassantPawn = 0xFF;
        }
        //Castling moves
        if(moverID % 6 == 0){
            if(moverID == 0)
                castlingRights &= 0b1100;
            else
                castlingRights &= 0b0011;
            if(flags == 2 || flags == 3) {
                castle(to);
                moveSound = 3;
            }
        }
        else if(moverID % 6 == 3){//Rook moves update castling rights
            rookMoveOrCap(from);
        }
        else if((flags & 0x8) != 0){
            //Some kind of promotion
            moverID = start + 2 + (flags & 0x3); //Get piece id
        }
        //Update bitmap
        bitMaps[moverID] |= toSq;
        boardMask = whiteMask | blackMask;
        whiteToMove = !whiteToMove;
        updateAttacks();
        depth++;
    }

    public void undoMove(){
        if(prevMoves.size() != 0)
            undoMove(prevMoves.pop());
    }

    private void undoMove(char move){
        Position prev = prevPositions.pop();
        whiteAttack = prev.whiteAttack;
        blackAttack = prev.blackAttack;
        pinnedMask = prev.pinnedMask;
        castlingRights = prev.castlingRights;
        enPassantPawn = prev.enPassantPawn;
        numCheckers = prev.numCheckers;
        blockerMask = prev.blockerMask;
        checkerMask = prev.checkerMask;

        int to = Move.getTo(move);
        int from = Move.getFrom(move);
        int flags = Move.getFlags(move);
        long toSq = 1L << to;
        long fromSq = 1L << from;
        int maskID;

        if(whiteToMove){ //Black made previous move
             maskID = findMask(toSq, 6);
             bitMaps[maskID] &= ~toSq; //Clear mover from current square
             if((flags & 0x4) != 0){//Capture
                 if((flags & 0x9) == 0x1){//Ep capture
                    int realPawn = enPassantPawn >> 8;
                    bitMaps[1] |= 1L << realPawn;
                    whiteMask |= 1L << realPawn;
                 }
                 else{
                     int capID = captured.pop();
                     bitMaps[capID] |= toSq;
                     whiteMask |= toSq;
                 }
             }
             if((flags & 0x8) != 0){
                 maskID = 7;
             }
            blackMask |= fromSq;
            blackMask &= ~toSq;
        }
        else{ //White made previous move
            maskID = findMask(toSq, 0);
            bitMaps[maskID] &= ~toSq; //Clear mover from current square
            if((flags & 0x4) != 0){//Capture
                if((flags & 0x9) == 0x1){//Ep capture
                    int realPawn = enPassantPawn >> 8;
                    bitMaps[7] |= 1L << realPawn;
                    blackMask |= 1L << realPawn;
                }
                else{
                    int capID = captured.pop();
                    bitMaps[capID] |= toSq;
                    blackMask |= toSq;
                }
            }
            if((flags & 0x8) != 0){ //promotion
                maskID = 1; //Change maskID so we will put a pawn instead of promoted piece
            }
            whiteMask |= fromSq;
            whiteMask &= ~toSq;
        }
        //Add mover to origin square
        if(flags == 2 || flags == 3){
            unCastle(to);
        }
        bitMaps[maskID] |= fromSq;
        boardMask = whiteMask | blackMask;
        whiteToMove = !whiteToMove;
        depth--;
    }


    private void updateAttacks(){
        blackAttack = 0L;
        whiteAttack = 0L;
        blockerMask = 0L;
        numCheckers = 0;
        checkerMask = 0;
        long boardMKing = boardMask & ~bitMaps[6]; //White attacks go through black king
        whiteAttack |= Attacks.kingAttack(Long.numberOfTrailingZeros(bitMaps[0]));
        whiteAttack |= Attacks.pawnAttack(bitMaps[1], true);
        whiteAttack |= Attacks.knightAttacks(bitMaps[2]);
        whiteAttack |= Attacks.rookAttacks(boardMKing, bitMaps[3]);
        whiteAttack |= Attacks.bishopAttacks(boardMKing, bitMaps[4]);
        whiteAttack |= Attacks.rookAttacks(boardMKing, bitMaps[5]) | Attacks.bishopAttacks(boardMKing, bitMaps[5]);

        boardMKing = boardMask & ~bitMaps[0]; //Black attacks go through white king
        blackAttack |= Attacks.kingAttack(Long.numberOfTrailingZeros(bitMaps[6]));
        blackAttack |= Attacks.pawnAttack(bitMaps[7], false);
        blackAttack |= Attacks.knightAttacks(bitMaps[8]);
        blackAttack |= Attacks.rookAttacks(boardMKing, bitMaps[9]);
        blackAttack |= Attacks.bishopAttacks(boardMKing, bitMaps[10]);
        blackAttack |= Attacks.rookAttacks(boardMKing, bitMaps[11]) | Attacks.bishopAttacks(boardMKing, bitMaps[11]);
        findPinsAlt();
        if((whiteAttack & bitMaps[6]) == bitMaps[6]) {
            findChecker(true);
            moveSound = 1;
        }
        if((blackAttack & bitMaps[0]) == bitMaps[0]) {
            moveSound = 1;
            findChecker(false);
        }
        //if(isCheckMate())
            //moveSound = 4;

    }

    //Description: When king is in check this routine finds the checker and also calculates
    //the currentPosition.blockerMask if there is one. It also checks for multiple checker, which in this case
    //only king moves would be legal
    private void findChecker(boolean white){
        //Offset for black pieces
        int plus = white ? 0 : 6;
        long king = (white ? bitMaps[6] : bitMaps[0]);
        int kingPos = Long.numberOfTrailingZeros(king);
        //If following bit boards aren't 0 then we found a checker
        long knightCheck =   Attacks.knightAttacks(king) & bitMaps[2 + plus];
        long pawnCheck =     Attacks.pawnAttack(king, !white) & bitMaps[1 + plus];
        long diagonalCheck = Attacks.bishopAttacks(boardMask, king) & (bitMaps[4 + plus] | bitMaps[5 + plus]);
        long straightCheck = Attacks.rookAttacks(boardMask, king) & (bitMaps[3 + plus] | bitMaps[5 + plus]);
        if(knightCheck != 0) {
            //The logical 'and' will contain only the checking piece
            checkerMask = knightCheck;
            numCheckers++;
        }
        else if(pawnCheck != 0){
            //The logical 'and' will contain only the checking piece
            checkerMask = pawnCheck;
            numCheckers++;
        }
        if(diagonalCheck != 0) {
            //Bishop or queen is checking
            checkerMask |= diagonalCheck;
            //Are they on the same anti diagonal
            int checkerPos = Long.numberOfTrailingZeros(diagonalCheck);
            //Find block mask
            blockerMask = findBlock(kingPos, checkerPos, true);
            numCheckers++;
        }
        if(straightCheck != 0){
            //Rook or queen check
            checkerMask |= straightCheck;
            int checkerPos = Long.numberOfTrailingZeros(straightCheck);
            //Find block mask
            blockerMask = findBlock(kingPos, checkerPos, false);
            numCheckers++;
        }
    }


    //Probably faster pin detection, also only considers pins for moving side
    private void findPinsAlt(){
        pinnedMask = 0;
        int start = whiteToMove ? 0 : 6;
        int kingPos = Long.numberOfTrailingZeros(bitMaps[start]);
        long straight = whiteToMove ? bitMaps[9] | bitMaps[11] : bitMaps[3] | bitMaps[5];
        long diagonal = whiteToMove ? bitMaps[10] | bitMaps[11] : bitMaps[4] | bitMaps[5];
        long kingFile = Attacks.getFiles()[kingPos % 8];
        long kingRank = Attacks.getRanks()[kingPos / 8];
        long kingMD = Attacks.getMainDiagonals()[7 + kingPos % 8 - kingPos / 8];
        long kingAD = Attacks.getAntiDiagonals()[kingPos % 8 + kingPos / 8];
        //Possible pinning pieces are aligned with the king
        long AD = kingAD & diagonal;
        long RANK = kingRank & straight;
        diagonal &= kingMD; //Reuse variable for main diagonal pins
        straight &= kingFile; //Reuse variable for file pins
        //Possible pinning pieces, INVESTIGATE
        long nSameMask = whiteToMove ? ~blackMask : ~whiteMask;
        if(diagonal != 0){
            pinnedMask |= (Attacks.diagonalAttack(boardMask, diagonal, true) & nSameMask)
                    & Attacks.diagonalAttack(boardMask, 1L << kingPos, true);
        }
        if(straight != 0){
            pinnedMask |= (Attacks.straightAttack(boardMask, straight, false) & nSameMask)
                    & Attacks.straightAttack(boardMask, 1L << kingPos, false);
        }
        if(AD != 0){
            pinnedMask |= (Attacks.diagonalAttack(boardMask, AD, false) & nSameMask)
                    & Attacks.diagonalAttack(boardMask, 1L << kingPos, false);
        }
        if(RANK != 0){
            pinnedMask |= (Attacks.straightAttack(boardMask, RANK, true) & nSameMask)
                    & Attacks.straightAttack(boardMask, 1L << kingPos, true);
        }
    }

    //Description: Find all pinned pieces and set "pin rays" for each piece i.e.
    //the direction it can move in
    private void findPins(){
        pinnedMask = 0L;
        //Find all of white's pinned pieces
        long bQ = bitMaps[11];
        long wQ = bitMaps[5];
        long wK = bitMaps[0];
        long bK = bitMaps[6];
        long tmpRes;

        //Start with diagonals for both sides
        //Starting with white's pinned pieces
        //If king sees a piece on its main diagonal that a bishop or queen also sees then that piece is pinned
        long bMainDiagonal =     Attacks.diagonalAttack(boardMask, bitMaps[10] | bQ, true) & ~blackMask;
        long wMainDiagonalKing = Attacks.diagonalAttack(boardMask, wK, true);
        //If king sees a piece on its anti diagonal that a bishop or queen also sees then that piece is pinned
        long bAntiDiagonal =     Attacks.diagonalAttack(boardMask, bitMaps[10] | bQ, false) & ~blackMask;
        long wAntiDiagonalKing = Attacks.diagonalAttack(boardMask, wK, false);
        tmpRes = (wMainDiagonalKing & bMainDiagonal) | (wAntiDiagonalKing & bAntiDiagonal);

        //Same method for black
        //If king sees a piece on its main diagonal that a bishop or queen also sees then that piece is pinned
        long wMainDiagonal =     Attacks.diagonalAttack(boardMask, bitMaps[4] | wQ, true) & ~whiteMask;
        long bMainDiagonalKing = Attacks.diagonalAttack(boardMask, bK, true);
        //If king sees a piece on its anti diagonal that a bishop or queen also sees then that piece is pinned
        long wAntiDiagonal =     Attacks.diagonalAttack(boardMask, bitMaps[4] | wQ, false) & ~whiteMask;
        long bAntiDiagonalKing = Attacks.diagonalAttack(boardMask, bK, false);

        tmpRes |=  (wMainDiagonal & bMainDiagonalKing) | (wAntiDiagonal & bAntiDiagonalKing);
        //Intersection with board mask will be all pinned pieces
        pinnedMask = tmpRes & boardMask;

        //Ranks and files for both sides
        //Starting white
        long bRank =     Attacks.straightAttack(boardMask, bitMaps[9] | bQ, true) & ~blackMask;
        long wRankKing = Attacks.straightAttack(boardMask, wK, true);

        long bFile =     Attacks.straightAttack(boardMask, bitMaps[9] | bQ, false) & ~blackMask;
        long wFileKing = Attacks.straightAttack(boardMask, wK, false);
        tmpRes = (bRank & wRankKing) | (bFile & wFileKing);

        long wRank =     Attacks.straightAttack(boardMask, bitMaps[3] | wQ, true) & ~whiteMask;
        long bRankKing = Attacks.straightAttack(boardMask, bK, true);

        long wFile =     Attacks.straightAttack(boardMask, bitMaps[3] | wQ, false) & ~whiteMask;
        long bFileKing = Attacks.straightAttack(boardMask, bK, false);
        tmpRes |= (wRank & bRankKing) | (wFile & bFileKing);
        //Intersection with boardMask will be all pinned pieces
        pinnedMask |= tmpRes & boardMask;
    }


    //Find the ray on which the pinned piece is able to move
    private long pinnedRay(int kingPos, int pinnedPos){
        //On the on the same rank
        if(kingPos / 8 == pinnedPos / 8)
            return MoveGenerator.getRanks()[kingPos / 8];
        //On the same file
        if(kingPos % 8 == pinnedPos % 8)
            return MoveGenerator.getFiles()[kingPos % 8];
        int id = kingPos / 8 + kingPos % 8;
        //On the same anti-diagonal
        if(id == pinnedPos / 8 + pinnedPos % 8)
            return MoveGenerator.getAntiDiagonals()[id];
        //Otherwise on the same main-diagonal
        return MoveGenerator.getMainDiagonals()[7 - kingPos / 8 + kingPos % 8];
    }


    private boolean isCheckMate(){
        return getAllMoves(numCheckers > 0).size() == 0;
    }

    private long findBlock(int king, int checker, boolean diagonal){
        long block = 0L;
        int dir;
        if(diagonal){
            if(king / 8 + king % 8 == checker / 8 + checker % 8)
                dir = king < checker ? 7 : -7;
            else
                dir = king < checker ? 9 : -9;
        }
        else{
            if(king / 8 == checker / 8)
                dir = king < checker ? 1 : -1;
            else
                dir = king < checker ? 8 : -8;
        }
        king += dir;
        while(king != checker){
            block |= 1L << king;
            king += dir;
        }
        return block;
    }

    private int findMask(long sq, int start){
        for(int i = start; i < start + 6; i++){
            if((sq & bitMaps[i]) == sq)
                return i;
        }
        System.out.println("NOT SYNCED\n");
        return -1;
    }

    private List<Character> getAllMovesInCheck(){
        long oppMask = whiteToMove ? blackMask : whiteMask;
        int start = whiteToMove ? 0 : 6;
        long attack = whiteToMove ? blackAttack : whiteAttack;

        List<Character> moves = new ArrayList<>(MoveGenerator.kingMoves(boardMask, attack,
                oppMask, (char)0, Long.numberOfTrailingZeros(bitMaps[start])));
        if(numCheckers > 1) //Return early since only king moves are available
            return moves;
        MoveGenerator.generatePawnMoveInCheck(moves, boardMask,
                oppMask, enPassantPawn, whiteToMove, bitMaps[start + 1], checkerMask | blockerMask);
        moves.addAll(MoveGenerator.generateInCheckMoves(boardMask, oppMask, bitMaps[2 + start], 2, checkerMask | blockerMask));
        moves.addAll(MoveGenerator.generateInCheckMoves(boardMask, oppMask, bitMaps[3 + start], 3, checkerMask | blockerMask));
        moves.addAll(MoveGenerator.generateInCheckMoves(boardMask, oppMask, bitMaps[4 + start], 4, checkerMask | blockerMask));
        moves.addAll(MoveGenerator.generateInCheckMoves(boardMask, oppMask, bitMaps[5 + start], 5, checkerMask | blockerMask));
        //Remove all pinned pieces illegal moves
        long pinnedPieces = (boardMask & ~oppMask) & pinnedMask;
        if(pinnedPieces != 0) {
            List<Character> legal = new ArrayList<>();
            int king = whiteToMove ? Long.numberOfTrailingZeros(bitMaps[0]) : Long.numberOfTrailingZeros(bitMaps[6]);
            for (char move : moves) {
                int from = Move.getFrom(move);
                if (((1L << from) & pinnedPieces) != 0) {
                    int to = Move.getTo(move);
                    long ray = pinnedRay(king, from);
                    if (((1L << to) & ray) != 0)
                        legal.add(move);
                } else
                    legal.add(move);
            }
            return legal;
        }
        return moves;
    }


    private List<Character> getAllMoves(boolean inCheck){
        if(inCheck)
            return getAllMovesInCheck();
        long oppMask = whiteToMove ? blackMask : whiteMask;
        int start = whiteToMove ? 0 : 6;
        char castling = whiteToMove ? castlingRights : (char) (castlingRights >> 2);
        //Legal king moves
        List<Character> moves = new ArrayList<>(MoveGenerator.kingMoves(boardMask, (whiteToMove ? blackAttack : whiteAttack), oppMask,
                castling, Long.numberOfTrailingZeros(bitMaps[start])));
        //Pseudo legal pawn moves
        moves.addAll(MoveGenerator.generatePawnMoves(boardMask, oppMask, enPassantPawn & 0xFF,
                whiteToMove, bitMaps[start + 1]));
        //Pseudo legal knight moves
        moves.addAll(MoveGenerator.generateKnightMoves(bitMaps[2 + start], boardMask, oppMask, blockerMask | checkerMask));
        //Pseudo legal rook moves
        moves.addAll(MoveGenerator.generateSlidingMoves(boardMask, oppMask,
                bitMaps[start + 3], 3));
        //Pseudo legal bishop moves
        moves.addAll(MoveGenerator.generateSlidingMoves(boardMask, oppMask,
                bitMaps[start + 4], 4));
        //Pseudo legal queen moves
        moves.addAll(MoveGenerator.generateSlidingMoves(boardMask, oppMask,
                bitMaps[start + 5], 5));

        //Remove all pinned pieces illegal moves
        if(pinnedMask != 0) {
            List<Character> legal = new ArrayList<>();
            int king = whiteToMove ? Long.numberOfTrailingZeros(bitMaps[0]) : Long.numberOfTrailingZeros(bitMaps[6]);
            for (char move : moves) {
                int from = Move.getFrom(move);
                if (((1L << from) & pinnedMask) != 0) {
                    int to = Move.getTo(move);
                    long ray = pinnedRay(king, from);
                    if (((1L << to) & ray) != 0)
                        legal.add(move);
                } else
                    legal.add(move);
            }
            return legal;
        }
        return moves;
    }



    public long search(int depth){
        long numPositions = 0;
        List<Character> moves = getAllMoves(numCheckers > 0);
        if(depth == 0)
            return 1;
        if(depth == 1)
            return moves.size();
        for(char move : moves){
            makeMove(move);
            numPositions += search(depth - 1);
            undoMove(move);
        }
        return numPositions;
    }

    public void perft(int depth){
        long start = System.currentTimeMillis();
        List<Character> moves = getAllMoves(numCheckers > 0);
        long numPositions = 0;
        for(char move : moves){
            makeMove(move);
            long part = search(depth - 1);
            numPositions += part;
            System.out.println(parseMove(move) + ": " + part);
            undoMove(move);
        }
        long end = System.currentTimeMillis();
        end = (end - start);
        System.out.println("Total number of nodes: " + numPositions + "\nTotal time elapsed: " + end / 1000.0);
    }

    public void test(){
        long start = System.currentTimeMillis();
        long numPos = 0;
        for(int i = 0; i < 9999999; i++){
            numPos += getAllMoves(false).size();
        }
        System.out.println("Counting " + numPos + " took: " + (System.currentTimeMillis() - start) / 1000.0);
    }

    public String parseMove(char move){
        int from = Move.getFrom(move);
        int to = Move.getTo(move);
        char fFile = (char) ('a' + from % 8);
        char fRank = (char) ('1' + 7 - from / 8);
        char tFile = (char) ('a' + to % 8);
        char tRank = (char) ('1' + 7 - to / 8);
        return "" + fFile + fRank + tFile + tRank;
    }

    private void fenToArray(String fen){
        char[] pPlace = fen.toCharArray();
        int j = 0;
            for(int i = 0; true; i++) {
                switch (pPlace[j]) {
                    case 'K' -> bitMaps[0] |= 1L << i;
                    case 'P' -> bitMaps[1]  |= 1L << i;
                    case 'N' -> bitMaps[2] |= 1L << i;
                    case 'B' -> bitMaps[4] |= 1L << i;
                    case 'R' -> bitMaps[3] |= 1L << i;
                    case 'Q' -> bitMaps[5] |= 1L << i;
                    case 'k' -> bitMaps[6] |= 1L << i;
                    case 'p' -> bitMaps[7] |= 1L << i;
                    case 'n' -> bitMaps[8] |= 1L << i;
                    case 'b' -> bitMaps[10] |= 1L << i;
                    case 'r' -> bitMaps[9] |= 1L << i;
                    case 'q' -> bitMaps[11] |= 1L << i;
                    case '/' -> i--;
                    case ' ' -> {
                        j++;
                        whiteToMove = pPlace[j++] == 'w';
                        while(j < pPlace.length){
                            switch (pPlace[j++]){
                                case 'K' -> castlingRights |= 0b0001; //White king side castling
                                case 'Q' -> castlingRights |= 0b0010; //White queen side castling
                                case 'k' -> castlingRights |= 0b0100; //Black king side castling
                                case 'q' -> castlingRights |= 0b1000; //Black queen side castling
                            }
                        }
                        return;
                    }
                    default -> i += pPlace[j] - 49;
                }
                j++;
            }
    }

    public long[] getBitMaps(){
        return bitMaps;
    }
    public long getWhiteAttack(){
        return pinnedMask;
    }
    public int getMoveSound(){
        return moveSound;
    }
}
