package de.martin_gutsche.campusdualstundenplan

import com.beust.klaxon.Json
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import java.text.SimpleDateFormat
import java.util.*

class CampusDualCalendarEvent(
        @Json(name = "title") val title: String,
        @Json(name = "start") val startString: String,
        @Json(name = "end") val endString: String,
        @Json(name = "allDay") val allDay: Boolean,
        @Json(name = "description") val description: String,
        @Json(name = "color") val color: String,
        @Json(name = "editable") val editable: Boolean,
        @Json(name = "room") val room: String,
        @Json(name = "sroom") val sroom: String,
        @Json(name = "instructor") val instructor: String,
        @Json(name = "sinstructor") val sinstructor: String,
        @Json(name = "remarks") val remarks: String
) {
    // Summary
    private var summary: String = if (title.contains("-")) { // remove "WI-"
        title.split("-")[1]
    } else {
        title
    }

    init {
        if (instructor != "" || sinstructor != "") {
            summary += if (sinstructor != "" && instructor != sinstructor) {
                " ($instructor, $sinstructor)"
            } else {
                " ($instructor)"
            }
        }
    }

    // Description
    private var desc: String = if (description == remarks) {
        description
    } else if (description != "" && remarks != "") {
        "$description; $remarks"
    } else {
        // is only reached when desc OR remarks is set
        "$description$remarks"
    }

    // Location
    private val location: String = if (sroom == "" || room == sroom) {
        room
    } else {
        "$room ($sroom)"
    }

    // Start
    private val start: EventDateTime = EventDateTime()
            .setDateTime(DateTime(dateFormat.parse(startString)))
            .setTimeZone("Europe/Berlin")

    // End
    private val end: EventDateTime = EventDateTime()
            .setDateTime(DateTime(dateFormat.parse(endString)))
            .setTimeZone("Europe/Berlin")


    fun toGoogle(): Event {
        return Event()
                .setSummary(summary)
                .setLocation(location)
                .setDescription(desc)
                .setStart(start)
                .setEnd(end)
                .setColorId("8")
    }

    override fun toString(): String {
        return "{\"title\":\"$title\",\"start\":\"$startString\",\"end\":\"$endString\",\"allDay\":$allDay,\"description\":\"$description\",\"color\":\"$color\",\"editable\":$editable,\"room\":\"$room\",\"sroom\":\"$sroom\",\"instructor\":\"$instructor\",\"sinstructor\":\"$sinstructor\",\"remarks\":\"$remarks\"}"
    }

    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.GERMAN).apply {
            timeZone = TimeZone.getTimeZone("Europe/Berlin")
        }
    }
}