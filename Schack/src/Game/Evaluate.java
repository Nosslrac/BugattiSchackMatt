package Game;

public class Evaluate{
    //Ordered like the bitmaps for each piece excluding the king
    private static final int[] pieceValue = {100, 300, 500, 300, 900};


    //Evaluate based on only piece value
    public static int evaluation(long[] bitMaps, boolean whiteToMove){
        int perspective = whiteToMove ? 1 : -1;
        int white = 0, black = 0;
        for(int i = 1; i < 6; i++){
            white += Long.bitCount(bitMaps[i]) * pieceValue[i - 1];
            black += Long.bitCount(bitMaps[i + 6]) * pieceValue[i - 1];
        }
        return (white - black) * perspective;
    }

}
