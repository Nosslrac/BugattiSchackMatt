package Game;

import java.util.HashMap;

import static java.lang.Math.abs;

public class Attacks {
    private static final long[] files = {0x0101010101010101L, 0x0202020202020202L, 0x0404040404040404L,
            0x0808080808080808L, 0x1010101010101010L, 0x2020202020202020L, 0x4040404040404040L,
            0x8080808080808080L};
    private static final long[] ranks = {0xFFL, 0xFF00L, 0xFF0000L, 0xFF000000L, 0xFF00000000L, 0xFF0000000000L,
            0xFF000000000000L, 0xFF00000000000000L};
    private static final long[] mainDiagonals = {0x0100000000000000L, 0x0201000000000000L, 0x0402010000000000L, 0x0804020100000000L,
            0x1008040201000000L, 0x2010080402010000L, 0x4020100804020100L, 0x8040201008040201L,
            0x80402010080402L, 0x804020100804L, 0x8040201008L, 0x80402010L, 0x804020L, 0x8040L, 0x80L};
    private static final long[] antiDiagonals = {0x1L, 0x0102L, 0x010204L, 0x01020408, 0x0102040810L, 0x010204081020L, 0x01020408102040L,
            0x0102040810204080L, 0x0204081020408000L, 0x0408102040800000L, 0x0810204080000000L, 0x1020408000000000L,
            0x2040800000000000L, 0x4080000000000000L, 0x8000000000000000L};
    private static final long[][] rowAttacks = initRankAttacks();
    private static final int[] maskBits = {0,0,1,3,7,15,31,63,31,15,7,3,1,0,0};
    private static final long shiftedAntiDiagonal = (1L) | (1L << 7) | (1L << 14)| (1L << 21) | (1L << 28) | (1L << 35) | (1L << 42) | (1L << 49);
    private static final long mainDiagonal = 0x8040201008040201L;
    public static final long[] kingAttacks = initKingAttack();
    public static final long[] knightAttacks = initKnightAttack();
    public static final HashMap<Integer , Long[][]> mainDiagonalAttacks = initMainDiagonalAttacks();
    public static final HashMap<Integer, Long[][]> antiDiagonalAttacks = initAntiDiagonalAttacks();


    //TODO SWITCH TO LEADING ZEROS INSTEAD FOR CLEANER BITBOARD MANIPULATION

    /////////////////////////////////////////////
    //Fast attack algorithms using lookup tables
    ////////////////////////////////////////////
    public static long bishopAttacks(long boardMask, long bishop){
        long attack = 0L;
        while (bishop != 0){
            int bPos = Long.numberOfTrailingZeros(bishop);
            attack |= bishopAttack(boardMask, bPos);
            bishop &= bishop - 1;
        }
        return attack;
    }

    private static long mainDiagonalAttacks(long boardMask, int pos){
        int offset = (pos % 8 - pos / 8);
        int index = 7 + offset;
        long diagonal = mainDiagonals[index] & boardMask;
        //Mask relevant bits
        int shift = index < 8 ? 2 : index - 5;
        //Shift down to get occupancy bits as LSBs
        int occupancy = (int)((diagonal * files[1]) >>> (56 + shift));
        //Mask edge bits for corresponding diagonal
        occupancy = occupancy & maskBits[index];
        //Fetch attack based on diagonal, position (column) and occupancy
        return mainDiagonalAttacks.get(index)[(offset > 0 ? pos % 8 - offset : pos % 8)][occupancy];
    }

    private static long antiDiagonalAttack(long boardMask, int pos){
        //On a given anti diagonal, sum of column and row is equal for all squares => can be indexed by the sum
        int index = pos / 8 + pos % 8;
        //Occupied bits of relevant diagonal
        long diagonal = antiDiagonals[index] & boardMask;
        //To get occupancy bits we will need to know how much to shift
        int shift = index < 8 ? 1 : index - 6;
        //Rotate diagonal to rank
        int occupancy = (int)(diagonal * files[0] >>> (56 + shift));
        //Get inner bits
        occupancy = occupancy & maskBits[index];
        //Attack based on which diagonal, which position on the diagonal, and occupancy of the diagonal
        return antiDiagonalAttacks.get(index)[(index < 8 ? pos % 8 : pos % 8 - shift + 1)][occupancy];
    }

