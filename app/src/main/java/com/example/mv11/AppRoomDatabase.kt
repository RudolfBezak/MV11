package com.example.mv11

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Hlavná trieda Room databázy.
 * @Database anotácia označuje túto triedu ako Room databázu.
 * 
 * entities - zoznam všetkých tabuliek (entít) v databáze
 * version - verzia databázy (pri zmene schémy je potrebné zvýšiť)
 * exportSchema = false - nevytvára JSON schému databázy (pre produkciu by malo byť true)
 */
@Database(
    entities = [
        UserEntity::class  // Zoznam všetkých tabuliek v databáze
    ],
    version = 1,           // Verzia databázy - pri zmene štruktúry zvýšiť!
    exportSchema = false
)
abstract class AppRoomDatabase : RoomDatabase() {

    /**
     * Poskytuje prístup k DAO (Data Access Object).
     * Room automaticky implementuje túto metódu.
     */
    abstract fun appDao(): DbDao

    companion object {
        /**
         * Singleton pattern - zabezpečuje že existuje iba jedna inštancia databázy.
         * 
         * @Volatile - zaručuje viditeľnosť zmien premennej naprieč vláknami.
         * Keď jedno vlákno zmení INSTANCE, ostatné vlákna uvidia túto zmenu okamžite.
         */
        @Volatile
        private var INSTANCE: AppRoomDatabase? = null

        /**
         * Vracia jedinú inštanciu databázy.
         * 
         * synchronized(this) - zabezpečuje že iba jedno vlákno môže vytvárať
         * inštanciu naraz (thread-safe).
         * 
         * Elvis operator (?:) - ak INSTANCE je null, vykoná pravú stranu.
         * also { } - po vytvorení novej inštancie ju uloží do INSTANCE.
         */
        fun getInstance(context: Context): AppRoomDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        /**
         * Vytvorí novú inštanciu databázy.
         * 
         * Room.databaseBuilder - builder na vytvorenie databázy
         * context.applicationContext - používa aplikačný kontext (nie activity)
         * "mv11DB" - názov databázového súboru
         * 
         * fallbackToDestructiveMigration() - pri zmene verzie databázy vymaže
         * starú databázu a vytvorí novú (pre produkciu použiť migrácie!)
         */
        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AppRoomDatabase::class.java,
                "mv11DB"
            ).fallbackToDestructiveMigration()
                .build()
    }
}

