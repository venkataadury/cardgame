package deckheroes;
import commons.X;
import commons.Y;
import maths.Maths;
import upgrade.ArrayFx;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import commons.macros.data.*;
import commons.macros.*;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JButton;
import draw.S;

public class GameBoard extends JFrame implements MouseListener,ActionListener
{
	public static final Color FADE=new Color(0.5f,0.5f,0.5f,0.5f);
	public static final long ROUND_TIME=1000; // Time in milliseconds for which a round must last
	public static final int HERO_HP=2700,INDEX_DEATH=1000;
	public static final int WID=1024,HEI=768,MARGINX=30,MARGINY=50,smallY=50,fB=30,ext=75,offset=25;
	private final Deck myDeck,oppDeck;
	private Deck myCurDeck,oppCurDeck,activeCurDeck;
	private CardStack myPlay,oppPlay,myHand,oppHand,myGrave,oppGrave,activePlay,activeHand,activeGrave;
	private CardStack myNewCards,oppNewCards,src=new CardStack(),targ=new CardStack();
	private IntData myHeroHP,myHeroMaxHP,oppHeroHP,oppHeroMaxHP;
	private IntData round=new IntData(0); // ?0 or 1?
	private boolean turn=true,sTurn=true,lock=false; //MyTurn TODO: Add in setup for starting player (turn=current turn ; sTurn=starting turn)
	private boolean[] myPlayed=new boolean[0],oppPlayed=new boolean[0];
	private Integer[] greyout=new Integer[0];
	private Rectangle[] myHandRects,oppHandRects;
	private Button roundButton=new Button(">>"),autoplayB=new Button("AUTO");
	private long time1,time2;
	private boolean autoplay=false,winner,running=true;
	private Graphics grp;
	public Graphics animGRP;
	public final GameBoard currentGame;
	protected AnimPane drawJP=new AnimPane();
	
	
	//In Game data
	IntData DAMAGE,DAMAGE_DEALT,MAGIC_DAMAGE=new IntData(0,"MAGIC_DAMAGE"),MAGIC_DAMAGE_DEALT=new IntData(0,"MAGIC_DAMAGE_DEALT"),SUSPEND_ATT=new IntData(0,"SUSPEND_ATT"),IMMUNITY=new IntData(0,"IMMUNITY");
	private Card srcCard=null,targCard=null,curCard,oldSrcCard=null,oldTargCard=null;
	StringData SUSPEND_SKILLS=new StringData("","SUSPEND_SKILLS"),CURRENT_EFFECT=new StringData(null,"STATUS_EFFECT");
	Skill curSkill;
	Image ANIM=null;
	
