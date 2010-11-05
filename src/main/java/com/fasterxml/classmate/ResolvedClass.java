package com.fasterxml.classmate;

import java.lang.reflect.Modifier;
import java.util.List;

public class ResolvedClass extends ResolvedType
{
    protected final ResolvedClass _superclass;
    
    public ResolvedClass(Class<?> erased, List<ResolvedType> typeParameters, ResolvedClass superclass, List<ResolvedType> interfaces)
    {
        super(erased, typeParameters, interfaces);
        _superclass = superclass;
    }

    public ResolvedType getParentClass() {
        return _superclass;
    }
    
    public boolean isInterface() { return false; }

    public boolean isConcrete() {
        return Modifier.isAbstract(_erasedType.getModifiers());
    }
}

