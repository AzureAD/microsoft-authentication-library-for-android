// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import com.google.gson.JsonParser
import java.net.URI

internal fun deletionConfigurationProblem(json: String, packageName: String, hashes: Set<String>): String? {
    return try {
        val config = JsonParser.parseString(json).asJsonObject
        val guid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        if (config.has("client_secret") || !guid.matches(config["client_id"].asString)) {
            return "Set a public-client client_id; never include a client_secret."
        }
        val redirect = URI(config["redirect_uri"].asString)
        if (redirect.scheme != "msauth" || redirect.host != packageName ||
            redirect.port != -1 || redirect.userInfo != null || redirect.query != null ||
            redirect.fragment != null || redirect.path.removePrefix("/") !in hashes) {
            return "Register this package and signing hash; set the matching encoded msauth redirect_uri."
        }
        if (config["account_mode"].asString != "MULTIPLE" ||
            config["authorization_user_agent"].asString != "BROWSER" ||
            config["broker_redirect_uri_registered"].asBoolean) {
            return "Deletion PoC requires MULTIPLE, BROWSER and broker_redirect_uri_registered=false."
        }
        val authorities = config["authorities"].asJsonArray
        if (authorities.size() != 1) return "Configure exactly one workforce tenant authority."
        val authority = authorities[0].asJsonObject
        val audience = authority["audience"].asJsonObject
        if (authority["type"].asString != "AAD" || audience["type"].asString != "AzureADMyOrg" ||
            !guid.matches(audience["tenant_id"].asString) ||
            (authority.has("authority_url") &&
                authority["authority_url"].asString.trimEnd('/') !=
                "https://login.microsoftonline.com/${audience["tenant_id"].asString}") ||
            (config.has("multiple_clouds_supported") && config["multiple_clouds_supported"].asBoolean)) {
            return "Configure AzureADMyOrg with the registration tenant GUID in the public cloud."
        }
        null
    } catch (_: Exception) {
        "Supply valid auth_config.json: client_id, redirect_uri, account_mode, browser and AAD tenant settings."
    }
}
