package Graphics;


import Game.Board;
import Game.Move;
import Game.MoveGenerator;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;


public class Draw extends JPanel implements ActionListener, MouseListener, MouseMotionListener {
    final int PANEL_WIDTH = 800;
    final int PANEL_HEIGHT = 640;
    final int sq_size = (PANEL_HEIGHT - 40) / 8;
    private boolean draw = false;
    private float soundVolume = 1.0f;
    private File[] sounds;
    private boolean flip = false;
    private int held_piece;
    private Board board;
    private List<Character> legalMoves;
    private int id;
    private int mX, mY;
    private int origin;
    private final BufferedImage[] pieceImage = new BufferedImage[12];
    private final Timer timer;
    Color CHESS_GREEN = new Color(0, 150, 80);


    public Draw() {
        this.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        board = new Board();
        try {
            getImages();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not retrieve image\n");
        }
        sounds = getSound();
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        timer = new Timer(20, this);
        timer.start();
    }


    public void paintComponent(Graphics g) {
        Graphics2D g2D = (Graphics2D) g;
        RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2D.setRenderingHints(rh);
        drawBoard(g2D);
        drawAttack(g2D);
        //drawPinned(g2D);
        drawPieces(g2D);
        drawPiece(g2D);
    }

    private File[] getSound(){
        File check = new File("assets/audio/check.wav");
        File castle = new File("assets/audio/castle.wav");
        File capture = new File("assets/audio/capture.wav");
        File checkmate = new File("assets/audio/checkmate.wav");
        File move = new File("assets/audio/move.wav");
        return new File[]{move, check, capture, castle, checkmate};
    }

    private void playSound(int soundID){
        try{
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(sounds[soundID]));
            clip.start();

        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }

    private void drawBoard(Graphics2D g2D){
        Color sigma = new Color(60, 63, 65);
        g2D.setColor(sigma);
        g2D.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        g2D.setColor(Color.BLUE);
        g2D.fillRect(0, 0, PANEL_HEIGHT, PANEL_HEIGHT);
        g2D.setColor(Color.WHITE);
        g2D.fillRect(18, 18, PANEL_HEIGHT - 18*2, PANEL_HEIGHT-18*2);

        g2D.setColor(CHESS_GREEN);
        for(int i = 0; i < 64; i++) {
            if (((i + i / 8) % 2) % 2 != 0) {
                g2D.fillRect(i % 8 * sq_size + 20, i / 8 * sq_size + 20, sq_size, sq_size);
            }
        }
        char w = 'a';
        char n = '8';
        for(int i = 0; i < 8; i++){
            g2D.drawString("" + w, i * sq_size + 20 + sq_size / 2 - 5, 10);
            g2D.drawString("" + n, 5 , i * sq_size + 20 + sq_size / 2 - 5);
            w++;
            n--;
        }


    }


    private void drawPieces(Graphics2D g2D) {
        int piece;
        long[] pieceMasks = board.getBitMaps();
        for(int i = 0; i < 64; i++) {
            long check = 1L << i;
            piece = -1;
            for(int j = 0; j < 12; j++){
                if((check & pieceMasks[j]) == check)
                    piece = j;
            }
            if(piece > -1 && (i != origin || !draw)){
                g2D.drawImage(pieceImage[piece], i % 8 * sq_size +20, i / 8 * sq_size + 20, null);
            }
        }
    }


    private void drawPiece(Graphics2D g2D) {
        if(draw && held_piece >= 0){
            g2D.drawImage(pieceImage[held_piece],  mX - sq_size / 2, mY - sq_size / 2, null);
        }
    }

    private void drawAttack(Graphics2D g2D) {
        g2D.setColor(Color.BLUE);
        if(legalMoves == null)
            return;
        for(char move : legalMoves){
            int pos = Move.getTo(move);
            g2D.fillRect(pos % 8 * sq_size + 22, (pos / 8 )*sq_size + 22, sq_size - 4, sq_size - 4);
        }
    }

    private void drawPinned(Graphics2D g2D) {
        long temp = board.getWhiteAttack();
        g2D.setColor(Color.RED);
        while(temp != 0){
            int pos = Long.numberOfTrailingZeros(temp);
            g2D.fillRect(pos % 8 * sq_size +22, (pos / 8 )*sq_size + 22, sq_size - 4, sq_size - 4);
            temp &= ~(1L << pos);
        }
    }

    private void getImages() throws IOException {
        pieceImage[0] = ImageIO.read(new File("C:/Users/axel0/Documents/Schack/assets/whiteKing.png"));
        pieceImage[1] = ImageIO.read(new File("C:/Users/axel0/Documents/Schack/assets/whitePawn.png"));
        pieceImage[2] = ImageIO.read(new File("C:/Users/axel0/Documents/Schack/assets/whiteKnight.png"));
        pieceImage[3] = ImageIO.read(new File("C:/Users/axel0/Documents/Schack/assets/whiteRook.png"));
        pieceImage[4] = ImageIO.read(new File("C:/Users/axel0/Documents/Schack/assets/whiteBishop.png"));
        pieceImage[5] = ImageIO.read(new File("C:/Users/axel0/Documents/Schack/assets/whiteQueen.png"));
        pieceImage[6] = ImageIO.read(new File("C:/Users/axel0/Documents/Schack/assets/blackKing.png"));
        pieceImage[7] = ImageIO.read(new File("C:/Users/axel0/Documents/Schack/assets/blackPawn.png"));
        pieceImage[8] = ImageIO.read(new File("C:/Users/axel0/Documents/Schack/assets/blackKnight.png"));
        pieceImage[9] = ImageIO.read(new File("C:/Users/axel0/Documents/Schack/assets/blackRook.png"));
        pieceImage[10] = ImageIO.read(new File("C:/Users/axel0/Documents/Schack/assets/blackBishop.png"));
        pieceImage[11] = ImageIO.read(new File("C:/Users/axel0/Documents/Schack/assets/blackQueen.png"));
    }

    public void undoMove(){
        board.undoMove();
    }

    public void perft(){
        board.perft(5);
    }

    public void setPromotion(int key){
        if(key != 0xff)
            board.setPromotion(key);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        mX = e.getX();
        mY = e.getY();
        origin = (mX - 20) / sq_size + ((mY- 20) / sq_size) * 8;
        if(legalMoves != null)
            legalMoves = null;
        else
            legalMoves = board.getLegalMoves(origin);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        draw = true;
        mX = e.getX();
        mY = e.getY();
        origin = (mX - 20) / sq_size + ((mY- 20) / sq_size) * 8;
        held_piece = board.getPiece(origin);
    }


    @Override
    public void mouseReleased(MouseEvent e) {
        if(held_piece > -1) {
            if(board.placePiece(origin, (mX - 20) / sq_size + ((mY - 20) / sq_size) * 8))
                playSound(board.getMoveSound());
        }
        held_piece = -1;
        legalMoves = null;
        draw = false;

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mX = e.getX();
        mY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

}

