package deckheroes;
import commons.X;
import java.io.*;
import upgrade.ArrayFx;
import maths.Maths;
import commons.macros.*;
import commons.macros.data.*;
import java.awt.image.*;
import java.awt.*;
import javax.imageio.ImageIO;

public class Card
{
	public static final int DEFAULT_LEVEL=10,ICON_SIZE=48,HEIGHT=200,WIDTH=125,HGT_MAR=20,WID_MAR=13,HERO_COST=100;
	public static Image CARDBACK,FIREFRAME,WATERFRAME,AIRFRAME,EARTHFRAME;
	public static final Font CARD_TEXT_FONT=new Font("Times New Roman",Font.PLAIN,15);
	public final int ID;
	private static int idC=0;
	final String name;
	IntData HP,MaxHP,baseATT,cost,level,stars,waitR,cWaitR,ATT;
	IntData curRound=new IntData(0);
	Skill sk1,sk2,sk3;
	StringData faction;
	final File cardFile;
	File iconFile;
	Image icon;
	BufferedImage cardImage;
	public Point loc=null;
	StatusEffect[] stateffs=new StatusEffect[0];
	
	static
	{
		try 
		{
			CARDBACK=ImageIO.read(new File("deckheroes/data/images/card_back.png"));
			FIREFRAME=ImageIO.read(new File("deckheroes/data/images/fire.png"));
			WATERFRAME=ImageIO.read(new File("deckheroes/data/images/water.png"));
			AIRFRAME=ImageIO.read(new File("deckheroes/data/images/air.png"));
			EARTHFRAME=ImageIO.read(new File("deckheroes/data/images/earth.png"));
		} catch(Exception e) {e.printStackTrace(); X.sopln("Could not initialize deckheroes. Card class");}
	}
	
	public Card(IntData hp,IntData maxhp)
	{
		ID=-1;
		HP=hp;
		MaxHP=maxhp;
		level=new IntData(10);
		ATT=new IntData(0);
		baseATT=new IntData(0);
		cost=new IntData(HERO_COST);
		stars=new IntData(5);
		waitR=new IntData(0);
		cWaitR=waitR;
		sk1=sk2=sk3=null;
		name="Hero";
		faction=new StringData("neutral");
		cardFile=null;
	}
	public Card(String n) {this(n,DEFAULT_LEVEL);}
	public Card(String n,int lev)
	{
		ID=idC++;
		name=n.replace("  "," ");
		cardFile=new File("deckheroes/data/cards/"+name.replace(" ","_")+".data");
		level=new IntData(lev);
		loadData();
		//X.sopln(iconFile.getAbsolutePath());
	}
	
	public void loadData()
	{
		if(cardFile==null)
		{
			X.sopln("Warning. Couldn't load file. Card File is null");
			return;
		}
		if(!cardFile.exists())
			throw new CardDoesNotExistException(name);
		
		try
		{
			BufferedReader br=new BufferedReader(new FileReader(cardFile));
			br.readLine();
			int hp=X.ipi(br.readLine().trim()),att=X.ipi(br.readLine().trim());
			hp+=(hp/10)*level.get();
			att+=(att/9)*level.get();
			HP=new IntData(hp);
			MaxHP=new IntData(hp);
			baseATT=new IntData(att);
			ATT=new IntData(att);
			stars=new IntData(X.ipi(br.readLine().trim()));
			faction=new StringData(br.readLine().trim());
			sk1=new Skill(br.readLine(),this);
			sk2=new Skill(br.readLine(),this);
			sk3=new Skill(br.readLine(),this);
			cost=new IntData(X.ipi(br.readLine().trim()));
			waitR=new IntData(X.ipi(br.readLine().trim()));
			iconFile=new File("deckheroes/data/images/"+br.readLine().trim());
			icon=ImageIO.read(iconFile);
			cardImage=preCreateCardImage();
		}
		catch(IOException e) {e.printStackTrace();return;}
	}
	
