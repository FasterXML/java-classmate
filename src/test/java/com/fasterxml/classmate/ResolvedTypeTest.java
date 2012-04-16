package com.fasterxml.classmate;

import com.fasterxml.classmate.types.ResolvedArrayType;
import com.fasterxml.classmate.types.ResolvedInterfaceType;
import com.fasterxml.classmate.types.ResolvedObjectType;
import org.junit.Test;

import java.io.Serializable;
import java.nio.CharBuffer;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 4/13/12
 * Time: 4:30 PM
 */
public class ResolvedTypeTest {

    @Test
    public void canCreateSubtype() {
        ResolvedObjectType stringType = new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES);
        assertTrue(stringType.canCreateSubtype(String.class));
        assertFalse(stringType.canCreateSubtype(CharBuffer.class));

        ResolvedObjectType objectType = new ResolvedObjectType(Object.class, null, null, ResolvedType.NO_TYPES);
        assertTrue(objectType.canCreateSubtype(Object.class));
        assertTrue(objectType.canCreateSubtype(String.class));
        assertTrue(objectType.canCreateSubtype(CharBuffer.class));

        ResolvedArrayType arrayType = new ResolvedArrayType(String[].class, null, null, stringType);
        assertFalse(arrayType.canCreateSubtype(String[].class));
        assertFalse(arrayType.canCreateSubtype(CharBuffer[].class));
        assertFalse(arrayType.canCreateSubtype(String.class));

    }

    @Test
    public void typeParametersFor() {
        ResolvedObjectType stringType = new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES);
        assertNull(stringType.typeParametersFor(CharBuffer.class));
    }

    @Test
    public void findSupertype() {
        ResolvedInterfaceType comparableType = new ResolvedInterfaceType(Comparable.class, TypeBindings.create(String.class, ResolvedType.NO_TYPES), ResolvedType.NO_TYPES);
        ResolvedObjectType stringType = new ResolvedObjectType(String.class, null, null, new ResolvedType[] { comparableType });
        assertNull(stringType.findSupertype(CharBuffer.class));

        assertNull(stringType.findSupertype(Serializable.class));

    }

    @Test
    public void isConcrete() {
        ResolvedObjectType stringType = new ResolvedObjectType(String.class, null, null, ResolvedType.NO_TYPES);
        assertTrue(stringType.isConcrete());
        ResolvedObjectType charBufferType = new ResolvedObjectType(CharBuffer.class, null, null, ResolvedType.NO_TYPES);
        assertFalse(charBufferType.isConcrete());
    }

    @Test
    public void accessors() {
        // the default accessor implementation is to return an empty-list; check for a new subtype
        ResolvedType type = new ResolvedType(String.class, null) {
            @Override public boolean canCreateSubtypes() {
                return false;
            }
            @Override public ResolvedType getParentClass() {
                return null;
            }
            @Override public ResolvedType getSelfReferencedType() {
                return null;
            }
            @Override public ResolvedType getArrayElementType() {
                return null;
            }
            @Override public List<ResolvedType> getImplementedInterfaces() {
                return null;
            }
            @Override public boolean isInterface() {
                return false;
            }
            @Override public boolean isAbstract() {
                return false;
            }
            @Override public boolean isArray() {
                return false;
            }
            @Override public boolean isPrimitive() {
                return false;
            }
            @Override public StringBuilder appendBriefDescription(StringBuilder sb) {
                return null;
            }
            @Override public StringBuilder appendFullDescription(StringBuilder sb) {
                return null;
            }
            @Override public StringBuilder appendSignature(StringBuilder sb) {
                return null;
            }
            @Override public StringBuilder appendErasedSignature(StringBuilder sb) {
                return null;
            }
        };
        assertEquals(0, type.getConstructors().size());
        assertEquals(0, type.getMemberFields().size());
        assertEquals(0, type.getMemberMethods().size());
        assertEquals(0, type.getStaticMethods().size());
        assertEquals(0, type.getStaticFields().size());
    }

}
