package volovyk.thenullpointer.data

import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType
import volovyk.thenullpointer.data.entity.UploadedFile
import volovyk.thenullpointer.data.remote.entity.FileUploadState
import java.io.InputStream

interface FileRepository {
    fun getFilesFlow(): Flow<List<UploadedFile>>
    suspend fun uploadFile(
        filename: String,
        fileSize: Long,
        inputStream: InputStream,
        mediaType: MediaType
    ): Flow<FileUploadState>

    suspend fun deleteFile(file: UploadedFile)
}