package com.example.mv11

/**
 * MockDataHelper - poskytuje testovacie dáta.
 * 
 * object - singleton objekt v Kotline (iba jedna inštancia)
 * Používa sa keď API endpoint neexistuje alebo na testovanie offline režimu.
 */
object MockDataHelper {
    
    /**
     * Vracia zoznam testovacích používateľov s GPS súradnicami v Bratislave.
     * Týchto používateľov používame ako fallback keď API vráti 404 chybu.
     * 
     * @return zoznam 5 testovacích používateľov
     */
    fun getMockUsers(): List<UserEntity> {
        return listOf(
            UserEntity(
                uid = "user1",
                name = "Peter Novák",
                updated = "2025-11-05 18:00:00",
                lat = 48.1486,
                lon = 17.1077,
                radius = 100.0,
                photo = ""
            ),
            UserEntity(
                uid = "user2",
                name = "Jana Kováčová",
                updated = "2025-11-05 18:30:00",
                lat = 48.1516,
                lon = 17.1127,
                radius = 150.0,
                photo = ""
            ),
            UserEntity(
                uid = "user3",
                name = "Martin Horák",
                updated = "2025-11-05 17:45:00",
                lat = 48.1456,
                lon = 17.1047,
                radius = 120.0,
                photo = ""
            ),
            UserEntity(
                uid = "user4",
                name = "Eva Štefanová",
                updated = "2025-11-05 19:00:00",
                lat = 48.1500,
                lon = 17.1100,
                radius = 80.0,
                photo = ""
            ),
            UserEntity(
                uid = "user5",
                name = "Tomáš Varga",
                updated = "2025-11-05 18:15:00",
                lat = 48.1470,
                lon = 17.1090,
                radius = 200.0,
                photo = ""
            )
        )
    }
}


