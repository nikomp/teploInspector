package ru.bingosoft.teploInspector.db.Orders

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import ru.bingosoft.teploInspector.util.DateConverter
import java.util.*

@Entity(tableName = "Orders")
@TypeConverters(DateConverter::class)
data class Orders (
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    var id: Long = 0,
    @SerializedName("guid")
    var guid: String,
    @SerializedName("number_order")
    var number: String? = null,
    @SerializedName("status")
    var status: String? = "В работе",
    @SerializedName("address")
    var address: String? = null,
    @SerializedName("fio")
    var contactFio: String? = null,
    @SerializedName("phone")
    var phone: String? = null,
    @SerializedName("date_create_order")
    var dateCreate: Date? = null,
    @SerializedName("date_visit")
    var dateVisit: String? = null,
    @SerializedName("time_visit")
    var timeVisit: String? = null,
    @SerializedName("purpose_object")
    var purposeObject: String? = null,
    @SerializedName("group_order")
    var groupOrder: String? = null,
    @SerializedName("count_node")
    var countNode: Int? = null,
    @SerializedName("type_order")
    var typeOrder: String? = "Тип заявки",

    @SerializedName("typeTransportation")
    var typeTransportation: String? = "Самостоятельно на общественном транспорте",

    @SerializedName("lat")
    var lat: Double = 0.0,
    @SerializedName("lon")
    var lon: Double = 0.0,

    @SerializedName("gi_contract_number")
    var giContractNumber: String? = null,
    @SerializedName("gi_contract_date")
    var giContractDate: Date? = null,
    @SerializedName("gi_legal_address")
    var giLegalAddress: String? = null,
    @SerializedName("gi_phone")
    var giPhone: String? = null,
    @SerializedName("gi_email")
    var giEmail: String? = null,
    @SerializedName("gi_post_address")
    var giPostAddress: String? = null,
    @SerializedName("gi_contractor")
    var giContractor: String? = null,
    @SerializedName("gi_responsible_tx")
    var gi_responsible_tx: String? = null,
    @SerializedName("gi_director")
    var giDirector: String? = null,
    @SerializedName("gi_responsible_phone_city")
    var giResponsiblePhoneCity: String? = null,
    @SerializedName("gi_responsible_phone_mob")
    var giResponsiblePhoneMob: String? = null,
    @SerializedName("gi_director_phone_city")
    var giDirectorPhoneCity: String? = null,
    @SerializedName("gi_director_phone_mob")
    var giDirectorPhoneMob: String? = null,
    @SerializedName("gi_belong_or")
    var gi_belong_or: String? = null,
    @SerializedName("gi_belong_uen")
    var gi_belong_uen: String? = null,
    @SerializedName("gi_managing_organization_uen")
    var giManagingOrganizationUen: String? = null,
    @SerializedName("gi_contracting_organization")
    var giContractingOrganization: String? = null,
    @SerializedName("gi_track_ownership")
    var giTrackOwnership: String? = null,
    @SerializedName("gi_node_ownership")
    var giNodeOwnership: String? = null,
    @SerializedName("gi_rtc")
    var giRtc: String? = null,
    @SerializedName("gi_master_rtc")
    var giMasterRtc: String? = null,


    var checked: Boolean=false,
    var questionCount: Int=0,
    var techParamsCount: Int=0,
    var answeredCount: Int=0
)

