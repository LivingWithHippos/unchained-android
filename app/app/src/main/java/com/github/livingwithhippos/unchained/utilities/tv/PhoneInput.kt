package com.github.livingwithhippos.unchained.utilities.tv

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.authentication.LocalTokenServer
import com.github.livingwithhippos.unchained.utilities.extension.isTv
import com.github.livingwithhippos.unchained.utilities.extension.loadQrCode
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber

/**
 * Reusable, transport only wrapper around a [LocalTokenServer]. Typing with a TV remote is painful,
 * so on Android TV any field can offer to receive its value from another device on the same local
 * network (e.g. the user's phone). This controller owns the server lifecycle and delivers its
 * callbacks on the main thread; the actual PIN, QR code and address are shown by
 * [showPhoneInputDialog]. The security properties (PIN, one shot, bounded lifetime, LAN only bind,
 * security headers) all live in [LocalTokenServer] and are untouched here.
 *
 * @param pages localized texts used to build the served web pages
 * @param isValueValid decides whether a submitted value is acceptable
 * @param onValueReceived called on the main thread with the submitted value
 * @param onStopped called on the main thread when the server stops itself (timeout, too many wrong
 *   PINs or a value received). Not called by [stop].
 */
/**
 * Remembers, for the rest of this app session, that the PIN has already been passed once, so later
 * phone inputs (e.g. a second search in a row) do not ask for it again. This used to be tracked per
 * phone via a session cookie set by the server, but a phone's cookie jar turned out not to be
 * something we can rely on here: each phone input starts a fresh [LocalTokenServer] (a new address,
 * since the previous one already stopped after its one accepted submission), and depending on the
 * exact browser or QR scanner preview used to open that address, the earlier cookie might not carry
 * over to it. Deciding trust up front, entirely on this side, sidesteps that: whether the PIN is
 * needed is now known before the server (or the dialog showing its PIN) is even created, instead of
 * depending on anything the phone sends back. The trust is dropped after [SESSION_TIMEOUT_MS] of
 * inactivity, and each use slides the expiry forward. This is a single, app wide flag rather than a
 * per phone one now, which matches how the feature is actually used (one person, one TV); every
 * server still enforces its own one shot acceptance, short lifetime and LAN only bind regardless.
 */
object PhoneInputSession {
    private const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L
    private var trustedUntil = 0L

    /** True if the PIN was successfully entered within the last [SESSION_TIMEOUT_MS]; slides the expiry forward. */
    @Synchronized
    fun isTrusted(): Boolean {
        val now = System.currentTimeMillis()
        if (now > trustedUntil) return false
        trustedUntil = now + SESSION_TIMEOUT_MS
        return true
    }

    /** Remember that the PIN was just entered successfully, for the rest of this app session. */
    @Synchronized
    fun grantTrust() {
        trustedUntil = System.currentTimeMillis() + SESSION_TIMEOUT_MS
    }
}

class PhoneInputController(
    private val pages: LocalTokenServer.Pages,
    private val isValueValid: (String) -> Boolean,
    private val onValueReceived: (String) -> Unit,
    private val onStopped: () -> Unit = {},
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var server: LocalTokenServer? = null

    /** The reachable http address of the running server, or null while it is not running */
    var address: String? = null
        private set

    /** The PIN the phone must submit, available once [start] succeeded */
    val pin: String?
        get() = server?.pin

    /** Whether the PIN can be skipped this time, decided once, before the server even starts. */
    var trusted: Boolean = false
        private set

    val isRunning: Boolean
        get() = server != null

    /**
     * Start the server. Returns the reachable address, or null if no local network address or free
     * port was found. Callbacks are marshalled onto the main thread.
     */
    fun start(): String? {
        trusted = PhoneInputSession.isTrusted()
        val newServer =
            LocalTokenServer(
                pages = pages,
                isValueValid = isValueValid,
                onValueReceived = { value -> mainHandler.post { onValueReceived(value) } },
                onStopped = {
                    mainHandler.post {
                        server = null
                        onStopped()
                    }
                },
                trusted = trusted,
                // a first valid PIN this session skips it on later phone inputs too
                onTrustEstablished = { PhoneInputSession.grantTrust() },
            )
        val started = newServer.start()
        if (started != null) {
            server = newServer
            address = started
        }
        return started
    }

    /** Stop the server, if running. Idempotent and safe to call more than once. */
    fun stop() {
        server?.stop()
        server = null
    }
}

