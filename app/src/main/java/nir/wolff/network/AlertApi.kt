package nir.wolff.network

import nir.wolff.model.AlertEvent
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AlertApi {
    @GET("alerts/history")
    suspend fun getAlertHistory(@Query("userEmail") userEmail: String): Response<List<AlertEvent>>

    @GET("alerts/test")
    suspend fun testAlert(
        @Query("userEmail") userEmail: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): Response<AlertEvent>
}
