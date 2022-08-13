package Game;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.Math.abs;

public class MoveGenerator {
    private static final long[] files = {0x0101010101010101L, 0x0202020202020202L, 0x0404040404040404L,
                            0x0808080808080808L, 0x1010101010101010L, 0x2020202020202020L, 0x4040404040404040L,
                            0x8080808080808080L};
    private static final long[] ranks = {0xFFL, 0xFF00L, 0xFF0000L, 0xFF000000L, 0xFF00000000L, 0xFF0000000000L,
                                0xFF000000000000L, 0xFF00000000000000L};
    private static final long shiftedAntiDiagonal = (1L) | (1L << 7) | (1L << 14)| (1L << 21) | (1L << 28) | (1L << 35) | (1L << 42) | (1L << 49);
    private static final long mainDiagonal = 0x8040201008040201L;//(1L) | (1L << 9)| (1L << 18) | (1L << 27) | (1L << 36) | (1L << 45) | (1L << 54) | (1L << 63);
    private static final long[] mainDiagonals = {0x0100000000000000L, 0x0201000000000000L, 0x0402010000000000L, 0x0804020100000000L,
            0x1008040201000000L, 0x2010080402010000L, 0x4020100804020100L, 0x8040201008040201L,
            0x80402010080402L, 0x804020100804L, 0x8040201008L, 0x80402010L, 0x804020L, 0x8040L, 0x80L};
    private static final long[] antiDiagonals = {0x1L, 0x0102L, 0x010204L, 0x01020408, 0x0102040810L, 0x010204081020L, 0x01020408102040L,
            0x0102040810204080L, 0x0204081020408000L, 0x0408102040800000L, 0x0810204080000000L, 0x1020408000000000L,
            0x2040800000000000L, 0x4080000000000000L, 0x8000000000000000L};






    ///////////////////////////////////////////////////////////
    //In check move generation
    //////////////////////////////////////////////////////////

    public static void generatePawnMoveInCheck(List<Character> moves, long boardMask, long oppMask, int enPassant, boolean white, long pawns, long target){
        final long promoPawn = white ? ranks[1] & pawns : ranks[6] & pawns;
        final long possDouble = white ? ranks[5] : ranks[2]; //where pawns on starting rank will be after one push
        final long emptyBoard = ~boardMask;
        final long noPromoPawn = pawns & ~promoPawn;

        //Quiet one move pushes
        final int pushUp = white ? -8 : 8;
        long push = shift(pushUp, noPromoPawn) & emptyBoard; //Push all pawns not being blocked
        long possibleDoublePush = push & possDouble; //Needed for next step
        push &= target;
        while(push != 0){
            int dest = Long.numberOfTrailingZeros(push);
            char move = (char) ( dest | (dest - pushUp) << 6);
            moves.add(move);
            push &= push - 1;
        }

        //Double pushes
        possibleDoublePush = shift(pushUp, possibleDoublePush) & emptyBoard & target;
        while(possibleDoublePush != 0){
            int dest = Long.numberOfTrailingZeros(possibleDoublePush);
            char move = (char) (dest | (dest - pushUp - pushUp) << 6 | 0x1000);
            moves.add(move);
            possibleDoublePush &= possibleDoublePush - 1;
        }

        //Add capture resolving the check
        final int leftUp = white ? -9 : 7;
        final int rightUp = white ? -7 : 9;
        long pawnLeft = shift(leftUp, noPromoPawn & ~files[0]) & oppMask & target;
        long pawnRight = shift(rightUp, noPromoPawn & ~files[7]) & oppMask & target;
        while (pawnLeft != 0){
            int dest = Long.numberOfTrailingZeros(pawnLeft);
            char move = (char)(dest | (dest - leftUp) << 6 | 0x4000); //Captures
            moves.add(move);
            pawnLeft &= pawnLeft - 1;
        }
        while (pawnRight != 0){
            int dest = Long.numberOfTrailingZeros(pawnRight);
            char move = (char)(dest | (dest - rightUp) << 6 | 0x4000); //Captures
            moves.add(move);
            pawnRight &= pawnRight - 1;
        }

        //EP capture only available if the double pushed pawn is the checker
        if(enPassant != 0xFF && ((1L << (enPassant >> 8)) & target) != 0){
            long epPos = 1L << (enPassant & 0xFF);
            long attackers = shift(-leftUp, epPos) | shift(-rightUp, epPos);
            attackers &= pawns;
            while(attackers != 0){
                int pos = Long.numberOfTrailingZeros(attackers);
                char move = (char)(enPassant & 0xFF | pos << 6 | 0x5000);
                moves.add(move);
                attackers &= attackers - 1;
            }
        }

        //All kinds of promotions
        if(promoPawn != 0){
            long promoPush = shift(pushUp, promoPawn) & emptyBoard & target;
            long promoCapLeft = shift(leftUp, promoPawn) & oppMask & target;
            long promoCapRight = shift(rightUp, promoPawn) & oppMask & target;
            while(promoPush != 0){
                int dest = Long.numberOfTrailingZeros(promoPush);
                addPromotion(moves, (char)(dest | (dest - pushUp) << 6 | 0x8000));
                promoPush &= promoPush - 1;
            }
            while(promoCapLeft != 0){
                int dest = Long.numberOfTrailingZeros(promoPush);
                addPromotion(moves, (char)(dest | (dest - leftUp) << 6 | 0xC000));
                promoCapLeft &= promoCapLeft - 1;
            }
            while(promoCapRight != 0){
                int dest = Long.numberOfTrailingZeros(promoPush);
                addPromotion(moves, (char)(dest | (dest - rightUp) << 6 | 0xC000));
                promoCapRight &= promoCapRight - 1;
            }

        }
    }

