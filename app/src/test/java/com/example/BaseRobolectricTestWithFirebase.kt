package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Before

open class BaseRobolectricTestWithFirebase {

    @Before
    fun initFirebase() {
        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:1234567890:android:testapp123")
                    .setApiKey("AIzaSyFakeKeyForRobolectricTesting123")
                    .setProjectId("omnicomm-test-project")
                    .setDatabaseUrl("https://omnicomm-test-project-default-rtdb.firebaseio.com")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
        } catch (ignored: Throwable) {}
    }
}
