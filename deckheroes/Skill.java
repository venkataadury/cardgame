package deckheroes;
import commons.X;
import commons.Y;
import commons.Strings;
import upgrade.ArrayFx;
import commons.macros.*;
import commons.macros.data.*;
import java.io.*;
import java.util.regex.*;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Button;

public class Skill
{
	String name;
	int nA=0; //false
	IntData level,invoke,target,negatable=new IntData(1,"NEGATABLE"); //Targets: -1=> Opponent not required/No target; 0 -Direct opposite/Self, 1 -Entire team (opp), 2-Entire team (self), 3-Random opponent, 4-Random Ally, -n (n is number) -n random opponents/team
	StringData desc;
	Macro macro;
	final File skillFile;
	Card myCard;
	GameBoard GB=null;
	private Image myAnim=null;
	public static final String macroPath="deckheroes/data/skills/macros/";
	/*public java.awt.image.ImageObserver IMGOB=new java.awt.image.ImageObserver() {
		public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
		{
			GB.getGraphics().drawImage(img,x,y,this);
			return true;
		}
	};*/
	public final MacroFunction ANIMATE=new MacroFunction("animate") {
		public String exec(Macro src,String p)
		{
			if(myAnim==null)
			{
				X.sopln("No anim to play");
				return "false";
			}
			int xC=(int)X.dpd(Y.cut(p,',',1).trim());
			int yC=(int)X.dpd(Y.cut(p,',',2).trim());
			X.sopln("Drawing anim");
			GIFAnim an=new GIFAnim(myAnim,xC,yC,GB);
			GB.getContentPane().add(an);
			GB.setSize(GB.WID,GB.HEI+GB.fB);
			GB.pack();
			an.setVisible(true);
			an.validate();
			an.repaint();
			GB.repaint();
			return "true";
		}
	};
	
	public Skill(String skill,int l)
	{
		level=new IntData(l);
		//name=skill;
		skillFile=new File("deckheroes/data/skills/"+skill);
		loadData();
	}
	public Skill(String s) {this(Y.cut(s,' ',1).trim(),X.ipi(Y.cut(s,' ',2).trim()));}
	public Skill(String s,Card c) {this(s);setCard(c);}
	
	public void loadData()
	{
		if(skillFile==null)
		{
			X.sopln("Warning. Couldn't load file. Skill File is null");
			return;
		}
		if(!skillFile.exists())
			throw new SkillDoesNotExistException(skillFile.getPath());
		
		try
		{
			BufferedReader br=new BufferedReader(new FileReader(skillFile));
			name=br.readLine().trim();
			desc=new StringData(br.readLine().trim());
			macro=new Macro(macroPath+br.readLine().trim());
			macro.addFunction(ANIMATE);
			invoke=new IntData(X.ipi(br.readLine().trim()));
			target=new IntData(X.ipi(br.readLine().trim()));
			negatable=new IntData(X.ipi(br.readLine().trim()));
			nA=X.ipi(br.readLine().trim());
			try 
			{
				String fn=br.readLine();
				myAnim=Toolkit.getDefaultToolkit().createImage("deckheroes/data/images/anims/"+fn);
			}
			catch(Exception e) {}
		}
		catch(IOException e) {e.printStackTrace(); return;}
		
	}
	
	public void setGameBoard(GameBoard gb) {GB=gb;}
	public GameBoard getGameBoard() {return GB;}
	public void setCard(Card c) {myCard=c;}
	public Card getCard() {return myCard;}
	public void describe()
	{
		X.sopln();
		X.sop("Name: ","yellow");
		X.sopln(getName(),"red");
		X.sop("Level: ","yellow");
		X.sopln(getLevel(),"red");
		X.sopln(desc.get(),"cyan");
		if(!isNegatable())
			X.sopln("This Skill cannot be negated","red");
	}
	public void setData(DataSet ds) {macro.setData(ds);}
	
	public int getActiCode() {return invoke.get();}
	public int getLevel() {return level.get();}
	public String getName() {return name;}
	public Macro getMacro() {return macro;}
	public int getTarget() {return target.get();}
	public boolean isNullActivatable() {return nA==1;}
	public boolean isNegatable() {return (negatable.get()==1);}
	public Image getAnim() {return myAnim;}
	
	public boolean satisfies(String filter) // Bu*,target=2
	{
		Pattern pat=Pattern.compile(Y.cut(filter,',',1).trim());
		int nf=Strings.countChar(filter,',');
		String par;
		for(int i=1;i<=nf;i++)
		{
			par=Y.cut(filter,',',i+1).trim();
			if(!satisfiesParam(par))
				return false;
		}
		Matcher m=pat.matcher(getName());
		return m.matches();
	}
	
	public boolean satisfiesParam(String param)
	{
		param=param.replace("target",target.get()+"");
		param=param.replace("invoke",invoke.get()+"");
			//return X.ipi(Y.cut(param,'=',2).trim())==target.get();
			//return X.ipi(Y.cut(param,'=',2).trim())==invoke.get();
		try {return maths.Maths.stringCondition(param);}
		catch(NumberFormatException e)
		{
			X.sopln("Warning. Unknown parameter for skill match: ","yellow");
			X.sopln(param,"red");
			return false;
		}
		
	}
	
	public boolean equals(Skill s2) {return ((s2.getName().equals(getName())) && myCard==s2.myCard);}
}

class SkillDoesNotExistException extends RuntimeException
{
	public SkillDoesNotExistException()
	{
		X.sopln("Skill does not exist!!","red");
	}
	public SkillDoesNotExistException(String cn)
	{
		this();
		X.sopln("Skill name: "+cn,"yellow");
	}
}
