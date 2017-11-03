package io.zenandroid.onlinego.model.ogs

/**
 * Created by alex on 03/11/2017.
 */
data class UIConfig (
        var cdn_release: String? = null,
        var lang: String? = null,
        var csrf_token: String? = null,
        var profanity_filter: Boolean? = null,
        var chat_auth: String,
        var server_name: String? = null,
        var cdn: String? = null,
        var notification_auth: String? = null,
        var paypal_server: String? = null,
        var incident_auth: String? = null,
        var ggs_host: String? = null,
        var braintree_cse: String? = null,
        var cdn_host: String? = null,
        var version: String? = null,
        var ogs: Ogs? = null,
        //public Ignores ignores;
        var release: String? = null,
        var bots: List<Bot>? = null,
        var aga_ratings_enabled: Boolean? = null,
        var paypal_email: String? = null,
        var user: User
)