package Graphics;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Frame extends JFrame implements KeyListener {
    Draw draw;

    public Frame()  {
        draw = new Draw();

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.add(draw);
        this.addKeyListener(this);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);

    }


    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()){
            case KeyEvent.VK_SPACE -> draw.undoMove();
            case KeyEvent.VK_S -> draw.perft();
            case KeyEvent.VK_K -> draw.setPromotion(0);
            case KeyEvent.VK_R -> draw.setPromotion(1);
            case KeyEvent.VK_B -> draw.setPromotion(2);
            case KeyEvent.VK_Q -> draw.setPromotion(3);
        }
    }
}