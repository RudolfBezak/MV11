package com.example.mv11

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO (Data Access Object) - rozhranie pre prístup k databáze.
 * Obsahuje metódy pre všetky databázové operácie s používateľmi.
 */
@Dao
interface DbDao {

    /**
     * Vloží zoznam používateľov do databázy.
     * @Insert anotácia automaticky generuje INSERT SQL príkaz
     * 
     * OnConflictStrategy.REPLACE - ak už používateľ s rovnakým uid existuje,
     * nahradí ho novými dátami (vykoná UPDATE)
     * 
     * suspend - označuje asynchrónnu funkciu (coroutine), ktorá nebude blokovať UI
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserItems(items: List<UserEntity>)

    /**
     * Získa jedného používateľa podľa jeho uid.
     * @Query anotácia umožňuje písať vlastné SQL dotazy
     * 
     * :uid - parameter funkcie, ktorý sa vloží do SQL dotazu
     * 
     * LiveData<UserEntity?> - návratový typ:
     * - LiveData = observable (automaticky aktualizuje UI pri zmene)
     * - UserEntity? = môže vrátiť null, ak používateľ neexistuje
     */
    @Query("select * from users where uid=:uid")
    fun getUserItem(uid: String): LiveData<UserEntity?>

    /**
     * Získa všetkých používateľov z databázy.
     * 
     * LiveData<List<UserEntity?>> - zoznam používateľov, ktorý sa automaticky
     * aktualizuje pri každej zmene v databáze (pridanie/odstránenie/úprava)
     */
    @Query("select * from users")
    fun getUsers(): LiveData<List<UserEntity?>>

    /**
     * Vymaže všetkých používateľov z databázy.
     * suspend - asynchrónna operácia
     */
    @Query("delete from users")
    suspend fun deleteUserItems()
}

