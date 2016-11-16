package com.fasterxml.classmate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.classmate.members.RawMember;
import com.fasterxml.classmate.members.ResolvedMember;

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

    protected void matchMembers(ResolvedMember<?>[] members, String[] names0)
    {
        List<String> names = Arrays.asList(names0);
        for (ResolvedMember<?> m : members) {
            String name = m.getName();
            if (!names.contains(name)) {
                fail("Expected names to be from "+names+": got '"+name+"'");
            }
        }
    }

    protected void matchRawMembers(Iterable<? extends RawMember> members, String[] names0)
    {
        Set<String> names = new HashSet<String>(Arrays.asList(names0));
        for (RawMember m : members) {
            String name = m.getName();
            if (!names.contains(name)) {
                fail("Expected names to be from "+names+": got '"+name+"'");
            }
        }
    }
}