    public static long diagonalAttack(long boardMask, long pieceMask, boolean main){
        long res = 0L;
        while(pieceMask != 0){
            int pos = Long.numberOfTrailingZeros(pieceMask);
            res |= main ? mainDiagonalAttacks(boardMask, pos) : antiDiagonalAttack(boardMask, pos);
            pieceMask &= pieceMask - 1;
        }
        return res;
    }

    public static long bishopAttack(long boardMask, int pos){
        return mainDiagonalAttacks(boardMask , pos) | antiDiagonalAttack(boardMask, pos);
    }

    //Get attack attack of specified current rank
    private static long rankAttack(long boardMask, int pos){
        //Row attack
        int file = pos % 8;
        int rank = pos / 8;
        int occupancy = (int)((boardMask >>> (rank * 8 + 1)) & 0b111111);
        return rowAttacks[file][occupancy] << (rank * 8);
    }

    //Get file attack based of position and board
    private static long fileAttack(long boardMask, int pos){
        //File attack
        int file = pos % 8;
        int occupancy = (int)((fileToRank(boardMask, file) >> 1) & 0b111111);
        long fileAttack = rowAttacks[pos / 8][occupancy];
        fileAttack = rankToFile(fileAttack, 0);
        return fileAttack >>> (7 - file);
    }

    public static long straightAttack(long boardMask, long pieceMask, boolean rank){
        long res = 0L;
        while(pieceMask != 0){
            int pos = Long.numberOfTrailingZeros(pieceMask);
            res |= rank ? rankAttack(boardMask, pos) : fileAttack(boardMask, pos);
            pieceMask &= pieceMask - 1;
        }
        return res;
    }

    //Using the lookup table, get rank attack and using rotation get file attack
    public static long rookAttack(long boardMask, int pos){
        return rankAttack(boardMask, pos) | fileAttack(boardMask, pos);
    }



    /*Description: Go through rookMask and return their combined attack
      Parameters: boardMask (board occupancy), rookMask (rook occupancy)
      returns: combined attack for given rooks
     */
    public static long rookAttacks(long boardMask, long rookMask){
        long res = 0L;
        while(rookMask != 0) {
            int pos = Long.numberOfTrailingZeros(rookMask);
            res |= rookAttack(boardMask, pos);
            rookMask &= rookMask - 1;
        }
        return res;
    }

    public static long pawnAttack(long pawnMask, boolean white){
        long res = 0L;
        if(white){
            res |= (pawnMask >> 9) & ~files[7];
            res |= (pawnMask >> 7) & ~files[0];
        }
        else{
            res |= (pawnMask << 9) & ~files[0];
            res |= (pawnMask << 7) & ~files[7];
        }
        return res;
    }

    public static long knightAttacks(long knightMask){
        long res = 0L;
        while(knightMask != 0){
            int pos = Long.numberOfTrailingZeros(knightMask);
            res |= knightAttacks[pos];
            knightMask &= knightMask - 1;
        }
        return res;
    }

    /////////////////////////////////////////////
    //Slow attack algorithms used to initialize lookup tables
    /////////////////////////////////////////////

    private static long mainDiagonalSlow(long boardMask, long bishopMask){
        int[] dirs = {9,-9};
        long res = 0L;
        while(bishopMask != 0) {
            int pos = Long.numberOfTrailingZeros(bishopMask);
            for (int dir : dirs) {
                res |= bishopAttack(boardMask, dir, pos);
            }
            bishopMask &= bishopMask - 1;
        }
        return res;
    }

    private static long antiDiagonalSlow(long boardMask, long bishopMask){
        int[] dirs = {7, -7};
        long res = 0L;
        while(bishopMask != 0) {
            int pos = Long.numberOfTrailingZeros(bishopMask);
            for (int dir : dirs) {
                res |= bishopAttack(boardMask, dir, pos);
            }
            bishopMask &= bishopMask - 1;
        }
        return res;
    }

    /*Description: Gives attack ray in a specified direction based on the postion
      Parameters: boardMask (board occupancy), dir (relevant direction), pos (bishop position)
      returns: attack ray in direction
     */
    private static long bishopAttack(long boardMask, int dir, int pos){
        int prev = pos % 8;
        pos += dir;
        int next = pos % 8;
        long square;
        long res = 0L;
        while(abs(prev - next) < 2 && (pos & ~63) == 0){
            square = 1L << pos;
            res |= square;
            if((square & boardMask) == square)
                break;
            prev = next;
            pos += dir;
            next = pos % 8;
        }
        return res;
    }

