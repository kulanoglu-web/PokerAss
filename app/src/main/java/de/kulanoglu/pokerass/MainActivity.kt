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
 var shuffling by remember{mutableStateOf(false)};var flash by remember{mutableStateOf(false)};var ladderLight by remember{mutableIntStateOf(-1)}
 var riskAvailable by remember{mutableStateOf(false)};var games by remember{mutableIntStateOf(0)};var riskRunning by remember{mutableStateOf(false)};var stopRequested by remember{mutableStateOf(false)};var status by remember{mutableStateOf("BEREIT")};var statusPulse by remember{mutableIntStateOf(0)};var lastWin by remember{mutableIntStateOf(0)}
 val amber=Color(0xFFFFB000);val dimAmber=Color(0xFF8A5A10);val scope=rememberCoroutineScope();val canPlay=!shuffling&&!riskRunning
 suspend fun dealAnimated(nd:MutableList<Card>){
  shuffling=true;status="MISCHEN";statusPulse++
  val preview=engine.newDeck()
  repeat(14){frame->
   cards=List(5){i->preview[(frame*4+i+frame)%preview.size]}
   if(frame%3==0)mechanicalSound(MechSound.SHUFFLE)
   delay(35L+frame*4)
  }
  var settled=cards
  repeat(5){i->
   settled=settled.toMutableList().also{it[i]=nd[i]};cards=settled
   mechanicalSound(MechSound.CARD);delay(78L+i*25)
  }
  shuffling=false;status="HALTEN / ZIEHEN";statusPulse++;flash=true;delay(90);flash=false
 }
 Column(Modifier.fillMaxSize().background(Color(0xFF050403)).padding(horizontal=5.dp,vertical=3.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceBetween){
  Column(Modifier.fillMaxWidth().padding(bottom=3.dp)){listOf(listOf("ROYAL FLUSH","STRAIGHT FLUSH","VIERLING","FULL HOUSE","FLUSH"),listOf("STRAIGHT","DRILLING","ZWEI PAAR","EIN PAAR","NIX")).forEach{r->Row(Modifier.fillMaxWidth()){r.forEach{label->Text(label,Modifier.weight(1f).padding(2.dp).border(1.dp,if(flash)Color(0xFFFFB000) else Color(0xFF765000)).padding(vertical=5.dp),if(label==handLabel(hand))Color(0xFFFFE46B) else amber,8.sp,fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center)}}}}
  Text(status,color=if(flash)Color(0xFFFFE46B) else amber,fontSize=11.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(2.dp));Row(Modifier.weight(1f).fillMaxWidth().border(2.dp,if(flash)amber else Color(0xFF6B450B),RoundedCornerShape(5.dp)).padding(4.dp),verticalAlignment=Alignment.CenterVertically){
   Row(Modifier.weight(1f).padding(horizontal=2.dp),horizontalArrangement=Arrangement.spacedBy(3.dp,Alignment.CenterHorizontally)){repeat(5){i->CardSlot(cards.getOrNull(i),i in held,drawing&&!shuffling){held=if(i in held)held-i else held+i;mechanicalSound(MechSound.BUTTON)}}}
   Column(Modifier.width(69.dp),horizontalAlignment=Alignment.CenterHorizontally){
 Text("RISIKO",color=Color(0xFFFF6A22),fontSize=11.sp,fontWeight=FontWeight.Bold)
 val rv=listOf(5000,2000,1000,500,200,100,50,20,10,0)
 rv.forEachIndexed{i,v->
  val left=i%4==0||i%4==3
  Row(Modifier.fillMaxWidth(),horizontalArrangement=if(left)Arrangement.Start else Arrangement.End){
   Box(Modifier.width(52.dp).height(28.dp).background(if(i==ladderLight)Color(0xFF932F00) else Color(0xFF140D05),RoundedCornerShape(12.dp)).border(if(i==ladderLight)3.dp else 1.5.dp,if(i==ladderLight)Color.Yellow else Color(0xFFB27516),RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){
    Text("$v",color=if(i==ladderLight)Color.Yellow else amber,fontSize=10.sp,fontWeight=FontWeight.Bold)
   }
  }
 }
}
  }
  Column(Modifier.fillMaxWidth().padding(top=3.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){Text("SPIEL $games   LETZTER GEWINN $lastWin",color=Color(0xFF8A6A28),fontSize=9.sp)};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){Led("PUNKTE",points);Led("EINSATZ",bet);Led("GEWINN",win)};Row(Modifier.fillMaxWidth().height(74.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){
   MachineButton(if(riskAvailable)"NEHMEN" else "EINSATZ",Color(0xFF3B352D),!drawing&&canPlay){if(riskAvailable){status="PUNKTE ZÄHLEN";statusPulse++;flash=true;val target=points+win;riskAvailable=false;riskRunning=true;scope.launch{while(points<target){val step=((target-points)/18).coerceAtLeast(1);points=(points+step).coerceAtMost(target);mechanicalSound(MechSound.RELAY);delay(34)};win=0;ladderLight=-1;riskRunning=false;flash=false;status="BEREIT";statusPulse++}}else bet=bets[(bets.indexOf(bet)+1)%bets.size];mechanicalSound(MechSound.BUTTON)}
   MachineButton(if(riskRunning)"STOP" else "RISIKO",Color(0xFFA41414),riskAvailable||riskRunning){if(riskRunning){stopRequested=true;mechanicalSound(MechSound.BUTTON)}else{scope.launch{riskRunning=true;stopRequested=false;status="RISIKO - STOP";statusPulse++;flash=true;var p=9;while(!stopRequested){p=if(p==0)9 else p-1;ladderLight=p;mechanicalSound(MechSound.RELAY);delay(if(p%3==0)56 else 72)};var pause=84L;repeat(4){p=if(p==0)9 else p-1;ladderLight=p;mechanicalSound(MechSound.RELAY);delay(pause);pause+=62};val values=listOf(5000,2000,1000,500,200,100,50,20,10,0);win=values[p];lastWin=win;statusPulse++;riskAvailable=win>0;riskRunning=false;stopRequested=false;flash=false;status=if(win>0)"NEHMEN ODER RISIKO" else "VERLOREN";if(win==0)ladderLight=-1}}}
   MachineButton(if(drawing)"ZIEHEN" else "GEBEN",Color(0xFFBD2020),canPlay){scope.launch{if(!drawing){if(points<bet){points=1000;win=0;hand=Hand.NONE;status="1000 PUNKTE NEU";statusPulse++};points-=bet;games++;statusPulse++;val nd=engine.newDeck();deck=nd;held=emptySet();win=0;hand=Hand.NONE;stopRequested=false;riskAvailable=false;ladderLight=-1;status="MISCHEN";lastWin=0;flash=false;dealAnimated(nd);deck=nd.drop(5).toMutableList();drawing=true}else{val d=deck.toMutableList();var next=cards;for(i in 0..4)if(i !in held){status="KARTEN DREHEN";statusPulse++;flash=true;delay(92L+i*21);next=next.toMutableList().also{it[i]=d.removeAt(0)};cards=next;mechanicalSound(MechSound.CARD);flash=!flash};flash=false;deck=d;hand=engine.evaluate(next);val targetWin=bet*hand.multiplier;lastWin=targetWin;win=0;drawing=false;if(targetWin>0){status=handLabel(hand);statusPulse++;flash=true;riskRunning=true;mechanicalSound(MechSound.WIN);var shown=0;while(shown<targetWin){val step=((targetWin-shown)/14).coerceAtLeast(1);shown=(shown+step).coerceAtMost(targetWin);win=shown;ladderLight=(9-(shown*9/targetWin)).coerceIn(0,9);mechanicalSound(MechSound.RELAY);delay(36)};riskRunning=false;riskAvailable=true;flash=false;status="NEHMEN ODER RISIKO";statusPulse++}else{ladderLight=-1;status="NIX";statusPulse++;flash=false;mechanicalSound(MechSound.BUTTON)}}}}}
  }}
 }

@Composable private fun MachineButton(label:String,color:Color,enabled:Boolean,onClick:()->Unit){Button(onClick=onClick,enabled=enabled,colors=ButtonDefaults.buttonColors(containerColor=color),modifier=Modifier.width(104.dp).height(64.dp).border(2.dp,Color(0xFFA28B68),RoundedCornerShape(14.dp)),contentPadding=PaddingValues(3.dp)){Text(label,fontSize=12.sp,fontWeight=FontWeight.Bold)}}
@Composable private fun CardSlot(card:Card?,held:Boolean,enabled:Boolean,onHold:()->Unit){
 val red=card?.suit==Suit.HEARTS||card?.suit==Suit.DIAMONDS
 val ink=if(red)Color(0xFFB5122B) else Color(0xFF111111)
 Column(Modifier.width(49.dp),horizontalAlignment=Alignment.CenterHorizontally){
  Box(Modifier.fillMaxWidth().aspectRatio(0.54f).background(if(card==null)Color(0xFF173A24) else Color(0xFFF8EED8),RoundedCornerShape(3.dp)).border(if(held)4.dp else 2.dp,if(held)Color(0xFFFFB000) else Color(0xFFA08B65),RoundedCornerShape(3.dp)).clickable(enabled=enabled){onHold()}){
   if(card==null) Text("POKER\nASS",Modifier.align(Alignment.Center),color=Color(0xFFFFB000),fontSize=15.sp,fontWeight=FontWeight.Bold)
   else{
    Column(Modifier.align(Alignment.TopStart).padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(card.rank.label,color=ink,fontSize=13.sp,fontWeight=FontWeight.Bold);Text(card.suit.symbol,color=ink,fontSize=12.sp)}
    Text(card.suit.symbol,Modifier.align(Alignment.Center),color=ink,fontSize=32.sp,fontWeight=FontWeight.Bold)
    Column(Modifier.align(Alignment.BottomEnd).padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(card.suit.symbol,color=ink,fontSize=12.sp);Text(card.rank.label,color=ink,fontSize=13.sp,fontWeight=FontWeight.Bold)}
   }
  }
  Spacer(Modifier.height(3.dp))
  Box(Modifier.fillMaxWidth().height(28.dp).background(if(held)Color(0xFFFFC400) else Color(0xFF601212),RoundedCornerShape(4.dp)).clickable(enabled=enabled){onHold()},contentAlignment=Alignment.Center){Text(if(held)"HALTEN" else "HALT",color=if(held)Color.Black else Color.LightGray,fontSize=9.sp,fontWeight=FontWeight.Bold)}
 }
}
@Composable private fun Led(label:String,value:Int){Column(Modifier.background(Color(0xFF0C0200),RoundedCornerShape(4.dp)).border(1.dp,Color(0xFF852E00),RoundedCornerShape(4.dp)).padding(horizontal=12.dp,vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(label,color=Color(0xFFFFB000),fontSize=9.sp);Text(value.toString().padStart(5,'0'),color=Color(0xFFFF3D00),fontSize=21.sp,fontWeight=FontWeight.Bold)}}
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