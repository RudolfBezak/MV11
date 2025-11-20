package com.example.mv11

import androidx.lifecycle.LiveData

/**
 * LocalCache - medzivrstva medzi DAO a Repository.
 * 
 * Účel:
 * - Oddelenie logiky databázy od Repository
 * - Ľahšie testovanie (môžeme mock-núť LocalCache)
 * - Možnosť pridať cache logiku (napr. in-memory cache)
 * - Flexibility pri zmene databázovej implementácie
 * 
 * @param dao - Data Access Object pre prístup k databáze
 */
class LocalCache(private val dao: DbDao) {

    /**
     * Odhlási používateľa - vymaže všetky lokálne dáta.
     * suspend - asynchrónna operácia
     */
    suspend fun logoutUser() {
        deleteUserItems()
    }

    /**
     * Uloží zoznam používateľov do databázy.
     * Ak používateľ s daným uid už existuje, nahradí ho (REPLACE stratégia).
     * 
     * @param items - zoznam používateľov na uloženie
     */
    suspend fun insertUserItems(items: List<UserEntity>) {
        dao.insertUserItems(items)
    }

    /**
     * Získa jedného používateľa podľa uid.
     * 
     * @param uid - jedinečný identifikátor používateľa
     * @return LiveData ktorá automaticky aktualizuje UI pri zmene
     */
    fun getUserItem(uid: String): LiveData<UserEntity?> {
        return dao.getUserItem(uid)
    }

    /**
     * Získa všetkých používateľov z databázy.
     * 
     * @return LiveData so zoznamom používateľov, automaticky sa aktualizuje
     * pri každej zmene v databáze (pridanie/odstránenie/úprava)
     */
    fun getUsers(): LiveData<List<UserEntity?>> = dao.getUsers()

    /**
     * Vymaže všetkých používateľov z databázy.
     * Používa sa pri odhlásení alebo reset aplikácie.
     */
    suspend fun deleteUserItems() {
        dao.deleteUserItems()
    }
}

