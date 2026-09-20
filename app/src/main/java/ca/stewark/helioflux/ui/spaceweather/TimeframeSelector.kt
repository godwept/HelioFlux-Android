package ca.stewark.helioflux.ui.spaceweather
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

val defaultTimeframe=Timeframe.TwelveHours
val timeframeOptions:List<Timeframe> get()=Timeframe.entries

@Composable fun rememberTimeframeSelection(initial:Timeframe=defaultTimeframe):MutableState<Timeframe> = rememberSaveable{mutableStateOf(initial)}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TimeframeSelector(selected:Timeframe,onSelected:(Timeframe)->Unit,modifier:Modifier=Modifier){
 SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth().testTag("timeframe-selector")){
  timeframeOptions.forEachIndexed{index,timeframe->
   SegmentedButton(selected=selected==timeframe,onClick={onSelected(timeframe)},shape=SegmentedButtonDefaults.itemShape(index,timeframeOptions.size),modifier=Modifier.weight(1f).testTag("timeframe-"+timeframe.name),label={Text(timeframe.label)})
  }
 }
}
val Timeframe.label:String get()=when(this){Timeframe.OneHour->"1h";Timeframe.ThreeHours->"3h";Timeframe.TwelveHours->"12h";Timeframe.TwoDays->"2d"}
