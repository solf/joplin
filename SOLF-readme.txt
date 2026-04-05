


FOR SANDBOXIE CONFIG MUST USE Open-for-All OPTION WHEN MAPPING FILESYSTEM OR ELSE WRITES WON'T GO TO OPEN FILESYSTEM!!!!!



~~~~~~~~~~


e:\jps>which node
e:\WORK\nodejs\node.EXE

e:\jps>node --version
v24.13.1

e:\jps>


~~~~~~~~~~~


If available, restoring .yarn/cache (and only that, don't touch other stuff!) may speed further steps A LOT



~~~~~~~~~~~


yarn install


~~~~~~~~~~~


To try running Joplin app:

cd packages\app-desktop
yarn start


~~~~~~~~~~~


Building android app (DO NOT USE DEBUG BUILD!)

[needs JDK at least 17]
[DEBUG build is useless, need to build release!]

set JAVA_HOME=c:\LANG\java\JDK\oracle\jdk-22.0.1\

set ANDROID_HOME=e:\Hangry\sdk-android\

cd packages/app-mobile/android
gradlew assembleRelease --no-daemon "-Pandroid.injected.signing.store.file=E:\jps\joplin\packages\app-mobile\android\app\debug.keystore" -Pandroid.injected.signing.store.password=android -Pandroid.injected.signing.key.alias=androiddebugkey -Pandroid.injected.signing.key.password=android


OUTPUT:
packages\app-mobile\android\app\build\outputs\apk\release\app-release.apk

