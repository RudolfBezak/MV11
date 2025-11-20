package com.example.mv11

/**
 * Evento - trieda pre jednorazové udalosti.
 * 
 * Problém: Pri rotácii obrazovky sa Fragment znovu vytvorí a LiveData observe
 * sa spustí znova, čo spôsobí duplicitné zobrazenie Toastu/Snackbaru.
 * 
 * Riešenie: Evento sleduje či už bola udalosť spracovaná.
 * 
 * Príklad použitia:
 * ```
 * // ViewModel
 * _message.postValue(Evento("Úspešne uložené!"))
 * 
 * // Fragment
 * viewModel.message.observe(viewLifecycleOwner) { evento ->
 *     evento.getContentIfNotHandled()?.let { message ->
 *         Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
 *         // Pri rotácii obrazovky sa Toast už nezobrazí
 *     }
 * }
 * ```
 * 
 * @param T - typ obsahu (String, Int, User, ...)
 * @param content - obsah udalosti
 */
open class Evento<out T>(private val content: T) {

    /**
     * Príznak či už bola udalosť spracovaná.
     * private set - môže byť zmenený iba v tejto triede
     */
    var hasBeenHandled = false
        private set

    /**
     * Vráti obsah iba ak ešte nebol spracovaný.
     * Po vrátení označí udalosť ako spracovanú.
     * 
     * @return obsah ak ešte nebol spracovaný, inak null
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null  // Už bol spracovaný
        } else {
            hasBeenHandled = true  // Označiť ako spracovaný
            content  // Vrátiť obsah
        }
    }

    /**
     * Vráti obsah bez označenia ako spracovaný.
     * Používa sa na čítanie obsahu bez jeho "spotrebovania".
     * 
     * @return obsah udalosti
     */
    fun peekContent(): T = content
}

