package dev.thomato.auth.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

/**
 * Serves the first-party pages (ADR 0004). An explicit list, not a catch-all, so an unknown path still gets
 * a real 404 and a mistyped OAuth2 endpoint never renders the app.
 */
@Controller
class SpaController {
    @GetMapping(REGISTER, VERIFY_EMAIL, SIGN_IN, ACCOUNT)
    @Suppress("FunctionOnlyReturningConstant") // the view name is the handler's whole job
    fun page() = "forward:/index.html"

    companion object {
        const val REGISTER = "/register"
        const val VERIFY_EMAIL = "/verify-email"
        const val SIGN_IN = "/sign-in"

        /** The one page that needs a Session; WebSecurityConfiguration is what enforces that. */
        const val ACCOUNT = "/account"
    }
}
