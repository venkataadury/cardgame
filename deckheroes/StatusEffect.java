package deckheroes;
import commons.X;
import upgrade.ArrayFx;
import commons.macros.*;
import commons.macros.data.*;
import java.io.*;
import java.awt.Image;
import java.awt.image.*;
import javax.imageio.ImageIO;

public class StatusEffect
{
	public static final int ICON_SIZE=20;
	public String name,desc;
	private final File datFile;
	IntData invoke,freq,limit,level,stackLimit,lastRun=new IntData(0,"LAST_RUN");
	public GameBoard gb;
	Skill source=null;
	Macro mac;
	private Image icon=null;
	public int execC=0;
	
	public StatusEffect(String n)
	{
		n=n.trim();
		datFile=new File("deckheroes/data/statuses/"+n);
		setup();
	}
	public StatusEffect(String n,int l) {this(n); level.set(l);}
	public StatusEffect(String n,int l,Skill s) {this(n,l); source=s;}
	public StatusEffect(String n,Skill s) {this(n); source=s;}
	
	private void setup()
	{
		try
		{
			BufferedReader br=new BufferedReader(new FileReader(datFile));
			String temp;
			name=br.readLine().trim();
			desc=br.readLine();
			mac=new Macro("deckheroes/data/statuses/macros/"+br.readLine().trim());
			invoke=new IntData(X.ipi(br.readLine().trim()));
			freq=new IntData(X.ipi(br.readLine().trim()));
			limit=new IntData(X.ipi(br.readLine().trim()));
			stackLimit=new IntData(X.ipi(br.readLine().trim()));
			File imgFile=new File("deckheroes/data/images/statuses/"+br.readLine().trim());
			icon=ImageIO.read(imgFile);
			level=new IntData(5,"STATUS_LEVEL");
			
		}
		catch(Exception e) {e.printStackTrace(); return;}
	}
	
	public void setSourceSkill(Skill s) {source=s;}
	public void setGameBoard(GameBoard g) {gb=g; g.addFunctions(this.getMacro());}
	
	public void setLastExec(int i) {lastRun.set(i);}
	public int getLastExec() {return lastRun.get();}
	
	public Skill getSourceSkill() {return source;}
	
	public String getName() {return name;}
	public int getActiCode() {return invoke.get();}
	public int getStackLimit() {return stackLimit.get();}
	public Macro getMacro() {return mac;}
	public void exec()
	{
		execC++;
		getMacro().exec();
	}
	
	public Image getIcon() {return icon;}
	public boolean equals(StatusEffect se2) {return (se2.source.equals(source) && name.equals(se2.name));}
}
