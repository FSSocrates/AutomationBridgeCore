# Contributing
1. Keep :core free of Android UI / WebView
2. JobManager owns queue; Engine owns active-job FSM; Coordinator owns execution
3. Add unit tests for lifecycle changes
4. ./gradlew :app:testDebugUnitTest :app:assembleDebug
5. PR against main