    private static long rowAttackSlow(long boardMask, int pos){
        int rStart = (pos / 8) * 8;
        int rEnd = rStart + 7;
        long attack = 0L;
        long check;
        int iter = pos - 1;
        while(iter >= rStart){
            check = 1L << iter;
            attack |= check;
            if((check & boardMask) == check)
                break;
            iter--;
        }
        iter = pos + 1;
        while(iter <= rEnd){
            check = 1L << iter;
            attack |= check;
            if((check & boardMask) == check)
                break;
            iter++;
        }
        return attack;
    }

    /////////////////////////////////////////////////
    //Initializing attack lookup tables
    /////////////////////////////////////////////////

    //Initialize the bishop attack map
    //Key is the diagonal and the matrix is [fileIndex][occupancy]
    private static HashMap<Integer, Long[][]> initMainDiagonalAttacks(){
        HashMap<Integer, Long[][]> attacks = new HashMap<>();
        attacks.put(0, new Long[][]{{0L}});
        attacks.put(1, new Long[][]{{1L << 57}, {1L << 48}});
        for(int d = 2; d < 13; d++){
            long diagonal = mainDiagonals[d];
            long tempDiagonal = diagonal;
            int numOccupancies = d < 8 ? (1 << (d - 1)) : 1 << (13 - d);
            int numPos = d < 8 ? d + 1 : 15 - d;
            Long[][] attackSet = new Long[numPos][numOccupancies];
            for(int occupancy = 0; occupancy < numOccupancies; occupancy++){
                long board = setDiagonalOccupancy(diagonal, occupancy);
                int arrayPos = 0;
                while(tempDiagonal != 0){
                    //First bit on diagonal
                    int pos = Long.numberOfTrailingZeros(tempDiagonal);
                    //Store attack for current bishop position with given occupancy
                    attackSet[arrayPos++][occupancy] = mainDiagonalSlow(board, 1L << pos);
                    //Clear bit to get next bit in next iteration
                    tempDiagonal &= tempDiagonal - 1;
                }
                tempDiagonal = diagonal;
            }
            attacks.put(d, attackSet);

        }
        attacks.put(13, new Long[][]{{1L << 15}, {1L << 6}});
        attacks.put(14, new Long[][]{{0L}});
        return attacks;
    }


    //Initialize the bishop attack map
    //Key is the diagonal and the matrix is [fileIndex][occupancy]
    private static HashMap<Integer, Long[][]> initAntiDiagonalAttacks(){
        HashMap<Integer, Long[][]> attacks = new HashMap<>();
        attacks.put(0, new Long[][]{{0L}});
        attacks.put(1, new Long[][]{{1L << 1}, {1L << 8}});
        for(int d = 2; d < 13; d++){
            long diagonal = antiDiagonals[d];
            long tempDiagonal = diagonal;
            int numOccupancies = d < 8 ? (1 << (d - 1)) : 1 << (13 - d);
            int numPos = d < 8 ? d + 1 : 15 - d;
            Long[][] attackSet = new Long[numPos][numOccupancies];
            for(int occupancy = 0; occupancy < numOccupancies; occupancy++){
                long board = setDiagonalOccupancyAnti(diagonal, occupancy);
                int arrayPos = 0;
                while(tempDiagonal != 0){
                    //First bit on diagonal
                    int pos = 63 - Long.numberOfLeadingZeros(tempDiagonal);
                    //Store attack for current bishop position with given occupancy
                    long res = antiDiagonalSlow(board, 1L << pos);
                    attackSet[arrayPos++][occupancy] = antiDiagonalSlow(board, 1L << pos);
                    //Clear bit to get next bit in next iteration
                    tempDiagonal &= ~(1L << pos);
                }
                tempDiagonal = diagonal;
            }
            attacks.put(d, attackSet);

        }
        attacks.put(13, new Long[][]{{1L << 55}, {1L << 62}});
        attacks.put(14, new Long[][]{{0L}});
        return attacks;
    }

