package com.example.shyam_assignment.data.local

import com.example.shyam_assignment.data.local.dao.RecordingSessionDao
import com.example.shyam_assignment.data.local.dao.SummaryDao
import com.example.shyam_assignment.data.local.dao.TranscriptSegmentDao
import com.example.shyam_assignment.data.local.entity.RecordingSessionEntity
import com.example.shyam_assignment.data.local.entity.SessionStatus
import com.example.shyam_assignment.data.local.entity.SummaryEntity
import com.example.shyam_assignment.data.local.entity.SummaryStatus
import com.example.shyam_assignment.data.local.entity.TranscriptSegmentEntity
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val sessionDao: RecordingSessionDao,
    private val summaryDao: SummaryDao,
    private val transcriptDao: TranscriptSegmentDao
) {
    suspend fun seedIfEmpty() {
        val existing = sessionDao.getAllSessionsFlow().first()
        if (existing.isNotEmpty()) return

        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        val minute = 60_000L

        // --- Session 1: Completed with summary ---
        val session1Id = UUID.randomUUID().toString()
        sessionDao.insert(
            RecordingSessionEntity(
                sessionId = session1Id,
                title = "Q1 Product Planning",
                startedAt = now - 2 * hour,
                endedAt = now - 2 * hour + 45 * minute,
                durationMs = 45 * minute,
                status = SessionStatus.COMPLETED,
                createdAt = now - 2 * hour,
                updatedAt = now - 2 * hour + 45 * minute
            )
        )
        summaryDao.insertOrUpdate(
            SummaryEntity(
                sessionId = session1Id,
                title = "Q1 Product Planning",
                summary = "Discussed the product roadmap for Q1 2026. The team agreed on three key initiatives: improving onboarding flow, launching the mobile SDK, and upgrading the analytics dashboard.",
                actionItemsJson = """["Design new onboarding mockups by March 15","Ship mobile SDK beta by April 1","Schedule analytics dashboard review with stakeholders"]""",
                keyPointsJson = """["Mobile SDK is the highest priority","Onboarding drop-off rate is currently 34%","Analytics dashboard needs real-time filtering"]""",
                status = SummaryStatus.COMPLETED,
                updatedAt = now - 2 * hour + 46 * minute
            )
        )
        transcriptDao.insert(
            TranscriptSegmentEntity(
                id = UUID.randomUUID().toString(),
                sessionId = session1Id,
                chunkId = "seed-chunk-1",
                chunkIndex = 0,
                text = "Let's go over the Q1 priorities. First up, the onboarding flow has a 34% drop-off rate which we need to fix.",
                createdAt = now - 2 * hour + minute
            )
        )
        transcriptDao.insert(
            TranscriptSegmentEntity(
                id = UUID.randomUUID().toString(),
                sessionId = session1Id,
                chunkId = "seed-chunk-2",
                chunkIndex = 1,
                text = "The mobile SDK is our top priority. We're targeting a beta release by April 1st.",
                createdAt = now - 2 * hour + 2 * minute
            )
        )

        // --- Session 2: Completed with summary ---
        val session2Id = UUID.randomUUID().toString()
        sessionDao.insert(
            RecordingSessionEntity(
                sessionId = session2Id,
                title = "Design Review — Settings Page",
                startedAt = now - 26 * hour,
                endedAt = now - 26 * hour + 30 * minute,
                durationMs = 30 * minute,
                status = SessionStatus.COMPLETED,
                createdAt = now - 26 * hour,
                updatedAt = now - 26 * hour + 30 * minute
            )
        )
        summaryDao.insertOrUpdate(
            SummaryEntity(
                sessionId = session2Id,
                title = "Design Review — Settings Page",
                summary = "Reviewed the new settings page design. Agreed on a tabbed layout with sections for Profile, Notifications, and Privacy. Dark mode toggle placement was debated.",
                actionItemsJson = """["Finalize tab order by end of week","Add dark mode toggle to top of Appearance tab","Share updated Figma link with engineering"]""",
                keyPointsJson = """["Tabbed layout approved","Dark mode toggle should be prominent","Privacy section needs legal review"]""",
                status = SummaryStatus.COMPLETED,
                updatedAt = now - 26 * hour + 31 * minute
            )
        )

        // --- Session 3: Recorded but not yet summarized ---
        val session3Id = UUID.randomUUID().toString()
        sessionDao.insert(
            RecordingSessionEntity(
                sessionId = session3Id,
                title = "Standup — March 8",
                startedAt = now - 50 * hour,
                endedAt = now - 50 * hour + 12 * minute,
                durationMs = 12 * minute,
                status = SessionStatus.STOPPED,
                createdAt = now - 50 * hour,
                updatedAt = now - 50 * hour + 12 * minute
            )
        )
    }
}

