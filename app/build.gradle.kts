plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace="de.kulanoglu.pokerass"; compileSdk=35
 defaultConfig { applicationId="de.kulanoglu.pokerass"; minSdk=26; targetSdk=35; versionCode=1; versionName="0.1.0" }
 buildFeatures { compose=true }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.05.01"))
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.compose.material3:material3")
}