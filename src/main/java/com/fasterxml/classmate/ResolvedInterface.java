package com.fasterxml.classmate;

import java.util.*;

public class ResolvedInterface extends ResolvedType
{
    public ResolvedInterface(Class<?> erased, List<ResolvedType> typeParameters, List<ResolvedType> interfaces)
    {
        super(erased, typeParameters, interfaces);
    }

    public ResolvedType getParentClass() {
        // interfaces do not have superclass (just super-interfaces)
        return null;
    }
    
    public boolean isInterface() { return true; }
    public boolean isConcrete() { return true; }
}


