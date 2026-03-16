package com.quantumstudio.smartpdf.util

import android.animation.ObjectAnimator
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * 为启动页 (SplashScreen) 配置自定义的退出动画。
 *
 * 此扩展函数封装了原生的 [ObjectAnimator] 逻辑，实现了启动页向上平滑滑出的效果。
 * 通过将业务逻辑注入 [onComplete] 回调，确保诸如 Intent 处理或页面导航等操作
 * 仅在视觉动画彻底完成、启动页视图被移除后才触发。
 *
 * @param durationMillis 动画持续时间（毫秒），默认为 500ms。
 * @param onComplete 动画结束后的回调函数。通常用于处理 [handleIntent] 或初始导航逻辑。
 * @return 返回 [SplashScreen] 实例本身，支持链式调用。
 *
 * @see SplashScreen.setOnExitAnimationListener
 */
fun SplashScreen.installCustomExitAnimation(
    durationMillis: Long = 500L,
    onComplete: () -> Unit = {}
): SplashScreen {
    this.setOnExitAnimationListener { splashScreenView ->
        val view = splashScreenView.view

        // 创建向上位移的动画
        ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_Y,
            0f,
            -view.height.toFloat()
        ).apply {
            // 使用 FastOutSlow 差值器，使动画更符合物理动效（初始快，结尾慢）
            interpolator = FastOutSlowInInterpolator()
            duration = durationMillis

            // 监听动画结束状态
            doOnEnd {
                // 必须手动调用 remove()，否则启动页会停留在屏幕上阻挡底层 UI
                splashScreenView.remove()

                // 执行外部注入的业务逻辑，确保 UI 状态切换的平滑
                onComplete()
            }

            start()
        }
    }
    return this
}