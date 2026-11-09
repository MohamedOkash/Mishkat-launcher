package com.mishkat.launcher.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Single app-wide preferences DataStore. */
val Context.mishkatPrefs: DataStore<Preferences> by preferencesDataStore(name = "mishkat_prefs")
