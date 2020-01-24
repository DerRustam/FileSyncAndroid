package com.example.filesync


import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filesync.fileproceder.FileWorker
import com.example.filesync.fileproceder.FileModel
import kotlinx.android.synthetic.main.fragment_send.*
import kotlinx.android.synthetic.main.item_fileinfo.view.*
import java.lang.Exception

interface OnItemClickListener {
    fun onClick()
    fun onLongClick()
}

class SendFragment : Fragment(), OnItemClickListener, OrientationSensable{
    private lateinit var fAdapter : FilesRecyclerAdapter
    private lateinit var fileWorker : FileWorker
    private lateinit var mCallback : OnItemClickListener
    private var optionMenu : Menu? = null
    //private var tableUriStrings = ArrayList<Uri>()
    //private var selArr = ArrayList<Uri>()
    private var isSortedByAlp = true
    companion object {
            const val INTENT_REQ_CODE = 0x01
            fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }
    fun initListeners() {
        fAdapter.onItemLongClickListener = {mCallback.onLongClick()}
        fAdapter.onItemClickListener = {mCallback.onClick()}
    }

    class Builder {
        lateinit var appContext: Context
        lateinit var activity : Activity
        var filesUri = ArrayList<Uri>()
        var selectedUri = ArrayList<Uri>()
        fun build(): SendFragment {
            val fragment = SendFragment()
            fragment.fAdapter = FilesRecyclerAdapter(appContext)
            fragment.initListeners()
            fragment.fileWorker = FileWorker(appContext)
            fragment.fAdapter.filesUri.addAll(filesUri)
            fragment.fAdapter.selectedUris.addAll(selectedUri)
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
        retainInstance = true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        /*savedInstanceState?.let {
            @Suppress("UNCHECKED_CAST")
            fAdapter.filesUri = (it.getSerializable("table_items") as? ArrayList<Uri> ?:  ArrayList())
            @Suppress("UNCHECKED_CAST")
            fAdapter.selectedUris = (it.getSerializable("selected_items") as? ArrayList<Uri> ?:  ArrayList())
            isSortedByAlp = it.getBoolean("sort_type")
        }*/
        initViews()
        checkTableStatus()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_send, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ll_empty_folder.setOnClickListener(this::onClickEmpty)

    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.clear()
        inflater?.inflate(R.menu.menu_options_send, menu)
        optionMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            mCallback = this
        }
        catch (ex: Exception){
            Log.e("err", "impelemntation failed SendFragment")
        }

    }

    override fun onClick(){
        if (fAdapter.selectedUris.isEmpty()){
            optionMenu?.findItem(R.id.item_send_selected)?.isVisible = false
            optionMenu?.findItem(R.id.item_deselect)?.isVisible = false
        }
        else{
            optionMenu?.findItem(R.id.item_send_selected)?.isVisible = true
            optionMenu?.findItem(R.id.item_deselect)?.isVisible = true
        }
    }

    private fun onLongClick(v : View) : Boolean{
        onLongClick()
        return true
    }

    override fun onLongClick(){
        val itemList = ArrayList<CharSequence>()
        val items = ArrayList<MenuItem>()
        optionMenu?.forEach {
            if (it.isVisible && it.icon == null){
                items.add(it)
                itemList.add(it.title)
            }
        }
        val array = arrayOfNulls<CharSequence>(itemList.size)
        val builder = AlertDialog.Builder(this.context)
        itemList.toArray(array)
        itemList.clear()
        with(builder){
            setTitle("Select operation")
            setItems(array){ _, which ->
                onOptionsItemSelected(items[which])
            }
            show()
        }
    }

    fun getAllUri() : ArrayList<Uri>{
        return fAdapter.filesUri
    }

    fun getSelectedUris() : ArrayList<Uri>{
        return fAdapter.selectedUris
    }

