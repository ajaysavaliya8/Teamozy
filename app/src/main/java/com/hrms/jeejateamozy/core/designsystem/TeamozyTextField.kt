package com.hrms.jeejateamozy.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Standard Teamozy text field: field-bg fill, light border, 12dp radius.
 * Optional label rendered above the field using [TeamozyType.FieldLabel].
 */
@Composable
fun TeamozyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(text = label, style = TeamozyType.FieldLabel, modifier = Modifier.padding(bottom = 8.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder?.let { { Text(it, color = TeamozyColors.Placeholder) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            isError = isError,
            singleLine = singleLine,
            shape = TeamozyShapes.Control,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = TeamozyColors.FieldBg,
                focusedContainerColor = TeamozyColors.FieldBg,
                unfocusedBorderColor = TeamozyColors.Border,
                focusedBorderColor = TeamozyColors.Primary,
                errorBorderColor = TeamozyColors.Error
            )
        )
    }
}
