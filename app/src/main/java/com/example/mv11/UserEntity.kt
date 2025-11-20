package com.example.mv11

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entita reprezentujúca používateľa v databáze.
 * @Entity anotácia označuje túto triedu ako tabuľku v Room databáze.
 * 
 * tableName = "users" - názov tabuľky v SQLite databáze
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,  // Primárny kľúč - jedinečný identifikátor používateľa
    val name: String,              // Meno používateľa
    val updated: String,           // Dátum a čas poslednej aktualizácie
    val lat: Double,               // Zemepisná šírka (latitude) - GPS súradnica
    val lon: Double,               // Zemepisná dĺžka (longitude) - GPS súradnica
    val radius: Double,            // Polomer geofence oblasti v metroch
    val photo: String = ""         // URL adresa fotky používateľa (voliteľné)
)

