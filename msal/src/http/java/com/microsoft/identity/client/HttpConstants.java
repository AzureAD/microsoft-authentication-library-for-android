package com.microsoft.identity.client;

final class HttpConstants {
    
    /**
     * HTTP header fields.
     */
    static final class HeaderField {

        /**
         * @see <a href="https://tools.ietf.org/html/rfc1945#appendix-D.2.1">RFC-1945</a>
         */
        static final String ACCEPT = "Accept";

        /**
         * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">RFC-2616</a>
         */
        static final String CONTENT_TYPE = "Content-Type";
    }

    /**
     * Identifiers for file formats and format contents.
     */
    static final class MediaType {

        /**
         * @see <a href="https://tools.ietf.org/html/rfc7159">RFC-7159</a>
         */
        static final String APPLICATION_JSON = "application/json";
    }
}
