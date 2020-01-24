package com.example.filesync.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.filesync.DeviceModel

enum class DeviceStatus(val code : Byte){

    DEVICE_REQUEST_PREPARED(0),
    DEVICE_REQUEST_SENDED(1),
    DEVICE_REQUEST_RECEIVED(2),
    DEVICE_DECLINE_PREPARED(3),
    DEVICE_REQUEST_ACCEPTED(4),
    DEVICE_CONNECTED(5);
    companion object {
        fun getStatus(code: Short): DeviceStatus {
            when (code.toInt()) {
                0 -> return DEVICE_REQUEST_PREPARED
                1 -> return DEVICE_REQUEST_SENDED
                2 -> return DEVICE_REQUEST_RECEIVED
                3 -> return DEVICE_DECLINE_PREPARED
                4 -> return DEVICE_REQUEST_ACCEPTED
                else -> {
                    return DEVICE_CONNECTED
                }
            }
        }
    }
}



class DBHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VER) {

    private companion object{
        const val DATABASE_NAME = "FileSync.db"
        const val DATABASE_VER = 2
        const val TABLE_DEVICE = "connected_device"
            const val DV_COLUMN_ID = "device_id"
            const val DV_COLUMN_NET_NAME = "network_name"
            const val DV_COLUMN_ADJ_NAME = "adjusted_name"
            const val DV_COLUMN_STATUS = "status"
        const val DV_CREATE_SEQUENCE = "CREATE TABLE $TABLE_DEVICE ("+
                "$DV_COLUMN_ID INTEGER PRIMARY KEY, " +
                "$DV_COLUMN_NET_NAME TEXT UNIQUE, " +
                "$DV_COLUMN_ADJ_NAME TEXT UNIQUE, " +
                "$DV_COLUMN_STATUS TINYINT NOT NULL CHECK ($DV_COLUMN_STATUS BETWEEN 0 AND 4))"

        const val TABLE_FILE = "file"
            const val FL_COLUMN_ID  = "file_id"
            const val FL_COLUMN_URI = "uri"
        const val FL_CREATE_SEQUENCE = "CREATE TABLE $TABLE_FILE ("+
                "$FL_COLUMN_ID INTEGER PRIMARY KEY, " +
                "$FL_COLUMN_URI TEXT UNIQUE)"

        const val TABLE_TASK_FILE = "task_file"
            const val TF_COLUMN_DEVICE_ID = "task_device_id"
            const val TF_COLUMN_FILE_ID = "task_file_id"

        const val TF_CREATE_SEQUENCE = "CREATE TABLE $TABLE_TASK_FILE ("+
                "$TF_COLUMN_DEVICE_ID INTEGER, " +
                "$TF_COLUMN_FILE_ID INTEGER, " +
                "FOREIGN KEY($TF_COLUMN_DEVICE_ID) REFERENCES ${TABLE_DEVICE}(${DV_COLUMN_ID}), " +
                "FOREIGN KEY($TF_COLUMN_FILE_ID) REFERENCES ${TABLE_FILE}(${FL_COLUMN_ID}), "+
                "PRIMARY KEY (${TF_COLUMN_DEVICE_ID}, ${TF_COLUMN_FILE_ID}))"

        const val DEV_TASK_QUERY_SEQUENCE = "SELECT tf.$FL_COLUMN_URI FROM $TABLE_DEVICE td" +
                " INNER JOIN $TABLE_TASK_FILE ttf ON td.${DV_COLUMN_ID} = ttf.${TF_COLUMN_DEVICE_ID}" +
                " INNER JOIN $TABLE_FILE tf ON ttf.${TF_COLUMN_FILE_ID} = tf.${FL_COLUMN_ID} " +
                " WHERE $DV_COLUMN_ADJ_NAME = ? "

        const val ALL_TASK_DEV_DELETE_SEQUENCE = "DELETE FROM $TABLE_TASK_FILE " +
                "WHERE $TF_COLUMN_DEVICE_ID IN (SELECT $TF_COLUMN_DEVICE_ID FROM $TABLE_TASK_FILE tf" +
                " INNER JOIN $TABLE_DEVICE td ON tf.${TF_COLUMN_DEVICE_ID} = td.${DV_COLUMN_ID}" +
                " WHERE td.${DV_COLUMN_ADJ_NAME} = ? )"

        const val FILE_TASK_DEV_DELETE_SEQUENCE = "DELETE FROM $TABLE_TASK_FILE " +
                "WHERE $TF_COLUMN_DEVICE_ID IN (SELECT $TF_COLUMN_DEVICE_ID FROM $TABLE_TASK_FILE ttf" +
                " INNER JOIN $TABLE_DEVICE td ON ttf.${TF_COLUMN_DEVICE_ID} = td.${DV_COLUMN_ID}} " +
                " INNER JOIN $TABLE_FILE tf ON ttf.${TF_COLUMN_FILE_ID} = tf.${FL_COLUMN_ID}" +
                " WHERE td.${DV_COLUMN_ADJ_NAME} = ?  AND tf.${FL_COLUMN_URI} = ? )"
        const val FILE_DELETE_SEQUENCE = "DELETE FROM $TABLE_TASK_FILE " +
                "WHERE $TF_COLUMN_FILE_ID IN (SELECT $TF_COLUMN_FILE_ID FROM $TABLE_TASK_FILE ttf " +
                "INNER JOIN $TABLE_FILE tf ON ttf.${TF_COLUMN_FILE_ID} = tf.${FL_COLUMN_ID}" +
                "WHERE tf.${FL_COLUMN_URI} = ? )"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(DV_CREATE_SEQUENCE)
        db.execSQL(FL_CREATE_SEQUENCE)
        db.execSQL(TF_CREATE_SEQUENCE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val seq = "DROP TABLE IF EXISTS "
        db.execSQL(seq + TABLE_TASK_FILE)
        db.execSQL(seq + TABLE_DEVICE)
        db.execSQL(seq + TABLE_FILE)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.let {
            val seq = "DROP TABLE IF EXISTS "
            it.execSQL(seq + TABLE_TASK_FILE)
            it.execSQL(seq + TABLE_DEVICE)
            it.execSQL(seq + TABLE_FILE)
            onCreate(it)
        }
    }

    fun addDeviceToRequest(devNetName : String){
        val vals = ContentValues().apply {
            put(DV_COLUMN_NET_NAME, devNetName)
            put(DV_COLUMN_ADJ_NAME, devNetName)
            put(DV_COLUMN_STATUS, DeviceStatus.DEVICE_REQUEST_PREPARED.code)
        }
        val db = this.writableDatabase
        db.insertWithOnConflict(TABLE_DEVICE, null, vals, SQLiteDatabase.CONFLICT_IGNORE)
        db.close()
    }

    fun addDeviceReceivedRequest(devNetName: String): Boolean{
        val projection = arrayOf(DV_COLUMN_NET_NAME, DV_COLUMN_ADJ_NAME, DV_COLUMN_STATUS)
        val selection = "$DV_COLUMN_NET_NAME = ?"
        val selectionArgs = arrayOf(devNetName)
        val db = this.readableDatabase
        val cursor = db.query(TABLE_DEVICE, projection, selection, selectionArgs,null, null, null)
        if (cursor.moveToNext()){
            val status = cursor.getShort(cursor.getColumnIndexOrThrow(DV_COLUMN_STATUS))
            if (status == DeviceStatus.DEVICE_REQUEST_PREPARED.code.toShort()){
                cursor.close()
                db.close()
                return false
            }
        }
        else {
            cursor.close()
            db.close()
            val dbw = this.writableDatabase
            val vals = ContentValues().apply {
                put(DV_COLUMN_NET_NAME, devNetName)
                put(DV_COLUMN_ADJ_NAME, devNetName)
                put(DV_COLUMN_STATUS, DeviceStatus.DEVICE_REQUEST_RECEIVED.code)
            }

            dbw.insert(TABLE_DEVICE,null, vals)
            dbw.close()
        }
        return true
    }

    fun setDeviceStatus(devNetName: String,status : DeviceStatus) : Boolean{
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(DV_COLUMN_STATUS, status.code)
        }
        val selection = "$DV_COLUMN_NET_NAME LIKE ?"
        val selectionArgs = arrayOf(devNetName)
        val count = db.update(TABLE_DEVICE,values, selection, selectionArgs)
        db.close()
        return count > 0
    }