    public static List<Character> generateInCheckMoves(long boardMask, long oppMask, long pieces, int type, long target){
        List<Character> moves = new ArrayList<>();
        while(pieces != 0) {
            int pos = Long.numberOfTrailingZeros(pieces);
            long dest = switch (type) {
                case 2 -> Attacks.knightAttacks[pos];
                case 3 -> Attacks.rookAttack(boardMask, pos);
                case 4 -> Attacks.bishopAttack(boardMask, pos);
                default -> (Attacks.bishopAttack(boardMask, pos) | Attacks.rookAttack(boardMask, pos)); //5
            };
            //Don't need to filter out friendlies since they are never part of block or checkerMask
            dest &= target;


            while (dest != 0) {
                int to = Long.numberOfTrailingZeros(dest);
                char move = (char) (to | pos << 6);
                if ((oppMask & (1L << to)) != 0)
                    move |= 0x4000;
                moves.add(move);
                dest &= dest - 1;
            }
            pieces &= pieces - 1;
        }
        return moves;
    }

    private static long shift(int dir, long mask){
        return switch (dir) {
            case 8  -> mask << 8; //Left down
            case 7  -> mask << 7; //right down
            case 9  -> mask << 9; //right up
            case -7 -> mask >> 7;
            case -8 -> mask >> 8; //left up
            default -> mask >> 9;
        };
    }

