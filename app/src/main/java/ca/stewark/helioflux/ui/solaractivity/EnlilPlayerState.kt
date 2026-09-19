package ca.stewark.helioflux.ui.solaractivity
import kotlinx.coroutines.delay
class EnlilPlayerState(private val frameCount:()->Int,private val cadenceMillis:Long=200L){var index:Int=0;private set;fun advance(){val count=frameCount();index=if(count<=0)0 else(index+1)%count};fun reset(){index=0};suspend fun play(isVisible:()->Boolean,onFrame:(Int)->Unit){while(isVisible()){delay(cadenceMillis);if(!isVisible())break;advance();onFrame(index)}}}