	//For Macros
	public final MacroFunction PRINT=new MacroFunction("print_text") {
		public String exec(Macro src,String p)
		{
			String plc=Y.cut(p,',',1).trim();
			String msg=Y.cut(p,',',2).trim();
			String clr=Y.cut(p,',',3).replace("\"","").trim();
			Color c=S.getC(clr);
			grp.setColor(c);
			Point tmp;
			if(plc.equalsIgnoreCase("src"))
				tmp=srcCard.getLocation();
			else
				tmp=targCard.getLocation();
			Point stP=new Point(tmp.x+Card.WID_MAR,tmp.y+3*Card.ICON_SIZE+25);
			if(clr.equals("red") || clr.equals("green"))
			{
				stP.x+=Card.WIDTH/3;
				stP.y+=Card.ICON_SIZE/3;
			}
			grp.drawString(msg,stP.x,stP.y);
			X.halt(1);
			return msg;
		}
	};
	public final MacroFunction REBIRTH=new MacroFunction("rebirth") {
		public String exec(Macro src,String tloc)
		{
			boolean chk=tloc.equalsIgnoreCase("deck");
			CardStack tstack=(chk)?activeCurDeck:activePlay;
			if(activeGrave.getLength()<=0)
				return null;
			activeGrave.moveLastCard(tstack);
			tstack.getLastCard().reset();
			if(chk)
				tstack.schuffle();
			return null;
		}
	};
	public final MacroFunction RECYCLE=new MacroFunction("recycle") {
		public String exec(Macro src,String tloc)
		{
			boolean chk=tloc.equalsIgnoreCase("deck");
			CardStack tstack=(chk)?((turn)?myCurDeck:oppCurDeck):((turn)?myPlay:oppPlay);
			CardStack grave=(turn)?myGrave:oppGrave;
			if(grave.getLength()<=0)
				return null;
			tstack.addCard(grave.removeAndGetRandomCard());
			tstack.getLastCard().reset();
			//grave.moveLastCard(tstack);
			if(chk)
				tstack.schuffle();
			return null;
		}
	};
	public final MacroFunction BURSTGRAVE=new MacroFunction("burst_grave") {
		public String exec(Macro src,String p)
		{
			CardStack grave=(!turn)?myGrave:oppGrave;
			if(grave.getLength()<1)
				return null;
			Card rem=grave.removeAndGetLastCard();
			return rem.getName();
		}
	};
	// To be debugged
	public final MacroFunction INTERVENTION=new MacroFunction("intervention") {
		public String exec(Macro src,String p)
		{
			CardStack hand=(turn)?oppHand:myHand;
			CardStack grave=(turn)?oppGrave:myGrave;
			if(hand.getLength()<=0)
				return null;
			Card rem=hand.removeAndGetLastCard();
			oldTargCard=targCard;
			targCard=rem;
			CURRENT_EFFECT.set("Intervention");
			boolean res=Maths.stringCondition(REQUEST_MODIFY.exec(null,"targ"));
			CURRENT_EFFECT.set(null);
			if(res)
				grave.addCard(rem);
			else
				hand.addCard(rem);
			return rem.getName();
		}
	};
	//Debug end
	public final MacroFunction PERFORM_ATTACK=new MacroFunction("phy_attack") {
		public String exec(Macro sorc,String p)
		{
			p=p.trim();
			int ind=X.ipi(Y.cut(p,',',1).trim());
			int dmg=(int)X.dpd(Y.cut(p,',',2).trim());
			Card tg=targ.getCard(ind);
			if(tg!=null)
				attackDirect(srcCard,tg,src.indexOf(srcCard),dmg);
			return ""+dmg;
		}
	};
	/*public final MacroFunction SUSPEND_ATTACK=new MacroFunction("suspend_attack") {
		public String exec(Macro src,String p)
		{
			if(p.trim().equalsIgnoreCase("self"))
				
		}
	};*/
	public final MacroFunction COUNT=new MacroFunction("cardcount") {
		
		public String exec(Macro src,String p)
		{
			if(p.equals("self"))
				return activePlay.getCardCount()+"";
			else
				return ((activePlay==myPlay)?oppPlay.getCardCount():myPlay.getCardCount())+"";
		}
	};
	public final MacroFunction ADD_SE=new MacroFunction("status_effect") {
		public String exec(Macro src,String p)
		{
			String p1=Y.cut(p,',',1).trim();
			String sename=Y.cut(p,',',2).trim();
			int selevel=X.ipi(Y.cut(p,',',3).trim());
			boolean tgb=p1.equalsIgnoreCase("src");
			Card crd=(tgb)?curCard:targCard;
			StatusEffect se=new StatusEffect(sename,selevel,curSkill);
			se.setGameBoard(currentGame);
			if(crd.getSECount(se)>=se.stackLimit.get())
				return "false";
			CURRENT_EFFECT.set(se.getName());
			String res="true";
			if(!tgb)
				res=REQUEST_MODIFY.exec(null,"targ");
			CURRENT_EFFECT.set(null);
			if(Maths.stringCondition(res))
				return ""+crd.addStatusEffect(se);
			return "false";
		}
	};
	public final MacroFunction REQUEST_MODIFY=new MacroFunction("modreq") {
		public String exec(Macro src,String p)
		{
			p=p.trim();
			boolean tgb=p.equalsIgnoreCase("src");
			Card crd=(tgb)?curCard:targCard;
			if(crd==null)
				return "true";
			IMMUNITY.set(0);
			for(Skill s : crd.getSkills())
			{
				if(s.getActiCode()==10)
					activateSkill(s,crd,curCard);
			}
			return "!"+IMMUNITY.get();
		}
	};
	
	//Modifying game vars
	public final MacroFunction REPLACE_TARGET=new MacroFunction("replace_target") {
		public String exec(Macro sorc,String p)
		{
			int replID=X.ipi(p.trim());
			if(replID==-1)
				return "false";
			Card nT=getCardByID(replID);
			if(nT!=null)
				oldTargCard=targCard=nT;
			else
			{
				X.sopln("Warning: Card with ID="+replID+" not found","yellow");
				return "false";
			}
			return "true";
		}
	};
	
	public final MacroFunction[] GAME_FUNCTIONS=new MacroFunction[] {PRINT,REBIRTH,RECYCLE,BURSTGRAVE,INTERVENTION,COUNT,PERFORM_ATTACK,ADD_SE,REQUEST_MODIFY,REPLACE_TARGET};
	
