# Face-to-Face LAN Smoke Checklist

Use two Android devices on the same Wi-Fi network.

## Host and Join

1. On device A, open Face to face, choose `Wi-Fi host`, pick a board size and handicap, and start hosting.
2. Confirm the host screen shows a reachable IPv4 address and `Waiting for guest`.
3. On device B, open Face to face, choose `Wi-Fi join`, enter the host IPv4 address, and connect.
4. Confirm both devices show the same board size and handicap after `StartGame`.
5. Confirm the first turn is correct:
   - no handicap: host/black moves first
   - handicap > 0: white moves first

## Move and Sync

1. Play 5-10 alternating moves and confirm both boards stay identical after every move.
2. Verify the turn banner flips correctly after each remote move.
3. Force an out-of-sync case if possible:
   - disconnect Wi-Fi on one device for a move
   - reconnect
   - confirm the board resyncs to the host state without duplicating or dropping stones
4. Verify captures update on both devices after a remote capture.
5. Verify KO rejection on one device is reflected by unchanged state on the other device.

## Pass and Estimation

1. Play a short game and pass once from device A.
2. Pass from device B and confirm estimation opens on both devices.
3. Close estimation on both devices and confirm no stones disappear or change ownership.
4. Verify repeated pass does not corrupt move history or desync the board.

## Disconnect and Reconnect

1. While connected, close the app or disable Wi-Fi on device B.
2. Confirm device A shows a disconnect message instead of remaining in `Connected`.
3. Restart hosting on device A and reconnect from device B.
4. Confirm the existing board state is restored after reconnect, not a fresh empty game.
5. Repeat the same flow with device A disconnecting and device B reconnecting.

## Cleanup

1. Start a brand-new hotseat game after LAN testing.
2. Confirm old LAN state does not leak into the new local game.
