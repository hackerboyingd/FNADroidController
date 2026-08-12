# FNADroid Controller v3

- Persistent `/dev/uinput` virtual keyboard.
- Independent key DOWN/UP states: multiple buttons can be held simultaneously.
- 8-way joystick: diagonals hold both axes simultaneously.
- Joystick visual stick follows the finger and returns to center.
- Config: `/storage/emulated/0/FNADroidController/config.json`
- Button fields: `label`, `key`, `x`, `y`.
- Joystick fields: `x`, `y`, `size`, `deadzone`.

Native build requires Android NDK 26.3.11579264. GitHub Actions installs it automatically.
The target device/kernel must expose `/dev/uinput` and allow the root process to open it.
