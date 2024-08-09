package com.example.cachinglibrary

/**
 * Created by Suraj Kumar
 * Date: 02.04.2024
 * Email: suraj.kumar@sharechat.co
 *
 * Description:
 * Jira:
 */
object CacheUtil {
    val size = (Runtime.getRuntime().maxMemory() / 10).toInt()
}