    //Initialize a attack "lookup table" for all possible states of the rank
    public static long[][] initRankAttacks(){
        long[][] result = new long[8][64];
        //All possible positions on a rank
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 64; j++){
                //Outer bits don't matter, therefore boardmask i.e occupancy only need 2^6 bits
                result[i][j] = rowAttackSlow(j << 1, i);
            }
        }
        return result;
    }


    //Knight attack initialization
    public static long initKnight(long knightMask){
        long res = 0L;
        res |= (knightMask >> 10) & ~(files[6] | files[7] | ranks[7]); //Two files to left and up
        res |= (knightMask << 6) & ~ (files[6] | files[7] | ranks[0]); //Two files to left and down
        res |= (knightMask >> 17) & ~(files[7] | ranks[6] | ranks[7]); //One file to left and up
        res |= (knightMask << 15) & ~(files[7] | ranks[0] | ranks[1]); //One file to left and down
        res |= (knightMask >> 15) & ~(files[0] | ranks[6] | ranks[7]); //One file to right and up
        res |= (knightMask << 17) & ~(files[0] | ranks[0] | ranks[1]); //One file to right and down
        res |= (knightMask >> 6) & ~ (files[0] | files[1] | ranks[7]); //Two files to right and up
        res |= (knightMask << 10) & ~(files[0] | files[1] | ranks[0]); //Two files to right and down
        return res;
    }
    //Knight attack initialization
    private static long[] initKnightAttack(){
        long[] attacks = new long[64];
        for(int pos = 0; pos < 64; pos++){
            attacks[pos] = initKnight(1L << pos);
        }
        return attacks;
    }

    //King attack initialization
    private static long[] initKingAttack(){
        long[] attacks = new long[64];
        for(int pos = 0; pos < 64; pos++){
            attacks[pos] = getKingAttacks(1L << pos);
        }
        return attacks;
    }

    private static long getKingAttacks(long kingMask){
        long res = 0L;
        res |= (kingMask >> 9) & ~(files[7] | ranks[7]); //Diagonal left up
        res |= (kingMask >> 1) & ~(files[7]); //One step left
        res |= (kingMask << 7) & ~(files[7] | ranks[0]); //Diagonal left down
        res |= (kingMask >> 8) & ~(ranks[7]); //One step up
        res |= (kingMask << 8) & ~(ranks[0]); //One step down
        res |= (kingMask << 1) & ~(files[0]); //One step right
        res |= (kingMask >> 7) & ~(files[0] | ranks[7]); //Diagonal right up
        res |= (kingMask << 9) & ~(files[0] | ranks[0]); //Diagonal right down
        return res;
    }

    public static long kingAttack(int kingPos){
        return kingAttacks[kingPos];
    }


    /////////////////////////////////////////////////
    //Occupancy methods and rotation methods
    /////////////////////////////////////////////////

    //Returns specified file as a rank on the first rank
    private static long fileToRank(long bitBoard, int file){
        bitBoard = (bitBoard << (7 - file)) & files[7]; //Move specified file to H - file and mask junk
        bitBoard = bitBoard * shiftedAntiDiagonal; //Multiply with shifted anti diagonal
        return bitBoard >> 56; //Move answer to rank0
    }

    //Returns specified rank as a file on the H - file
    private static long rankToFile(long bitBoard, int rank){
        bitBoard = (bitBoard >>> rank * 8) & ranks[0];
        bitBoard = (((bitBoard * 0x80200802L) & 0x0884422110L) * 0x0101010101010101L) >>> 56; //Mirror rank0
        bitBoard = bitBoard * mainDiagonal; //Flip to H-file
        return bitBoard & files[7]; //Remove junk
    }

    private static long setDiagonalOccupancy(long diagonal, int occupancy){
        int[] bits = new int[8];
        long res = 0L;
        int i;
        for(i = 0; diagonal != 0; i++){
            int bPos = Long.numberOfTrailingZeros(diagonal);
            bits[i] = bPos;
            diagonal &= diagonal - 1; //Reset rightmost set bit
        }
        for(int j = 1; j < i - 1; j++){
            if((occupancy & 1) == 1)
                res |= 1L << bits[j];
            occupancy >>= 1;
        }
        return res;
    }

    private static long setDiagonalOccupancyAnti(long diagonal, int occupancy){
        int[] bits = new int[8];

        long res = 0L;
        int i;
        for(i = 0; diagonal != 0; i++){
            int bPos = 63 - Long.numberOfLeadingZeros(diagonal);
            bits[i] = bPos;
            diagonal &= ~(1L << bPos);
        }
        for(int j = 1; j < i - 1; j++){
            if((occupancy & 1) == 1)
                res |= 1L << bits[j];
            occupancy >>= 1;
        }
        return res;
    }

    public static long[] getFiles(){
        return files;
    }

    public static long[] getRanks(){
        return ranks;
    }

    public static long[] getMainDiagonals(){
        return mainDiagonals;
    }

    public static long[] getAntiDiagonals(){
        return antiDiagonals;
    }

}
