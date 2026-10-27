import Shared

final class NoOpAnalytics: Analytics {
    func logEvent(name: String, params: [String: Any]) {}
    func logScreenView(screenName: String) {}
}

final class NoOpGameNotificationCanceller: GameNotificationCanceller {
    func cancelGameNotification(gameId: Int64) {}
}

final class BundleTutorialsSource: TutorialsSource {
    func readJson() -> String {
        let url = Bundle.main.url(forResource: "tutorials", withExtension: "json")!
        return try! String(contentsOf: url, encoding: .utf8)
    }
}
