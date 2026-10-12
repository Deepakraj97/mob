# SMS Expense Tracker (Android, Kotlin)

Reads bank/UPI/card SMS, extracts DEBIT / CREDIT, amount, merchant, account last digits
and balance, and logs each to `transactions.csv` (opens in Excel / Google Sheets).

## Build
1. Open this folder in Android Studio (Koala or newer) and let Gradle sync.
2. Plug in your phone (USB debugging on) and press Run, or Build > Build APK(s).
3. On first launch grant the SMS permission, then tap "Scan past SMS".
New SMS are captured automatically afterwards.

Note: Android may flag "restricted setting" for SMS on sideloaded apps:
Settings > Apps > SMS Expense Tracker > ⋮ > Allow restricted settings.

## No Android Studio? Build the APK in the cloud
1. Create a free GitHub account and a new (private) repository.
2. Upload the contents of this folder (including the hidden .github folder).
3. Open the repo's Actions tab, run "Build APK" (it also runs on every push).
4. When it finishes (~3-5 min), download the "SmsExpenseTracker-apk" artifact, unzip it,
   and install app-debug.apk on your phone (allow "install unknown apps").
