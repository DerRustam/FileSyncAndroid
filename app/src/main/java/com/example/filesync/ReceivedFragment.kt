package com.example.filesync

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filesync.fileproceder.FileModel
import kotlinx.android.synthetic.main.fragment_received.*
import kotlinx.android.synthetic.main.item_download.*
import kotlinx.android.synthetic.main.item_download.view.*
import kotlinx.android.synthetic.main.item_download_file.view.*
import java.io.Serializable
import java.lang.Exception
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.Year
import java.util.*
import kotlin.collections.ArrayList

data class DownloadModel(
    var dwDate : Long,
    var dwDateString : String,
    val modelList : ArrayList<FileModel> = ArrayList()
) : Serializable

interface OnReceiveListener {
    fun onClick(uri : Uri?)
    fun onLongItemClick()
}

class ReceivedFragment : Fragment (), OnReceiveListener, OrientationSensable{
    private lateinit var mCallback : OnReceiveListener
    private lateinit var fAdapter : DownloadsRecyclerAdapter
    private val positiveBClick = {dialog : DialogInterface, which : Int ->
        clearNotifications()
    }
    private val negativeBClick = {dialog : DialogInterface, which : Int ->

    }
    private var upDay: Int = 0
    private var upMonth: Int = 0
    private var upYear: Int = 0
    private fun initListeners() {
        fAdapter.onLongClickListener = {mCallback.onLongItemClick()}
        //fAdapter.onItemClickListener = {mCallback.onClick()}
    }


