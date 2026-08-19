package com.fssocrates.abc

/** Public IPC contract (v1). */
object IpcProtocol {
    const val VERSION = 1
    /** Frozen public IPC major for 1.0. */
    const val API_LEVEL = 1

    const val ACTION_START = "com.fssocrates.abc.ACTION_START_JOB"
    const val ACTION_CANCEL = "com.fssocrates.abc.ACTION_CANCEL_JOB"
    const val ACTION_STATUS = "com.fssocrates.abc.ACTION_STATUS"
    const val ACTION_RESUME = "com.fssocrates.abc.ACTION_RESUME"
    const val EXTRA_RESULT_PENDING_INTENT = "com.fssocrates.abc.EXTRA_RESULT_PENDING_INTENT"

    const val EXTRA_PROTOCOL_VERSION = "com.fssocrates.abc.EXTRA_PROTOCOL_VERSION"
    const val EXTRA_JOB_ID = "com.fssocrates.abc.EXTRA_JOB_ID"
    const val EXTRA_TARGET_URL = "com.fssocrates.abc.EXTRA_TARGET_URL"
    const val EXTRA_SCRIPT = "com.fssocrates.abc.EXTRA_SCRIPT"
    const val EXTRA_RESULT_URL = "com.fssocrates.abc.EXTRA_RESULT_URL"
    const val EXTRA_EVENT = "com.fssocrates.abc.EXTRA_EVENT"
    const val EXTRA_REASON = "com.fssocrates.abc.EXTRA_REASON"
    const val EXTRA_ERROR_CODE = "com.fssocrates.abc.EXTRA_ERROR_CODE"
    const val EXTRA_ERROR_MESSAGE = "com.fssocrates.abc.EXTRA_ERROR_MESSAGE"
    const val EXTRA_STATUS = "com.fssocrates.abc.EXTRA_STATUS"

    const val BROADCAST_RESULT = "com.fssocrates.abc.ACTION_LINK_EXTRACTED"
    const val BROADCAST_EVENT = "com.fssocrates.abc.ACTION_JOB_EVENT"
}
