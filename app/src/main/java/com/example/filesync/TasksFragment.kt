package com.example.filesync

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_tasks.*
import kotlinx.android.synthetic.main.item_task.view.*
import kotlinx.android.synthetic.main.item_task_file.view.*

enum class SendStatus{
    STATUS_WAIT {
    override fun toString(): String {
        return "Waiting for device"
    }
    },
    STATUS_SEND {
        override fun toString(): String {
            return "Sending..."
        }
    },
    STATUS_FILE_NOT_FOUND{
        override fun toString(): String {
            return "File not found!"
        }
    },
    STATUS_COMPLETED{
        override fun toString(): String {
            return "Sended"
        }
    },
}

data class FileSendingStatus(
    val fileName : String,
    val status: SendStatus
)

data class TaskModel(
    val adjDeviceName : String,
    val modelList : ArrayList<FileSendingStatus> = ArrayList()
)

data class SelectedTaskModel(
    val adjDevName: String,
    val filesList : ArrayList<String> = ArrayList()
)

interface OnTaskClickListener {
    fun onClick()
    fun onLongClick()
}

class TasksFragment :Fragment(), OnTaskClickListener{

    private lateinit var mCallback : OnTaskClickListener
    private lateinit var fAdapter : TasksRecyclerAdapter
    private var optionsMenu : Menu? = null
    private var statSelected : Boolean = false
    private val positiveBClick = { dialog : DialogInterface, which : Int ->
        removeSelectedTasks()
    }
    companion object {
        fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }
    private fun initListeners() {
        fAdapter.onItemLongClickListener = { mCallback.onLongClick() }
    }

    class Builder {
        lateinit var appContext : Context
        var tasksList : ArrayList<TaskModel> = ArrayList()
        fun build(): TasksFragment {
            val fragment = TasksFragment()
            fragment.fAdapter = TasksRecyclerAdapter(appContext)
            fragment.initListeners()
            fragment.fAdapter.addTaskItems(tasksList)
            return fragment
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mCallback = this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.clear()
        inflater?.inflate(R.menu.menu_options_tasks, menu)
        optionsMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        //TODO
        return super.onOptionsItemSelected(item)
    }

    private fun initViews(){
        rv_tasks.layoutManager = LinearLayoutManager(context)
        rv_tasks.adapter = fAdapter
        tl_tasks.setOnLongClickListener(this::onLongClick)
        /*fAdapter.taskList.clear()
        val dItem = TaskModel(adjDeviceName = "SecondEmul")
        dItem.modelList.add(FileSendingStatus("images",SendStatus.STATUS_WAIT))
        dItem.modelList.add(FileSendingStatus("user-male",SendStatus.STATUS_WAIT))
        fAdapter.addTaskItems(arrayListOf(dItem))*/
        updateTableStatus()
    }

    override fun onClick() {
        updateTableStatus()
    }

    override fun onLongClick() {
        if (fAdapter.selectedList.isNotEmpty()){
            val builder = AlertDialog.Builder(this.context)
            with(builder){
                setMessage(getString(R.string.msg_canclel_selected_tasks))
                setPositiveButton("OK", positiveBClick)
                setNegativeButton("Cancel") { _, _ -> }
                show()
                }
            }
    }

    private fun onLongClick(v : View) :Boolean {
        onLongClick()
        return true
    }

    private fun removeSelectedTasks(){
        fAdapter.removeSelectedTasks()
    }

    private fun updateTableStatus(){
        statSelected = fAdapter.selectedList.isNotEmpty()
        val isTasks = fAdapter.taskList.isNotEmpty()
        optionsMenu?.apply {
            findItem(R.id.item_clear_selected_tasks)?.
                isVisible = statSelected
            findItem(R.id.item_clear_tasks)?.
                isVisible = isTasks
        }
        ll_empty_tasks?.visibility = if(isTasks) View.GONE else View.VISIBLE
    }

    fun getTasksModels() : ArrayList<TaskModel> {
        return fAdapter.taskList
    }
}

class TasksRecyclerAdapter(context: Context) : RecyclerView.Adapter<TasksRecyclerAdapter.ViewHolderTask>(){
    private val drItemSelected = context.resources.getDrawable(R.drawable.border_item_selected, null)
    private val drItem = context.resources.getDrawable(R.drawable.border_item, null)
    var onItemClickListener: (() -> Unit)? = null
    var onItemLongClickListener: (() -> Unit)? = null
    val mInflater : LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val taskList = ArrayList<TaskModel>()
    val selectedList = ArrayList<SelectedTaskModel>()

    fun addTaskItems(arr : ArrayList<TaskModel>){
        taskList.addAll(arr)
        notifyDataSetChanged()
    }

