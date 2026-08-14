package com.transbuddy.app.utils

import android.os.Handler
import android.os.Looper
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * EmailSender — UTILITY
 * Sends emails automatically in the background using JavaMail SMTP protocol without requiring
 * external email app interaction or manual user intervention.
 */
object EmailSender {

    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "587"
    private const val SMTP_USER = "shisa3023@gmail.com"
    private const val SMTP_PASS = "xpin ghea utud cnjd" // App Password

    fun sendEmailInBackground(
        toEmail: String,
        subject: String,
        bodyText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val mainHandler = Handler(Looper.getMainLooper())

        Thread {
            try {
                val props = Properties().apply {
                    put("mail.smtp.host", SMTP_HOST)
                    put("mail.smtp.port", SMTP_PORT)
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.ssl.protocols", "TLSv1.2")
                    put("mail.smtp.timeout", "10000")
                    put("mail.smtp.connectiontimeout", "10000")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SMTP_USER, SMTP_PASS)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SMTP_USER, "Transbuddy Fleet Management"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                    setSubject(subject, "UTF-8")
                    setText(bodyText, "UTF-8")
                }

                Transport.send(message)

                mainHandler.post {
                    onSuccess()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                val errorMsg = ex.localizedMessage ?: "Failed to send email."
                mainHandler.post {
                    onError(errorMsg)
                }
            }
        }.start()
    }
}
