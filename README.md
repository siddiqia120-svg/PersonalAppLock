# Personal AppLock (Android Security App)

Ye ek mukammal Android Security application hai jisme:
1. **App Locker:** Kisi bhi application (WhatsApp, Gallery, etc.) par 4-digit PIN lock lagta hai.
2. **Intruder Silent Video Capture:** Agar koi galat PIN enter karta hai, to front camera se chupke se 5-second ki video record ho jati hai.
3. **Intruder Vault:** Sari pakri gayi videos app ke andar safe folder me date aur time k sath save hoti hain.

---

## APK Hasil Karne Ke 2 Aasan Tareeqay:

### Tareeqa 1: GitHub Actions (Online - Sab se aasan bina kisi computer software k)
1. GitHub.com par apna free account banayein ya login karein.
2. Ek new repository banayein (naam: `PersonalAppLock`).
3. Is folder ki tamam files ko us repository me upload kar dein.
4. Repository me ja kar **"Actions"** tab par click karein.
5. GitHub internet par khud hi 2 minute me `app-debug.apk` build kar dega.
6. Actions me ja kar "PersonalAppLock-APK" par click karein aur APK direct download kar k phone me install kar lein!

### Tareeqa 2: Android Studio (Agar computer par karna chahein)
1. Free me **Android Studio** install karein.
2. "Open Existing Project" par click kar ke ye folder (`PersonalAppLock`) select karein.
3. Android Studio dependencies download karega.
4. Upar menu me **Build > Build Bundle(s) / APK(s) > Build APK(s)** par click karein.
5. 1 minute me notification aayega: `locate APK`. Us par click karein aur `app-debug.apk` apne phone me transfer kar lein!

---

## Phone Me Install Karne Ke Baad Zaroori Settings:
App install karne ke baad jab pehli dafa open karenge:
1. Apna 4-digit Master PIN set karein.
2. "Grant Permissions" button dabayein:
   - **Draw Over Other Apps (Overlay)**: Allow karein.
   - **Camera**: Allow karein (ta k intruder ki video record ho saky).
   - **Accessibility Service**: Phone settings me "Personal AppLock" ko On karein.
3. List me se apni pasandeeda apps (maslan WhatsApp, Gallery) ko toggle On karein.
