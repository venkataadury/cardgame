package deckheroes;
import commons.X;
import maths.Maths;
import upgrade.ArrayFx;

public class CardStack
{
	public Card[] cards=new Card[0];
	
	public CardStack() {}
	public CardStack(Card c) {this(new Card[] {c});}
	public CardStack(Card[] ca) {cards=ca;}
	
	public Card getLastCard() {if(cards.length==0) return null;return cards[cards.length-1];}
	public Card getFirstCard() {if(cards.length==0) return null;return cards[0];}
	
	public Card removeAndGetLastCard()
	{
		Card c=getLastCard();
		removeLastCard();
		return c;
	}
	
	public void addCard(Card c) {cards=ArrayFx.append(cards,c);}
	public void removeCard(Card c) {cards=ArrayFx.remove(cards,c);}
	public void removeFirstCard() {cards=ArrayFx.remove(cards,0);}
	public void removeLastCard() {if(cards.length==0) return; cards=ArrayFx.remove(cards,cards.length-1);}
	
	public boolean isEmpty() {return cards.length==0;}
	public int getLength() {return cards.length;}
	public Card[] getCards() {return cards;}
	public Card getCard(int i) {if(i<0) return null;return cards[i];}
	public void setCards(Card[] ca) {cards=ca;}
	public int getCardCount()
	{
		int c=0;
		for(Card crd : cards)
		{
			if(crd==null)
				continue;
			c++;
		}
		return c;
	}
	
	public Card deleteCard(int i) {Card temp=cards[i]; cards[i]=null; return temp;}
	public boolean contains(Card c)
	{
		for(Card card : cards)
		{
			if(c==card)
				return true;
		}
		return false;
	}
	public void sweep()
	{
		int l1=getLength();
		for(int i=0;i<cards.length;i++)
		{
			if(cards[i]==null)
			{
				cards=ArrayFx.remove(cards,i);
				break;
			}
		}
		int l2=getLength();
		if(l1!=l2)
			sweep();
	}
	
	public void moveLastCard(CardStack cs2)
	{
		Card c=removeAndGetLastCard();
		cs2.addCard(c);
	}
	public int indexOf(Card c)
	{
		for(int i=0;i<cards.length;i++)
		{
			if(cards[i]==c)
				return i;
		}
		return -1;
	}
	
	//Special Operations
	public void schuffle()
	{
		if(getLength()==0 || getLength()==1)
			return;
		Card[] cds=new Card[getLength()];
		int c=0,n;
		while(c<getLength())
		{
			n=Maths.randInt(getLength());
			if(cds[n-1]==null)
				cds[n-1]=cards[c++];
		}
		cards=cds;
	}
	public Card getRandomCard()
	{
		if(getLength()<=1)
			return getLastCard();
		int r=Maths.randInt(getLength());
		Card ret= getCard(r-1);
		if(ret==null)
			return getRandomCard();
		return ret;
	}
	public Card removeAndGetRandomCard()
	{
		Card c=getRandomCard();
		cards=ArrayFx.remove(cards,c);
		return c;
	}
	public void mergeInto(CardStack cs2)
	{
		cs2.cards=ArrayFx.join(cs2.cards,this.cards);
		this.cards=new Card[0];
	}
	public void decTurn()
	{
		for(Card c : cards)
			c.decWR();
	}
}
