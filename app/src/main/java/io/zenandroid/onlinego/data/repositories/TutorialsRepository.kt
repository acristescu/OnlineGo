package io.zenandroid.onlinego.data.repositories

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.local.Tutorial
import io.zenandroid.onlinego.data.model.local.TutorialGroup
import io.zenandroid.onlinego.data.model.local.TutorialStep
import io.zenandroid.onlinego.data.model.local.TutorialStep.*
import okio.BufferedSource
import okio.Okio
import okio.buffer
import okio.source

class TutorialsRepository {

    private val moshiAdapter = Moshi.Builder()
            .add(PolymorphicJsonAdapterFactory.of(TutorialStep::class.java, "type")
                    .withSubtype(Interactive::class.java, "Interactive")
                    .withSubtype(Lesson::class.java, "Lesson")
            )
            .addLast(KotlinJsonAdapterFactory())
            .build()
            .adapter<List<TutorialGroup>>(Types.newParameterizedType(List::class.java, TutorialGroup::class.java))
    private val hardcodedTutorialsData = readJSONFromResources()

    fun loadTutorial(tutorialName: String): Tutorial? {
        hardcodedTutorialsData.forEach { group ->
            group.tutorials.find {
                it.name == tutorialName
            } ?. let {
                return it
            }
        }
        return null
    }

    fun getTutorialGroups() = hardcodedTutorialsData

    private fun readJSONFromResources(): List<TutorialGroup> {
        OnlineGoApplication.instance.assets.open("tutorials.json").source().buffer().use {
            return moshiAdapter.fromJson(it)!!
        }
    }

}