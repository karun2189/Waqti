package uk.whitecrescent.waqti.frontend.fragments.create

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import kotlinx.android.synthetic.main.fragment_create_task.*
import kotlinx.android.synthetic.main.property_card.*
import kotlinx.android.synthetic.main.property_card.view.*
import uk.whitecrescent.waqti.R
import uk.whitecrescent.waqti.Time
import uk.whitecrescent.waqti.backend.persistence.Caches
import uk.whitecrescent.waqti.backend.task.DEFAULT_DESCRIPTION
import uk.whitecrescent.waqti.backend.task.DEFAULT_TIME
import uk.whitecrescent.waqti.backend.task.ID
import uk.whitecrescent.waqti.backend.task.Task
import uk.whitecrescent.waqti.frontend.GoToFragment
import uk.whitecrescent.waqti.frontend.addAfterTextChangedListener
import uk.whitecrescent.waqti.frontend.customview.dialogs.DateTimePickerDialog
import uk.whitecrescent.waqti.frontend.customview.dialogs.EditTextDialog
import uk.whitecrescent.waqti.frontend.fragments.parents.WaqtiCreateFragment
import uk.whitecrescent.waqti.getViewModel
import uk.whitecrescent.waqti.hideSoftKeyboard
import uk.whitecrescent.waqti.isNotDefault
import uk.whitecrescent.waqti.requestFocusAndShowSoftKeyboard
import uk.whitecrescent.waqti.rfcFormatted

@Suppress("NOTHING_TO_INLINE")
class CreateTaskFragment : WaqtiCreateFragment<Task>() {

    private lateinit var viewModel: CreateTaskFragmentViewModel
    private var boardID: ID = 0L
    private var listID: ID = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create_task, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = getViewModel()

        boardID = mainActivityViewModel.boardID
        listID = mainActivityViewModel.listID

        setUpViews()
    }

    override fun setUpViews() {

        taskName_editText.apply {
            requestFocusAndShowSoftKeyboard()
            addAfterTextChangedListener {
                if (it != null) {
                    addTask_button.isVisible = !(it.isEmpty() || it.isBlank())
                }
            }
        }

        addTask_button.apply {
            isVisible = false
            setOnClickListener {
                Caches.boards[boardID][listID].add(createElement()).update()
                finish()
            }
        }

        setUpTimeViews()

        setUpDeadlineViews()

        setUpDescriptionViews()

    }

    @SuppressLint("SetTextI18n")
    private inline fun setUpTimeViews() {
        taskTime_propertyCard.apply {
            onClick {
                DateTimePickerDialog().apply {
                    initialTime = viewModel.taskTime
                    onConfirm = {
                        viewModel.taskTime = it
                        this@CreateTaskFragment.title_textView.text = getString(R.string.timeColon) + it.rfcFormatted
                        dismiss()
                    }
                }.show(mainActivity.supportFragmentManager, "")
            }
            clear_imageButton.setOnClickListener {
                viewModel.taskTime = DEFAULT_TIME
                this@CreateTaskFragment.title_textView.text = getString(R.string.selectTimeProperty)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private inline fun setUpDeadlineViews() {
        taskDeadline_propertyCard.apply {
            onClick {
                DateTimePickerDialog().apply {
                    initialTime = viewModel.taskDeadline
                    onConfirm = {
                        viewModel.taskDeadline = it
                        this@CreateTaskFragment.title_textView.text = getString(R.string.deadlineColon) + it.rfcFormatted
                        dismiss()
                    }
                }.show(mainActivity.supportFragmentManager, "")
            }
            clear_imageButton.setOnClickListener {
                viewModel.taskDeadline = DEFAULT_TIME
                this@CreateTaskFragment.title_textView.text = getString(R.string.selectDeadlineProperty)
            }
        }
    }

    private inline fun setUpDescriptionViews() {
        taskDescription_propertyCard.apply {
            onClick {
                EditTextDialog().apply {
                    hint = this@CreateTaskFragment.getString(R.string.enterDescription)
                    initialText = viewModel.taskDescription
                    onConfirm = {
                        viewModel.taskDescription = it
                        this@CreateTaskFragment.title_textView.text = it
                        dismiss()
                    }
                }.show(mainActivity.supportFragmentManager, "")
            }
            clear_imageButton.setOnClickListener {
                viewModel.taskDescription = DEFAULT_DESCRIPTION
                this@CreateTaskFragment.title_textView.text = getString(R.string.selectDescriptionProperty)
            }
        }
    }

    override fun createElement(): Task {
        return Task(taskName_editText.text.toString()).apply {
            setTime()
            setDeadline()
            setDescription()
        }
    }

    private inline fun Task.setTime() {
        viewModel.taskTime.also {
            if (it.isNotDefault) {
                if (taskTime_propertyCard.constraint_checkBox.isChecked) setTimeConstraintValue(it)
                else setTimePropertyValue(it)
            }
        }
    }

    private inline fun Task.setDeadline() {
        viewModel.taskDeadline.also {
            if (it.isNotDefault) {
                if (taskDeadline_propertyCard.constraint_checkBox.isChecked) setDeadlineConstraintValue(it)
                else setDeadlinePropertyValue(it)
            }
        }
    }

    private inline fun Task.setDescription() {
        viewModel.taskDescription.also {
            if (it.isNotDefault) setDescriptionValue(it)
        }
    }

    override fun finish() {
        taskName_editText.hideSoftKeyboard()
        @GoToFragment
        mainActivity.supportFragmentManager.popBackStack()
    }

}

class CreateTaskFragmentViewModel : ViewModel() {

    var taskTime: Time = DEFAULT_TIME
    var taskDeadline: Time = DEFAULT_TIME
    var taskDescription: String = DEFAULT_DESCRIPTION

}