    private fun callIntentToPick(){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION //!
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(Intent.createChooser(intent, "Select"), INTENT_REQ_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == INTENT_REQ_CODE) {
            data?.also { dta ->
                val clip = dta.clipData
                if (clip != null) {
                    val arr = Array<Uri>(clip.itemCount) { i -> clip.getItemAt(i).uri}
                    arr.let{a ->
                        if (a.isNotEmpty())
                        {
                            val argArr = ArrayList<Uri>()
                            a.forEach {uri ->
                                if (!fAdapter.isFileContains(uri)) {
                                    argArr.add(uri)
                                }
                            }
                            addTableItems(argArr)
                        }
                    }
                }
                else{
                    val uri = dta.data
                    uri?.let{
                        if (!fAdapter.isFileContains(it)) {
                            addTableItems(arrayListOf(it))
                        }
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun clearSelections(){
        fAdapter.clearSelections()
        optionMenu?.findItem(R.id.item_send_selected)?.isVisible = false
        optionMenu?.findItem(R.id.item_deselect)?.isVisible = false
    }

    private fun clearTable(){
        fAdapter.clearTable()
        checkTableStatus()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.item_pick -> { callIntentToPick()}
            R.id.item_deselect -> {clearSelections()}
            R.id.item_clear_table -> {clearTable()}
            R.id.item_send_selected -> {}
        }
        return super.onOptionsItemSelected(item)
    }

    /*override fun onClick(fileModel: FileModel) {
        if (selArr.contains(fileModel.uri)){
            selArr.remove(fileModel.uri)
            fAdapter.selectedUris.remove(fileModel.uri)
            if (selArr.isEmpty()){
                optionMenu?.findItem(R.id.item_send_selected)?.isVisible = false
            }
        }
        else{
            selArr.add(fileModel.uri)
            fAdapter.selectedUris.add(fileModel.uri)
            optionMenu?.findItem(R.id.item_send_selected)?.isVisible = true
        }
    }*/

    /*override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("sort_type", isSortedByAlp)
        outState.putSerializable("selected_items", fAdapter.selectedUris)
        outState.putSerializable("table_items", fAdapter.filesUri)
    }*/

    private fun onClickEmpty(v : View){
        callIntentToPick()
    }

    fun initViews(){
        val spanCount =
            if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 2
            else 3
        rv_table?.apply{
            val glm = GridLayoutManager(context, spanCount)
            layoutManager = GridLayoutManager(context, spanCount)

            adapter = fAdapter
            //registerForContextMenu(it)
        }
        tl_send.setOnLongClickListener(this::onLongClick)
        updateFilesMeta()
    }

    override fun onOrientationChanged(orientation : Int) {
        val spanCount =
            if(orientation == Configuration.ORIENTATION_PORTRAIT) 2
            else 3
        rv_table?.layoutManager = GridLayoutManager(context, spanCount)
    }

    private fun getSortedFiles(list : ArrayList<FileModel>) : ArrayList<FileModel>{
        return if (isSortedByAlp) ArrayList(list.sortedWith(compareBy {it.name}))
        else ArrayList(list.sortedWith(compareByDescending {it.dateModif}) as ArrayList<FileModel>)
    }

    /*private fun setTableItems(){
        val content = filesFilterExisting(tableUriStrings)
        fAdapter.filesList = sortAlg(content)
        fAdapter.updateTable()
    }*/

    private fun filesFilterExisting(list : ArrayList<Uri>) : ArrayList<FileModel>{
        val newElems = ArrayList<FileModel>()
        for (uri in list){
            val fm = fileWorker.getFileModelByUri(uri)
            if (fm != null){
                newElems.add(fm)
            }
        }
        return newElems
    }

    private fun addTableItems(arr : ArrayList<Uri>) {
        fAdapter.filesList.clear()
        fAdapter.filesUri.addAll(arr)
        fAdapter.selectedUris.addAll(arr)
        arr.clear()
        fAdapter.setTableFiles(getSortedFiles(filesFilterExisting(fAdapter.filesUri)))
        optionMenu?.findItem(R.id.item_send_selected)?.isVisible = true
        optionMenu?.findItem(R.id.item_deselect)?.isVisible = true
        checkTableStatus()
    }

    private fun checkTableStatus(){
        if (fAdapter.filesUri.isEmpty()){
            optionMenu?.findItem(R.id.item_clear_table)?.isVisible = false
            ll_empty_folder.visibility = View.VISIBLE
            rv_table?.apply {
                visibility = View.GONE
            }
        }
        else{
            optionMenu?.findItem(R.id.item_clear_table)?.isVisible = true
            ll_empty_folder.visibility = View.GONE
            rv_table?.apply {
                visibility = View.VISIBLE
            }
        }
    }

    private fun updateFilesMeta(){
        fAdapter.setTableFiles(filesFilterExisting(fAdapter.filesUri))
    }
}

class FilesRecyclerAdapter(context: Context) : RecyclerView.Adapter<FilesRecyclerAdapter.ViewHolder>() {
    private val drItemSelected = context.resources.getDrawable(R.drawable.border_item_selected, null)
    private val drItem = context.resources.getDrawable(R.drawable.border_item, null)
    var onItemClickListener: (() -> Unit)? = null
    var onItemLongClickListener: (() -> Unit)? = null

    var filesList = ArrayList<FileModel>()

    val filesUri = ArrayList<Uri>()

    val selectedUris = ArrayList<Uri>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fileinfo, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = filesList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bindView(position)

    fun isFileContains(uri : Uri) : Boolean{
        filesList.forEach{
            if (it.uri == uri){
                return true
            }
        }
        return false
    }

    fun setTableFiles(fList : ArrayList<FileModel>) {
        filesList = fList
        filesUri.clear()
        val iteratorFL = filesList.iterator()
        while (iteratorFL.hasNext()){
            filesUri.add(iteratorFL.next().uri)
        }
        val iteratorSU = selectedUris.iterator()
        while (iteratorSU.hasNext()){
            if (!filesUri.contains(iteratorSU.next()))
                iteratorSU.remove()
        }
        notifyDataSetChanged()
    }

    fun clearTable(){
        filesList.clear()
        filesUri.clear()
        selectedUris.clear()
        notifyDataSetChanged()
    }

    fun clearSelections(){
        selectedUris.clear()
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener,  View.OnLongClickListener {

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(v: View?) {
                if (v?.tag == false){
                    v.tag = true
                    v.background = drItemSelected
                    selectedUris.add(filesList[adapterPosition].uri)
                }
                else{
                    v?.tag = false
                    v?.background = drItem
                    selectedUris.remove(filesList[adapterPosition].uri)
                }
            onItemClickListener?.invoke()
        }

        override fun onLongClick(v: View?): Boolean {
            onItemLongClickListener?.invoke()
            return true
        }


        fun bindView(position: Int) {
            val fileModel = filesList[position]
            if (selectedUris.contains(fileModel.uri)) {
                itemView.tag = true
                itemView.background = drItemSelected
            }
            else{
                itemView.tag = false
                itemView.background = drItem
            }
            itemView.tv_filename.text = fileModel.name
            if (fileModel.thumbnail != null){
                itemView.iv_file_icon.setImageBitmap(fileModel.thumbnail)
                itemView.iv_file_icon.visibility = View.VISIBLE
                itemView.tv_extension.visibility = View.GONE
            }
            else{
                itemView.tv_extension.text = fileModel.extension
                itemView.iv_file_icon.visibility = View.GONE
                itemView.tv_extension.visibility = View.VISIBLE
            }
            itemView.iv_file_icon.setImageBitmap(fileModel.thumbnail)
            itemView.tv_size.text = fileModel.sizeMB
            itemView.tv_date_modify.text = fileModel.dateModif
        }
    }
}