package com.maksimowiczm.foodyou.common.infrastructure.room.tag

import com.maksimowiczm.foodyou.common.domain.tag.TagRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.scope.Scope
import org.koin.dsl.bind

fun Module.tagModule() {
    factory { database.tagDao }

    factoryOf(::RoomTagRepository).bind<TagRepository>()
}

private val Scope.database: TagDatabase
    get() = get()
