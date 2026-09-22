import SwiftUI
import Shared

struct ContentView: View {
    var body: some View {
        Text("Hello from \(Platform_iosKt.getPlatform().name)")
            .padding()
    }
}

#Preview {
    ContentView()
}
