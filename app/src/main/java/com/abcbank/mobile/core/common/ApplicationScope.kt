package com.abcbank.mobile.core.common

import javax.inject.Qualifier

/**
 * The process-wide CoroutineScope, for work that must outlive the screen that started it, such as
 * preparing a persona's data after login (ARCHITECTURE.md section 04).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
