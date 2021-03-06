package uk.whitecrescent.waqti.backend.task

import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import java.util.concurrent.TimeUnit

var DEBUG = true

//region Type Aliases

// Type Aliases for more readable code
typealias Description = String

typealias Optional = Boolean
typealias Target = String
typealias ID = Long

//endregion Type Aliases

//region Properties & Constraints


// Is Visible?
const val HIDDEN = false
const val SHOWING = true

// Is Constrained?
const val CONSTRAINED = true
const val NOT_CONSTRAINED = false

// Is Met?
const val MET = true
const val UNMET = false

//endregion Properties & Constraints

// Used for Optionals
const val OPTIONAL = true
const val MANDATORY = false

//region Task Things

// Used for Tasks
const val DEFAULT_FAILABLE = false
const val DEFAULT_KILLABLE = true
val DEFAULT_TASK_STATE = TaskState.EXISTING

//endregion Task Things

//region Default Property Values

val DEFAULT_TIME: LocalDateTime = LocalDateTime.MIN
val DEFAULT_DURATION: Duration = Duration.ZERO
val DEFAULT_PRIORITY = Priority("", -1)
val DEFAULT_LABELS_LIST = arrayListOf<Label>()
const val DEFAULT_OPTIONAL = MANDATORY
const val DEFAULT_DESCRIPTION = ""
val DEFAULT_CHECKLIST = Checklist()
const val DEFAULT_TARGET = ""
val DEFAULT_DEADLINE: LocalDateTime = LocalDateTime.MAX
const val DEFAULT_TASK_ID: ID = 0L
val DEFAULT_SUB_TASKS = arrayListOf<ID>()

//endregion Default Property Values

//region Default Properties

val DEFAULT_TIME_PROPERTY = TimeProperty(HIDDEN, DEFAULT_TIME, NOT_CONSTRAINED, UNMET)
val DEFAULT_DURATION_PROPERTY = DurationProperty(HIDDEN, DEFAULT_DURATION, NOT_CONSTRAINED, UNMET)
val DEFAULT_PRIORITY_PROPERTY = PriorityProperty(HIDDEN, DEFAULT_PRIORITY, NOT_CONSTRAINED, UNMET)
val DEFAULT_LABELS_PROPERTY = LabelArrayListProperty(HIDDEN, DEFAULT_LABELS_LIST, NOT_CONSTRAINED, UNMET)
val DEFAULT_OPTIONAL_PROPERTY = BooleanProperty(HIDDEN, DEFAULT_OPTIONAL, NOT_CONSTRAINED, UNMET)
val DEFAULT_DESCRIPTION_PROPERTY = StringProperty(HIDDEN, DEFAULT_DESCRIPTION, NOT_CONSTRAINED, UNMET)
val DEFAULT_CHECKLIST_PROPERTY = ChecklistProperty(HIDDEN, DEFAULT_CHECKLIST, NOT_CONSTRAINED, UNMET)
val DEFAULT_TARGET_PROPERTY = StringProperty(HIDDEN, DEFAULT_TARGET, NOT_CONSTRAINED, UNMET)
val DEFAULT_DEADLINE_PROPERTY = TimeProperty(HIDDEN, DEFAULT_DEADLINE, NOT_CONSTRAINED, UNMET)
val DEFAULT_BEFORE_PROPERTY = LongProperty(HIDDEN, DEFAULT_TASK_ID, NOT_CONSTRAINED, UNMET)
val DEFAULT_SUB_TASKS_PROPERTY = LongArrayListProperty(HIDDEN, DEFAULT_SUB_TASKS, NOT_CONSTRAINED, UNMET)

//endregion Default Properties

// Used for deadlines
var GRACE_PERIOD: Duration = Duration.ZERO

//region Concurrency

// Used for how often will Observers check stuff on the background threads
const val TIME_CHECKING_PERIOD = 1000L
val TIME_CHECKING_UNIT = TimeUnit.MILLISECONDS

//endregion Concurrency