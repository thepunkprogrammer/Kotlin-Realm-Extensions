package com.vicpin.kotlinrealmextensions.model

import io.realm.RealmObject

/**
 * Created by magillus on 8/14/2017.
 */
open class User(var name: String? = null, var address: Address = Address()) : RealmObject()

open class Address(var street: String? = null, var city: String? = null, var zip: String? = null) : RealmObject()