	static {S.init();} //Initiate colour to name bindings (see draw.S)
	
	public GameBoard(Deck d1,Deck d2)
	{
		myDeck=d1;
		oppDeck=d2;
		myCurDeck=myDeck; oppCurDeck=oppDeck;
		setup();
		currentGame=this;
	}
	
	public final void addFunctions(Macro m)
	{
		for(MacroFunction mf : GAME_FUNCTIONS)
			m.addFunction(mf);
	}
	private void setup()
	{
		for(Card c : myDeck.getCards())
		{
			for(Skill s : c.getSkills())
			{
				addFunctions(s.getMacro());
				s.setGameBoard(this);
			}
		}
		for(Card c : oppDeck.getCards())
		{
			for(Skill s : c.getSkills())
			{
				addFunctions(s.getMacro());
				s.setGameBoard(this);
			}
		}
		myPlay=new CardStack(); oppPlay=new CardStack();
		myHand=new CardStack(); oppHand=new CardStack();
		myGrave=new CardStack(); oppGrave=new CardStack();
		myNewCards=new CardStack(); oppNewCards=new CardStack();
		myCurDeck.schuffle(); oppCurDeck.schuffle();
		myHeroHP=new IntData(HERO_HP);
		myHeroMaxHP=new IntData(HERO_HP);
		oppHeroHP=new IntData(HERO_HP);
		oppHeroMaxHP=new IntData(HERO_HP);
		this.setSize(new Dimension(WID,HEI+fB));
		this.setResizable(false);
		this.addMouseListener(this);
		this.addWindowListener(draw.AWT.WINPROPS);
		this.getContentPane().setPreferredSize(new Dimension(WID,HEI+fB));
		this.setVisible(true);
		update();
		roundButton.setBounds(MARGINX,HEI/3+fB,40,40);
		roundButton.addActionListener(this);
		roundButton.setVisible(true);
		autoplayB.setLocation(MARGINX,HEI/3+fB+45);
		autoplayB.setSize(50,20);
		autoplayB.setPreferredSize(new Dimension(50,20));
		autoplayB.addActionListener(this);
		autoplayB.setVisible(true);
		X.halt(1);
		//autoplayB.pack();
		this.add(roundButton);
		this.add(autoplayB);
		
		//this.pack();
		animGRP=drawJP.getGraphics();
	}
	
