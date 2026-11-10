# Line Call releases

## Current release version

The app version is read from the Gradle version configuration. The current release target is **1.01** (Android versionCode **10100**).

Use a tag in the form `v1.01` to publish this version. The release workflow reads the tag, builds the signed APK, and attaches it to a GitHub Release. Future tags such as `v1.02` automatically produce versionName `1.02` and a higher Android versionCode.

## One-time signing setup

Android only accepts an update over an installed app when the APK is signed with the same signing certificate. Keep the upload keystore safe and backed up. Never commit it to GitHub or send its passwords in chat.

In the repository, open **Settings → Secrets and variables → Actions → New repository secret** and add:

- `KEYSTORE_BASE64`: base64-encoded contents of your upload keystore file.
- `STORE_PASSWORD`: keystore password.
- `KEY_PASSWORD`: private-key password.

The release workflow expects the key alias to be `upload`. The keystore must be the same one used for every future release.

Example command to encode your keystore locally (Windows PowerShell):
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes(".\my-upload-key.jks"))
```
Copy the output into the `KEYSTORE_BASE64` secret. Do not paste it into a source file.

## Publish a future version

1. Update the version tag you intend to publish. The current prepared tag is `v1.01`.
2. Open the repository on GitHub and select **Releases → Draft a new release** only if you are publishing manually; the automated path below is simpler.
3. Open **Code → Tags** or the repository's **Actions** area and create/push a tag named `v1.01` pointing to the commit you want to release. A pushed `v*` tag automatically starts **Publish Line Call release**.
4. Open **Actions → Publish Line Call release** and wait for all steps to pass.
5. Open **Releases** and download the attached `app-release.apk`.
6. Test the APK on a device before sharing it.

For the next update, use the next tag (for example `v1.02`). Do not reuse a tag, and do not decrease the version.

## Important distribution notes

- This repository is currently private. GitHub Releases and their APK assets are therefore private too. The app's unauthenticated in-app updater cannot fetch a private GitHub release. Before distributing to ordinary users, either make the repository public or move the release manifest and APK to a public download host and point the app updater there.
- The previous CI debug APKs were signed with a CI-generated debug key. A new release keystore will not match those signatures. Users installing this first consistently signed release may need to uninstall the older test APK first; uninstalling can remove local app data. After the first signed release, retain and reuse the same upload keystore for every update.
- Never publish an APK signed with a different key over an existing installation and never discard the signing keystore.
