package com.vicpin.krealmextensions

import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmQuery
import io.realm.Sort

typealias Query<T> = (RealmQuery<T>) -> Unit

/**
 * Created by victor on 2/1/17.
 * Extensions for Realm. All methods here are synchronous.
 */

/**
 * Query to the database with RealmQuery instance as argument
 */
fun <T : RealmObject> T.query(query: Query<T>): List<T> {
    getRealm().use { realm ->
        val result = realm.forEntity(this).withQuery(query).findAll()
        return realm.copyFromRealm(result)
    }
}

/**
 * Query to the database with RealmQuery instance as argument and returns all items founded
 */
fun <T : RealmObject> T.queryAll(): List<T> {
    getRealm().use { realm ->
        val result = realm.forEntity(this).findAll()
        return realm.copyFromRealm(result)
    }
}

/**
 * Query to the database with RealmQuery instance as argument. Return first result, or null.
 */
fun <T : RealmObject> T.queryFirst(): T? {
    getRealm().use { realm ->
        val item: T? = realm.forEntity(this).findFirst()
        return if (item != null && item.isValid) realm.copyFromRealm(item) else null
    }
}

/**
 * Query to the database with RealmQuery instance as argument. Return first result, or null.
 */
fun <T : RealmObject> T.queryFirst(query: Query<T>): T? {
    getRealm().use { realm ->
        val item: T? = realm.forEntity(this).withQuery(query).findFirst()
        return if (item != null && item.isValid) realm.copyFromRealm(item) else null
    }
}

/**
 * Query to the database with RealmQuery instance as argument. Return last result, or null.
 */
fun <T : RealmObject> T.queryLast(): T? {
    getRealm().use { realm ->
        val result = realm.forEntity(this).findAll()
        return if (result != null && result.isNotEmpty()) realm.copyFromRealm(result.last()) else null
    }
}

/**
 * Query to the database with RealmQuery instance as argument. Return last result, or null.
 */
fun <T : RealmObject> T.queryLast(query: Query<T>): T? {
    getRealm().use { realm ->
        val result = realm.forEntity(this).withQuery(query).findAll()
        return if (result != null && result.isNotEmpty()) realm.copyFromRealm(result.last()) else null
    }
}

/**
 * Query to the database with RealmQuery instance as argument
 */
fun <T : RealmObject> T.querySorted(fieldName: String, order: Sort, query: Query<T>): List<T> {
    getRealm().use { realm ->
        val result = realm.forEntity(this).withQuery(query).findAll().sort(fieldName, order)
        return realm.copyFromRealm(result)
    }
}

/**
 * Query to the database with a specific order and a RealmQuery instance as argument
 */
fun <T : RealmObject> T.querySorted(fieldName: List<String>, order: List<Sort>, query: Query<T>): List<T> {
    getRealm().use { realm ->
        val result = realm.forEntity(this).withQuery(query).findAll().sort(fieldName.toTypedArray(), order.toTypedArray())
        return realm.copyFromRealm(result)
    }
}

/**
 * Query to the database with a specific order
 */
fun <T : RealmObject> T.querySorted(fieldName: String, order: Sort): List<T> {
    getRealm().use { realm ->
        val result = realm.forEntity(this).findAll().sort(fieldName, order)
        return realm.copyFromRealm(result)
    }
}

/**
 * Query to the database with a specific order
 */
fun <T : RealmObject> T.querySorted(fieldName: List<String>, order: List<Sort>): List<T> {
    getRealm().use { realm ->
        val result = realm.forEntity(this).findAll().sort(fieldName.toTypedArray(), order.toTypedArray())
        return realm.copyFromRealm(result)
    }
}

/**
 * Utility extension for modifying database. Create a transaction, run the function passed as argument,
 * commit transaction and close realm instance.
 */
fun Realm.transaction(action: (Realm) -> Unit) {
    use { executeTransaction { action(this) } }
}

/**
 * Creates a new entry in database. Usefull for RealmObject with no primary key.
 */
fun <T : RealmObject> T.create() {
    getRealm().transaction { it.copyToRealm(this) }
}

/**
 * Creates a new entry in database. Useful for RealmObject with no primary key.
 * @return a managed version of a saved object
 */
fun <T : RealmObject> T.createManaged(realm: Realm): T {
    var result: T? = null
    realm.executeTransaction { result = it.copyToRealm(this) }
    return result!!
}

/**
 * Creates or updates a entry in database. Requires a RealmObject with primary key, or IllegalArgumentException will be thrown
 */
fun <T : RealmObject> T.createOrUpdate() {
    getRealm().transaction { it.copyToRealmOrUpdate(this) }
}

/**
 * Creates or updates a entry in database. Requires a RealmObject with primary key, or IllegalArgumentException will be thrown
 * @return a managed version of a saved object
 */
fun <T : RealmObject> T.createOrUpdateManaged(realm: Realm): T {
    var result: T? = null
    realm.executeTransaction { result = it.copyToRealmOrUpdate(this) }
    return result!!
}

/**
 * Creates a new entry in database or updates an existing one. If entity has no primary key, always create a new one.
 * If has primary key, it tries to updates an existing one.
 */
inline fun <reified T : RealmObject> T.save() {
        getRealm().transaction { realm ->
            if (isAutoIncrementPK()) {
                initPk(realm)
            }
            if (this.hasPrimaryKey(realm)) realm.copyToRealmOrUpdate(this) else realm.copyToRealm(this)
        }
}

