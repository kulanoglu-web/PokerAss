package de.kulanoglu.pokerass
enum class Suit(val symbol:String){CLUBS("♣"),DIAMONDS("♦"),HEARTS("♥"),SPADES("♠")}
enum class Rank(val value:Int,val label:String){TWO(2,"2"),THREE(3,"3"),FOUR(4,"4"),FIVE(5,"5"),SIX(6,"6"),SEVEN(7,"7"),EIGHT(8,"8"),NINE(9,"9"),TEN(10,"10"),JACK(11,"J"),QUEEN(12,"Q"),KING(13,"K"),ACE(14,"A")}
data class Card(val rank:Rank,val suit:Suit)
enum class Hand(val multiplier:Int){NONE(0),PAIR(1),TWO_PAIR(2),THREE(3),STRAIGHT(4),FLUSH(6),FULL_HOUSE(9),FOUR(25),STRAIGHT_FLUSH(50),ROYAL_FLUSH(250)}
class PokerEngine{
 private val fullDeck=Suit.entries.flatMap{s->Rank.entries.map{r->Card(r,s)}}

 fun newDeck()=fullDeck.shuffled().toMutableList()
 fun isValidDeck(cards:List<Card>)=cards.size==52&&cards.distinct().size==52
 fun isValidHand(cards:List<Card>)=cards.size==5&&cards.distinct().size==5
 fun evaluate(cards:List<Card>):Hand{
  require(isValidHand(cards))
  val v=cards.map{it.rank.value}.sorted(); val groups=v.groupingBy{it}.eachCount().values.sortedDescending()
  val flush=cards.map{it.suit}.distinct().size==1
  val straight=v.zipWithNext().all{(a,b)->b==a+1}||v==listOf(2,3,4,5,14)
  return when{
   flush&&v==listOf(10,11,12,13,14)->Hand.ROYAL_FLUSH
   flush&&straight->Hand.STRAIGHT_FLUSH
   groups.first()==4->Hand.FOUR
   groups==listOf(3,2)->Hand.FULL_HOUSE
   flush->Hand.FLUSH
   straight->Hand.STRAIGHT
   groups.first()==3->Hand.THREE
   groups.take(2)==listOf(2,2)->Hand.TWO_PAIR
   groups.first()==2->Hand.PAIR
   else->Hand.NONE
  }
 }
}