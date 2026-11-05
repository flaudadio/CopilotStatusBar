# Copilot Usage Status for Rider

Available on [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/36018-copilot-usage-status).

A JetBrains Rider plugin that displays the AI Credits already cached by the
official GitHub Copilot plugin in the status bar. When available, the widget
shows usage, the credit limit, remaining credits, and the reset date.

## Requirements and limitations

- Rider 2025.3 or later, JDK 21, and Gradle to build the plugin.
- The official **GitHub Copilot** plugin must be installed, enabled, and signed in.
- The official plugin must confirm that the account uses AI Credits. If it does
  not, this plugin will not substitute another metric.

This integration does not request a GitHub token or require the **View
Organization Copilot Metrics** permission. It reads the official plugin's
internal quota service and uses its session and cache. This API is not public or
stable and may change in a GitHub Copilot update. If the service is unavailable,
the widget explains why in its tooltip rather than displaying an invented
value. Clicking the widget requests an update from the official plugin; the
displayed number changes after that plugin refreshes its cache.

## Build and installation

From the project root, run in PowerShell:

```powershell
.\gradlew.bat buildPlugin
```

The installable ZIP is created in `build/distributions/`. In Rider, open
**Settings | Plugins**, choose **Install Plugin from Disk...** from the gear
menu, select the ZIP, and restart the IDE when prompted.

## Settings

In **Settings | Tools | Copilot Usage Status**, choose English or Spanish and
configure the alert threshold and refresh interval. The selected language is
saved in the IDE settings. The default alert threshold is `6000` AI Credits;
set it to `0` to disable the alert. Notifications are informational and do not
limit or block Copilot usage. The default refresh interval is `60` seconds and
accepts values from `30` to `3600`; the official plugin may apply its own
refresh limits.

Click the widget to request an immediate update from the official plugin, or
double-click it to open this plugin's settings. If a temporary refresh fails,
the last valid value remains in the status bar and the tooltip describes the
problem. Before the first valid value is available, the widget displays
`AI Credits: --`.
