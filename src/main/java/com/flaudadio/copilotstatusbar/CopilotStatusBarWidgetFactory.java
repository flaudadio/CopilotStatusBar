package com.flaudadio.copilotstatusbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import org.jetbrains.annotations.NotNull;

public final class CopilotStatusBarWidgetFactory implements StatusBarWidgetFactory {
    @Override
    public @NotNull String getId() {
        return "CopilotUsageStatus";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Copilot Usage Status";
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true;
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new CopilotStatusBarWidget();
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        widget.dispose();
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }
}