    fun removeSelectedTasks(){
        var devName : String
        selectedList.forEach{tModel->
            devName = tModel.adjDevName
            val selList = tModel.filesList
            val list = getAllDevTasks(devName)
            list?.let{
                val iterator = it.iterator()
                with(iterator){
                    while (hasNext()){
                        with(next()){
                            if (selList.contains(fileName))
                                iterator.remove()
                        }
                    }
                }
            }
        }
        removeEmptyDevTasks()
        selectedList.clear()
        notifyDataSetChanged()
    }

    fun removeEmptyDevTasks(){
        val iterator = taskList.iterator()
        with(iterator){
            while(hasNext()){
                with(next()){
                    if (modelList.isEmpty())
                        remove()
                }
            }
        }
    }

    fun removeIfEmptySelected(devName : String){
        val iterator = selectedList.iterator()
        with(iterator){
            while (hasNext()){
                with(next()){
                    if (adjDevName == devName){
                        if (filesList.isEmpty()){
                            remove()
                        }
                        return
                    }
                }
            }
        }
    }

    fun updateTasks() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolderTask {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return ViewHolderTask(view)
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    private fun getSelDevTasks(adjDevName: String) : ArrayList<String>{
        for (task in selectedList){
            if (task.adjDevName == adjDevName){
                return task.filesList
            }
        }
        val tm = SelectedTaskModel(adjDevName)
        selectedList.add(tm)
        return tm.filesList
    }

    private fun getAllDevTasks(adjDevName: String) : ArrayList<FileSendingStatus>?{
        for (task in taskList){
            if (task.adjDeviceName == adjDevName){
                return task.modelList
            }
        }
        return null
    }

    private fun isFileInSelected(adjDevName: String, fileName: String) : Boolean{
        for (sel in selectedList){
            if (sel.adjDevName == adjDevName){
                return sel.filesList.contains(fileName)
            }
        }
        return false
    }

    override fun onBindViewHolder(holder: ViewHolderTask, position: Int)
            = holder.bindView(position)

    inner class ViewHolderTask(itemViewTask: View) : RecyclerView.ViewHolder(itemViewTask),
        View.OnLongClickListener, View.OnClickListener{

        init {
            itemView.tv_task_device.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onLongClick(v: View?): Boolean {
            onItemLongClickListener?.invoke()
            return true
        }

        override fun onClick(v: View) {
            val llList = (v.parent as ConstraintLayout).ll_task_files
            val devName = (v as TextView).text.toString()
            val listSel = getSelDevTasks(devName)
            if (listSel.isEmpty()){
                val allTasks = getAllDevTasks(devName)
                allTasks?.forEach{
                    listSel.add(it.fileName)
                }
                for (view in llList.children){
                    view.background = drItemSelected
                    view.tag = true
                }
            }
            else {
                //val list = getAllDevTasks(devName)
                listSel.clear()
                for (view in llList.children){
                    view.background = drItem
                    view.tag = false
                }
            }
            onItemClickListener?.invoke()
        }

        private fun singleItemClick(v: View){
            if (v.tag == false){
                v.tag = true
                v.background = drItemSelected
                val list = getSelDevTasks((v.parent as LinearLayout).tag as String)
                list.add(v.tv_task_file_name.text.toString())
            }
            else{
                v.tag = false
                v.background = drItem
                val devName = (v.parent as LinearLayout).tag as String
                val list = getSelDevTasks(devName)
                list.remove(v.tv_task_file_name.text.toString())
                removeIfEmptySelected(devName)
            }
        }

        fun bindView(position: Int){
            val dModel = taskList[position]
            val dName = dModel.adjDeviceName
            itemView.tv_task_device.text = dName
            itemView.ll_task_files.tag = dName
            dModel.modelList.forEach{fss ->
                val v : ConstraintLayout = mInflater.
                        inflate(R.layout.item_task_file, itemView.ll_task_files, false)
                        as ConstraintLayout
                if (isFileInSelected(dName, fss.fileName)){
                    v.tag = true
                    v.background = drItemSelected
                }
                else{
                    v.tag = false
                    v.background = drItem
                }
                    v.tv_task_file_name.text = fss.fileName
                    v.tv_send_status.text = fss.status.toString()
                    if (fss.status == SendStatus.STATUS_SEND) {
                        v.pb_task_file.visibility = View.VISIBLE
                    } else {
                        v.pb_task_file.visibility = View.GONE
                    }
                itemView.ll_task_files.addView(v)
                v.setOnClickListener(this::singleItemClick)
                v.setOnLongClickListener(this)
            }
        }
    }
}