    //Returns a list of all pseudo legal pawn moves encoded with capture, double pushes and en passant
    public static List<Character> generatePawnMoves(long boardMask, long oppMask, int enPassant, boolean white, long pawns){
        List<Character> moves = new ArrayList<>();
        final long promoPawn = white ? ranks[1] & pawns : ranks[6] & pawns;
        final long possDouble = white ? ranks[5] : ranks[2]; //where pawns on starting rank will be after one push
        final long emptyBoard = ~boardMask;
        final long noPromoPawn = pawns & ~promoPawn;

        //Quiet one move pushes
        final int pushUp = white ? -8 : 8;
        long push = shift(pushUp, noPromoPawn) & emptyBoard; //Push all pawns not being blocked
        long possibleDoublePush = push & possDouble;
        while(push != 0){
            int dest = Long.numberOfTrailingZeros(push);
            char move = (char) ( dest | (dest - pushUp) << 6);
            moves.add(move);
            push &= push - 1;
        }

        //Double pushes
        possibleDoublePush = shift(pushUp, possibleDoublePush) & emptyBoard;
        while(possibleDoublePush != 0) {
            int dest = Long.numberOfTrailingZeros(possibleDoublePush);
            char move = (char) (dest | (dest - pushUp - pushUp) << 6 | 0x1000);
            moves.add(move);
            possibleDoublePush &= possibleDoublePush - 1;
        }


        //Add all non promotion captures
        final int leftUp = white ? -9 : 7;
        final int rightUp = white ? -7 : 9;
        long pawnLeft = shift(leftUp, noPromoPawn & ~files[0]) & oppMask; //Captures
        long pawnRight = shift(rightUp, noPromoPawn & ~files[7]) & oppMask;
        while (pawnLeft != 0){
            int dest = Long.numberOfTrailingZeros(pawnLeft);
            char move = (char)(dest | (dest - leftUp) << 6 | 0x4000); //Captures
            moves.add(move);
            pawnLeft &= pawnLeft - 1;
        }
        while (pawnRight != 0){
            int dest = Long.numberOfTrailingZeros(pawnRight);
            char move = (char)(dest | (dest - rightUp) << 6 | 0x4000); //Captures
            moves.add(move);
            pawnRight &= pawnRight - 1;
        }
        if(enPassant != 0xFF){
            long epPos = 1L << enPassant;
            //Reverse pawnAttack gives potential attackers of the ep square
            long attackers = Attacks.pawnAttack(epPos, !white);
            attackers &= pawns & (ranks[4] | ranks[3]);
            while(attackers != 0){
                int pos = Long.numberOfTrailingZeros(attackers);
                char move = (char)(enPassant | pos << 6 | 0x5000);
                moves.add(move);
                attackers &= attackers - 1;
            }
        }

        //All kinds of promotions
        if(promoPawn != 0){
            long promoPush = shift(pushUp, promoPawn) & emptyBoard;
            long promoCapLeft = shift(leftUp, promoPawn) & oppMask;
            long promoCapRight = shift(rightUp, promoPawn) & oppMask;
            while(promoPush != 0){
                int dest = Long.numberOfTrailingZeros(promoPush);
                addPromotion(moves, (char)(dest | (dest - pushUp) << 6 | 0x8000));
                promoPush &= promoPush - 1;
            }
            while(promoCapLeft != 0){
                int dest = Long.numberOfTrailingZeros(promoCapLeft);
                addPromotion(moves, (char)(dest | (dest - leftUp) << 6 | 0xC000));
                promoCapLeft &= promoCapLeft - 1;
            }
            while(promoCapRight != 0){
                int dest = Long.numberOfTrailingZeros(promoCapRight);
                addPromotion(moves, (char)(dest | (dest - rightUp) << 6 | 0xC000));
                promoCapRight &= promoCapRight - 1;
            }

        }
        return moves;
    }

    private static void addPromotion(List<Character> moves, char move){
        moves.add(move);
        moves.add((char) (move + 0x1000));
        moves.add((char) (move + 0x2000));
        moves.add((char) (move + 0x3000));
    }

    //Generate pseudo legal moves for given pawn
    public static List<Character> pawnMove(long boardMask, long oppMask, int enPassant, boolean white, int pos){
        long sq = 1L << pos;
        List<Character> moves = new ArrayList<>();
        long attack = Attacks.pawnAttack(sq, white);
        //If en passant is available then we need to add it to the moves
        if(enPassant != 0xFF){ //TODO reset enPassantPawn instead AND only send pseudoPawn
            //If we attack the square behind the double pushed pawn then we can capture it
            long enPassantSq = 1L << enPassant;
            if ((attack & enPassantSq) != 0) {
                moves.add((char) (enPassant | pos << 6 | 0x5000)); //0x5000 is code for ep capture
            }
        }
        attack &= oppMask;
        while(attack != 0){
            int check = Long.numberOfTrailingZeros(attack);
            moves.add((char) (check | pos << 6 | 0x4000)); //0x4000 is code for capture
            attack &= ~(1L << check); //Clears the first set bit
        }
        if(white){
            sq >>= 8;
            if((sq & boardMask) == 0){
                char move = (char) (pos - 8 | pos << 6);
                if(pos - 8 < 8)
                    move |= 0x8000;
                moves.add(move);
                if(pos / 8 == 6){
                    sq >>= 8;
                    if((sq & boardMask) == 0)
                        moves.add((char) (pos - 16 | pos << 6 | 0x1000)); //0x1000 is code for pawn double push
                }
            }

        }
        else{
            sq <<= 8;
            if((sq & boardMask) == 0){
                char move = (char) (pos + 8 | pos << 6);
                if(pos + 8 > 55)
                    move |= 0x8000; //0x8000 is code for promotion
                moves.add(move);
                if(pos / 8 == 1){
                    sq <<= 8;
                    if((sq & boardMask) == 0)
                        moves.add((char) (pos + 16 | pos << 6 | 0x1000));
                }
            }

        }
        return moves;
    }


