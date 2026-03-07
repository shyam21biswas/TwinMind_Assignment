package com.example.shyam_assignment.data.repository

import com.example.shyam_assignment.data.model.Meeting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepository @Inject constructor() {

    // In-memory store — will be replaced by Room in Part 2
    private val _meetings = MutableStateFlow<List<Meeting>>(emptyList())

    fun getMeetings(): Flow<List<Meeting>> = _meetings.asStateFlow()

    fun getMeetingById(id: String): Flow<Meeting?> =
        _meetings.map { list -> list.find { it.id == id } }

    suspend fun addMeeting(meeting: Meeting) {
        _meetings.value = _meetings.value + meeting
    }

    suspend fun updateMeeting(meeting: Meeting) {
        _meetings.value = _meetings.value.map {
            if (it.id == meeting.id) meeting else it
        }
    }

    suspend fun deleteMeeting(id: String) {
        _meetings.value = _meetings.value.filter { it.id != id }
    }
}


