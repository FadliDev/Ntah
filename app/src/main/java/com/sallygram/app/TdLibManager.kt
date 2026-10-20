package com.sallygram.app

import android.content.Context
import android.os.Build
import com.sallygram.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

/** Owns the TDLib client and translates TDLib authorization updates into UI state. */
class TdLibManager(context: Context) {
    enum class LoginStep { CONFIGURATION, PHONE, CODE, PASSWORD, REGISTRATION, READY, NONE }

    data class UiState(
        val status: String = "Menyiapkan TDLib…",
        val step: LoginStep = LoginStep.NONE,
        val isReady: Boolean = false
    )

    private val appContext = context.applicationContext
    private val mutableState = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = mutableState
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
        if (update !is TdApi.UpdateAuthorizationState) return
        when (val auth = update.authorizationState) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val directory = appContext.filesDir.resolve("tdlib").apply { mkdirs() }
                val filesDirectory = directory.resolve("files").apply { mkdirs() }
                val parameters = TdApi.SetTdlibParameters(
                    false, // useTestDc
                    directory.absolutePath,
                    filesDirectory.absolutePath,
                    ByteArray(0), // databaseEncryptionKey; empty means no database encryption
                    true, // useFileDatabase
                    true, // useChatInfoDatabase
                    true, // useMessageDatabase
                    true, // useSecretChats
                    BuildConfig.TELEGRAM_API_ID,
                    BuildConfig.TELEGRAM_API_HASH,
                    "id",
                    Build.MODEL ?: "Android",
                    Build.VERSION.RELEASE ?: "Android",
                    BuildConfig.VERSION_NAME
                )
                send(parameters)
                setState("Menginisialisasi database Telegram…", LoginStep.NONE)
            }
            is TdApi.AuthorizationStateWaitPhoneNumber ->
                setState("Masukkan nomor telepon akun Telegram.", LoginStep.PHONE)
            is TdApi.AuthorizationStateWaitCode ->
                setState("Masukkan kode login Telegram yang dikirim ke perangkat atau aplikasi Telegram.", LoginStep.CODE)
            is TdApi.AuthorizationStateWaitPassword ->
                setState("Akun ini memakai verifikasi dua langkah. Masukkan kata sandi Telegram.", LoginStep.PASSWORD)
            is TdApi.AuthorizationStateWaitRegistration ->
                setState("Akun perlu didaftarkan. Form nama akan ditambahkan pada tahap berikutnya.", LoginStep.REGISTRATION)
            is TdApi.AuthorizationStateReady ->
                mutableState.value = UiState("Terhubung ke Telegram", LoginStep.READY, true)
            is TdApi.AuthorizationStateLoggingOut -> setState("Keluar dari akun Telegram…", LoginStep.NONE)
            is TdApi.AuthorizationStateClosing -> setState("Menutup koneksi Telegram…", LoginStep.NONE)
            is TdApi.AuthorizationStateClosed -> setState("Koneksi TDLib ditutup.", LoginStep.NONE)
            else -> setState("Menunggu status otorisasi Telegram…", LoginStep.NONE)
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

    private fun send(function: TdApi.Function<*>) {
        val activeClient = client ?: return
        activeClient.send(function) { result ->
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