    companion object {
        fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    class Builder {
        var pauseDay = 0
        var pauseMonth = 0
        var pauseYear = 0
        var downloadsList = ArrayList<DownloadModel>()
        lateinit var appContext: Context
        fun build(): ReceivedFragment {
            val fragment = ReceivedFragment()
            fragment.fAdapter = DownloadsRecyclerAdapter(appContext)
            fragment.initListeners()
            fragment.upDay = pauseDay
            fragment.upMonth = pauseMonth
            fragment.upYear = pauseYear
            fragment.fAdapter.addDownloadItems(downloadsList)
            return fragment
        }
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
        return inflater.inflate(R.layout.fragment_received,container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        rv_received?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = fAdapter
        }
        fAdapter.setColCountOrient(resources.configuration.orientation)

        tl_received.setOnLongClickListener(this::onLongClick)
        val curDate = Calendar.getInstance()
        val curDay = curDate.get(Calendar.DATE)
        val curMonth = curDate.get(Calendar.MONTH)
        val curYear = curDate.get(Calendar.YEAR)
        if (curDay != upDay || curMonth != upMonth || curYear != upYear){
            upDay = curDay
            upMonth = curMonth
            upYear = curYear
            updateDownloadDates()
            fAdapter.updateDownloads()
        }
    }

    override fun onOrientationChanged(orientation: Int) {
        fAdapter.setColCountOrient(resources.configuration.orientation)
        fAdapter.notifyDataSetChanged()
    }

    @SuppressLint("SimpleDateFormat")
    private fun updateDownloadDates(){
        val dateFormatter = SimpleDateFormat("MMM dd")
        val calendar = Calendar.getInstance()
        calendar.set(upYear, upMonth,upDay)
        val iterator = fAdapter.dlList.listIterator(fAdapter.dlList.size)
        with(iterator){
            while (hasPrevious() )
            {
                with(previous()){
                    if (fAdapter.dlList.size > 6)
                        iterator.remove()
                    else{
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = dwDate
                        dwDateString = if (cal.get(Calendar.DATE) == calendar.get(Calendar.DATE)
                            && cal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
                            && cal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)){
                            "Today"
                        } else{
                            dateFormatter.format(dwDate)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.clear()
        inflater?.inflate(R.menu.menu_options_recv, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        fAdapter
        /*outState.run {
            this.putSerializable("notifications", fAdapter.dlList as Serializable)
        }*/
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        /*savedInstanceState?.let {
            fAdapter.
        }*/
        checkDownloadItems()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let{
            clearNotifications()
        }
        return true
    }

    fun onLongClick(v : View?) : Boolean{
        onLongItemClick()
        return true
    }

    override fun onLongItemClick() {
        val builder = AlertDialog.Builder(this.context)
        with(builder){
            setMessage(getString(R.string.msg_clear_recv))
            setPositiveButton("OK", positiveBClick)
            setNegativeButton("Cancel",   negativeBClick)
            show()
        }

    }

    fun clearNotifications () {
        fAdapter.clearAll()
        checkDownloadItems()
    }

    override fun onClick(uri: Uri?) {
        uri?.toString()?.isNotEmpty()?.let{
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try{
            mCallback = this
        }
        catch (ex : Exception) {
            Log.e("EX", "Recv fragment attach exception")
        }
    }

    private fun checkDownloadItems(){
        if (fAdapter.dlList.isEmpty()){
            ll_empty_received.visibility = View.VISIBLE
            rv_received.visibility = View.GONE
        }
        else{
            ll_empty_received.visibility = View.GONE
            rv_received.visibility = View.VISIBLE
        }
    }

    fun getDownloadModels() : ArrayList<DownloadModel>{
        return fAdapter.dlList
    }

    fun getUpDay() : Int{
        return upDay
    }

    fun getUpMonth() : Int{
        return upMonth
    }
    fun getUpYear() : Int{
        return upYear
    }
}

class DownloadsRecyclerAdapter(context: Context) : RecyclerView.Adapter<DownloadsRecyclerAdapter.ViewHolderDownload>(){
    var onLongClickListener: (() -> Unit)? = null
    var onItemClickListener: ((Uri) -> Unit)? = null
    private var filesColCount : Int = 2
    private val drItem = context.resources.getDrawable(R.drawable.border_item, null)
    val mInflater : LayoutInflater =
        context.applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val dlList = ArrayList<DownloadModel>()

    fun setColCountOrient(orientation: Int){
        filesColCount = if (orientation == Configuration.ORIENTATION_PORTRAIT) 2 else 3
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderDownload {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_download, parent, false)
        return ViewHolderDownload(view)
    }

    fun addDownloadItems(arr : ArrayList<DownloadModel>){
        dlList.addAll(arr)
        notifyDataSetChanged()
    }

    fun updateDownloads() {
        notifyDataSetChanged()
    }

    fun clearAll(){
        dlList.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return dlList.size
    }

    override fun onBindViewHolder(holder: ViewHolderDownload, position: Int) = holder.bindView(position)

    inner class ViewHolderDownload(itemViewDownload: View) :
        RecyclerView.ViewHolder(itemViewDownload), View.OnClickListener, View.OnLongClickListener{
        override fun onClick(v: View?) {
            onItemClickListener?.invoke(v?.tag as Uri)
        }

        override fun onLongClick(v: View?): Boolean {
            onLongClickListener?.invoke()
            return true
        }

        fun bindView(position: Int){
            val dModel = dlList[position]
            itemView.tv_download_date.text = dModel.dwDateString
            (itemView.ll_download_files as ViewGroup).removeAllViews()
            itemView.ll_download_files.columnCount = filesColCount
            dModel.modelList.forEach{
                val view : ConstraintLayout = mInflater.
                    inflate(R.layout.item_download_file, itemView.ll_download_files,false)
                as ConstraintLayout
                if (it.thumbnail != null){
                    view.iv_dw_file_icon.setImageBitmap(it.thumbnail)
                    view.iv_dw_file_icon.visibility = View.VISIBLE
                    view.tv_dw_extension.visibility = View.GONE
                }
                else{
                    view.tv_dw_extension.text = it.extension
                    view.iv_dw_file_icon.visibility = View.GONE
                    view.tv_dw_extension.visibility = View.VISIBLE
                }
                view.tv_download_file_name.text = it.name
                view.tv_download_file_size.text = it.sizeMB
                view.tag = it.uri
                itemView.ll_download_files.addView(view)
                view.setOnLongClickListener(this)
                view.setOnClickListener(this)
            }
        }
    }
}