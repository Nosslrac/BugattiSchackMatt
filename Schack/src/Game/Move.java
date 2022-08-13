package Game;

public class Move {
    /*
     16 - bit move encoding
     bit 0-5: the "to" square
     bit 6-11: the "from" square
     bit 12-15: the move flags:
        -special bits 12-13: 0 0 -> quiet moves, 0 1 -> double pawn push, 1 0 -> king castle, 11 -> queen castle
        -capPromo bits 14-15 0 1 -> capture
        see: https://www.chessprogramming.org/Encoding_Moves
     */

    public static int getTo(char move){
        return move & 0x3F; //To square is bits 0-5
    }
    public static int getFrom(char move){
        return (move >> 6) & 0x3F; //From square is bits 6-11
    }
    public static int getFlags(char move){
        return (move >> 12); //Flags are bits 12-15
    }
}
