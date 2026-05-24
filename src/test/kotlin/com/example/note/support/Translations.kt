package com.example.note.support

import com.example.note.common.extentions.TranslationContext
import org.springframework.context.support.StaticMessageSource

/**
 * `String.tr()` resolves i18n keys through a static [TranslationContext] that Spring
 * normally wires up at startup. Pure unit tests have no Spring context, so any service
 * method that returns a translated message would hit an uninitialized `lateinit` field.
 *
 * Calling [initTranslations] installs an empty [StaticMessageSource]; since
 * `TranslationContext.resolve` falls back to the key itself on a missing message, every
 * `"some.key".tr()` simply returns `"some.key"` — which is exactly what assertions check.
 */
fun initTranslations() {
    TranslationContext(StaticMessageSource())
}
