package com.example.gigwork.data.database

// data/db/entity/UserEntity.kt
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.Index


@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val type: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_profiles",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")] // Add index on userId column
)
data class UserProfileEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val photo: String?,
    val dateOfBirth: String?,
    val gender: String?,
    val currentLocation: String?, // Add this
    val preferredLocation: String?,
    val qualification: String?,
    val computerKnowledge: Boolean?,
    val aadharNumber: String?,
    val companyName: String?,
    val companyFunction: String?,
    val staffCount: Int?,
    val yearlyTurnover: String?,
    val timestamp: Long = System.currentTimeMillis()
)

data class UserWithProfile(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "userId"
    )
    val profile: UserProfileEntity?
)