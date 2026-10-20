package com.sallygram.app

import android.content.Context
import com.sallygram.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

/** Owns the TDLib client, authentication, and live chat-list state. */
class TdLibManager(context: Context) {
    enum class LoginStep { CONFIGURATION, PHONE, CODE, PASSWORD, REGISTRATION, READY, NONE }

    data class UiState(
        val status: String = "Menyiapkan TDLib…",
        val step: LoginStep = LoginStep.NONE,
        val isReady: Boolean = false
    )

    data class ChatItem(
        val id: Long,
        val title: String,
        val lastMessage: String,
        val date: Int,
        val unreadCount: Int
    )

    private val appContext = context.applicationContext
    private val mutableState = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = mutableState

    private val mutableChats = MutableStateFlow<List<ChatItem>>(emptyList())
    val chats: StateFlow<List<ChatItem>> = mutableChats
    private val chatItems = LinkedHashMap<Long, ChatItem>()

    private var client: Client? = null
    private var started = false

    @Synchronized
    fun start() {
        if (started) return
        started = true

        if (BuildConfig.TELEGRAM_API_ID <= 0 || BuildConfig.TELEGRAM_API_HASH.isBlank()) {
            mutableState.value = UiState(
                status = "TDLib siap, tetapi api_id dan api_hash belum dikonfigurasi.",
                step = LoginStep.CONFIGURATION
            )
            return
        }

        try {
            client = Client.create(
                { update -> handleUpdate(update) },
                { error -> updateError(error) },
                { error -> updateError(error) }
            )
            send(TdApi.SetLogVerbosityLevel(1))
            send(TdApi.GetAuthorizationState())
            mutableState.value = UiState("Menghubungkan ke Telegram…", LoginStep.NONE)
        } catch (error: Throwable) {
            mutableState.value = UiState(
                status = "TDLib gagal dimulai: ${error.message ?: error.javaClass.simpleName}",
                step = LoginStep.NONE
            )
        }
    }

