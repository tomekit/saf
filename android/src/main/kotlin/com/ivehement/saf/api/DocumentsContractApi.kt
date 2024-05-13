package com.ivehement.saf.api

import android.content.ContentResolver
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import com.ivehement.saf.ROOT_CHANNEL
import com.ivehement.saf.SafPlugin
import com.ivehement.saf.api.utils.*
import com.ivehement.saf.plugin.*
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Closeable
import java.util.LinkedList


internal class DocumentsContractApi(private val plugin: SafPlugin) :
  MethodChannel.MethodCallHandler,
  Listenable,
  ActivityListener {
  private var channel: MethodChannel? = null
  private var util: SafUtil? = null

  companion object {
    private const val CHANNEL = "documentscontract"
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    when (call.method) {
      BUID_CHILD_DOCUMENTS_URI_USING_TREE -> {
        try {
          val sourceTreeUri = Uri.parse(call.argument<String>("sourceTreeUriString"))
          val fileType = call.argument<String>("fileType")
        if(Build.VERSION.SDK_INT >= 21) {
//            val parentUri = DocumentsContract.buildChildDocumentsUriUsingTree(sourceTreeUri, DocumentsContract.getTreeDocumentId(sourceTreeUri))
//            val contentResolver: ContentResolver = plugin.context.contentResolver
            var childrenUris = listOf<String>()
//            val cursor = contentResolver.query(
//              parentUri, arrayOf(
//                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
//                DocumentsContract.Document.COLUMN_MIME_TYPE,
//                DocumentsContract.Document.COLUMN_LAST_MODIFIED
//                ),
//                null, null, null
//                )
            try {
//              while (cursor!!.moveToNext()) {
//                val docId = cursor.getString(0)
//                val mime = cursor.getString(1)
//                if (FILETYPES.contains(mime) || fileType == "any") {
//                    val eachUri =
//                        DocumentsContract.buildChildDocumentsUriUsingTree(
//                            parentUri,
//                            docId
//                        ).toString().replace("/children", "")
//                        childrenUris += eachUri
//                      }
//                }

              childrenUris = traverseDirectoryEntries(sourceTreeUri);
            }
            catch(e: Exception) {
              Log.e("CONTENT_RESOLVER_EXCEPTION: ", e.message!!)
            }
            finally {
//              if (cursor != null) {
//                  try {
//                      cursor.close()
//                  } catch (re: RuntimeException) {
//
//                  }
//              }
            }
            result.success(childrenUris)
          }
          else {
            result.notSupported(call.method, API_21)
          }
        }
        catch(e: Exception) {
          Log.e("BUID_CHILD_DOCUMENTS_PATH_USING_TREE_EXCEPTION: ", e.message!!)
          result.success(null)
        }
      }
      BUID_CHILD_DOCUMENTS_PATH_USING_TREE -> {
        try {
          val sourceTreeUri = Uri.parse(call.argument<String>("sourceTreeUriString"))
          val fileType = call.argument<String>("fileType")
          if(Build.VERSION.SDK_INT >= 21) {
            val parentUri = DocumentsContract.buildChildDocumentsUriUsingTree(sourceTreeUri, DocumentsContract.getTreeDocumentId(sourceTreeUri))
            val contentResolver: ContentResolver = plugin.context.contentResolver
            var childrenPaths = listOf<String>()
            val cursor = contentResolver.query(
                        parentUri, arrayOf(
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_MIME_TYPE,
                            DocumentsContract.Document.COLUMN_LAST_MODIFIED
                        ),
                        null, null, null
                    )
            try {
              while (cursor!!.moveToNext()) {
              val docId = cursor.getString(0)
              val mime = cursor.getString(1)
              // val lastModified = cursor.getString(2)
              if (FILETYPES.contains(mime) || fileType == "any") {
                  val child =
                        DocumentsContract.buildChildDocumentsUriUsingTree(
                            parentUri,
                            docId
                        ).toString().replace("/children", "")
                        childrenPaths += util?.getPath(Uri.parse(child))!!
                }
              }
            }
            catch(e: Exception) {
              Log.e("CONTENT_RESOLVER_EXCEPTION: ", e.message!!)
            }
            finally {
              if (cursor != null) {
                  try {
                      cursor.close()
                  } catch (re: RuntimeException) {
                      Log.e("RUNTIME_EXCEPTION", re.message!!)
                  }
              }
            }
            result.success(childrenPaths)
          }
          else {
            result.notSupported(call.method, API_21)
          }
        }
        catch(e: Exception) {
          Log.e("BUID_CHILD_DOCUMENTS_PATH_USING_TREE_EXCEPTION: ", e.message!!)
          result.success(null)
        }
      }
      GET_DOCUMENT_THUMBNAIL -> {
        if (Build.VERSION.SDK_INT >= API_21) {
          val rootUri = Uri.parse(call.argument("rootUri"))
          val documentId = call.argument<String>("documentId")
          val width = call.argument<Int>("width")!!
          val height = call.argument<Int>("height")!!

          val uri =
            DocumentsContract.buildDocumentUriUsingTree(rootUri, documentId)

          val bitmap = DocumentsContract.getDocumentThumbnail(
            plugin.context.contentResolver,
            uri,
            Point(width, height),
            null
          )

          CoroutineScope(Dispatchers.Default).launch {
            if (bitmap != null) {
              val base64 = bitmapToBase64(bitmap)

              val data = mapOf(
                "base64" to base64,
                "uri" to "$uri",
                "width" to bitmap.width,
                "height" to bitmap.height,
                "byteCount" to bitmap.byteCount,
                "density" to bitmap.density
              )

              launch(Dispatchers.Main) {
                result.success(data)
              }
            }
          }
        } else {
          result.notSupported(call.method, API_21)
        }
      }
      BUILD_DOCUMENT_URI_USING_TREE -> {
        val treeUri = Uri.parse(call.argument<String>("treeUriString"))
        val documentId = call.argument<String>("documentId")

        if (Build.VERSION.SDK_INT >= API_21) {
          val documentUri =
            DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

          result.success("$documentUri")
        } else {
          result.notImplemented()
        }
      }
      BUILD_DOCUMENT_URI -> {
        val authority = call.argument<String>("authority")
        val documentId = call.argument<String>("documentId")

        if (Build.VERSION.SDK_INT >= API_21) {
          val documentUri = DocumentsContract.buildDocumentUri(authority,documentId)

          result.success("$documentUri")
        } else {
          result.notSupported(call.method, API_21)
        }
      }
      BUILD_TREE_DOCUMENT_URI -> {
        val authority = call.argument<String>("authority")
        val documentId = call.argument<String>("documentId")

        if (Build.VERSION.SDK_INT >= API_21) {
          val treeDocumentUri =
            DocumentsContract.buildTreeDocumentUri(authority, documentId)

          result.success("$treeDocumentUri")
        } else {
          result.notSupported(call.method, API_21)
        }
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  // Inspired by Java version of: https://stackoverflow.com/a/68789978/2263395
  fun traverseDirectoryEntries(rootUri: Uri?): ArrayList<String> {
    val listUri = ArrayList<String>()
    val contentResolver: ContentResolver = plugin.context.contentResolver
    var childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
      rootUri,
      DocumentsContract.getTreeDocumentId(rootUri)
    )

    // Keep track of our directory hierarchy
    val dirNodes: MutableList<Uri> = LinkedList()
    dirNodes.add(childrenUri)
    while (!dirNodes.isEmpty()) {
      childrenUri = dirNodes.removeAt(0) // get the item from top
      val c = contentResolver.query(
        childrenUri, arrayOf(
          DocumentsContract.Document.COLUMN_DOCUMENT_ID,
          DocumentsContract.Document.COLUMN_DISPLAY_NAME,
          DocumentsContract.Document.COLUMN_MIME_TYPE
        ),
        null, null, null
      )
      try {
        while (c!!.moveToNext()) {
          val docId = c.getString(0)
          val name = c.getString(1)
          val mime = c.getString(2)
          if (isDirectory(mime)) {
            val newNode = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, docId)
            dirNodes.add(newNode)
          } else {
            val newNode = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, docId)
            listUri.add(newNode.toString().removeSuffix("/children"))
          }
        }
      } finally {
        closeQuietly(c)
      }
    }
    return listUri
  }

  // Util method to check if the mime type is a directory
  @RequiresApi(Build.VERSION_CODES.KITKAT)
  private fun isDirectory(mimeType: String): Boolean {
    return DocumentsContract.Document.MIME_TYPE_DIR == mimeType
  }

  // Util method to close a closeable
  private fun closeQuietly(closeable: Closeable?) {
    if (closeable != null) {
      try {
        closeable.close()
      } catch (re: java.lang.RuntimeException) {
        throw re
      } catch (ignore: java.lang.Exception) {
        // ignore exception
      }
    }
  }

  override fun startListening(binaryMessenger: BinaryMessenger) {
    if (channel != null) stopListening()

    channel = MethodChannel(binaryMessenger, "$ROOT_CHANNEL/$CHANNEL")
    util = SafUtil(plugin.context)
    channel?.setMethodCallHandler(this)
  }

  override fun stopListening() {
    if (channel == null) return

    channel?.setMethodCallHandler(null)
    channel = null
  }

  override fun startListeningToActivity() {
    /// Implement if needed
  }

  override fun stopListeningToActivity() {
    /// Implement if needed
  }
}
