package de.kulanoglu.pokerass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { PokerAssScreen() } }
}

@Composable fun PokerAssScreen() {
 val engine=remember{PokerEngine()}; val bets=listOf(10,20,50,100,200)
 var points by remember{mutableIntStateOf(1000)}; var bet by remember{mutableIntStateOf(20)}
 var deck by remember{mutableStateOf(engine.newDeck())}; var cards by remember{mutableStateOf(emptyList<Card>())}
 var held by remember{mutableStateOf(setOf<Int>())}; var drawing by remember{mutableStateOf(false)}
 var win by remember{mutableIntStateOf(0)}; var hand by remember{mutableStateOf(Hand.NONE)}
 val amber=Color(0xFFFFB000)
 Column(Modifier.fillMaxSize().background(Color(0xFF080808)).padding(10.dp), horizontalAlignment=Alignment.CenterHorizontally) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceEvenly) {
   listOf("ROYAL FLUSH","STRAIGHT FLUSH","VIERLING","FULL HOUSE","FLUSH","STRAIGHT","DRILLING","ZWEI PAAR","EIN PAAR").forEach { label ->
    Text(text=label, modifier=Modifier.border(1.dp,Color(0xFF684600)).padding(5.dp), color=if(label==handLabel(hand)) Color.Yellow else amber, fontSize=10.sp, fontWeight=FontWeight.Bold)
   }
  }
  Spacer(Modifier.height(12.dp))
  Row(Modifier.weight(1f), verticalAlignment=Alignment.CenterVertically) {
   Row(Modifier.weight(1f), horizontalArrangement=Arrangement.SpaceEvenly) {
    repeat(5){ i -> CardSlot(cards.getOrNull(i),i in held,drawing){ held=if(i in held) held-i else held+i } }
   }
   Column(Modifier.width(88.dp),horizontalAlignment=Alignment.CenterHorizontally) {
    listOf(5000,2000,1000,500,200,100,50,20,10,0).forEach { level ->
     Text(text=level.toString(), modifier=Modifier.padding(1.dp).border(1.dp,Color(0xFF684600)).padding(horizontal=7.dp,vertical=2.dp), color=amber, fontSize=13.sp)
    }
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
   Led("PUNKTE",points); Led("EINSATZ",bet); Led("GEWINN",win)
   Button(onClick={bet=bets[(bets.indexOf(bet)+1)%bets.size]},enabled=!drawing,colors=ButtonDefaults.buttonColors(containerColor=Color.DarkGray)){Text(text="EINSATZ")}
   Button(onClick={
    if(!drawing){
     if(points<bet){points=1000;win=0;hand=Hand.NONE}
     else {points-=bet;deck=engine.newDeck();cards=deck.take(5);deck=deck.drop(5).toMutableList();held=emptySet();win=0;hand=Hand.NONE;drawing=true}
    } else {
     val d=deck.toMutableList(); val next=cards.mapIndexed{i,c->if(i in held)c else d.removeAt(0)}
     cards=next; deck=d; hand=engine.evaluate(next); win=bet*hand.multiplier; points+=win; drawing=false
    }
   },colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF9E1717)),modifier=Modifier.size(118.dp,50.dp)){
    Text(text=if(drawing)"ZIEHEN" else "GEBEN",fontWeight=FontWeight.Bold)
   }
  }
 }
}

@Composable private fun CardSlot(card:Card?,held:Boolean,enabled:Boolean,onHold:()->Unit) {
 val isRed=card?.suit==Suit.HEARTS||card?.suit==Suit.DIAMONDS
 Column(horizontalAlignment=Alignment.CenterHorizontally) {
  Box(Modifier.size(98.dp,136.dp).background(Color.White,RoundedCornerShape(6.dp)).border(if(held)4.dp else 2.dp,if(held)Color(0xFFFFB000) else Color.Gray,RoundedCornerShape(6.dp)).clickable(enabled=enabled){onHold()},contentAlignment=Alignment.Center) {
   Text(text=if(card==null)"★" else card.rank.label+"\n"+card.suit.symbol,color=if(isRed)Color.Red else Color.Black,fontSize=31.sp,fontWeight=FontWeight.Bold)
  }
  Spacer(Modifier.height(4.dp))
  Box(Modifier.width(72.dp).height(22.dp).background(if(held)Color(0xFFFFB000) else Color(0xFF282828),RoundedCornerShape(4.dp)).clickable(enabled=enabled){onHold()},contentAlignment=Alignment.Center) {
   Text(text=if(held)"HALTEN" else "HALT",color=if(held)Color.Black else Color.LightGray,fontSize=10.sp,fontWeight=FontWeight.Bold)
  }
 }
}
@Composable private fun Led(label:String,value:Int){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(text=label,color=Color(0xFFFFB000),fontSize=9.sp);Text(text=value.toString().padStart(5,'0'),color=Color(0xFFFF3D00),fontSize=22.sp,fontWeight=FontWeight.Bold)}}
private fun handLabel(h:Hand)=when(h){Hand.ROYAL_FLUSH->"ROYAL FLUSH";Hand.STRAIGHT_FLUSH->"STRAIGHT FLUSH";Hand.FOUR->"VIERLING";Hand.FULL_HOUSE->"FULL HOUSE";Hand.FLUSH->"FLUSH";Hand.STRAIGHT->"STRAIGHT";Hand.THREE->"DRILLING";Hand.TWO_PAIR->"ZWEI PAAR";Hand.PAIR->"EIN PAAR";else->""}
