package com.quantumstudio.smartpdf.ui.common

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * 权限自动刷新观察者。
 *
 * 该类实现了 [DefaultLifecycleObserver]，用于在 Activity 或 Fragment 回到前台（onResume）时，
 * 自动触发权限检查。如果权限已授予，则通过回调执行相应的刷新逻辑。
 *
 * ### 使用场景：
 * 当用户从系统设置界面手动开启权限并返回 App 时，此观察者能确保 UI 立即响应权限变化。
 *
 * @property checkPermission 权限检查逻辑块。返回 `true` 表示权限已获得。
 * @property onPermissionGranted 权限获得后的回调动作（通常用于发射刷新信号或重新加载数据）。
 */
class RefreshPermissionObserver(
    private val checkPermission: () -> Boolean,
    private val onPermissionGranted: () -> Unit
) : DefaultLifecycleObserver {

    /**
     * 当关联的生命周期组件（Activity/Fragment）进入 [Lifecycle.State.RESUMED] 状态时触发。
     * * 此时是检查权限变更的最佳时机，因为用户刚刚返回应用。
     */
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        // 执行传入的权限检查闭包
        if (checkPermission()) {
            // 若权限通过，执行授权后的回调动作
            onPermissionGranted()
        }
    }
}