/**
 * Show a dialog with the address, PIN and a QR code of a freshly started [PhoneInputController], so
 * the user can send [fieldLabel]'s value from a phone instead of typing it with the remote. The
 * value fills the field through [onValueReceived]; the dialog dismisses itself when a value arrives
 * or when the server stops. Dismissing the dialog without waiting for a submission stops the server
 * too. TV only affordances call this; the caller is expected to have gated on [Context.isTv].
 */
fun showPhoneInputDialog(
    context: Context,
    scope: CoroutineScope,
    fieldLabel: String,
    linkUrl: String? = null,
    linkLabel: String? = null,
    errorMessage: String = context.getString(R.string.phone_input_invalid),
    isValueValid: (String) -> Boolean = { it.isNotBlank() },
    onValueReceived: (String) -> Unit,
) {
    val view = LayoutInflater.from(context).inflate(R.layout.dialog_phone_input, null)
    val addressView = view.findViewById<TextView>(R.id.tvPhoneInputAddress)
    val pinView = view.findViewById<TextView>(R.id.tvPhoneInputPin)
    val qrView = view.findViewById<ImageView>(R.id.ivPhoneInputQrCode)

    var dialog: AlertDialog? = null

    val controller =
        PhoneInputController(
            pages =
                LocalTokenServer.Pages(
                    title = context.getString(R.string.app_name),
                    fieldLabel = fieldLabel,
                    pinLabel = context.getString(R.string.token_web_pin_label),
                    submitLabel = context.getString(R.string.send),
                    successMessage = context.getString(R.string.value_web_received),
                    errorMessage = errorMessage,
                    wrongPinMessage = context.getString(R.string.token_web_wrong_pin),
                    linkUrl = linkUrl,
                    linkLabel = linkLabel,
                ),
            isValueValid = isValueValid,
            onValueReceived = { value ->
                onValueReceived(value)
                dialog?.dismiss()
            },
            onStopped = {
                // the server stopped itself: close the panel if it is still shown
                dialog?.dismiss()
            },
        )

    val address = controller.start()
    if (address == null) {
        context.showToast(R.string.phone_input_unavailable)
        Timber.w("The phone input server could not be started")
        return
    }

    addressView.text = context.getString(R.string.send_value_from_phone_format, address)
    // the PIN this session was already established a moment ago, in start(); the served form
    // will not ask for it either, so showing it here would just be confusing, unused clutter
    if (controller.trusted) pinView.visibility = View.GONE
    else pinView.text = context.getString(R.string.token_server_pin_format, controller.pin)

    dialog =
        MaterialAlertDialogBuilder(context)
            .setTitle(fieldLabel)
            .setView(view)
            .setNegativeButton(R.string.close) { d, _ -> d.dismiss() }
            .setOnDismissListener {
                // dismissing without waiting for a submission must also stop the server
                controller.stop()
            }
            .create()
    dialog.show()
    // load the QR only once the dialog view is attached, otherwise loadQrCode skips it
    qrView.loadQrCode(address, scope)
}

/**
 * On Android TV, add a QR code start icon to this field that opens [showPhoneInputDialog] so its
 * value can be sent from a phone. Does nothing on phones, so it is always safe to call. By default
 * the received value fills this field's [TextInputLayout.getEditText] and the served form label is
 * this field's hint.
 */
fun TextInputLayout.enablePhoneInput(
    scope: CoroutineScope,
    fieldLabel: String = hint?.toString() ?: context.getString(R.string.app_name),
    linkUrl: String? = null,
    linkLabel: String? = null,
    errorMessage: String = context.getString(R.string.phone_input_invalid),
    isValueValid: (String) -> Boolean = { it.isNotBlank() },
    onValueReceived: (String) -> Unit = { value -> editText?.setText(value) },
) {
    if (!context.isTv()) return
    setStartIconDrawable(R.drawable.icon_qr_code)
    startIconContentDescription = context.getString(R.string.type_from_phone)
    isStartIconVisible = true
    setStartIconOnClickListener {
        showPhoneInputDialog(
            context = context,
            scope = scope,
            fieldLabel = fieldLabel,
            linkUrl = linkUrl,
            linkLabel = linkLabel,
            errorMessage = errorMessage,
            isValueValid = isValueValid,
            onValueReceived = onValueReceived,
        )
    }
}
