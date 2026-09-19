package ca.stewark.helioflux.ui.home
import kotlinx.coroutines.delay
class SolarFramePlayerState(private val frameCount:()->Int,private val cadenceMillis:Long=200L){
 var index:Int=0;private set
 fun advance(){val count=frameCount();index=if(count<=0)0 else(index+1)%count}
 fun reset(){index=0}
 suspend fun play(isEnabled:()->Boolean,onFrame:(Int)->Unit){while(isEnabled()){delay(cadenceMillis);if(!isEnabled())break;advance();onFrame(index)}}
}