	public void repaint() {paint(this.getGraphics());}
	public void paint(Graphics g)
	{
		if(g==null)
		{
			X.sopln("NULL Graphics.","red");
			return;
		}
		this.grp=g;
		g.setColor(Color.white);
		g.fillRect(0,0,WID,HEI+fB);
		g.setColor(Color.black);
		if(!running)
		{
			g.setColor((winner)?Color.green:Color.red);
			g.setFont(draw.AWT.LargeFont);
			g.drawString(((winner)?"VICTORY!!":"DEFEAT!!"),150,150);
			return;
		}
		g.drawRect(MARGINX+ext,MARGINY+fB,WID-2*MARGINX-ext,smallY);
		g.drawRect(MARGINX+ext,HEI-MARGINY-smallY+fB,WID-2*MARGINX-ext,smallY);
		g.drawString("Hero HP: "+myHeroHP.get()+"/"+myHeroMaxHP.get(),WID/2,HEI-smallY/2+fB);
		g.drawString("Hero HP: "+oppHeroHP.get()+"/"+oppHeroMaxHP.get(),WID/2,2*smallY/3+fB);
		g.setColor(getHPColor(myHeroHP.get(),myHeroMaxHP.get()));
		g.fillRect(MARGINX+ext,HEI-MARGINY+fB+2,(int)((WID-2*MARGINX-ext)*((double)myHeroHP.get()/myHeroMaxHP.get())),6);
		g.setColor(getHPColor(oppHeroHP.get(),oppHeroMaxHP.get()));
		g.fillRect(MARGINX+ext,MARGINY+fB-8,(int)((WID-2*MARGINX-ext)*((double)oppHeroHP.get()/oppHeroMaxHP.get())),6);
		g.setColor(Color.black);
		g.drawString("Round: "+round.get(),MARGINX,HEI/2+fB);
		
		if(!myCurDeck.isEmpty())
			g.drawImage(Card.CARDBACK,MARGINX,HEI-smallY-MARGINY+fB-25,null);
		if(!oppCurDeck.isEmpty())
			g.drawImage(Card.CARDBACK,MARGINX,MARGINY+fB+25,null);
		int xC=MARGINX+2+ext,yC=HEI-MARGINY-smallY+fB+1,K=0;
		myHandRects=new Rectangle[myHand.getLength()];
		oppHandRects=new Rectangle[oppHand.getLength()];
		myPlayed=new boolean[myHand.getLength()];
		oppPlayed=new boolean[oppHand.getLength()];
		for(Card c : myHand.getCards())
		{
			g.drawImage(c.getIcon(),xC,yC,null);
			myHandRects[K++]=new Rectangle(xC,yC,Card.ICON_SIZE,Card.ICON_SIZE);
			c.setLocation(xC,yC-100);
			g.drawString("Wait: "+c.getCWait(),xC,yC-15);
			xC+=Card.ICON_SIZE+2;
		}
		xC=MARGINX+2+ext; yC=MARGINY+fB+1; K=0;
		for(Card c : oppHand.getCards())
		{
			g.drawImage(c.getIcon(),xC,yC,null);
			oppHandRects[K++]=new Rectangle(xC,yC,Card.ICON_SIZE,Card.ICON_SIZE);
			c.setLocation(xC,yC-120-Card.ICON_SIZE);
			g.drawString("Wait: "+c.getCWait(),xC,yC+10+smallY);
			xC+=Card.ICON_SIZE+2;
		}
		//Drawing the Cards
		xC=MARGINX+2+ext; yC=HEI-smallY-MARGINY-offset+fB-Card.HEIGHT;
		for(Card c : myPlay.getCards())
		{
			if(c!=null)
			{
				g.drawImage(c.createCardImage(),xC,yC,null);
				c.setLocation(xC,yC);
			}
			xC+=Card.WIDTH+5;
		}
		g.setColor(FADE);
		for(Card c : myNewCards.getCards())
		{
			g.drawImage(c.createCardImage(),xC,yC,null);
			g.fillRect(xC,yC,Card.WIDTH,Card.HEIGHT);
			c.setLocation(xC,yC);
			xC+=Card.WIDTH+5;
		}
		
		xC=MARGINX+2+ext; yC=smallY+MARGINY+fB+offset;
		for(Card c : oppPlay.getCards())
		{
			if(c!=null)
			{
				g.drawImage(c.createCardImage(),xC,yC,null);
				c.setLocation(xC,yC);
			}
			xC+=Card.WIDTH+5;
		}
		for(Card c : oppNewCards.getCards())
		{
			g.drawImage(c.createCardImage(),xC,yC,null);
			g.fillRect(xC,yC,Card.WIDTH,Card.HEIGHT);
			c.setLocation(xC,yC);
			xC+=Card.WIDTH+5;
		}
		if(!myGrave.isEmpty())
			g.drawImage(myGrave.getLastCard().getIcon(),MARGINX,HEI-smallY-MARGINY+fB+25,null);
		if(!oppGrave.isEmpty())
			g.drawImage(oppGrave.getLastCard().getIcon(),MARGINX,MARGINY+fB-25,null);
		g.setColor(FADE);
		g.fillRect(MARGINX,HEI-smallY-MARGINY+fB+25,Card.ICON_SIZE,Card.ICON_SIZE);
		g.fillRect(MARGINX,MARGINY+fB-25,Card.ICON_SIZE,Card.ICON_SIZE);
		//End
		Rectangle[] recs=(turn)?myHandRects:oppHandRects;
		g.setColor(FADE);
		for(Integer i : greyout)
			((Graphics2D)g).fill(recs[i.intValue()]);
		
		if(turn)
		{
			g.setColor(Color.green);
			g.fillRect(5,2*HEI/3,20,20);
		}
		else
		{
			g.setColor(Color.red);
			g.fillRect(5,HEI/3,20,20);
		}
	}
	
