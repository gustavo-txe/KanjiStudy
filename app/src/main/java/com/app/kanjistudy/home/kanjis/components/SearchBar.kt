package com.app.kanjistudy.home.kanjis.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchBarKanji(
    queryInput: String,
    onQueryChange: (String) -> Unit,
    onSearchRequest: () -> Unit,
    label: String = "Search for kanji, readings, meanings..."
) {

    OutlinedTextField(
        value = queryInput,
        onValueChange = onQueryChange,
        label = { Text(
            text = label,
            fontSize = 14.sp) },
        trailingIcon = {
            IconButton(onClick = onSearchRequest) {
                Icon(Icons.Default.Search, contentDescription = "Confirm search")
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchRequest() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp, 4.dp, 12.dp, 4.dp)
    )
}