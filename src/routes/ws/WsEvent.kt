package com.dartcaller.routes.ws

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
open class WsEvent(val type: String)