    private fun handleUpdate(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> {
                when (val auth = update.authorizationState) {
                    is TdApi.AuthorizationStateWaitTdlibParameters -> {
                        val parameters = TdApi.SetTdlibParameters(
                            appContext.filesDir.resolve("tdlib").absolutePath,
                            true,
                            false,
                            BuildConfig.TELEGRAM_API_ID,
                            BuildConfig.TELEGRAM_API_HASH,
                            "id",
                            android.os.Build.MODEL ?: "Android",
                            android.os.Build.VERSION.RELEASE ?: "Android",
                            BuildConfig.VERSION_NAME,
                            true,
                            false
                        )
                        send(parameters)
                        setState("Menginisialisasi database Telegram…", LoginStep.NONE)
                    }
                    is TdApi.AuthorizationStateWaitEncryptionKey -> {
                        send(TdApi.CheckDatabaseEncryptionKey(""))
                        setState("Membuka database Telegram…", LoginStep.NONE)
                    }
                    is TdApi.AuthorizationStateWaitPhoneNumber ->
                        setState("Masukkan nomor telepon akun Telegram.", LoginStep.PHONE)
                    is TdApi.AuthorizationStateWaitCode ->
                        setState("Masukkan kode login Telegram yang dikirim ke perangkat atau aplikasi Telegram.", LoginStep.CODE)
                    is TdApi.AuthorizationStateWaitPassword ->
                        setState("Akun ini memakai verifikasi dua langkah. Masukkan kata sandi Telegram.", LoginStep.PASSWORD)
                    is TdApi.AuthorizationStateWaitRegistration ->
                        setState("Akun perlu didaftarkan. Form nama akan ditambahkan pada tahap berikutnya.", LoginStep.REGISTRATION)
                    is TdApi.AuthorizationStateReady -> {
                        mutableState.value = UiState("Terhubung ke Telegram · memuat chat…", LoginStep.READY, true)
                        requestChats()
                    }
                    is TdApi.AuthorizationStateLoggingOut ->
                        setState("Keluar dari akun Telegram…", LoginStep.NONE)
                    is TdApi.AuthorizationStateClosing ->
                        setState("Menutup koneksi Telegram…", LoginStep.NONE)
                    is TdApi.AuthorizationStateClosed ->
                        setState("Koneksi TDLib ditutup.", LoginStep.NONE)
                    else -> setState("Menunggu status otorisasi Telegram…", LoginStep.NONE)
                }
            }
            is TdApi.UpdateNewChat -> updateChat(update.chat)
            is TdApi.UpdateChatTitle -> {
                synchronized(chatItems) {
                    val existing = chatItems[update.chatId]
                    if (existing != null) {
                        chatItems[update.chatId] = existing.copy(title = update.title)
                        publishChats()
                    } else {
                        requestChat(update.chatId)
                    }
                }
            }
            is TdApi.UpdateChatLastMessage -> {
                synchronized(chatItems) {
                    val existing = chatItems[update.chatId]
                    if (existing != null) {
                        chatItems[update.chatId] = existing.copy(
                            lastMessage = messagePreview(update.lastMessage),
                            date = update.lastMessage?.date ?: existing.date
                        )
                        publishChats()
                    } else {
                        requestChat(update.chatId)
                    }
                }
            }
        }
    }

    private fun requestChats() {
        // Load the main chat list first; then ask TDLib for its chat IDs.
        send(TdApi.LoadChats(TdApi.ChatListMain(), 100)) { result ->
            if (result is TdApi.Ok) {
                send(TdApi.GetChats(TdApi.ChatListMain(), 100)) { chatsResult ->
                    if (chatsResult is TdApi.Chats) {
                        chatsResult.chatIds.forEach { chatId -> requestChat(chatId) }
                    } else if (chatsResult is TdApi.Error) {
                        setState("Gagal memuat daftar chat: ${chatsResult.message}", LoginStep.READY)
                    }
                }
            } else if (result is TdApi.Error) {
                setState("Gagal memuat chat: ${result.message}", LoginStep.READY)
            }
        }
    }

    private fun requestChat(chatId: Long) {
        send(TdApi.GetChat(chatId)) { result ->
            if (result is TdApi.Chat) updateChat(result)
        }
    }

    private fun updateChat(chat: TdApi.Chat) {
        synchronized(chatItems) {
            chatItems[chat.id] = ChatItem(
                id = chat.id,
                title = chat.title.orEmpty().ifBlank { "Chat" },
                lastMessage = messagePreview(chat.lastMessage),
                date = chat.lastMessage?.date ?: 0,
                unreadCount = chat.unreadCount
            )
            publishChats()
        }
    }

    private fun publishChats() {
        mutableChats.value = chatItems.values
            .sortedWith(compareByDescending<ChatItem> { it.date }.thenBy { it.title.lowercase() })
    }

    private fun messagePreview(message: TdApi.Message?): String {
        if (message == null) return "Belum ada pesan"
        return when (val content = message.content) {
            is TdApi.MessageText -> content.text.text
            is TdApi.MessagePhoto -> "📷 Foto"
            is TdApi.MessageVideo -> "🎥 Video"
            is TdApi.MessageDocument -> "📎 ${content.document.fileName}"
            else -> "Pesan"
        }
    }

    fun submitPhone(phone: String) {
        if (phone.isBlank()) return
        send(TdApi.SetAuthenticationPhoneNumber(phone.trim(), null))
        setState("Meminta kode login…", LoginStep.NONE)
    }

    fun submitCode(code: String) {
        if (code.isBlank()) return
        send(TdApi.CheckAuthenticationCode(code.trim()))
        setState("Memeriksa kode…", LoginStep.NONE)
    }

    fun submitPassword(password: String) {
        if (password.isBlank()) return
        send(TdApi.CheckAuthenticationPassword(password))
        setState("Memeriksa kata sandi…", LoginStep.NONE)
    }

    private fun send(function: TdApi.Function, onResult: ((TdApi.Object) -> Unit)? = null) {
        val activeClient = client ?: return
        activeClient.send(function) { result ->
            onResult?.invoke(result)
            if (result is TdApi.Error) {
                setState("Telegram: ${result.message}", currentStep())
            }
        }
    }

    private fun updateError(error: Throwable) {
        setState("Kesalahan TDLib: ${error.message ?: error.javaClass.simpleName}", currentStep())
    }

    private fun currentStep(): LoginStep = mutableState.value.step

    private fun setState(status: String, step: LoginStep) {
        mutableState.value = UiState(status, step, step == LoginStep.READY)
    }

    fun close() {
        val activeClient = client ?: return
        client = null
        try {
            activeClient.send(TdApi.Close()) { }
        } catch (_: Throwable) {
            // The OS may already be tearing down the process.
        }
    }
}
