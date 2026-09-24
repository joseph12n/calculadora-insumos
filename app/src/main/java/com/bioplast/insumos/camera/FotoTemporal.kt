package com.bioplast.insumos.camera

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Carpeta de caché donde viven las fotos temporales que se leen con OCR. */
private const val CARPETA_FOTOS = "fotos"

/**
 * Crea un archivo temporal en la caché de la app y devuelve la URI `content://`
 * que la cámara del sistema puede escribir (vía [FileProvider]).
 *
 * Ventaja de la cámara del sistema frente a la vista previa propia: es la
 * interfaz que la persona ya conoce, NO requiere permiso de cámara en esta app
 * y no hay que esperar a que "detecte" el número: primero se toma la foto y
 * después se lee tranquilamente.
 */
fun crearUriParaFoto(context: Context): Uri {
    val carpeta = File(context.cacheDir, CARPETA_FOTOS).apply { mkdirs() }
    val archivo = File(carpeta, "foto_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        archivo,
    )
}

/**
 * Borra la foto temporal [uri] (creada con [crearUriParaFoto]) después de
 * leerla, para que la caché no acumule imágenes. Nunca lanza.
 */
fun borrarFotoTemporal(context: Context, uri: Uri) {
    runCatching {
        val nombre = uri.lastPathSegment ?: return
        val archivo = File(File(context.cacheDir, CARPETA_FOTOS), nombre)
        if (archivo.exists()) archivo.delete()
    }
}
