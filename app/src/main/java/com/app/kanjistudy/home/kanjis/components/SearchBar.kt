package com.app.kanjistudy.home.kanjis.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchBarKanji(
    query: String,
    onSearch: (String) -> Unit,
    label: String = "Search for kanji, readings, meanings..."
) {

    OutlinedTextField(
        value = query,
        onValueChange = onSearch,
        label = { Text(
            text = label,
            fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp, 4.dp, 12.dp, 4.dp)
    )
}