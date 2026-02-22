package com.vakildiary.app.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ShareUtils {
    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    fun shareToWhatsApp(context: Context, text: String): Result<Unit> {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                setPackage("com.whatsapp")
            }
            context.startActivity(intent)
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error("WhatsApp not available", t)
        }
    }

    fun shareToWhatsAppBusiness(context: Context, text: String): Result<Unit> {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                setPackage("com.whatsapp.w4b")
            }
            context.startActivity(intent)
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error("WhatsApp Business not available", t)
        }
    }

    fun shareHearingDateText(
        context: Context,
        clientName: String,
        caseName: String,
        date: String,
        court: String,
        advocateName: String
    ): Result<Unit> {
        val message = "Dear $clientName, Your next hearing in $caseName is on $date at $court. Regards, Adv. $advocateName"
        val result = shareToWhatsApp(context, message)
        if (result is Result.Success) return result
        return shareToWhatsAppBusiness(context, message)
    }

    fun shareFeeSummaryText(
        context: Context,
        clientName: String,
        caseName: String,
        agreed: String,
        received: String,
        outstanding: String
    ): Result<Unit> {
        val message = "Fee Summary for $clientName\nCase: $caseName\nAgreed: $agreed\nReceived: $received\nOutstanding: $outstanding"
        val result = shareToWhatsApp(context, message)
        if (result is Result.Success) return result
        return shareToWhatsAppBusiness(context, message)
    }

    fun shareFile(context: Context, file: File, mimeType: String): Result<Unit> {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error("Unable to share file", t)
        }
    }

    fun openFile(context: Context, file: File, mimeType: String): Result<Unit> {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error("Unable to open file", t)
        }
    }
}
