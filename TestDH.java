import deckheroes.*;
import commons.X;
import commons.Y;
import commons.Strings;
import upgrade.ArrayFx;

public class TestDH
{
	public static void main(String[] args)throws Exception
	{
		/*Card[] lst=new Card[] {new Card("Fire Wisp")};
		lst=ArrayFx.remove(lst,0);*/
		String crd1="Fire Wisp";
		String crd2="Blazing King";
		if(args.length<1)
		{
			X.sop("Enter Card 1 names: ");
			crd1=X.rL().trim();
		}
		else
			crd1=args[0].trim();
		if(args.length<2)
		{
			X.sop("Enter Card 2 names: ");
			crd2=X.rL().trim();
		}
		else
			crd2=args[1].trim();
		
		Card[] c1=getCards(crd1);
		Card[] c2=getCards(crd2);
		Deck d1=new Deck(c1),d2=new Deck(c2);
		
		GameBoard gb=new GameBoard(d1,d2);
	}
	
	public static Card[] getCards(String str)
	{
		int c=Strings.countChar(str,',')+1;
		Card[] ret=new Card[c];
		for(int i=1;i<=c;i++)
			ret[i-1]=new Card(Y.cut(str,',',i));
		return ret;
	}
}
