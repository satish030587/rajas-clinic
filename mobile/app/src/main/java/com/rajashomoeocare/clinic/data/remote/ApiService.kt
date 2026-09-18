package com.rajashomoeocare.clinic.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // --- auth ---

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
    ): LoginResponse

    @GET("auth/me")
    suspend fun me(): UserDto

    @PATCH("auth/me")
    suspend fun updateMe(@Body body: UserPatch): UserDto

    // --- patients ---

    @GET("patients")
    suspend fun patients(@Query("q") query: String? = null): List<PatientRowDto>

    @GET("patients/{id}")
    suspend fun patient(@Path("id") id: String): PatientDto

    /** Duplicate guard — returns an empty body when the number is free. */
    @GET("patients/by-phone/{phone}")
    suspend fun patientByPhone(@Path("phone") phone: String): Response<PatientRowDto>

    @POST("patients")
    suspend fun createPatient(@Body body: PatientCreate): PatientDto

    @POST("patients/quick-add")
    suspend fun quickAddPatient(@Body body: QuickAddRequest): PatientDto

    @PATCH("patients/{id}")
    suspend fun updatePatient(
        @Path("id") id: String,
        @Body body: PatientCreate,
    ): PatientDto

    // --- visits ---

    @GET("visits/queue")
    suspend fun queue(): List<VisitDto>

    @GET("visits/patient/{id}")
    suspend fun visitsForPatient(@Path("id") patientId: String): List<VisitDto>

    @GET("visits/{id}")
    suspend fun visit(@Path("id") id: String): VisitDto

    @POST("visits/start")
    suspend fun startVisit(@Body body: StartVisitRequest): VisitDto

    @PUT("visits/{id}/vitals")
    suspend fun setVitals(@Path("id") id: String, @Body body: VitalsDto): VisitDto

    @POST("visits/{id}/open")
    suspend fun openVisit(@Path("id") id: String): VisitDto

    @PATCH("visits/{id}/clinical")
    suspend fun updateClinical(
        @Path("id") id: String,
        @Body body: ClinicalUpdate,
    ): VisitDto

    // --- recall ---

    @GET("recall/today")
    suspend fun today(): TodaySummaryDto

    @GET("recall/overdue")
    suspend fun overdue(): List<RecallItemDto>

    @GET("recall/reminders-due")
    suspend fun remindersDue(): List<AppointmentDto>

    // --- appointments ---

    @POST("appointments")
    suspend fun bookAppointment(@Body body: AppointmentCreate): AppointmentDto

    // --- investigations ---

    @GET("investigations/patient/{id}")
    suspend fun investigations(@Path("id") patientId: String): List<InvestigationDto>

    @POST("investigations")
    suspend fun createInvestigation(@Body body: InvestigationCreate): InvestigationDto

    @Multipart
    @POST("investigations/{id}/files")
    suspend fun uploadInvestigationFile(
        @Path("id") investigationId: String,
        @Part file: MultipartBody.Part,
    ): InvestigationDto

    @DELETE("investigations/{id}")
    suspend fun deleteInvestigation(@Path("id") id: String): Response<Unit>

    // --- catalog ---

    @GET("cards")
    suspend fun cards(): List<CardDto>

    @GET("templates")
    suspend fun templates(): List<TemplateDto>

    @PUT("templates/{key}/{language}")
    suspend fun updateTemplate(
        @Path("key") key: String,
        @Path("language") language: String,
        @Body body: TemplateUpdate,
    ): TemplateDto

    @POST("messages/log")
    suspend fun logMessage(@Body body: MessageLogCreate): Response<Unit>

    @GET("clinic")
    suspend fun clinic(): ClinicProfileDto

    @PATCH("clinic")
    suspend fun updateClinic(@Body body: ClinicProfileDto): ClinicProfileDto

    @GET("appointments")
    suspend fun appointments(
        @Query("from_date") from: String? = null,
        @Query("to_date") to: String? = null,
        @Query("patient_id") patientId: String? = null,
    ): List<AppointmentDto>
}
