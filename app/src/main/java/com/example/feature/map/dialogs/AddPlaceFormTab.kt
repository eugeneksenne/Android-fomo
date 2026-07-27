package com.example.feature.map.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The three data-entry tabs of `AddPlaceOverlayDialog`: permanent Venue,
 * one-off Event, and expiring Temp Party. Each tab is a small stack of
 * [OutlinedTextField]s over the fields [AddPlaceFormState] holds.
 *
 * Extracted from the `when (activeTab) { 0 -> ...; 1 -> ...; 2 -> ... }`
 * block inline in `MapScreen.kt`'s `AddPlaceOverlayDialog`, purely to keep
 * the dialog's root composable under the Map architecture's 250-line-per-
 * composable guideline.
 */
internal class AddPlaceFormState {
    var name by mutableStateOf("")
    var subcategory by mutableStateOf("")
    var address by mutableStateOf("")
    var categorySelect by mutableStateOf("Nightlife")

    var eventTitle by mutableStateOf("")
    var eventTime by mutableStateOf("")
    var eventHeadliner by mutableStateOf("")

    var partyName by mutableStateOf("")
    var partyType by mutableStateOf("House Party")
    var durationHrs by mutableStateOf("6 Hours")
}

@Composable
internal fun AddPlaceFormTab(activeTab: Int, formState: AddPlaceFormState) {
    when (activeTab) {
        0 -> PermanentVenueForm(formState)
        1 -> EventForm(formState)
        else -> TempPartyForm(formState)
    }
}

@Composable
private fun PermanentVenueForm(formState: AddPlaceFormState) {
    Column {
        LabeledField(value = formState.name, onValueChange = { formState.name = it }, label = "Venue Name")
        Spacer(modifier = Modifier.height(10.dp))
        LabeledField(value = formState.subcategory, onValueChange = { formState.subcategory = it }, label = "Subcategory (e.g. VIP Lounge)")
        Spacer(modifier = Modifier.height(10.dp))
        LabeledField(value = formState.address, onValueChange = { formState.address = it }, label = "Address Area")
    }
}

@Composable
private fun EventForm(formState: AddPlaceFormState) {
    Column {
        LabeledField(value = formState.eventTitle, onValueChange = { formState.eventTitle = it }, label = "Event Title")
        Spacer(modifier = Modifier.height(10.dp))
        LabeledField(value = formState.eventTime, onValueChange = { formState.eventTime = it }, label = "Time (e.g. 18:00 - Midnight)")
        Spacer(modifier = Modifier.height(10.dp))
        LabeledField(value = formState.eventHeadliner, onValueChange = { formState.eventHeadliner = it }, label = "Headliner / Lineup")
    }
}

@Composable
private fun TempPartyForm(formState: AddPlaceFormState) {
    Column {
        LabeledField(value = formState.partyName, onValueChange = { formState.partyName = it }, label = "Party / Venue Name")
        Spacer(modifier = Modifier.height(10.dp))
        LabeledField(value = formState.address, onValueChange = { formState.address = it }, label = "Secret Location Address")
    }
}

@Composable
private fun LabeledField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF00E5FF),
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
