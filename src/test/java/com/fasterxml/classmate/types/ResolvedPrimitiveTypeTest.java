package com.fasterxml.classmate.types;

import org.junit.Test;

import static junit.framework.Assert.*;

@SuppressWarnings("deprecation")
public class ResolvedPrimitiveTypeTest {

    private static interface Callback {
        void test(ResolvedPrimitiveType primitiveType);
    }

    @Test
    public void canCreateSubtypes() {
        runAgainstAllTypes(new Callback() {
            @Override public void test(ResolvedPrimitiveType primitiveType) {
                assertFalse(primitiveType.canCreateSubtypes());
            }
        });
    }

    @Test
    public void getSelfReferencedType() {
        runAgainstAllTypes(new Callback() {
            @Override public void test(ResolvedPrimitiveType primitiveType) {
                assertNull(primitiveType.getSelfReferencedType());
            }
        });
    }

    @Test
    public void getParentClass() {
        runAgainstAllTypes(new Callback() {
            @Override public void test(ResolvedPrimitiveType primitiveType) {
                assertNull(primitiveType.getParentClass());
            }
        });
    }

    @Test
    public void getArrayElementType() {
        runAgainstAllTypes(new Callback() {
            @Override public void test(ResolvedPrimitiveType primitiveType) {
                assertNull(primitiveType.getArrayElementType());
            }
        });
    }

    @Test
    public void isInterface() {
        runAgainstAllTypes(new Callback() {
            @Override public void test(ResolvedPrimitiveType primitiveType) {
                assertFalse(primitiveType.isInterface());
            }
        });
    }

    @Test
    public void isAbstract() {
        runAgainstAllTypes(new Callback() {
            @Override public void test(ResolvedPrimitiveType primitiveType) {
                assertFalse(primitiveType.isAbstract());
            }
        });
    }

    @Test
    public void isArray() {
        runAgainstAllTypes(new Callback() {
            @Override public void test(ResolvedPrimitiveType primitiveType) {
                assertFalse(primitiveType.isArray());
            }
        });
    }

    @Test
    public void getImplementedInterfaces() {
        runAgainstAllTypes(new Callback() {
            @Override public void test(ResolvedPrimitiveType primitiveType) {
                assertEquals(0, primitiveType.getImplementedInterfaces().size());
            }
        });
    }

    @Test
    public void appendSignature() {
        ResolvedPrimitiveType primitiveType = new ResolvedPrimitiveType(boolean.class, 'Z', "boolean");
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = primitiveType.appendSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("Z", returned.toString());
        buffer = new StringBuilder("something already ");
        returned = primitiveType.appendSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("something already Z", returned.toString());
    }

    @Test
    public void appendErasedSignature() {
        ResolvedPrimitiveType primitiveType = new ResolvedPrimitiveType(boolean.class, 'Z', "boolean");
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = primitiveType.appendErasedSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("Z", returned.toString());
        buffer = new StringBuilder("something already ");
        returned = primitiveType.appendErasedSignature(buffer);
        assertSame(buffer, returned);
        assertEquals("something already Z", returned.toString());
    }

    @Test
    public void appendFullDescription() {
        ResolvedPrimitiveType primitiveType = new ResolvedPrimitiveType(boolean.class, 'Z', "boolean");
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = primitiveType.appendFullDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("boolean", returned.toString());
        buffer = new StringBuilder("something already ");
        returned = primitiveType.appendFullDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("something already boolean", returned.toString());
    }

    @Test
    public void appendBriefDescription() {
        ResolvedPrimitiveType primitiveType = new ResolvedPrimitiveType(boolean.class, 'Z', "boolean");
        StringBuilder buffer = new StringBuilder();
        StringBuilder returned = primitiveType.appendBriefDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("boolean", returned.toString());
        buffer = new StringBuilder("something already ");
        returned = primitiveType.appendBriefDescription(buffer);
        assertSame(buffer, returned);
        assertEquals("something already boolean", returned.toString());
    }

    private void runAgainstAllTypes(Callback callback) {
        ResolvedPrimitiveType primitiveType = new ResolvedPrimitiveType(boolean.class, 'Z', "boolean");
        callback.test(primitiveType);
        primitiveType = new ResolvedPrimitiveType(Byte.TYPE, 'B', "byte");
        callback.test(primitiveType);
        primitiveType = new ResolvedPrimitiveType(Short.TYPE, 'S', "short");
        callback.test(primitiveType);
        primitiveType = new ResolvedPrimitiveType(Character.TYPE, 'C', "char");
        callback.test(primitiveType);
        primitiveType = new ResolvedPrimitiveType(Integer.TYPE, 'I', "int");
        callback.test(primitiveType);
        primitiveType = new ResolvedPrimitiveType(Long.TYPE, 'J', "long");
        callback.test(primitiveType);
        primitiveType = new ResolvedPrimitiveType(Float.TYPE, 'F', "float");
        callback.test(primitiveType);
        primitiveType = new ResolvedPrimitiveType(Double.TYPE, 'D', "double");
        callback.test(primitiveType);
        primitiveType = new ResolvedPrimitiveType(Void.TYPE, 'V', "void");
        callback.test(primitiveType);
    }

}
