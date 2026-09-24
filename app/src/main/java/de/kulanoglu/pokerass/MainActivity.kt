package de.kulanoglu.pokerass

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.*
import kotlin.random.Random
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{PokerAssScreen()}}
}
@Composable fun PokerAssScreen(){
 val engine=remember{PokerEngine()}; val bets=listOf(10,20,50,100,200); 
 var points by remember{mutableIntStateOf(1000)};var bet by remember{mutableIntStateOf(20)}
 var deck by remember{mutableStateOf(engine.newDeck())};var cards by remember{mutableStateOf(emptyList<Card>())};var held by remember{mutableStateOf(setOf<Int>())}
 var drawing by remember{mutableStateOf(false)};var win by remember{mutableIntStateOf(0)};var hand by remember{mutableStateOf(Hand.NONE)};var shuffling by remember{mutableStateOf(false)};var ladderLight by remember{mutableIntStateOf(-1)};var riskAvailable by remember{mutableStateOf(false)};var riskRunning by remember{mutableStateOf(false)}
 val amber=Color(0xFFFFB000);val scope=rememberCoroutineScope()
 suspend fun dealAnimated(newDeck:MutableList<Card>){
  shuffling=true; cards=emptyList(); repeat(8){mechanicalClick();delay(45)}
  var shown=emptyList<Card>(); repeat(5){i->shown=shown+newDeck[i];cards=shown;mechanicalClick();delay(100)}
  shuffling=false
 }
 Column(Modifier.fillMaxSize().background(Color(0xFF080808)).padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){listOf("ROYAL FLUSH","STRAIGHT FLUSH","VIERLING","FULL HOUSE","FLUSH","STRAIGHT","DRILLING","ZWEI PAAR","EIN PAAR").forEach{label->Text(text=label,modifier=Modifier.border(1.dp,Color(0xFF684600)).padding(4.dp),color=if(label==handLabel(hand))Color.Yellow else amber,fontSize=9.sp,fontWeight=FontWeight.Bold)}}
  Row(Modifier.weight(1f).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
   Row(Modifier.weight(1f),horizontalArrangement=Arrangement.SpaceEvenly){repeat(5){i->CardSlot(cards.getOrNull(i),i in held,drawing&&!shuffling){held=if(i in held)held-i else held+i;mechanicalClick()}}}
   Column(Modifier.width(88.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("RISIKO",color=Color.Red,fontSize=11.sp,fontWeight=FontWeight.Bold);listOf(5000,2000,1000,500,200,100,50,20,10,0).forEachIndexed{i,v->Row(Modifier.fillMaxWidth(),horizontalArrangement=if(i%2==0)Arrangement.Start else Arrangement.End){Text(text="$v",modifier=Modifier.width(55.dp).border(if(i==ladderLight)3.dp else 1.dp,if(i==ladderLight)Color.Yellow else Color(0xFF684600)).padding(horizontal=5.dp,vertical=2.dp),color=if(i==ladderLight)Color.Yellow else amber,fontSize=11.sp,fontWeight=if(i==ladderLight)FontWeight.Bold else FontWeight.Normal)}}}
  }
  Row(Modifier.fillMaxWidth().height(105.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){
   Led("PUNKTE",points);Led("EINSATZ",bet);Led("GEWINN",win)
   MachineButton(if(riskAvailable)"NEHMEN" else "EINSATZ",Color.DarkGray,!drawing&&!shuffling&&!riskRunning){if(riskAvailable){points+=win;win=0;riskAvailable=false;ladderLight=-1}else bet=bets[(bets.indexOf(bet)+1)%bets.size];mechanicalClick()}
   MachineButton("RISIKO",Color(0xFF7A1111),riskAvailable&&!riskRunning){scope.launch{riskRunning=true;val path=listOf(9,8,7,6,5,4,3,2,1,0,1,2,3,4,5,6,7,8,9);repeat(2){for(x in path){ladderLight=x;mechanicalClick();delay(42)}};val stop=(0..9).random();var p=9;while(p!=stop){p=if(p==0)9 else p-1;ladderLight=p;mechanicalClick();delay(70L+(9-p)*18L)};val values=listOf(5000,2000,1000,500,200,100,50,20,10,0);win=values[stop];riskAvailable=win>0;riskRunning=false}}
   MachineButton(if(drawing)"ZIEHEN" else "GEBEN",Color(0xFF9E1717),!shuffling){
    scope.launch{
     if(!drawing){
      if(points<bet){points=1000;win=0;hand=Hand.NONE}
      points-=bet;val nd=engine.newDeck();deck=nd;held=emptySet();win=0;hand=Hand.NONE;dealAnimated(nd);deck=nd.drop(5).toMutableList();drawing=true
     }else{
      val d=deck.toMutableList();var next=cards
      for(i in 0..4)if(i !in held){delay(90);next=next.toMutableList().also{it[i]=d.removeAt(0)};cards=next;mechanicalClick()}
      deck=d;hand=engine.evaluate(next);win=bet*hand.multiplier;drawing=false;riskAvailable=win>0
      mechanicalClick()
     }
    }
   }
  }
 }
}
@Composable private fun MachineButton(label:String,color:Color,enabled:Boolean,onClick:()->Unit){Button(onClick=onClick,enabled=enabled,colors=ButtonDefaults.buttonColors(containerColor=color),modifier=Modifier.width(64.dp).height(118.dp),contentPadding=PaddingValues(4.dp)){Text(text=label,fontSize=11.sp,fontWeight=FontWeight.Bold)}}
@Composable private fun CardSlot(card:Card?,held:Boolean,enabled:Boolean,onHold:()->Unit){val isRed=card?.suit==Suit.HEARTS||card?.suit==Suit.DIAMONDS;Column(horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.width(62.dp).height(92.dp).background(if(card==null)Color(0xFF173A24) else Color.White,RoundedCornerShape(6.dp)).border(if(held)4.dp else 2.dp,if(held)Color(0xFFFFB000) else Color.Gray,RoundedCornerShape(6.dp)).clickable(enabled=enabled){onHold()},contentAlignment=Alignment.Center){if(card==null)Text(text="POKER\nASS",color=Color(0xFFFFB000),fontSize=16.sp,fontWeight=FontWeight.Bold)else Text(text=card.rank.label+"\n"+card.suit.symbol,color=if(isRed)Color.Red else Color.Black,fontSize=24.sp,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(3.dp));Box(Modifier.width(58.dp).height(20.dp).background(if(held)Color(0xFFFFB000)else Color(0xFF282828),RoundedCornerShape(4.dp)).clickable(enabled=enabled){onHold()},contentAlignment=Alignment.Center){Text(text=if(held)"HALTEN"else"HALT",color=if(held)Color.Black else Color.LightGray,fontSize=9.sp,fontWeight=FontWeight.Bold)}}}
@Composable private fun Led(label:String,value:Int){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(text=label,color=Color(0xFFFFB000),fontSize=9.sp);Text(text=value.toString().padStart(5,'0'),color=Color(0xFFFF3D00),fontSize=21.sp,fontWeight=FontWeight.Bold)}}
private fun handLabel(h:Hand)=when(h){Hand.ROYAL_FLUSH->"ROYAL FLUSH";Hand.STRAIGHT_FLUSH->"STRAIGHT FLUSH";Hand.FOUR->"VIERLING";Hand.FULL_HOUSE->"FULL HOUSE";Hand.FLUSH->"FLUSH";Hand.STRAIGHT->"STRAIGHT";Hand.THREE->"DRILLING";Hand.TWO_PAIR->"ZWEI PAAR";Hand.PAIR->"EIN PAAR";else->""}

private fun mechanicalClick(){
 val sr=12000;val n=720;val data=ShortArray(n)
 for(i in 0 until n){val t=i.toDouble()/sr;val env=exp(-42.0*t);val noise=Random.nextDouble(-1.0,1.0);val thump=sin(2.0*PI*105.0*t);data[i]=((noise*.52+thump*.28)*env*Short.MAX_VALUE).toInt().coerceIn(-32768,32767).toShort()}
 val a=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
 val f=AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
 AudioTrack.Builder().setAudioAttributes(a).setAudioFormat(f).setBufferSizeInBytes(data.size*2).setTransferMode(AudioTrack.MODE_STATIC).build().also{it.write(data,0,data.size);it.setNotificationMarkerPosition(data.size);it.setPlaybackPositionUpdateListener(object:AudioTrack.OnPlaybackPositionUpdateListener{override fun onMarkerReached(t:AudioTrack){t.release()};override fun onPeriodicNotification(t:AudioTrack){}});it.play()}
}
