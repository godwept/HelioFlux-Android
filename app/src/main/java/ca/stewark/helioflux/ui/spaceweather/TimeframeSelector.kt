package ca.stewark.helioflux.ui.spaceweather
import androidx.compose.foundation.layout.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
@Composable fun rememberTimeframeSelection(initial:Timeframe=Timeframe.ThreeHours):MutableState<Timeframe> = rememberSaveable{mutableStateOf(initial)}
@Composable fun TimeframeSelector(selected:Timeframe,onSelected:(Timeframe)->Unit,modifier:Modifier=Modifier){
 Row(modifier.fillMaxWidth().testTag("timeframe-selector"),horizontalArrangement=Arrangement.spacedBy(6.dp)){Timeframe.entries.forEach{timeframe->
  FilterChip(selected=selected==timeframe,onClick={onSelected(timeframe)},label={Text(timeframe.label)},modifier=Modifier.weight(1f).testTag("timeframe-"+timeframe.name))
 }}
}
val Timeframe.label:String get()=when(this){Timeframe.OneHour->"1h";Timeframe.ThreeHours->"3h";Timeframe.TwelveHours->"12h";Timeframe.TwoDays->"2d"}
