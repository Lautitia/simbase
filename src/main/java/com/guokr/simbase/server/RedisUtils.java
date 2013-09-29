package com.guokr.simbase.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Date;

public class RedisUtils {

    public static final Charset UTF_8  = Charset.forName("utf8");

    public static final byte    PLUS   = 43;                     // '+'
    public static final byte    MINUS  = 45;                     // '-'
    public static final byte    COLON  = 58;                     // ':'
    public static final byte    DOLLAR = 36;                     // '$'
    public static final byte    STAR   = 42;                     // '*'

    public static final byte    CR     = 13;                     // \r
    public static final byte    LF     = 10;                     // \n

    public static final byte    DOT    = 46;                     // '.'
    public static final byte    SPACE  = 32;                     // ' '

    public static void printError(String msg, Throwable t) {
        String error = String.format("%s [%s] ERROR - %s", new Date(), Thread.currentThread().getName(), msg);
        StringWriter str = new StringWriter();
        PrintWriter pw = new PrintWriter(str, false);
        pw.println(error);
        t.printStackTrace(pw);
        System.err.print(str.getBuffer().toString());
    }

}