# Release

## 1.x stable surface
- AutomationBridge / IpcProtocol v1
- JS API v1 (result != complete)
- Signature permission USE_ENGINE

## Build
./gradlew :app:assembleDebug :app:testDebugUnitTest
./gradlew :core:publishReleasePublicationToLocalBuildRepository

## Emulator
Actions → Android CI → Run workflow → run_emulator=true
