package com.github.raftcrast.repl_yes

import java.lang.Exception

class GetResourceException(msg: String?, cause: Throwable?) : Exception(msg, cause) {
    constructor(msg: String): this(msg, null)
    constructor(cause: Throwable): this(null, cause)
}