	private void round()
	{
		if(myHeroHP.get()<=0)
		{
			winner=false;
			running=false;
			return;
		}
		if(oppHeroHP.get()<=0)
		{
			winner=true;
			running=false;
			return;
		}
		if(autoplay)
		{
			lock=true;
			CardStack hnd,ncd;
			int ad=0;
			if(turn)
			{
				hnd=myHand;
				ncd=myNewCards;
			}
			else
			{
				hnd=oppHand;
				ncd=oppNewCards;
			}
			for(Card c : hnd.getCards())
			{
				if(c.getCWait()<=0)
				{
					ncd.addCard(c);
					ad++;
				}
			}
			if(ad>0)
			{
				update();
				X.halt(0.5);
				playCards();
				//update();
			}
			lock=false;
		}
		if((turn && !playable(myHand)) || (!turn && !playable(oppHand)) || autoplay)
			startRound();
	}
	private void startRound()
	{
		lock=true;
		myPlay.sweep();
		oppPlay.sweep();
		time1=System.currentTimeMillis();
		if(turn && myCurDeck.getLength()!=0)
		{
			myCurDeck.moveLastCard(myHand);
			myHand.getLastCard().resetWR();
		}
		if(!turn && oppCurDeck.getLength()!=0)
		{
			oppCurDeck.moveLastCard(oppHand);
			oppHand.getLastCard().resetWR();
		}
		update();
		battle();
		//Ending
		turn=!turn;
		if(turn)
			myHand.decTurn();
		else
			oppHand.decTurn();
		if(turn==sTurn)
			round=new IntData(round.get()+1);
		while(System.currentTimeMillis()-time1<ROUND_TIME)
			gWait();
		lock=false;
		greyout=new Integer[0];
		myNewCards=new CardStack(); oppNewCards=new CardStack(); // ?Check?
		update();
	}
	private void battle() 
	{
		if(turn)
		{
			src=myPlay;
			targ=oppPlay;
		}
		else
		{
			src=oppPlay;
			targ=myPlay;
		}
		check();
		for(int i=0;i<src.getLength();i++)
			playRound(src.getCard(i),i);
	}
	
