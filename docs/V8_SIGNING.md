# V8 update signing

Android only installs an APK over an existing installation when the package name and signing certificate match. V8 keeps the package name `com.maldawr.chatsimulator` and gives every CI build an increasing versionCode.

For durable release updates, add these GitHub Actions repository secrets:

- `CHAT_SIM_KEYSTORE_B64` — base64 of the release JKS/keystore file.
- `CHAT_SIM_KEY_ALIAS` — alias inside the keystore.
- `CHAT_SIM_STORE_PASSWORD` — keystore password.
- `CHAT_SIM_KEY_PASSWORD` — key password.

The workflow never commits the keystore. If the release secrets are absent, CI builds a debug APK and caches `~/.android/debug.keystore` under a stable cache key so later debug builds can update each other while that cache remains available.

Because older APKs were produced by ephemeral GitHub runners without a retained signing key, the first move to the new V8 signing identity can require one final uninstall/reinstall. After that, keep the same release keystore for all future versions.
