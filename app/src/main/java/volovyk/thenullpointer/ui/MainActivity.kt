package volovyk.thenullpointer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import volovyk.thenullpointer.R
import volovyk.thenullpointer.data.entity.FileUploadState
import volovyk.thenullpointer.data.entity.UploadedFile
import volovyk.thenullpointer.ui.theme.AppTheme
import volovyk.thenullpointer.util.getFileName
import volovyk.thenullpointer.util.length

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleIntent(intent)
        }
        setContent {
            AppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    val fileDeletionDialogOpen = remember { mutableStateOf(false) }
                    val fileToBeDeleted = remember { mutableStateOf<UploadedFile?>(null) }
                    MainScreen(
                        uiState = uiState,
                        onFileClick = {
                            copyLinkToClipboard(it.link)
                        },
                        onShareButtonClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, it.link)
                                type = "text/plain"
                            }

                            val shareIntent = Intent.createChooser(sendIntent, null)
                            startActivity(shareIntent)
                        },
                        onOpenButtonClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(it.link)
                            }

                            startActivity(intent)
                        },
                        onDeleteButtonClick = {
                            fileDeletionDialogOpen.value = true
                            fileToBeDeleted.value = it
                        },
                        onUploadFileFabClick = {
                            val mimetypes = arrayOf("*/*")
                            openDocumentLauncher.launch(mimetypes)
                        },
                        onFileUploadStateClick = {
                            if (it !is FileUploadState.InProgress) {
                                viewModel.fileUploadResultShown(it)
                            }
                        }
                    )
                    ConfirmationDialog(
                        shown = fileDeletionDialogOpen.value,
                        title = stringResource(id = R.string.confirm),
                        description = stringResource(
                            id = R.string.confirm_file_deletion,
                            fileToBeDeleted.value?.name ?: ""
                        ),
                        onConfirm = {
                            fileToBeDeleted.value?.let { viewModel.deleteFile(it) }
                            fileDeletionDialogOpen.value = false
                        },
                        onDismiss = { fileDeletionDialogOpen.value = false })
                }
            }
        }
    }

    private fun copyLinkToClipboard(link: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.app_name), link)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.link_in_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val clipData = intent.clipData
            if (clipData != null && clipData.itemCount > 0) {
                val clipDataUris = mutableListOf<Uri>()
                for (i in 0 until clipData.itemCount) {
                    clipDataUris.add(clipData.getItemAt(i)!!.uri)
                }
                uploadFiles(clipDataUris)
            }
        }
    }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(), ::uploadFiles
    )

    private fun uploadFiles(fileUris: List<Uri>) {
        fileUris.forEach { fileUri ->
            val filename = fileUri.getFileName(contentResolver)
            val fileSize = fileUri.length(contentResolver)
            val fileInputStream = contentResolver.openInputStream(fileUri)
            val mimeType = contentResolver.getType(fileUri)

            if (filename != null && fileInputStream != null && mimeType != null) {
                if (filename !in viewModel.uiState.value.fileUploadState.map { it.filename }) {
                    viewModel.uploadFile(
                        filename,
                        fileSize,
                        fileInputStream,
                        mimeType
                    )
                } else {
                    Toast.makeText(this, R.string.file_already_being_uploaded, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}