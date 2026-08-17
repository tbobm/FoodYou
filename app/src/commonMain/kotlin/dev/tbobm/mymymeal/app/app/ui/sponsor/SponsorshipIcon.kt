package dev.tbobm.mymymeal.app.app.ui.sponsor

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.tbobm.mymymeal.app.sponsorship.domain.entity.Bitcoin
import dev.tbobm.mymymeal.app.sponsorship.domain.entity.Dash
import dev.tbobm.mymymeal.app.sponsorship.domain.entity.Ko_Fi
import dev.tbobm.mymymeal.app.sponsorship.domain.entity.Monero
import dev.tbobm.mymymeal.app.sponsorship.domain.entity.PayPal
import dev.tbobm.mymymeal.app.sponsorship.domain.entity.SponsorMethod
import foodyou.app.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun SponsorMethod.Icon(modifier: Modifier = Modifier) {
    when (this) {
        Bitcoin ->
            Image(
                painter = painterResource(Res.drawable.bitcoin_logo),
                contentDescription = null,
                modifier = modifier,
            )

        Monero ->
            Image(
                painter = painterResource(Res.drawable.monero_logo),
                contentDescription = null,
                modifier = modifier,
            )

        Ko_Fi ->
            Image(
                painter = painterResource(Res.drawable.kofi_logo),
                contentDescription = null,
                modifier = modifier,
            )

        Dash ->
            Image(
                painter = painterResource(Res.drawable.dash_coin),
                contentDescription = null,
                modifier = modifier,
            )

        PayPal ->
            Image(
                painter = painterResource(Res.drawable.paypal_logo),
                contentDescription = null,
                modifier = modifier,
            )
    }
}

internal fun SponsorMessageUiModel.icon(): (@Composable (Modifier) -> Unit)? {
    val method =
        when (method) {
            "Ko-fi" -> Ko_Fi
            "Crypto" ->
                when (currency) {
                    "BTC" -> Bitcoin
                    "XMR" -> Monero
                    "DASH" -> Dash
                    else -> return null
                }

            "PayPal" -> PayPal
            else -> return null
        }

    return { modifier -> method.Icon(modifier) }
}
