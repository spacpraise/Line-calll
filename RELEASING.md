# Line Call release guide

## Current release candidate

- Version name: **1.02**
- Android versionCode: **10200**
- Release tag: **v1.02**
- Application ID: `com.aistudio.linecall.vczr`

The release workflow reads the version name from the pushed tag. A tag such as `v1.02` produces versionName `1.02` and versionCode `10200`. Future versions must use a higher numeric version and a new tag.

## Signing requirements

Android only accepts an update over an existing installation when the APK is signed with the same signing certificate. Keep the upload keystore backed up securely. Never commit it to GitHub or share the key or passwords in chat.

Repository Actions secrets required by the release workflow:

- `KEYSTORE_BASE64` — base64-encoded upload keystore.
- `STORE_PASSWORD` — keystore password.
- `KEY_PASSWORD` — private-key password.
- Optional `KEY_ALIAS` — defaults to `upload`; the release workflow currently validates alias `upload`.

The release workflow validates the keystore, alias, and private-key password before building. If validation fails, it stops before publication.

## Publish v1.02

1. Confirm the `main` branch CI run passes, including JVM tests and emulator UI tests.
2. Confirm the same permanent signing key used by previous signed releases is present in the repository secrets.
3. Create and push the tag `v1.02` at the exact tested `main` commit.
4. The **Publish Line Call release** workflow runs unit tests and builds the signed release APK.
5. The workflow creates the GitHub Release if it does not exist and attaches `app-release.apk`; otherwise it replaces that asset.
6. Download the APK from the release and verify its package name, version name, version code, and signing certificate before distribution.

Do not reuse an existing tag or publish a lower version code. Do not publish a release if the signed release build or tests fail.

## In-app updater

The Android updater checks the latest non-draft, non-prerelease GitHub Release, downloads its APK, verifies the GitHub-provided SHA-256 digest when available, checks the package/version, and retains the downloaded APK until installation is confirmed. Android may require the user to allow Line Call to install unknown apps.

The repository is public, so the latest release and APK asset can be read by the in-app updater.

## First consistently signed release

If any installed preview APK was signed with a different certificate, Android will reject an in-place update. The user may need to uninstall that preview and install the signed release. Uninstalling can erase local app data. After the first release signed with the permanent upload key, keep using the same key for all future updates.