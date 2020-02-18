package de.martin_gutsche.campusdualstundenplan

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.beust.klaxon.Klaxon
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class CalendarWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        // Migration to v2
        // calendar_id.json only existed prior to 2.0.0 in current versions the file is called calendar_id.txt

//        reset(getService())

        val file = File(applicationContext.filesDir, "calendar_id.json")
        if (file.exists()) {
            reset(getService())
            file.delete()
        }

        // Recreate Campus Dual User
        val loginData = Util.getLoginData(applicationContext) ?: return Result.retry()
        val username = loginData.getString("username")
        val hash = loginData.getString("hash")

        val campusDualUser = CampusDualUser(username, hash)

        // Get Calendars
        val storedCal = Klaxon().parseArray<CampusDualCalendarEvent>(Util.getCalendarString(applicationContext))!!
        val freshCal = campusDualUser.lectures

        val first = max(min(storedCal.size, freshCal.size) - 400, 0)

        // Comparison using contains does not work as expected
        // val removed = storedCal.filter { !freshCal.contains(it) }
        val removed = mutableListOf<CampusDualCalendarEvent>().apply { addAll(storedCal.subList(first, storedCal.size)) }
        for (stored in storedCal.subList(first, storedCal.size)) {
            for (fresh in freshCal.subList(first, freshCal.size)) {
                if (stored.toString() == fresh.toString()) {
                    removed.remove(stored)
                    break
                }
            }
        }

        // Comparison using contains does not work as expected
        // val new = freshCal.filter { !storedCal.contains(it) }
        val new = mutableListOf<CampusDualCalendarEvent>().apply { addAll(freshCal.subList(first, freshCal.size)) }
        for (fresh in freshCal.subList(first, freshCal.size)) {
            for (stored in storedCal) {
                if (fresh.toString() == stored.toString()) {
                    new.remove(fresh)
                    break
                }
            }
        }

        if (new.isNotEmpty() || removed.isNotEmpty()) {
            val service: Calendar = getService()

            val calendarId: String = if (Util.getCalendarId(applicationContext) != null && Util.getCalendarId(applicationContext) != "") {
                Util.getCalendarId(applicationContext)!!
            } else {
                createCalendar(service)!!.id
            }

            val db = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java, "event-mapping"
            ).build()

            // remove old events
            if (removed.isNotEmpty()) {
                for (i in 0..ceil((removed.size / 50).toDouble()).toInt()) {
                    val batch = service.batch().apply { batchUrl = BATCH_URL }
                    for (cdEvent in removed.subList(i * 50, min(((i + 1) * 50), removed.size))) {
                        val eventMapping = db.eventMappingDao().get(cdEvent.toString())
                        service.events().delete(calendarId, eventMapping.id).queue(batch,
                                object : JsonBatchCallback<Void>() {
                                    override fun onSuccess(event: Void?, responseHeaders: HttpHeaders) {
                                        db.eventMappingDao().delete(EventMapping(cdEvent.toString(), ""))
                                    }

                                    override fun onFailure(e: GoogleJsonError, responseHeaders: HttpHeaders) {
                                        println(e.errors)
                                    }
                                })
                    }
                    batch.execute()
                }
            }

            // insert new events
            if (new.isNotEmpty()) {
                for (i in 0..ceil((new.size / 50).toDouble()).toInt()) {
                    val batch = service.batch().apply { batchUrl = BATCH_URL }
                    for (cdEvent in new.subList(i * 50, min(((i + 1) * 50), new.size))) {
                        service.events().insert(calendarId, cdEvent.toGoogle()).queue(batch,
                                object : JsonBatchCallback<Event>() {
                                    override fun onSuccess(event: Event, responseHeaders: HttpHeaders) {
                                        try {
                                            db.eventMappingDao().insert(EventMapping(cdEvent.toString(), event.id))
                                        } catch (e: SQLiteConstraintException) {
                                            println(cdEvent.toString())
                                            // TODO wouldn't happen ideally but we need to figure out how to reset here
                                        }
                                    }

                                    override fun onFailure(e: GoogleJsonError, responseHeaders: HttpHeaders) {
                                        println(e.errors)
                                    }
                                })
                    }
                    batch.execute()
                }
            }

            db.close()

            val sb = StringBuilder()
            sb.append("[")
            for (lecture in freshCal) {
                sb.append(lecture.toString())
            }
            sb.append("]")
            Util.saveCalendarString(sb.toString(), applicationContext)
        }
        return Result.success()
    }

    private fun reset(service: Calendar) {
        println("resetting...")
        // Delete Google Calendar
        val calId = try {
            applicationContext.openFileInput("calendar_id.json").bufferedReader().use {
                it.readText()
            }
        } catch (e: FileNotFoundException) {
            return
        }
        if (calId != "") service.calendars().delete(calId).execute()
        Util.saveCalendarId("", applicationContext)

        // Delete Room
        val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "event-mapping"
        ).build()
        db.clearAllTables()
        db.close()

        // Delete storedCal
        Util.saveCalendarString("[]", applicationContext)
    }


    @Throws(NullPointerException::class)
    private fun getService(): Calendar {
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        val mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(applicationContext)
        val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext,
                setOf(applicationContext.getString(R.string.gscope))
        )
        credential.selectedAccount = mGoogleSignInAccount!!.account

        return Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(applicationContext.getString(R.string.app_name))
                .build()
    }

    private fun createCalendar(service: Calendar): com.google.api.services.calendar.model.Calendar? {
        val createdCalendar: com.google.api.services.calendar.model.Calendar
        val calendar = com.google.api.services.calendar.model.Calendar()
        calendar.summary = applicationContext.getString(R.string.calendar_name)
        calendar.timeZone = "Europe/Berlin"
        try {
            createdCalendar = service.calendars().insert(calendar).execute()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        Util.saveCalendarId(createdCalendar.id, applicationContext)
        return createdCalendar
    }

    companion object {
        private val BATCH_URL = GenericUrl("https://www.googleapis.com/batch/calendar/v3")
        private val HTTP_TRANSPORT = NetHttpTransport()
        private val JSON_FACTORY = JacksonFactory.getDefaultInstance()
    }
}