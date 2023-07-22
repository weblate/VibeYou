package app.suhasdissa.mellowmusic.ui.screens.settings

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import app.suhasdissa.mellowmusic.R
import app.suhasdissa.mellowmusic.backend.viewmodel.DatabaseViewModel
import app.suhasdissa.mellowmusic.ui.components.SettingItem
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseSettingsScreen(
    databaseViewModel: DatabaseViewModel = viewModel(factory = DatabaseViewModel.Factory)
) {
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(title = { Text(stringResource(R.string.backup_restore)) })
    }) { innerPadding ->
        val context = LocalContext.current
        val backupLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/vnd.sqlite3")
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult

                databaseViewModel.backupDatabase(uri, context)
            }
        val restoreLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult

                databaseViewModel.restoreDatabase(uri, context)
            }

        LazyColumn(
            Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            item {
                SettingItem(
                    title = stringResource(R.string.backup),
                    description = stringResource(R.string.backup_description),
                    icon = Icons.Default.Backup
                ) {
                    @SuppressLint("SimpleDateFormat")
                    val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")

                    try {
                        backupLauncher.launch("mellowmusic_${dateFormat.format(Date())}.db")
                    } catch (e: Exception) {
                        Toast.makeText(context, "Something went wrong", Toast.LENGTH_LONG).show()
                    }
                }
            }
            item {
                SettingItem(
                    title = stringResource(R.string.restore),
                    description = stringResource(R.string.restore_description),
                    icon = Icons.Default.Restore
                ) {
                    try {
                        restoreLauncher.launch(
                            arrayOf(
                                "application/vnd.sqlite3",
                                "application/x-sqlite3",
                                "application/octet-stream"
                            )
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Something went wrong", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
