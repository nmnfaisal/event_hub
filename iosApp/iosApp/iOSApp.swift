import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        KoinInitKt.doInitKoin(platformModules: [])
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
