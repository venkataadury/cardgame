package deckheroes;
import java.awt.*;
import javax.swing.*;
import draw.S;
import commons.X;

public class GIFAnim extends JPanel
{
	private final Image myGIF;
	private Thread myThread=null;
	private JFrame myFrame=null;
	
	public GIFAnim(Image img){myGIF=img; setup();}
	public GIFAnim(Image img,int X,int Y) {this(img); setLocation(X,Y);}
	public GIFAnim(Image img,int X,int Y,JFrame f) {this(img,X,Y); myFrame=f;}
	
	private void setup()
	{
		this.setSize(Card.WIDTH,Card.HEIGHT);
		this.setBackground(new Color(0,0,0,0));
		myThread=new Thread()
		{
			public void run()
			{
				while(true)
				{
					myFrame.repaint(getLocation().x,getLocation().y,Card.WIDTH,Card.HEIGHT);
					validate();
					repaint();
					X.sopln("Repainted","violet");
					X.halt(0.5);
				}
			}
		};
		myThread.start();
	}
	
	public int getX() {return getLocation().x;}
	public int getY() {return getLocation().y;}
	
	public void setFrame(JFrame f) {myFrame=f;}
	public Image getImage() {return myGIF;}
	
	public void paintComponent(Graphics g)
	{
		if(g==null)
			return;
		super.paintComponent(g);
		g.clearRect(0,0,Card.WIDTH,Card.HEIGHT);
		if(myGIF!=null)
			g.drawImage(myGIF,0,0,this);
	}
}
