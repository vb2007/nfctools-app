package hu.vb2007.nfctool.di

import hu.vb2007.nfctool.nfc.NfcController
import hu.vb2007.nfctool.nfc.read.NdefParser
import hu.vb2007.nfctool.nfc.read.TagReader
import hu.vb2007.nfctool.nfc.read.TagTypeDetector
import hu.vb2007.nfctool.nfc.read.UltralightDumpReader
import hu.vb2007.nfctool.nfc.write.NdefWriter
import hu.vb2007.nfctool.ui.read.ReadViewModel
import hu.vb2007.nfctool.ui.write.WriteViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { NdefParser() }
    single { TagTypeDetector() }
    single { UltralightDumpReader() }
    single { TagReader(get(), get(), get()) }
    single { NdefWriter() }
    single { NfcController(get(), get(), get()) }

    viewModel { ReadViewModel(get()) }
    viewModel { WriteViewModel(get()) }
}
