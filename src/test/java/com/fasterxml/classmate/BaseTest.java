package com.fasterxml.classmate;

import java.util.Arrays;

import junit.framework.TestCase;

public abstract class BaseTest extends TestCase
{
    protected void verifyException(Throwable e, String... matches)
    {
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : matches) {
            String lmatch = match.toLowerCase();
            if (lmsg.indexOf(lmatch) >= 0) {
                return;
            }
        }
        fail("Expected an exception with one of substrings ("+Arrays.asList(matches)+"): got one with message \""+msg+"\"");
    }

    protected void verify(Throwable throwable, String format, Object ... args) {
        verifyException(throwable, String.format(format, args));
    }
}
