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

    private long nodes;
    //private long whiteAttack;
    private char castlingRights;
    //private long checkerMask;
    //private long blockerMask;
    private int enPassantPawn;
    //private long pinnedMask;
    private int numCheckers;
    private int currPiece;

    //{whiteAttack, blackAttack, checkerMask, blockerMask, pinnedMask}
    private static final int whiteAttack = 0, blackAttack = 1,
             checkerMask = 2, blockerMask = 3, pinnedMask = 4, boardMask = 0, whiteMask = 1, blackMask = 2;

    private long[] positionProps;
    private long[] boardMaps;
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
        boardMaps = new long[3];
        positionProps = new long[5];
        prevPositions = new Stack<>();
        prevMoves = new Stack<>();
        captured = new Stack<>();
        castlingRights = 0;
        depth = 0; //Initial depth
        enPassantPawn = 0xFF; //Pawns can't be on square 255
        castlingRights = 0;
        fenToArray(castling);
        boardMaps[whiteMask] = bitMaps[0] | bitMaps[1] | bitMaps[2] | bitMaps[3] | bitMaps[4] | bitMaps[5];
        boardMaps[blackMask] = bitMaps[6] | bitMaps[7] | bitMaps[8] | bitMaps[9] | bitMaps[10] | bitMaps[11];
        boardMaps[boardMask] = boardMaps[whiteMask] | boardMaps[blackMask];
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

    public void engineMove(){
        char move = findBestMove(4);
        if(move == 0){
            if(numCheckers > 0)
                System.out.println("Checkmate");
            else
                System.out.println("Stalemate");
            return;
        }
        makeMove(move);
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
                long slideAttack = Attacks.straightAttack(boardMaps[boardMask], sliders, true);
                long kingAttack = Attacks.straightAttack(boardMaps[boardMask], bitMaps[6], true);
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
                long slideAttack = Attacks.straightAttack(boardMaps[boardMask], sliders, true);
                long kingAttack = Attacks.straightAttack(boardMaps[boardMask], bitMaps[0], true);
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
                boardMaps[blackMask] &= reset;
                bitMaps[9] &= reset;
                boardMaps[blackMask] |= set;
                bitMaps[9] |= set;
            }
            case 6 -> {
                long set = 1L << 5;
                long reset = ~(1L << 7);
                boardMaps[blackMask] &= reset;
                bitMaps[9] &= reset;
                boardMaps[blackMask] |= set;
                bitMaps[9] |= set;
            }
            case 58 -> {
                long set = 1L << 59;
                long reset = ~(1L << 56);
                boardMaps[whiteMask] &= reset;
                bitMaps[3] &= reset;
                boardMaps[whiteMask] |= set;
                bitMaps[3] |= set;
            }
            case 62 -> {
                long set = 1L << 61;
                long reset = ~(1L << 63);
                boardMaps[whiteMask] &= reset;
                bitMaps[3] &= reset;
                boardMaps[whiteMask] |= set;
                bitMaps[3] |= set;
            }
        }
    }

    private void unCastle(int kingPos){
        switch (kingPos){
            case 2 -> {
                long reset = ~(1L << 3);
                long set = 1L;
                boardMaps[blackMask] &= reset;
                bitMaps[9] &= reset;
                boardMaps[blackMask] |= set;
                bitMaps[9] |= set;
            }
            case 6 -> {
                long reset = ~(1L << 5);
                long set = 1L << 7;
                boardMaps[blackMask] &= reset;
                bitMaps[9] &= reset;
                boardMaps[blackMask] |= set;
                bitMaps[9] |= set;
            }
            case 58 -> {
                long reset = ~(1L << 59);
                long set = 1L << 56;
                boardMaps[whiteMask] &= reset;
                bitMaps[3] &= reset;
                boardMaps[whiteMask] |= set;
                bitMaps[3] |= set;
            }
            case 62 -> {
                long reset = ~(1L << 61);
                long set = 1L << 63;
                boardMaps[whiteMask] &= reset;
                bitMaps[3] &= reset;
                boardMaps[whiteMask] |= set;
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
        prevPositions.push(new Position(positionProps, boardMaps, enPassantPawn, numCheckers, castlingRights));
        prevMoves.push(move);
        int from = Move.getFrom(move);
        int to = Move.getTo(move);
        long fromSq = 1L << from;
        long toSq = 1L << to;
        int flags = Move.getFlags(move);
        int start = whiteToMove ? 0 : 6;
        int opMask = whiteToMove ? 2 : 1;
        int sameMask = whiteToMove ? 1 : 2;
        int opStart = whiteToMove ? 6 : 0;

        int moverID = findMask(fromSq, start);
        bitMaps[moverID] &= ~fromSq; // Clear mover from square
        boardMaps[sameMask] &= ~fromSq; //Clear from team

        //Special moves and captures
        switch (flags){
            case 1 -> enPassant(to);
            case 2, 3 -> castle(to);
            case 4 -> {
                //Regular capture
                int capID = findMask(toSq, opStart);
                if(capID % 6 == 3)
                    rookMoveOrCap(to);
                bitMaps[capID] &= ~toSq;
                boardMaps[opMask] &= ~toSq;
                captured.push(capID);
            }
            case 5 -> {
                long realPawn = ~(1L << (enPassantPawn >> 8));
                bitMaps[opStart + 1] &= realPawn; //Pawns are the first mask in array
                boardMaps[opMask] &= realPawn;
            }
            case 8,9,10,11,12,13,14,15 -> {
                if(flags > 11){ //Capture as well
                    int capID = findMask(toSq, opStart);
                    if(capID % 6 == 3)
                        rookMoveOrCap(to);
                    bitMaps[capID] &= ~toSq;
                    boardMaps[opMask] &= ~toSq;
                    captured.push(capID);
                }
                //Promotes to piece specified by the move
                moverID = start + 2 + (flags & 0x3);
            }
        }

        if(flags != 1)
            enPassantPawn = 0xFF;
        //King moves change castling rights
        if(moverID % 6 == 0){
            if(moverID == 0)
                castlingRights &= 0b1100;
            else
                castlingRights &= 0b0011;
        }
        else if(moverID % 6 == 3){//Rook moves update castling rights
            rookMoveOrCap(from);
        }
        //Update bitmap
        bitMaps[moverID] |= toSq;
        boardMaps[sameMask] |= toSq;
        boardMaps[boardMask] = boardMaps[whiteMask] | boardMaps[blackMask];
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
        positionProps = prev.posProps;
        boardMaps = prev.boardMasks;
        castlingRights = prev.castlingRights;
        enPassantPawn = prev.enPassantPawn;
        numCheckers = prev.numCheckers;

        int to = Move.getTo(move);
        int from = Move.getFrom(move);
        int flags = Move.getFlags(move);
        long toSq = 1L << to;
        long fromSq = 1L << from;

        int opStart = whiteToMove ? 6 : 0;
        int movingPiece = findMask(toSq, opStart);
        //Remove from movingPiece
        bitMaps[movingPiece] &= ~toSq;
        int cap = 0;

        //En passant, the captured piece will not be placed on the to square
        if(flags == 5){
           toSq = whiteToMove ? 1L << (to - 8) : 1L << (to + 8);
           cap = whiteToMove ? 1 : 7;
        }
        else if((flags & 0x4) != 0){
            cap = captured.pop();
        }
        else if(flags == 3 || flags == 2){
            unCastle(to); //Based on where the king is now
        }
        if(flags > 8){
            movingPiece = opStart + 1; //Make the piece a pawn again
        }
        if(cap != 0){
            bitMaps[cap] |= toSq;
        }
        //Update pieceMaps
        bitMaps[movingPiece] |= fromSq;
        whiteToMove = !whiteToMove;
        depth--;
    }


    private void updateAttacks(){
        long wA = 0L, bA = 0L;
        positionProps[blockerMask] = 0L;
        positionProps[checkerMask] = 0L;
        numCheckers = 0;
        long boardMKing = boardMaps[boardMask] & ~bitMaps[6]; //White attacks go through black king
        wA |= Attacks.kingAttack(Long.numberOfTrailingZeros(bitMaps[0]));
        wA |= Attacks.pawnAttack(bitMaps[1], true);
        wA |= Attacks.knightAttacks(bitMaps[2]);
        wA |= Attacks.rookAttacks(boardMKing, bitMaps[3]);
        wA |= Attacks.bishopAttacks(boardMKing, bitMaps[4]);
        wA |= Attacks.rookAttacks(boardMKing, bitMaps[5]) | Attacks.bishopAttacks(boardMKing, bitMaps[5]);

        boardMKing = boardMaps[boardMask] & ~bitMaps[0]; //Black attacks go through white king
        bA |= Attacks.kingAttack(Long.numberOfTrailingZeros(bitMaps[6]));
        bA |= Attacks.pawnAttack(bitMaps[7], false);
        bA |= Attacks.knightAttacks(bitMaps[8]);
        bA |= Attacks.rookAttacks(boardMKing, bitMaps[9]);
        bA |= Attacks.bishopAttacks(boardMKing, bitMaps[10]);
        bA |= Attacks.rookAttacks(boardMKing, bitMaps[11]) | Attacks.bishopAttacks(boardMKing, bitMaps[11]);
        findPinsAlt();
        positionProps[whiteAttack] = wA;
        positionProps[blackAttack] = bA;
        if((wA & bitMaps[6]) == bitMaps[6]) {
            findChecker(true);
            moveSound = 1;
        }
        if((bA & bitMaps[0]) == bitMaps[0]) {
            moveSound = 1;
            findChecker(false);
        }
        //Slows down perft
        /*
        if(isCheckMate()) {
            moveSound = 4;
            System.out.println("Checkmate");
        }*/

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
        //The intersection with potential checking squares with actual opponent pieces finds the checker
        long knightCheck   = Attacks.knightAttacks(king) & bitMaps[2 + plus];
        long pawnCheck     = Attacks.pawnAttack(king, !white) & bitMaps[1 + plus];
        long diagonalCheck = Attacks.bishopAttacks(boardMaps[boardMask], king) & (bitMaps[4 + plus] | bitMaps[5 + plus]);
        long straightCheck = Attacks.rookAttacks(boardMaps[boardMask], king) & (bitMaps[3 + plus] | bitMaps[5 + plus]);


        if(knightCheck != 0) {positionProps[checkerMask] = knightCheck; numCheckers++;}
        else if(pawnCheck != 0){positionProps[checkerMask] = pawnCheck; numCheckers++;}
        if(diagonalCheck != 0) {
            //Bishop or queen is checking
            positionProps[checkerMask] |= diagonalCheck;
            //Are they on the same anti diagonal
            int checkerPos = Long.numberOfTrailingZeros(diagonalCheck);
            //Find block mask
            positionProps[blockerMask] = findBlock(kingPos, checkerPos, true);
            numCheckers++;
        }
        if(straightCheck != 0){
            //Rook or queen check
            positionProps[checkerMask] |= straightCheck;
            int checkerPos = Long.numberOfTrailingZeros(straightCheck);
            //Find block mask
            positionProps[blockerMask] = findBlock(kingPos, checkerPos, false);
            numCheckers++;
        }
    }


    //Probably faster pin detection, only considers pins for moving side
    private void findPinsAlt(){
        positionProps[pinnedMask] = 0L;
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
        long nSameMask = whiteToMove ? ~boardMaps[blackMask] : ~boardMaps[whiteMask];
        if(diagonal != 0){
            positionProps[pinnedMask] |= (Attacks.diagonalAttack(boardMaps[boardMask], diagonal, true) & nSameMask)
                    & Attacks.diagonalAttack(boardMaps[boardMask], 1L << kingPos, true);
        }
        if(straight != 0){
            positionProps[pinnedMask] |= (Attacks.straightAttack(boardMaps[boardMask], straight, false) & nSameMask)
                    & Attacks.straightAttack(boardMaps[boardMask], 1L << kingPos, false);
        }
        if(AD != 0){
            positionProps[pinnedMask] |= (Attacks.diagonalAttack(boardMaps[boardMask], AD, false) & nSameMask)
                    & Attacks.diagonalAttack(boardMaps[boardMask], 1L << kingPos, false);
        }
        if(RANK != 0){
            positionProps[pinnedMask] |= (Attacks.straightAttack(boardMaps[boardMask], RANK, true) & nSameMask)
                    & Attacks.straightAttack(boardMaps[boardMask], 1L << kingPos, true);
        }
    }

    //Description: Find all pinned pieces and set "pin rays" for each piece i.e.
    //the direction it can move in
    private void findPins(){
        positionProps[pinnedMask] = 0L;
        //Find all of white's pinned pieces
        long bQ = bitMaps[11];
        long wQ = bitMaps[5];
        long wK = bitMaps[0];
        long bK = bitMaps[6];
        long tmpRes;

        //Start with diagonals for both sides
        //Starting with white's pinned pieces
        //If king sees a piece on its main diagonal that a bishop or queen also sees then that piece is pinned
        long bMainDiagonal      = Attacks.diagonalAttack(boardMaps[boardMask], bitMaps[10] | bQ, true) & ~boardMaps[blackMask];
        long wMainDiagonalKing  = Attacks.diagonalAttack(boardMaps[boardMask], wK, true);
        //If king sees a piece on its anti diagonal that a bishop or queen also sees then that piece is pinned
        long bAntiDiagonal      = Attacks.diagonalAttack(boardMaps[boardMask], bitMaps[10] | bQ, false) & ~boardMaps[blackMask];
        long wAntiDiagonalKing  = Attacks.diagonalAttack(boardMaps[boardMask], wK, false);
        tmpRes = (wMainDiagonalKing & bMainDiagonal) | (wAntiDiagonalKing & bAntiDiagonal);

        //Same method for black
        //If king sees a piece on its main diagonal that a bishop or queen also sees then that piece is pinned
        long wMainDiagonal      = Attacks.diagonalAttack(boardMaps[boardMask], bitMaps[4] | wQ, true) & ~boardMaps[whiteMask];
        long bMainDiagonalKing  = Attacks.diagonalAttack(boardMaps[boardMask], bK, true);
        //If king sees a piece on its anti diagonal that a bishop or queen also sees then that piece is pinned
        long wAntiDiagonal      = Attacks.diagonalAttack(boardMaps[boardMask], bitMaps[4] | wQ, false) & ~boardMaps[whiteMask];
        long bAntiDiagonalKing  = Attacks.diagonalAttack(boardMaps[boardMask], bK, false);

        tmpRes |=  (wMainDiagonal & bMainDiagonalKing) | (wAntiDiagonal & bAntiDiagonalKing);
        //Intersection with board mask will be all pinned pieces
        positionProps[pinnedMask] = tmpRes & boardMaps[boardMask];

        //Ranks and files for both sides
        //Starting white
        long bRank      = Attacks.straightAttack(boardMaps[boardMask], bitMaps[9] | bQ, true) & ~boardMaps[blackMask];
        long wRankKing  = Attacks.straightAttack(boardMaps[boardMask], wK, true);

        long bFile      = Attacks.straightAttack(boardMaps[boardMask], bitMaps[9] | bQ, false) & ~boardMaps[blackMask];
        long wFileKing  = Attacks.straightAttack(boardMaps[boardMask], wK, false);
        tmpRes = (bRank & wRankKing) | (bFile & wFileKing);

        long wRank      = Attacks.straightAttack(boardMaps[boardMask], bitMaps[3] | wQ, true) & ~boardMaps[whiteMask];
        long bRankKing  = Attacks.straightAttack(boardMaps[boardMask], bK, true);

        long wFile      = Attacks.straightAttack(boardMaps[boardMask], bitMaps[3] | wQ, false) & ~boardMaps[whiteMask];
        long bFileKing  = Attacks.straightAttack(boardMaps[boardMask], bK, false);
        tmpRes |= (wRank & bRankKing) | (wFile & bFileKing);
        //Intersection with boardMaps[boardMask will be all pinned pieces
        positionProps[pinnedMask] |= tmpRes & boardMaps[boardMask];
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
        long oppMask = whiteToMove ? boardMaps[blackMask] : boardMaps[whiteMask];
        int start = whiteToMove ? 0 : 6;
        long attack = whiteToMove ? positionProps[blackAttack] : positionProps[whiteAttack];

        List<Character> moves = new ArrayList<>(MoveGenerator.kingMoves(boardMaps[boardMask], attack,
                oppMask, (char)0, Long.numberOfTrailingZeros(bitMaps[start])));
        if(numCheckers > 1) //Return early since only king moves are available
            return moves;
        MoveGenerator.generatePawnMoveInCheck(moves, boardMaps[boardMask],
                oppMask, enPassantPawn, whiteToMove, bitMaps[start + 1],
                positionProps[checkerMask] | positionProps[blockerMask]);
        moves.addAll(MoveGenerator.generateInCheckMoves(boardMaps[boardMask], oppMask, bitMaps[2 + start], 2,
                positionProps[checkerMask] | positionProps[blockerMask]));
        moves.addAll(MoveGenerator.generateInCheckMoves(boardMaps[boardMask], oppMask, bitMaps[3 + start], 3,
                positionProps[checkerMask] | positionProps[blockerMask]));
        moves.addAll(MoveGenerator.generateInCheckMoves(boardMaps[boardMask], oppMask, bitMaps[4 + start], 4,
                positionProps[checkerMask] | positionProps[blockerMask]));
        moves.addAll(MoveGenerator.generateInCheckMoves(boardMaps[boardMask], oppMask, bitMaps[5 + start], 5,
                positionProps[checkerMask] | positionProps[blockerMask]));
        //Remove all pinned pieces illegal moves
        long pinnedPieces = (boardMaps[boardMask] & ~oppMask) & positionProps[pinnedMask];
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
        long oppMask = whiteToMove ? boardMaps[blackMask] : boardMaps[whiteMask];
        int start = whiteToMove ? 0 : 6;
        char castling = whiteToMove ? castlingRights : (char) (castlingRights >> 2);
        //Legal king moves
        List<Character> moves = new ArrayList<>(MoveGenerator.kingMoves(boardMaps[boardMask],
                (whiteToMove ? positionProps[blackAttack] : positionProps[whiteAttack]), oppMask,
                castling, Long.numberOfTrailingZeros(bitMaps[start])));
        //Pseudo legal pawn moves
        moves.addAll(MoveGenerator.generatePawnMoves(boardMaps[boardMask], oppMask, enPassantPawn & 0xFF,
                whiteToMove, bitMaps[start + 1]));
        //Pseudo legal knight moves
        moves.addAll(MoveGenerator.generateKnightMoves(bitMaps[2 + start], boardMaps[boardMask], oppMask));
        //Pseudo legal rook moves
        moves.addAll(MoveGenerator.generateSlidingMoves(boardMaps[boardMask], oppMask,
                bitMaps[start + 3], 3));
        //Pseudo legal bishop moves
        moves.addAll(MoveGenerator.generateSlidingMoves(boardMaps[boardMask], oppMask,
                bitMaps[start + 4], 4));
        //Pseudo legal queen moves
        moves.addAll(MoveGenerator.generateSlidingMoves(boardMaps[boardMask], oppMask,
                bitMaps[start + 5], 5));

        //Remove all pinned pieces illegal moves
        long pin = positionProps[pinnedMask] & (boardMaps[boardMask] & ~oppMask);
        if(pin != 0) {
            List<Character> legal = new ArrayList<>();
            int king = whiteToMove ? Long.numberOfTrailingZeros(bitMaps[0]) : Long.numberOfTrailingZeros(bitMaps[6]);
            for (char move : moves) {
                int from = Move.getFrom(move);
                if (((1L << from) & pin) != 0) {
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


    public char findBestMove(int depth){
        nodes = 0;
        List<Character> moves = getAllMoves(numCheckers > 0);
        if(moves.size() == 0)
            return 0;
        int bestEval = -99999; //Initiate as worst eval
        int perspective = whiteToMove ? -1 : 1; //Inverse since we make a move first
        char bestMove = moves.get(0);
        long start = System.currentTimeMillis();

        for(char move : moves){
            nodes++;
            makeMove(move);
            int eval = perspective * -1 * searchEval(depth, -99999 * perspective, 99999 * perspective);
            undoMove(move);
            if(eval > bestEval)
                bestMove = move;
            bestEval = Math.max(eval, bestEval);
        }
        System.out.println("Number of nodes: " + nodes);
        long end = System.currentTimeMillis();
        end -= start;
        System.out.println( "Total time elapsed: " + end / 1000.0);
        return bestMove;
    }

    private int searchEval(int depth, int alpha, int beta){
        if(depth == 0)
            return Evaluate.evaluation(bitMaps, whiteToMove);
        List<Character> moves = getAllMoves(numCheckers > 0);
        //Checkmate or stalemate
        if(moves.size() == 0){
            if(numCheckers > 0)
                return -99999;
            else
                return 0;
        }
        for(char move : moves){
            nodes++;
            makeMove(move);
            int eval = -searchEval(depth - 1, -beta, -alpha);
            undoMove(move);
            if(eval >= beta)
                return beta;
            //Update current worst eval as long as it's not better than the the worst for our opponent
            alpha = Math.max(eval, alpha);
        }
        return alpha;
    }


    public long search(int depth){
        long numPositions = 0;
        if(depth == 0)
            return 1;
        List<Character> moves = getAllMoves(numCheckers > 0);
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
        return positionProps[pinnedMask];
    }
    public int getMoveSound(){
        return moveSound;
    }
}
