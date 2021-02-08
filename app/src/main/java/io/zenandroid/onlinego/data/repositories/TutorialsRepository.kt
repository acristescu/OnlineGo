package io.zenandroid.onlinego.data.repositories

import android.preference.PreferenceManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.local.Tutorial
import io.zenandroid.onlinego.data.model.local.TutorialGroup
import io.zenandroid.onlinego.data.model.local.TutorialStep
import io.zenandroid.onlinego.data.model.local.TutorialStep.*
import okio.BufferedSource
import okio.Okio
import okio.buffer
import okio.source

private const val COMPLETED_TUTORIALS_KEY = "COMPLETED_TUTORIALS_KEY"

class TutorialsRepository : SocketConnectedRepository{

    private val moshiAdapter = Moshi.Builder()
            .add(PolymorphicJsonAdapterFactory.of(TutorialStep::class.java, "type")
                    .withSubtype(Interactive::class.java, "Interactive")
                    .withSubtype(Lesson::class.java, "Lesson")
                    .withSubtype(GameExample::class.java, "Game")
            )
            .addLast(KotlinJsonAdapterFactory())
            .build()
            .adapter<List<TutorialGroup>>(Types.newParameterizedType(List::class.java, TutorialGroup::class.java))

    private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance)

    private lateinit var hardcodedTutorialsData: List<TutorialGroup>
    private val _completedTutorialsNames = BehaviorSubject.create<Set<String>>()
    val completedTutorialsNames = _completedTutorialsNames.hide()

    init {
        val completed = prefs.getStringSet(COMPLETED_TUTORIALS_KEY, emptySet())!!
        _completedTutorialsNames.onNext(completed)
        if(!this::hardcodedTutorialsData.isInitialized) {
            Completable.fromAction {
                hardcodedTutorialsData = readJSONFromResources()
            }
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.single())
                    .subscribe()
        }
    }

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

    fun getTutorialGroups(): List<TutorialGroup> {
        if(!this::hardcodedTutorialsData.isInitialized) {
            hardcodedTutorialsData = readJSONFromResources()
        }
        return hardcodedTutorialsData
    }

    private fun readJSONFromResources(): List<TutorialGroup> {
        OnlineGoApplication.instance.assets.open("tutorials.json").source().buffer().use {
            return moshiAdapter.fromJson(it)!!
        }
    }

    fun markTutorialCompleted(tutorial: Tutorial) {
        var alreadyCompletedNames: Set<String> = _completedTutorialsNames.value!!
        if(alreadyCompletedNames.contains(tutorial.name)) {
            return
        }
        alreadyCompletedNames += tutorial.name
        prefs.edit()
                .putStringSet(COMPLETED_TUTORIALS_KEY, alreadyCompletedNames)
                .apply()
        _completedTutorialsNames.onNext(alreadyCompletedNames)
    }

    override fun onSocketConnected() {
    }

    override fun onSocketDisconnected() {

    }

}