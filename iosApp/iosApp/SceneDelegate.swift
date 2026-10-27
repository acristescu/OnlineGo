import Shared
import UIKit

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }

        let density = Float(windowScene.traitCollection.displayScale)
        DispatchQueue.global(qos: .userInitiated).async {
            Prewarm_iosKt.prewarmApp(density: density)
        }

        let window = UIWindow(windowScene: windowScene)
        window.rootViewController = MainViewControllerKt.MainViewController(versionCode: versionCode)
        window.makeKeyAndVisible()
        self.window = window
    }

    private var versionCode: Int32 {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String
        return Int32(version ?? "") ?? 0
    }
}
