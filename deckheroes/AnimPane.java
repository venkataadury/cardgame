package deckheroes;
import java.awt.*;
import javax.swing.JPanel;
import upgrade.ArrayFx;
import commons.X;
import draw.S;


public class AnimPane extends JPanel
{
	public final Graphics myGraphics;
	GIFAnim[] myImages=new GIFAnim[0];
	public AnimPane()
	{
		setup();
		myGraphics=this.getGraphics();
	}
	
	private void setup()
	{
		this.setBounds(0,0,GameBoard.WID,GameBoard.HEI+GameBoard.fB);
	}
	
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		g.clearRect(0,0,getWidth(),getHeight());
		for(GIFAnim i : myImages)
		{
			if(i==null)
				continue;
			X.sopln("Drawing image","blue");
			g.drawImage(i.getImage(),i.getX(),i.getY(),this);
		}
	}
	
	public void update() {this.paintComponent(this.getComponentGraphics(this.getGraphics()));}
	public void addImage(GIFAnim img) {myImages=ArrayFx.append(myImages,img);}
	public void removeImages() {myImages=new GIFAnim[0];}
}
