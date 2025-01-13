package com.github.livingwithhippos.unchained.di

import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.local.ProtoStoreImpl
import org.koin.dsl.module

val datastoreModule = module {
    single<ProtoStore> { ProtoStoreImpl(get()) }
}