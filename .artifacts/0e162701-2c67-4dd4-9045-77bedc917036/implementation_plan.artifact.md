# Implementation Plan - Two-Screen Robot Controller

Add a two-screen navigation (Connection vs. Control) and implement movement/servo preset controls via Bluetooth.

## Proposed Changes

### Resources

#### [MODIFY] [strings.xml](file:///C:/Users/User/AndroidStudioProjects/Kontroler_robota/app/src/main/res/values/strings.xml)
- Add labels for Forward (F), Backward (B), Left (L), Right (R), Stop (S).
- Add labels for Servo presets: Squat (Čučanj - 60°), Normal (Normalno - 80°), Stand (Stajanje - 100°).

#### [MODIFY] [activity_main.xml](file:///C:/Users/User/AndroidStudioProjects/Kontroler_robota/app/src/main/res/layout/activity_main.xml)
- Restructure the layout to include two main containers: `layoutConnect` and `layoutControl`.
- `layoutConnect`: Initial screen with the "Connect" button.
- `layoutControl`: Control screen with movement buttons (D-pad style), Servo preset buttons, and the existing PID/Height controls.

### Java Code

#### [MODIFY] [MainActivity.java](file:///C:/Users/User/AndroidStudioProjects/Kontroler_robota/app/src/main/java/com/example/kontroler_robota/MainActivity.java)
- Add variables for the two main layout containers.
- Update `onCreate` to initialize new buttons and set their click listeners.
- Movement buttons will send characters: 'F', 'B', 'L', 'R', 'S'.
- Servo preset buttons will send commands: 'Č' (60°), 'N' (80°), 'S' (100°).
- Update the connection logic to switch visibility from `layoutConnect` to `layoutControl` upon a successful Bluetooth connection.

## Verification Plan

### Manual Verification
- Deploy the app to a device.
- Verify that only the "Connect" screen is visible initially.
- Tap "Connect" and ensure the "Control" screen appears after connection (mocking the connection if necessary for testing).
- Verify that tapping movement buttons sends the correct characters via Bluetooth.
- Verify that tapping servo preset buttons sends the correct command characters.