	public void setLocation(int x,int y) {setLocation(new Point(x,y));};
	public void setLocation(Point pt) {loc=pt;}
	public boolean addStatusEffect(StatusEffect se) 
	{
		int nE=getSECount(se);
		if(nE>=se.getStackLimit())
			return false;
		stateffs=ArrayFx.append(stateffs,se);
		return true;
	}
	public StatusEffect[] getStatusEffects() {return stateffs;}
	public Point getLocation() {return loc;}
	
	public void fix()
	{
		if(HP.get()>MaxHP.get())
			HP.set(MaxHP.get());
		if(HP.get()<0)
			HP.set(0);
		if(ATT.get()<1)
			ATT.set(1);
		if(cWaitR.get()<0)
			cWaitR.set(0);
	}
	public int getHP() {return HP.get();}
	public int getMaxHP() {return MaxHP.get();}
	public int getATT() {return ATT.get();}
	public String getFaction() {return faction.get();}
	public Skill getSkill1() {return sk1;}
	public Skill getSkill2() {if(level.get()<5) return null; return sk2;}
	public Skill getSkill3() {if(level.get()<10) return null; return sk3;}
	public Skill[] getSkills() 
	{
		Skill[] ret=new Skill[] {sk1,null,null};
		if(level.get()>=5)
			ret[1]=sk2;
		if(level.get()>=10)
			ret[2]=sk3;
		return ret;
	}
	private BufferedImage preCreateCardImage()
	{
		BufferedImage img=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB);
		final Graphics g=img.createGraphics();
		g.drawImage(getFactionFrame(),0,0,null);
		g.drawImage(icon,(WIDTH-ICON_SIZE)/2,ICON_SIZE+5,null);
		g.setFont(decideFont());
		g.setColor(Color.black);
		g.drawString(name,WID_MAR+2,ICON_SIZE-15);
		g.setFont(draw.AWT.MediumFont);
		g.drawString(getStarString(),WID_MAR,ICON_SIZE+6);
		g.drawString(level.get()+"",WIDTH-21-WID_MAR,HEIGHT-HGT_MAR);
		try {ImageIO.write(img,"PNG",new File("/home/venkata/testimg.png"));} catch(Exception e) {e.printStackTrace();} finally {return img;}
	}
	public final Image getFactionFrame()
	{
		if(faction.get().equals("fire"))
			return FIREFRAME;
		else if(faction.get().equals("water"))
			return WATERFRAME;
		else if(faction.get().equals("air"))
			return AIRFRAME;
		else if(faction.get().equals("earth"))
			return EARTHFRAME;
		else
			throw new UnknownDataPointException("faction",faction.get());
	}
	public BufferedImage createCardImage()
	{
		BufferedImage img=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB);
		final Graphics g=img.createGraphics();
		g.drawImage(cardImage,0,0,null);
		g.setFont(CARD_TEXT_FONT);
		g.setColor(Color.black);
		g.drawString("HP: "+HP.get()+"/"+MaxHP.get(),WID_MAR,2*ICON_SIZE+20);
		g.drawString("ATT: "+ATT.get(),WID_MAR,2*ICON_SIZE+20*2);
		int xC=WID_MAR,yC=HEIGHT-HGT_MAR;
		for(StatusEffect se : stateffs)
		{
			g.drawImage(se.getIcon(),xC,yC,null);
			xC+=StatusEffect.ICON_SIZE+1;
		}
		return img;
	}
	
	public String toString() {return getName()+" "+level.get();}
	public void describe()
	{
		X.sopln(name+"\t"+getStarString(),"red");
		X.sop("Level: ","yellow");
		X.sopln(level.get(),"cyan");
		X.sop("HP:","yellow"); X.sopln(HP.get()+"/"+MaxHP.get(),"red");
		X.sop("ATT: ","yellow"); X.sopln(ATT.get(),"red");
		X.sop("Cost: ","yellow"); X.sopln(cost.get(),"red");
		X.sop(sk1.getName(),"red");
		X.sopln(" "+sk1.getLevel(),"green");
		X.sop(sk2.getName(),"red");
		X.sopln(" "+sk2.getLevel(),"green");
		X.sop(sk3.getName(),"red");
		X.sopln(" "+sk3.getLevel(),"green");
		X.sop("Waiting rounds: ","yellow");
		X.sopln(waitR.get(),"red");
		X.sopln();
		int dN=-1;
		while(dN!=0)
		{
			X.sopln("Enter skill number for description: ");
			try {dN=X.rI();} catch(java.io.IOException e) {e.printStackTrace(); return;}
			if(dN!=0)
				getSkills()[dN-1].describe();
			else
				break;
		}
	}
	public Font decideFont()
	{
		int l=name.length();
		int eL=(WIDTH-2*WID_MAR);
		int sz=eL/l;
		sz*=2;
		if(sz>30)
			sz=30;
		return new Font("Times New Roman",Font.PLAIN,sz);
	}
	
	public String getStarString()
	{
		String s="";
		for(int i=0;i<stars.get();i++)
			s+="*";
		return s;
	}
	
	public String getName() {return name;}
	public Image getIcon() {return icon;}
	public void setWaitR(int wR) {cWaitR.set(new Integer(wR));}
	public void setWaitR(IntData wR) {setWaitR(wR.get());}
	public void decWR() {if(cWaitR.get()>0) cWaitR.set(cWaitR.get()-1);}
	public int getCWait() {return cWaitR.get();}
	public void resetWR() {cWaitR=new IntData(waitR.get());}
	public void reset()
	{
		HP.set(MaxHP.get());
		ATT.set(baseATT.get());
		stateffs=new StatusEffect[0];
	}
	public void takeDamage(int dmg)
	{
		HP.set(HP.get()-dmg);
		if(HP.get()<0)
			HP.set(0);
	}
	
	public DataSet getMacroData(boolean self)
	{
		DataSet DS=new DataSet();
		Data d;
		//modifiable directly
		HP.setName((self)?"HP":"oppHP"); DS.append(HP);
		MaxHP.setName((self)?"MaxHP":"oppMaxHP"); DS.append(MaxHP);
		ATT.setName((self)?"ATT":"oppATT"); DS.append(ATT);
		
		//NOT directly modifiable
		d=new IntData(ID,(self)?"myID":"oppID"); DS.append(d);
		d=new IntData(level.get(),(self)?"LEVEL":"oppLEVEL"); DS.append(d);
		d=new StringData(name,(self)?"NAME":"oppNAME"); DS.append(d);
		d=new StringData(faction.get(),(self)?"faction":"oppfaction"); DS.append(d);
		d=new IntData(loc.x,(self)?"myX":"oppX"); DS.append(d);
		d=new IntData(loc.y,(self)?"myY":"oppY"); DS.append(d);
		
		//d=new IntData(HP.get(),(self)?"HP":"oppHP"); DS.append(d);
		//d=new IntData(MaxHP.get(),(self)?"MaxHP":"oppMaxHP"); DS.append(d);
		//d=new IntData(ATT.get(),(self)?"ATT":"oppATT"); DS.append(d);
		return DS;
	}
	
	public void removeSE(StatusEffect se) {stateffs=ArrayFx.remove(stateffs,se);}
	public int getSECount(StatusEffect sef)
	{
		int c=0;
		for(StatusEffect se : stateffs)
		{
			if(se.equals(sef))
				c++;
		}
		return c;
	}
	public boolean isHeroCard() {return getName().equals("Hero");}
}

class CardDoesNotExistException extends RuntimeException
{
	public CardDoesNotExistException()
	{
		X.sopln("Card does not exist!!","red");
	}
	public CardDoesNotExistException(String cn)
	{
		this();
		X.sopln("Card name: "+cn,"yellow");
	}
}
class UnknownDataPointException extends RuntimeException
{
	public UnknownDataPointException()
	{
		X.sopln("deckheroes.Card encountered and unknown data point","red");
	}
	public UnknownDataPointException(String cn)
	{
		this();
		X.sopln("Data Point: "+cn,"yellow");
	}
	public UnknownDataPointException(String dp,String val)
	{
		this(dp);
		X.sopln("Value: "+val,"yellow");
	}
}
