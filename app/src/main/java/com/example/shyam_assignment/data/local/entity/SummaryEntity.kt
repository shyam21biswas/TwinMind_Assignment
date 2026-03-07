package com.example.shyam_assignment.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "summaries",
    foreignKeys = [
        ForeignKey(
            entity = RecordingSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SummaryEntity(
    @PrimaryKey
    val sessionId: String,
    val title: String = "",
    val summary: String = "",
    val actionItemsJson: String = "[]",
    val keyPointsJson: String = "[]",
    val status: String = SummaryStatus.PENDING,
    val errorMessage: String? = null,
    val partialContent: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

object SummaryStatus {
    const val PENDING = "PENDING"
    const val GENERATING = "GENERATING"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
}

