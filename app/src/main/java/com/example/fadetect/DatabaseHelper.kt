import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import com.example.fadetect.DataModel
import java.io.*
import java.io.File




class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "rsrpLoc.db"
        private const val TBL_DATA = "tbl_data"
        private const val RSRP = "rsrp"
        private const val LATITUDE = "latitude"
        private const val LONGITUDE = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTblData = ("CREATE TABLE " + TBL_DATA + "("
                + RSRP + " INTEGER,"
                + LATITUDE + " REAL,"
                + LONGITUDE + " REAL,"
                + "PRIMARY KEY (" + RSRP + ", " + LATITUDE + ", " + LONGITUDE + ")"
                + ")")

        db?.execSQL(createTblData)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades if needed
        db!!.execSQL("DROP TABLE IF EXISTS $TBL_DATA")
        onCreate(db)
    }

    fun insertData(std: DataModel): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(RSRP, std.rsrp)
        contentValues.put(LATITUDE, std.latitude)
        contentValues.put(LONGITUDE, std.longitude)

        val success = db.insert(TBL_DATA, null, contentValues)
        db.close()
        return success
    }

    fun exportDatabase(context: Context): Boolean {
        val inputPath = context.getDatabasePath(DATABASE_NAME).absolutePath
        val outputPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val outputFileName = "exported_database.sql"

        try {
            val outputFile = File(outputPath, outputFileName)

            if (outputFile.exists()) {
                outputFile.delete()
            }
            outputFile.createNewFile()

            val inputFile = File(inputPath)
            inputFile.copyTo(outputFile, overwrite = true)

            return true
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return false
    }
}
