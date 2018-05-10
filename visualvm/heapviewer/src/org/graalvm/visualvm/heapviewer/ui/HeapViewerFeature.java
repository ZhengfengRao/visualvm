/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.heapviewer.ui;

import javax.swing.Icon;
import org.graalvm.visualvm.heapviewer.HeapContext;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class HeapViewerFeature extends HeapView {
   
    private final String id;
    private final int position;
    
    private int scope;
        
    
    public HeapViewerFeature(String id, String name, String description, Icon icon, int position) {
        super(name, description, icon);
        this.id = id;
        this.position = position;
    }

    
    public String getID() {
        return id;
    }
    
    public int getPosition() {
        return position;
    }
    
    public boolean isDefault() {
        return false;
    }
    
    
    void setScope(int scope) {
        this.scope = scope;
    }
    
    int getScope() {
        return scope;
    }
    
    
    public static abstract class Provider {
    
        public abstract HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions);

    }
    
}