    //Generate pseudo legal moves when not in check
    public static List <Character> generateKnightMoves(long knightMask, long boardMask, long oppMask, long block){
        List<Character> moves = new ArrayList<>();
        while(knightMask != 0){
            int pos = Long.numberOfTrailingZeros(knightMask);
            moves.addAll(knightMove(boardMask, oppMask, pos, block));
            knightMask &= ~(1L << pos);
        }
        return moves;
    }

    //Generates legal moves when not in check
    public static List<Character> knightMove(long boardMask, long oppMask, int pos, long block){
        List <Character> moves = new ArrayList<>();
        long sameMask = boardMask & ~oppMask;
        long dest = Attacks.knightAttacks[pos] & ~sameMask;
        if(block != 0) //Means we are in check
            dest &= block;
        while(dest != 0){
            int to = Long.numberOfTrailingZeros(dest);
            char move = (char) (to | pos << 6);
            if((oppMask & (1L << to)) != 0)
                move |= 0x4000;
            moves.add(move);
            dest &= dest - 1;
        }
        return moves;
    }



    public static List <Character> kingMoves(long boardMask, long attack, long oppMask, char castling, int pos){
        List<Character> moves = new ArrayList<>();
        if((castling & 1) > 0){
            //Check two nearby squares and king square for attack
            long bits = 1L << pos | 1L << (pos + 1) | 1L << (pos + 2); //King side castling
            if((attack & bits) == 0){
                bits = 1L << (pos + 1) | 1L << (pos + 2);
                if((bits & boardMask) == 0)
                    moves.add((char) (pos + 2 | pos << 6 | 0x2000));
            }
        }
        if((castling & 2) == 2){
            long bits = 1L << pos | 1L << (pos - 1) | 1L << (pos - 2); //Queen side castling
            if((attack & bits) == 0){
                bits = 1L << (pos - 1) | 1L << (pos - 2) | 1L << (pos - 3);
                if((bits & boardMask) == 0)
                    moves.add((char) (pos - 2 | pos << 6 | 0x3000));
            }
        }
        long sameMask = boardMask & ~oppMask;
        long bitMoves = Attacks.kingAttacks[pos] & ~attack & ~sameMask; //Lookup table based on position
        while(bitMoves != 0){
            int to = Long.numberOfTrailingZeros(bitMoves);
            char move = (char)(to | pos << 6);
            if((oppMask & (1L << to)) != 0)
                move |= 0x4000;
            moves.add(move);
            bitMoves &= ~(1L << to);
        }
        return moves;
    }



    public static List<Character> generateSlidingMoves(long boardMask, long oppMask, long sliders, int type){
        List<Character> moves = new ArrayList<>();
        while(sliders != 0){
            int pos = Long.numberOfTrailingZeros(sliders);
            moves.addAll(slidingMoves(boardMask, oppMask, pos, type));
            sliders &= ~(1L << pos);
        }
        return moves;
    }

    public static List<Character> slidingMoves(long boardMask, long oppMask, int pos, int type){
        List<Character> moves = new ArrayList<>();
        long sameMask = boardMask & ~oppMask;
        long destinations;
        if(type == 3)
             destinations = Attacks.rookAttack(boardMask, pos);
        else if(type == 4)
            destinations = Attacks.bishopAttack(boardMask, pos);
        else
            destinations = Attacks.bishopAttack(boardMask, pos) | Attacks.rookAttack(boardMask, pos);
        destinations &= ~sameMask;
        while(destinations != 0){
            int dest = Long.numberOfTrailingZeros(destinations);
            char move = (char)(dest | pos << 6);
            long destSq = 1L << dest;
            if((oppMask & destSq) != 0)
                move |= 0x4000;
            moves.add(move);
            destinations &= ~destSq;
        }
        return moves;
    }


    /*Description: Attacks for a bishop on a given square
      Parameters: boardMask (board occupancy), pos (rook position)
      returns: the bishops attack
     */




    public static void print_bitboard(long mask){
        long temp = mask;
        StringBuilder p = new StringBuilder();
        for(int i = 0; i < 64; i++){
            p.append(( (temp & 1) == 1 ? "X " : "- "));
            temp >>= 1;
        }
        for(int i = 0; i < 8; i++){
            System.out.println(p.substring(i * 16, (i + 1) * 16));
        }
        System.out.println("\n\n");
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