	private void playRound(Card c,int ind)
	{
		if(c==null)
			return;
		SUSPEND_ATT=new IntData(0,"SUSPEND_ATT");
		SUSPEND_SKILLS.set("");
		oldSrcCard=srcCard;
		curCard=srcCard=c;
		for(StatusEffect se : c.getStatusEffects())
		{
			if(se==null)
				continue;
			if(se.getActiCode()==0)
				invokeStatusEffect(se,c);
		}
		Skill s1=c.getSkill1(),s2=c.getSkill2(),s3=c.getSkill3();
		int aC1,aC2,aC3;
		if(s1==null)
			return;
		aC1=s1.getActiCode();
		if(aC1==0)
			activateSkill(s1,c,ind);
		update();
		s2=c.getSkill2();
		if(s2==null)
			return;
		aC2=s2.getActiCode();
		if(aC2==0)
			activateSkill(s2,c,ind);
		update();
		s3=c.getSkill3();
		if(s3==null)
			return;
		aC3=s3.getActiCode();
		if(aC3==0)
			activateSkill(s3,c,ind);
		check();
		update();
		
		int cATT=c.getATT();
		if(aC1==1)
			activateSkill(s1,c,ind);
		if(aC2==1)
			activateSkill(s2,c,ind);
		if(aC3==1)
			activateSkill(s3,c,ind);
		int dATT=c.ATT.get()-cATT;
		update();
		//X.halt(10);
		src=getContainingPlay(c);
		targ=(src==myPlay)?oppPlay:myPlay;
		Card targOpp=(targ.getLength()-1<ind)?null:targ.getCard(ind);
		if(targOpp==null)
			targOpp=getOppHeroCard();
		attackDirect(c,targOpp,ind);
		c.ATT.set(c.ATT.get()-dATT);
		for(StatusEffect se : c.getStatusEffects())
		{
			if(se==null)
				continue;
			if(se.getActiCode()==2)
				invokeStatusEffect(se,c);
		}
		SUSPEND_SKILLS.set("");
		srcCard=oldSrcCard;
	}
	private void invokeStatusEffect(StatusEffect se,Card source)
	{
		if(round.get()-se.getLastExec()<se.freq.get())
			return;
		DataSet d=source.getMacroData(true);
		d.append(se.level); d.append(SUSPEND_ATT); d.append(SUSPEND_SKILLS);
		se.getMacro().setData(d); se.exec();
		se.setLastExec(round.get());
		if(se.execC>=se.limit.get() && se.limit.get()!=0)
			source.removeSE(se);
	}
	private void activateSkill(Skill sk,Card source,int ind)
	{
		X.sopln("SUSPEND_SKILLS: "+SUSPEND_SKILLS.get(),"yellow");
		if(!SUSPEND_SKILLS.get().equals("") && sk.isNegatable())
		{
			if(sk.satisfies(SUSPEND_SKILLS.get()))
				return;
		}
		src=getContainingPlay(source);
		targ=(src==myPlay)?oppPlay:myPlay;
		setActives(source);
		curSkill=sk;
		if(sk.getActiCode()!=7)
		{
			MAGIC_DAMAGE.set(0);
			MAGIC_DAMAGE_DEALT.set(0);
		}
			
		//if(sk.getActiCode()==7) // || sk.getActiCode()==8)
		//	swap(src,targ);
		Card opp=null;
		if(targ.getLength()-1<ind)
			opp=null;
		else
			opp=targ.getCard(ind);
		//if(sk.getActiCode()==7)// || sk.getActiCode()==8)
	//		swap(src,targ);
		
		CardStack curtarg=targ,cursrc=src; 
		switch(sk.getTarget())
		{
			case -1:
				runSkill(sk,source,null,sk.getActiCode());
				break;
			case 0:
				runSkill(sk,source,opp,sk.getActiCode());
				break;
			case 1:
				for(int i=0;i<curtarg.getLength();i++)
					runSkill(sk,source,curtarg.getCard(i),sk.getActiCode());
				break;
			case 2:
				for(int i=0;i<src.getLength();i++)
					runSkill(sk,source,cursrc.getCard(i),sk.getActiCode());
				break;
			case 3:
				X.sopln(sk.getName()+":\tSkill activated when played.","yellow");
				runSkill(sk,source,curtarg.getRandomCard(),sk.getActiCode());
				break;
			case 4:
				Card c=cursrc.getRandomCard();
				while(c==source)
					c=cursrc.getRandomCard();
				runSkill(sk,source,c,sk.getActiCode());
				break;
			default:
				if(sk.getTarget()>0)
					X.sopln("Unknown target option: "+sk.getTarget());
		}
		
	}
	private void activateSkill(Skill sk,Card source,Card target) {activateSkill(sk,source,(myPlay.contains(target))?myPlay.indexOf(target):oppPlay.indexOf(target));}
	private void runSkill(Skill sk,Card source,Card opp,int inv)
	{
		X.sopln(sk.getName()+" "+opp);
		if(source==null)
			return;
		if(opp==null && !sk.isNullActivatable())
			return;
// 		if(opp==null)
// 			X.sopln("Skill: "+sk.getName()+"\tNull Opponent");
		src=getContainingPlay(source);
		if(opp!=null)
			targ=getContainingPlay(opp);
		else
			targ=(src==myPlay)?oppPlay:myPlay;
		oldSrcCard=srcCard;
		oldTargCard=targCard;
		srcCard=source; targCard=opp;
		DataSet mD=source.getMacroData(true),oD=(opp!=null)?opp.getMacroData(false):new DataSet(); //oppHP dominates HP owing to Mass Heal Macro when source and target coincide
		DataSet d=DataSet.merge(mD,oD);
		d.append(new IntData(sk.getLevel(),"SKILL_LEVEL"));
		d.append(MAGIC_DAMAGE);
		d.append(SUSPEND_SKILLS);
		if(CURRENT_EFFECT!=null)
			d.append(CURRENT_EFFECT);
		if(inv==7 || inv==9)
			d.append(MAGIC_DAMAGE_DEALT);
		if(inv==10)
			d.append(IMMUNITY);
		if(inv==6 || inv==8)
			d.append(DAMAGE);
		if(inv==8)
			d.append(DAMAGE_DEALT);
		ANIM=sk.getAnim();
		sk.getMacro().setData(d); sk.getMacro().exec();
		if(MAGIC_DAMAGE.get()!=0 && inv!=7)
		{
			attackMagic(source,opp,sk);
			MAGIC_DAMAGE.set(0);
		}
		targCard=oldTargCard;
		srcCard=oldSrcCard;
	}
	private void attackDirect(Card source,Card target,int ind) {attackDirect(source,target,ind,source.getATT());}
	private void attackDirect(Card source,Card target,int ind,int dmg)
	{
		for(StatusEffect se : source.getStatusEffects())
		{
			if(se.getActiCode()==1)
				invokeStatusEffect(se,source);
		}
		if(SUSPEND_ATT.get()==1)
		{
			SUSPEND_ATT.set(0);
			PRINT.exec(null,"src,BOUND!,blue");
			return;
		}
		DAMAGE=new IntData(dmg,"DAMAGE");
		X.sopln(source.getName()+" attacking "+target.getName()+"\t DMG: "+DAMAGE.get());
		oldSrcCard=srcCard;
		oldTargCard=targCard;
		srcCard=source;
		targCard=target;
		CardStack c1=getContainingPlay(srcCard),c2=getContainingPlay(targCard);
		if(c1!=null)
			src=c1;
		if(c2!=null)
			targ=c2;
		for(Card c : targ.getCards())
		{
			if(c==null)
				continue;
			for(Skill sk : c.getSkills())
			{
				if(sk==null)
					continue;
				if(sk.getActiCode()==11)
					activateSkill(sk,c,targ.indexOf(c));
			}
		}
		update();
		target=targCard;
		if(targCard==null)
			X.sopln("Warning. Null Card","cyan");
		int oldHP=targCard.getHP();
		for(Skill sk : targCard.getSkills())
		{
			if(sk==null)
				continue;
			if(sk.getActiCode()==6)
				activateSkill(sk,targCard,source);
		}
		srcCard=source;
		targCard=target;
		src=getContainingPlay(source);
		targ=(src==myPlay)?oppPlay:myPlay;
		X.sop("After Macro: ","yellow");
		X.sopln("\t DMG: "+DAMAGE.get(),"red");
		target.takeDamage(DAMAGE.get());
		DAMAGE_DEALT=new IntData(oldHP-target.getHP(),"DAMAGE_DEALT");
		boolean attackSuccess=DAMAGE_DEALT.get()>0;
		if(attackSuccess)
			PRINT.exec(null,"targ,"+DAMAGE_DEALT.get()+",red");
		for(Skill sk : source.getSkills())
		{
			if(sk==null)
				continue;
			if(sk.getActiCode()==8 && attackSuccess)
				activateSkill(sk,source,targ.indexOf(target));
		}
		targCard=oldTargCard;
		srcCard=oldSrcCard;
		check();
		update();
	}
	private void attackMagic(Card source,Card target,Skill skill)
	{
		if(target==null)
			return;
		X.sopln(source.getName()+" attacking (magic) "+target.getName()+"\t DMG: "+MAGIC_DAMAGE.get());
		oldSrcCard=srcCard;
		oldTargCard=targCard;
		srcCard=source;
		targCard=target;
		int oldHP=target.getHP();
		for(Skill sk : target.getSkills())
		{
			if(sk==null)
				continue;
			if(sk.getActiCode()==7)
				activateSkill(sk,target,source);
		}
		srcCard=source;
		targCard=target;
		X.sop("After Macro: ","yellow");
		X.sopln("\t Magic DMG: "+MAGIC_DAMAGE.get(),"red");
		target.takeDamage(MAGIC_DAMAGE.get());
		MAGIC_DAMAGE_DEALT=new IntData(oldHP-target.getHP(),"MAGIC_DAMAGE_DEALT");
		PRINT.exec(null,"targ,"+MAGIC_DAMAGE_DEALT.get()+",red");
		for(Skill sk : source.getSkills())
		{
			if(sk==null)
				continue;
			if(sk.getActiCode()==9)
				activateSkill(sk,source,source);
		}
		targCard=oldTargCard;
		srcCard=oldSrcCard;
		check();
		update();
	}
	
