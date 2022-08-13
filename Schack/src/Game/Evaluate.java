package Game;

public class Evaluate{
    //Ordered like the bitmaps for each piece excluding the king
    private final int[] pieceValue = {100, 300, 500, 300, 900};


    //Evaluate based on only piece value
    public int evaluation(long[] bitMaps){
        int white = 0, black = 0;
        for(int i = 1; i < 6; i++) {
            white += Long.bitCount(bitMaps[i]) * pieceValue[i];
            black += Long.bitCount(bitMaps[i + 6]) * pieceValue[i + 6];
        }
        return white - black;
    }

}
