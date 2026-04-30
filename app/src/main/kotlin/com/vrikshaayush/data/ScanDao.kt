package com.vrikshaayush.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ScanDao {

    @Insert
    suspend fun insert(scan: ScanRecord): Long

    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC")
    fun getAllScans(): LiveData<List<ScanRecord>>

    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC")
    suspend fun getAllScansOnce(): List<ScanRecord>

    @Query("SELECT COUNT(*) FROM scan_records")
    suspend fun getTotalScans(): Int

    @Query("SELECT COUNT(DISTINCT diseaseName) FROM scan_records WHERE diseaseName != 'healthy'")
    suspend fun getTotalDiseases(): Int

    @Query("SELECT COUNT(DISTINCT cropType) FROM scan_records")
    suspend fun getTotalCrops(): Int

    @Query("SELECT * FROM scan_records WHERE cropType LIKE '%' || :query || '%' OR diseaseName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchScans(query: String): LiveData<List<ScanRecord>>

    @Delete
    suspend fun delete(scan: ScanRecord)

    @Query("DELETE FROM scan_records")
    suspend fun deleteAll()

    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastScan(): ScanRecord?
}