	public final void gWait() {X.halt(0.01);}
	private void check()
	{
		if(myHeroHP.get()>myHeroMaxHP.get())
			myHeroHP.set(myHeroMaxHP.get());
		if(oppHeroHP.get()>oppHeroMaxHP.get())
			oppHeroHP.set(oppHeroMaxHP.get());
		for(Card c : myPlay.getCards())
		{
			if(c==null)
				continue;
			c.fix();
		}
		for(Card c : oppPlay.getCards())
		{
			if(c==null)
				continue;
			c.fix();
		}
		
		for(int i=0;i<myPlay.getLength();i++)
		{
			if(myPlay.getCard(i)==null)
				continue;
			if(myPlay.getCard(i).getHP()<=0)
				killCard(myPlay,i,myGrave,true);
		}
		for(int i=0;i<oppPlay.getLength();i++)
		{
			if(oppPlay.getCard(i)==null)
				continue;
			if(oppPlay.getCard(i).getHP()<=0)
				killCard(oppPlay,i,oppGrave,false);
		}
	}
	private void killCard(CardStack play,int ind,CardStack grave,boolean side)
	{
		grave.addCard(play.deleteCard(ind));
		Card dead=grave.getLastCard();
		src=(side)?myPlay:oppPlay; targ=(!side)?myPlay:oppPlay;
		for(Skill s : dead.getSkills())
		{
			if(s.getActiCode()==4) //On Death
				activateSkill(s,dead,INDEX_DEATH);
		}
		src=(turn)?myPlay:oppPlay; targ=(!turn)?myPlay:oppPlay;
	}
	public Card getOppHeroCard()
	{
		Card c;
		if(!turn)
		{
			c=new Card(myHeroHP,myHeroMaxHP);
			c.setLocation(WID/2,HEI-Card.HEIGHT+fB);
		}
		else
		{
			c=new Card(oppHeroHP,oppHeroMaxHP);
			c.setLocation(WID/2,fB);
		}
		return c;
	}
	
