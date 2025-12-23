package com.vencehoje.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String = "#1976D2", // Azul padrão
    val iconName: String = "person",
    val isMain: Boolean = false // Para saber qual é o seu perfil principal
)