package hu.vb2007.nfctool

import android.app.Application
import hu.vb2007.nfctool.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NfcToolApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@NfcToolApp)
            modules(appModule)
        }
    }
}