	public boolean playable(CardStack hand)
	{
		for(Card c : hand.getCards())
		{
			if(c.getCWait()<=0)
				return true;
		}
		return false;
	}
	public void update() 
	{
		if(!lock)
			round();
		paint(this.getGraphics());
	}
	public void setActives(Card source)
	{
		if(myPlay.contains(source) || myGrave.getLastCard()==source)
		{
			srcCard=source;
			activePlay=myPlay;
			activeGrave=myGrave;
			activeCurDeck=myCurDeck;
			activeHand=myHand;
		}
		else if(oppPlay.contains(source) || oppGrave.getLastCard()==source)
		{
			activePlay=oppPlay;
			activeGrave=oppGrave;
			activeCurDeck=oppCurDeck;
			activeHand=oppHand;
		}
		else
			X.sopln("WARNING: Source Card not present anywhere.","yellow");
	}
	public CardStack getContainingPlay(Card c) {if(c==null || c.isHeroCard()) return null; if(myPlay.contains(c) || myNewCards.contains(c))return myPlay;else return oppPlay;}
	public Card getCardByID(int id)
	{
		for(Card c : myPlay.getCards())
		{
			if(c.ID==id)
				return c;
		}
		for(Card c : oppPlay.getCards())
		{
			if(c.ID==id)
				return c;
		}
		return null;
	}
	
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseClicked(MouseEvent e)
	{
		if((turn && !playable(myHand)) || (!turn && !playable(oppHand)))
			return;
		Point pt=e.getPoint();
		CardStack hand,place;
		Rectangle[] chk;
		int ind=-1;
		if(turn)
		{
			hand=myHand;
			place=myNewCards;
			chk=myHandRects;
		}
		else
		{
			hand=oppHand;
			place=oppNewCards;
			chk=oppHandRects;
		}
		for(int i=0;i<chk.length;i++)
		{
			if(!chk[i].contains(pt))
				continue;
			ind=i;
			break;
		}
		if(ind==-1 || hand.getCard(ind).getCWait()>0)
			return;
		if(!isSelected(ind))
		{
			greyout=ArrayFx.append(greyout,new Integer(ind));
			place.addCard(hand.getCard(ind));
		}
		else
		{
			greyout=ArrayFx.remove(greyout,new Integer(ind));
			place.removeCard(hand.getCard(ind));
		}
		update();
	}
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource()==roundButton)
		{
			if(lock)
				return;
			X.sopln("Round Complete.");
			playCards();
			greyout=new Integer[0];
			startRound();
		}
		else if(e.getSource()==autoplayB)
		{
			roundButton.setVisible(false);
			autoplayB.setEnabled(false);
			autoplay=true;
			update();
		}
	}
	
	private void playCards()
	{
		if(turn)
		{
			for(Card c : myNewCards.getCards())
				myHand.removeCard(c);
			for(int i=0;i<myNewCards.getLength();i++)
			{
				for(Skill s : myNewCards.getCard(i).getSkills())
				{
					if(s.getActiCode()==3) //When played
						activateSkill(s,myNewCards.getCard(i),myPlay.getLength()+i);
				}
			}
			myNewCards.mergeInto(myPlay);
		}
		else
		{
			for(Card c : oppNewCards.getCards())
				oppHand.removeCard(c);
			for(int i=0;i<oppNewCards.getLength();i++)
			{
				for(Skill s : oppNewCards.getCard(i).getSkills())
				{
					if(s.getActiCode()==3) //When played
						activateSkill(s,oppNewCards.getCard(i),oppPlay.getLength()+i);
				}
			}
			oppNewCards.mergeInto(oppPlay);
		}
	}
	public boolean isSelected(int ind)
	{
		for(int i : greyout)
		{
			if(i==ind)
				return true;
		}
		return false;
	}
	
	public boolean onSameSide(Card c1,Card c2)
	{
		CardStack cs=getContainingPlay(c1);
		if(cs!=null)
			return cs.contains(c2);
		else
			return false;
	}
	public static void swap(CardStack c1,CardStack c2)
	{
		Card[] cs=c1.getCards();
		c1.setCards(c2.getCards());
		c2.setCards(cs);
	}
	
	
	public static Color getHPColor(int hp,int mhp)
	{
		if(hp<=mhp/4)
			return Color.red;
		if(hp<=mhp/2)
			return Color.yellow;
		return Color.green;
	}
}
