package de.kulanoglu.pokerass

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import kotlin.math.*
import kotlin.random.Random

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{PokerAssScreen()}}}

@Composable fun PokerAssScreen(){
 val engine=remember{PokerEngine()};val bets=listOf(10,20,50,100,200)
 var points by remember{mutableIntStateOf(1000)};var bet by remember{mutableIntStateOf(20)}
 var deck by remember{mutableStateOf(engine.newDeck())};var cards by remember{mutableStateOf(emptyList<Card>())};var held by remember{mutableStateOf(setOf<Int>())}
 var drawing by remember{mutableStateOf(false)};var win by remember{mutableIntStateOf(0)};var hand by remember{mutableStateOf(Hand.NONE)}
 var shuffling by remember{mutableStateOf(false)};var ladderLight by remember{mutableIntStateOf(-1)}
 var riskAvailable by remember{mutableStateOf(false)};var riskRunning by remember{mutableStateOf(false)};var stopRequested by remember{mutableStateOf(false)}
 val amber=Color(0xFFFFB000);val scope=rememberCoroutineScope()
 suspend fun dealAnimated(nd:MutableList<Card>){
  shuffling=true
  val preview=engine.newDeck()
  repeat(8){frame->
   cards=List(5){i->preview[(frame*5+i)%preview.size]}
   if(frame%2==0)mechanicalSound(MechSound.SHUFFLE)
   delay(52L+frame*5)
  }
  var settled=cards
  repeat(5){i->
   settled=settled.toMutableList().also{it[i]=nd[i]};cards=settled
   mechanicalSound(MechSound.CARD);delay(105L+i*18)
  }
  shuffling=false
 }
 Column(Modifier.fillMaxSize().background(Color(0xFF080808)).padding(horizontal=4.dp,vertical=2.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceBetween){
  Column(Modifier.fillMaxWidth().padding(bottom=1.dp)){listOf(listOf("ROYAL FLUSH","STRAIGHT FLUSH","VIERLING","FULL HOUSE","FLUSH"),listOf("STRAIGHT","DRILLING","ZWEI PAAR","EIN PAAR","NIX")).forEach{r->Row(Modifier.fillMaxWidth()){r.forEach{label->Text(label,Modifier.weight(1f).padding(2.dp).border(1.dp,Color(0xFF684600)).padding(vertical=3.dp),if(label==handLabel(hand))Color.Yellow else amber,9.sp,fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center)}}}}
  Row(Modifier.weight(1f).fillMaxWidth().border(2.dp,Color(0xFF5A3A0A),RoundedCornerShape(6.dp)).padding(3.dp),verticalAlignment=Alignment.CenterVertically){
   Row(Modifier.weight(1f).padding(horizontal=2.dp),horizontalArrangement=Arrangement.spacedBy(3.dp,Alignment.CenterHorizontally)){repeat(5){i->CardSlot(cards.getOrNull(i),i in held,drawing&&!shuffling){held=if(i in held)held-i else held+i;mechanicalSound(MechSound.BUTTON)}}}
   Column(Modifier.width(68.dp),horizontalAlignment=Alignment.CenterHorizontally){
 Text("RISIKO",color=Color(0xFFFF5722),fontSize=11.sp,fontWeight=FontWeight.Bold)
 val rv=listOf(5000,2000,1000,500,200,100,50,20,10,0)
 rv.forEachIndexed{i,v->
  val left=i%4==0||i%4==3
  Row(Modifier.fillMaxWidth(),horizontalArrangement=if(left)Arrangement.Start else Arrangement.End){
   Box(Modifier.width(52.dp).height(25.dp).background(if(i==ladderLight)Color(0xFF7A2600) else Color(0xFF241708),RoundedCornerShape(14.dp)).border(if(i==ladderLight)3.dp else 1.dp,if(i==ladderLight)Color.Yellow else Color(0xFF8A5A12),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){
    Text("$v",color=if(i==ladderLight)Color.Yellow else amber,fontSize=10.sp,fontWeight=FontWeight.Bold)
   }
  }
 }
}
  }
  Column(Modifier.fillMaxWidth().padding(top=2.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){Led("PUNKTE",points);Led("EINSATZ",bet);Led("GEWINN",win)};Row(Modifier.fillMaxWidth().height(70.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){
   MachineButton(if(riskAvailable)"NEHMEN" else "EINSATZ",Color.DarkGray,!drawing&&!shuffling&&!riskRunning){if(riskAvailable){val target=points+win;riskAvailable=false;riskRunning=true;scope.launch{while(points<target){val step=((target-points)/18).coerceAtLeast(1);points=(points+step).coerceAtMost(target);mechanicalSound(MechSound.RELAY);delay(38)};win=0;ladderLight=-1;riskRunning=false}}else bet=bets[(bets.indexOf(bet)+1)%bets.size];mechanicalSound(MechSound.BUTTON)}
   MachineButton(if(riskRunning)"STOP" else "RISIKO",Color(0xFF7A1111),riskAvailable||riskRunning){if(riskRunning){stopRequested=true;mechanicalSound(MechSound.BUTTON)}else{scope.launch{riskRunning=true;stopRequested=false;var p=9;while(!stopRequested){p=if(p==0)9 else p-1;ladderLight=p;mechanicalSound(MechSound.RELAY);delay(if(p%2==0)64 else 78)};var pause=92L;repeat(4){p=if(p==0)9 else p-1;ladderLight=p;mechanicalSound(MechSound.RELAY);delay(pause);pause+=55};val values=listOf(5000,2000,1000,500,200,100,50,20,10,0);win=values[p];riskAvailable=win>0;riskRunning=false;stopRequested=false}}}
   MachineButton(if(drawing)"ZIEHEN" else "GEBEN",Color(0xFF9E1717),!shuffling){scope.launch{if(!drawing){if(points<bet){points=1000;win=0;hand=Hand.NONE};points-=bet;val nd=engine.newDeck();deck=nd;held=emptySet();win=0;hand=Hand.NONE;dealAnimated(nd);deck=nd.drop(5).toMutableList();drawing=true}else{val d=deck.toMutableList();var next=cards;for(i in 0..4)if(i !in held){delay(110);next=next.toMutableList().also{it[i]=d.removeAt(0)};cards=next;mechanicalSound(MechSound.CARD)};deck=d;hand=engine.evaluate(next);val targetWin=bet*hand.multiplier;win=0;drawing=false;if(targetWin>0){riskRunning=true;mechanicalSound(MechSound.WIN);var shown=0;while(shown<targetWin){val step=((targetWin-shown)/14).coerceAtLeast(1);shown=(shown+step).coerceAtMost(targetWin);win=shown;ladderLight=(9-(shown*9/targetWin)).coerceIn(0,9);mechanicalSound(MechSound.RELAY);delay(42)};riskRunning=false;riskAvailable=true}else{ladderLight=-1;mechanicalSound(MechSound.BUTTON)}}}}}
  }}
 }
}

@Composable private fun MachineButton(label:String,color:Color,enabled:Boolean,onClick:()->Unit){Button(onClick=onClick,enabled=enabled,colors=ButtonDefaults.buttonColors(containerColor=color),modifier=Modifier.width(100.dp).height(60.dp),contentPadding=PaddingValues(4.dp)){Text(label,fontSize=11.sp,fontWeight=FontWeight.Bold)}}
@Composable private fun CardSlot(card:Card?,held:Boolean,enabled:Boolean,onHold:()->Unit){
 val red=card?.suit==Suit.HEARTS||card?.suit==Suit.DIAMONDS
 val ink=if(red)Color(0xFFB00020) else Color(0xFF111111)
 Column(Modifier.width(45.dp),horizontalAlignment=Alignment.CenterHorizontally){
  Box(Modifier.fillMaxWidth().aspectRatio(0.54f).background(if(card==null)Color(0xFF173A24) else Color(0xFFF2E9D2),RoundedCornerShape(3.dp)).border(if(held)4.dp else 2.dp,if(held)Color(0xFFFFB000) else Color(0xFF8C7A58),RoundedCornerShape(3.dp)).clickable(enabled=enabled){onHold()}){
   if(card==null) Text("POKER\nASS",Modifier.align(Alignment.Center),color=Color(0xFFFFB000),fontSize=15.sp,fontWeight=FontWeight.Bold)
   else{
    Column(Modifier.align(Alignment.TopStart).padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(card.rank.label,color=ink,fontSize=13.sp,fontWeight=FontWeight.Bold);Text(card.suit.symbol,color=ink,fontSize=12.sp)}
    Text(card.suit.symbol,Modifier.align(Alignment.Center),color=ink,fontSize=29.sp,fontWeight=FontWeight.Bold)
    Column(Modifier.align(Alignment.BottomEnd).padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(card.suit.symbol,color=ink,fontSize=12.sp);Text(card.rank.label,color=ink,fontSize=13.sp,fontWeight=FontWeight.Bold)}
   }
  }
  Spacer(Modifier.height(3.dp))
  Box(Modifier.fillMaxWidth().height(24.dp).background(if(held)Color(0xFFFFC400) else Color(0xFF351010),RoundedCornerShape(4.dp)).clickable(enabled=enabled){onHold()},contentAlignment=Alignment.Center){Text(if(held)"HALTEN" else "HALT",color=if(held)Color.Black else Color.LightGray,fontSize=9.sp,fontWeight=FontWeight.Bold)}
 }
}
@Composable private fun Led(label:String,value:Int){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(label,color=Color(0xFFFFB000),fontSize=9.sp);Text(value.toString().padStart(5,'0'),color=Color(0xFFFF3D00),fontSize=21.sp,fontWeight=FontWeight.Bold)}}
private fun handLabel(h:Hand)=when(h){Hand.ROYAL_FLUSH->"ROYAL FLUSH";Hand.STRAIGHT_FLUSH->"STRAIGHT FLUSH";Hand.FOUR->"VIERLING";Hand.FULL_HOUSE->"FULL HOUSE";Hand.FLUSH->"FLUSH";Hand.STRAIGHT->"STRAIGHT";Hand.THREE->"DRILLING";Hand.TWO_PAIR->"ZWEI PAAR";Hand.PAIR->"EIN PAAR";else->"NIX"}

private enum class MechSound{SHUFFLE,CARD,BUTTON,RELAY,WIN}
private fun mechanicalSound(type:MechSound){
 val sr=12000
 val duration=when(type){MechSound.SHUFFLE->.32;MechSound.CARD->.09;MechSound.BUTTON->.055;MechSound.RELAY->.045;MechSound.WIN->.18}
 val n=(sr*duration).toInt();val data=ShortArray(n)
 for(i in 0 until n){val t=i.toDouble()/sr
  val (decay,freq,noiseGain,toneGain)=when(type){MechSound.SHUFFLE->arrayOf(8.0,72.0,.34,.18);MechSound.CARD->arrayOf(38.0,112.0,.50,.27);MechSound.BUTTON->arrayOf(72.0,145.0,.56,.12);MechSound.RELAY->arrayOf(60.0,175.0,.45,.13);MechSound.WIN->arrayOf(13.0,128.0,.22,.30)}
  val env=exp(-decay*t);val noise=Random.nextDouble(-1.0,1.0)
  val motor=if(type==MechSound.SHUFFLE)sin(2*PI*freq*t)+.45*sin(2*PI*(freq*1.9)*t) else sin(2*PI*freq*t)
  data[i]=((noise*noiseGain+motor*toneGain)*env*Short.MAX_VALUE).toInt().coerceIn(-32768,32767).toShort()
 }
 val a=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
 val f=AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
 AudioTrack.Builder().setAudioAttributes(a).setAudioFormat(f).setBufferSizeInBytes(data.size*2).setTransferMode(AudioTrack.MODE_STATIC).build().also{it.write(data,0,data.size);it.setNotificationMarkerPosition(data.size);it.setPlaybackPositionUpdateListener(object:AudioTrack.OnPlaybackPositionUpdateListener{override fun onMarkerReached(t:AudioTrack){t.release()};override fun onPeriodicNotification(t:AudioTrack){}});it.play()}
}