/**
 * Creates a new entry in database or updates an existing one. If entity has no primary key, always create a new one.
 * If has primary key, it tries to update an existing one.
 * @return a managed version of a saved object
 */
inline fun <reified T : RealmObject> T.saveManaged(realm: Realm): T {
    var result: T? = null
    realm.executeTransaction {
        if (isAutoIncrementPK()) {
            initPk(realm)
        }

        result = if (this.hasPrimaryKey(it)) it.copyToRealmOrUpdate(this) else it.copyToRealm(this)
    }
    return result!!
}

inline fun <reified D : RealmObject, T : Collection<D>> T.saveAll() {
    if (size > 0) {
        getRealm().transaction { realm ->
            if (first().isAutoIncrementPK()) {
                initPk(realm)
            }
            forEach { if (it.hasPrimaryKey(realm)) realm.copyToRealmOrUpdate(it) else realm.copyToRealm(it) }
        }

    }
}

inline fun <reified T : RealmObject> Collection<T>.saveAllManaged(realm: Realm): List<T> {
    val results = mutableListOf<T>()
    realm.executeTransaction {
        if (first().isAutoIncrementPK()) {
            initPk(realm)
        }
        forEach { results += if (it.hasPrimaryKey(realm)) realm.copyToRealmOrUpdate(it) else realm.copyToRealm(it) }
    }
    return results
}

inline fun <reified D : RealmObject> Array<D>.saveAll() {
    getRealm().transaction { realm ->
        if (first().isAutoIncrementPK()) {
            initPk(realm)
        }
        forEach { if (it.hasPrimaryKey(realm)) realm.copyToRealmOrUpdate(it) else realm.copyToRealm(it) }
    }
}

inline fun <reified T : RealmObject> Array<T>.saveAllManaged(realm: Realm): List<T> {
    val results = mutableListOf<T>()
    realm.executeTransaction {
        if (first().isAutoIncrementPK()) {
            initPk(realm)
        }
        forEach { results += if (it.hasPrimaryKey(realm)) realm.copyToRealmOrUpdate(it) else realm.copyToRealm(it) }
    }
    return results
}

/**
 * Delete all entries of this type in database
 */
fun <T : RealmObject> T.deleteAll() {
    getRealm().transaction { it.forEntity(this).findAll().deleteAllFromRealm() }
}

/**
 * Delete all entries returned by the specified query
 */
fun <T : RealmObject> T.delete(myQuery: Query<T>) {
    getRealm().transaction {
        it.forEntity(this).withQuery(myQuery).findAll().deleteAllFromRealm()
    }
}

/**
 * Update first entry returned by the specified query
 */
inline fun <reified T : RealmObject> T.queryAndUpdate(noinline query: Query<T>, noinline modify: (T) -> Unit) {
    queryFirst(query).let {
        modify(this)
        save()
    }
}

/**
 * Get count of entries
 */
fun <T : RealmObject> T.count(): Long {
    getRealm().use { realm ->
        return realm.where(this::class.java).count()
    }
}

inline fun <reified T : RealmObject> T.count(realm: Realm): Long {
    return realm.where(T::class.java).count()
}

/**
 * UTILITY METHODS
 */
private fun <T : RealmObject> Realm.forEntity(instance: T): RealmQuery<T> {
    return RealmQuery.createQuery(this, instance.javaClass)
}

private fun <T> T.withQuery(block: (T) -> Unit): T {
    block(this); return this
}

inline fun <reified T : RealmObject> T.hasPrimaryKey(realm: Realm): Boolean {
    if (realm.schema.get(this.javaClass.simpleName) == null) {
        throw IllegalArgumentException(this.javaClass.simpleName + " is not part of the schema for this Realm. Did you added realm-android plugin in your build.gradle file?")
    }
    return realm.schema.get(this.javaClass.simpleName).hasPrimaryKey()
}

inline fun <reified T : RealmObject> T.getLastPk(realm: Realm): Long {
    val result = realm.where(this.javaClass).max(getPrimaryKeyFieldName(realm))
    return result?.toLong() ?: 0
}

inline fun <reified T : RealmObject> T.getPrimaryKeyFieldName(realm: Realm): String {
    return realm.schema.get(this.javaClass.simpleName).primaryKey
}

inline fun <reified T : RealmObject> T.setPk(realm: Realm, value: Long) {
    val fieldName = realm.schema.get(this.javaClass.simpleName).primaryKey
    val f1 = javaClass.getDeclaredField(fieldName)
    try {
        val accesible = f1.isAccessible
        f1.isAccessible = true
        f1.set(this, value)
        f1.isAccessible = accesible
    } catch (ex: IllegalArgumentException) {
        throw IllegalArgumentException("Primary key field $fieldName must be of type Long to set a primary key automatically")
    }
}

fun Collection<RealmObject>.initPk(realm: Realm) {
    val nextPk = first().getLastPk(realm) + 1
    for ((index, value) in withIndex()) {
        value.setPk(realm, nextPk + index)
    }
}

fun Array<out RealmObject>.initPk(realm: Realm) {
    val nextPk = first().getLastPk(realm) + 1
    for ((index, value) in withIndex()) {
        value.setPk(realm, nextPk + index)
    }
}

fun RealmObject.initPk(realm: Realm) {
    setPk(realm, getLastPk(realm) + 1)
}

inline fun <reified T : RealmObject> T.isAutoIncrementPK(): Boolean {
    return this.javaClass.declaredAnnotations.filter { it.annotationClass == AutoIncrementPK::class }.isNotEmpty()

}