    fun setNewAdjustedName(oldDevAdjName: String, newDevAdjName : String) : Boolean{
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(DV_COLUMN_ADJ_NAME, newDevAdjName)
        }
        val selection = "$DV_COLUMN_ADJ_NAME LIKE ?"
        val selectionArgs = arrayOf(oldDevAdjName)
        val count = db.update(TABLE_DEVICE, values, selection, selectionArgs)
        db.close()
        return (count > 0)
    }

    fun deleteDevice(devAdjName : String){
        val dbw = this.writableDatabase
        dbw.execSQL(ALL_TASK_DEV_DELETE_SEQUENCE, arrayOf(devAdjName))
        val selection = "$DV_COLUMN_ADJ_NAME LIKE ?"
        val selectionArgs = arrayOf(devAdjName)
        dbw.delete(TABLE_DEVICE, selection,selectionArgs)
        dbw.close()
    }

    fun getAllDevices() : ArrayList<DeviceModel>{
        val projection = arrayOf(DV_COLUMN_ADJ_NAME, DV_COLUMN_STATUS)
        val sortOrder = "$DV_COLUMN_ADJ_NAME ASC"
        val db = this.readableDatabase
        val cursor = db
            .query(TABLE_DEVICE,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
            )
        val devices = ArrayList<DeviceModel>()
        with(cursor){
            var ind = 0
            val indAdj = getColumnIndexOrThrow(DV_COLUMN_ADJ_NAME)
            val indSt = getColumnIndexOrThrow(DV_COLUMN_STATUS)
            while(moveToNext()){
                devices.add(DeviceModel(getString(indAdj), DeviceStatus.getStatus(getShort(indSt))))
                ind++
            }
        }
        cursor.close()
        db.close()
        return devices
    }

    private fun addFiles(uriArr : Array<String>) : Map<String, Int?>{
        val flMap = mutableMapOf<String, Int?>()
        uriArr.forEach {
            flMap[it] = null
        }
        val dwr = this.readableDatabase
        val cursor = dwr.query(
            TABLE_FILE,
            arrayOf(FL_COLUMN_ID, FL_COLUMN_URI),
            null,
            null,
            null,
            null,
            null
        )
        with(cursor){
            val indId = getColumnIndexOrThrow(FL_COLUMN_ID)
            val indUri = getColumnIndexOrThrow(FL_COLUMN_URI)
            var uri : String
            while (moveToNext()){
                uri = getString(indUri).trimEnd()
                if (flMap.containsKey(uri)){
                    flMap[uri] = getInt(indId)
                }
            }
        }
        val dbw = this.writableDatabase
        dbw.beginTransaction()
        val values = ContentValues()
        for (uri in uriArr){
            if (flMap[uri] == null) {
                values.put(FL_COLUMN_URI, uri)
                flMap[uri] = dbw.insertWithOnConflict(TABLE_FILE, null, values, SQLiteDatabase.CONFLICT_IGNORE).toInt()
            }
        }
        dbw.setTransactionSuccessful()
        dbw.endTransaction()
        dbw.close()
        return flMap
    }

    fun addTasks(uriArr : Array<String>, devicesAdjArr : Array<String>){
        val mapFiles = addFiles(uriArr)
        val dbr = this.readableDatabase
        val dbw = this.writableDatabase
        for (adj_name in devicesAdjArr) {
            val cursor_dv = dbr.query(
                TABLE_DEVICE,
                arrayOf(DV_COLUMN_ID),
                "$DV_COLUMN_ADJ_NAME LIKE ?",
                arrayOf(adj_name),
                null,
                null,
                null
            )
            var id: Int? = null
            with(cursor_dv) {

                val index = getColumnIndex(DV_COLUMN_ID)
                if (index != -1 && moveToNext()) {
                    id = getInt(getColumnIndex(DV_COLUMN_ID))
                }
            }
            cursor_dv.close()
            id?.let {
                val tempList = uriArr.toCollection(ArrayList())
                val cursor = dbr.rawQuery(DEV_TASK_QUERY_SEQUENCE, arrayOf(adj_name))
                with(cursor) {
                    val ind = getColumnIndexOrThrow(FL_COLUMN_URI)
                    var uri: String
                    while (moveToNext()) {
                        uri = getString(ind).trimEnd()
                        if (tempList.contains(uri)) {
                            tempList.remove(uri)
                        }
                    }
                }
                cursor.close()

                val values = ContentValues().apply {
                    put(TF_COLUMN_DEVICE_ID, id)
                }
                var idUri : Int?
                dbw.beginTransaction()
                tempList.forEach{
                    idUri = mapFiles[it]
                    idUri?.let {
                        values.put(TF_COLUMN_FILE_ID, idUri)
                    }
                    dbw.insertWithOnConflict(TABLE_TASK_FILE,null, values, SQLiteDatabase.CONFLICT_IGNORE)
                }
                dbw.setTransactionSuccessful()
                dbw.endTransaction()
            }
        }
        dbr.close()
        dbw.close()
    }

    fun removeAllTasksForDevice(devAdjName : String){
        val dbw = this.writableDatabase
        dbw.execSQL(ALL_TASK_DEV_DELETE_SEQUENCE, arrayOf(devAdjName))
        dbw.close()
    }

    fun removeFileFromTaskDevice(devAdjName: String, uri : String){
        val dbw = this.writableDatabase
        dbw.execSQL(FILE_TASK_DEV_DELETE_SEQUENCE, arrayOf(devAdjName, uri))
    }

    fun removeFile(uri : String){
        val dbw = this.writableDatabase
        val arr = arrayOf(uri)
        dbw.rawQuery(FILE_DELETE_SEQUENCE, arr).close()
        dbw.delete(TABLE_FILE,"$FL_COLUMN_URI LIKE ?", arr)
        dbw.close()
    }

    fun getAllFiles() : ArrayList<String>{
        val dbr = this.readableDatabase
        val arrayList = ArrayList<String>()
        val cursor = dbr.query(TABLE_FILE, arrayOf(FL_COLUMN_ID, FL_COLUMN_URI),
            null,
            null,
            null,
            null,
            null)
        with(cursor){
            val indId = getColumnIndexOrThrow(FL_COLUMN_ID)
            val indUri = getColumnIndexOrThrow(FL_COLUMN_URI)
            while (moveToNext()){
                arrayList.add(String.format("id = %d : uri = %s", getInt(indId), getString(indUri)))
            }
        }
        cursor.close()
        return arrayList
    }

    fun getAllTaskFiles() : ArrayList<String>{
        val dbr = this.readableDatabase
        val arrayList = ArrayList<String>()
        val cursor = dbr.query(
            TABLE_TASK_FILE, arrayOf(TF_COLUMN_FILE_ID, TF_COLUMN_DEVICE_ID),
            null,
            null,
            null,
            null,
            null)
        with(cursor){
            val indIdFl = getColumnIndexOrThrow(TF_COLUMN_FILE_ID)
            val indIdDev = getColumnIndexOrThrow(TF_COLUMN_DEVICE_ID)
            while (moveToNext()){
                arrayList.add(String.format("dev_id = %d : file_id = %d", getInt(indIdDev), getInt(indIdFl)))
            }
        }
        cursor.close()
        return arrayList
    }

    fun getDevices() : ArrayList<String>{
        val dbr = this.readableDatabase
        val arrayList = ArrayList<String>()
        val cursor = dbr.query(
            TABLE_DEVICE, arrayOf(DV_COLUMN_ID,
                DV_COLUMN_NET_NAME,
                DV_COLUMN_ADJ_NAME,
                DV_COLUMN_STATUS
            ),
            null,
            null,
            null,
            null,
            null)
        with(cursor){
            val indIdDev = getColumnIndexOrThrow(DV_COLUMN_ID)
            val indNetName = getColumnIndexOrThrow(DV_COLUMN_NET_NAME)
            val indAdjName = getColumnIndexOrThrow(DV_COLUMN_ADJ_NAME)
            val indStat = getColumnIndexOrThrow(DV_COLUMN_STATUS)
            while (moveToNext()){
                arrayList.add(String.format(
                    "dev_id = %d, net_name = %s, adj_name = %s, status = %s",
                    getInt(indIdDev),
                    getString(indNetName),
                    getString(indAdjName),
                    DeviceStatus.getStatus(getInt(indStat).toShort())
                ))
            }
        }
        cursor.close()
        return arrayList
    }

}