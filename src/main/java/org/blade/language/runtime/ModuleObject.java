/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2025, Richard Ore
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.blade.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;

public final class ModuleObject extends GlobalScopeObject {
  public final String name;
  public final String path;

  private final DynamicObjectLibrary UNCACHED_LIB;

  public ModuleObject(Shape shape, String path, String name) {
    super(shape);
    this.path = path;
    this.name = name;
    UNCACHED_LIB = DynamicObjectLibrary.getFactory().getUncached(this);
  }

  public void addExport(String name, Object value) {
    writeMember(name, value, UNCACHED_LIB);
  }

  public Object getExport(String name) throws UnknownIdentifierException {
    return readMember(name, UNCACHED_LIB);
  }

  public boolean hasExport(String name) {
    return isMemberModifiable(name, UNCACHED_LIB);
  }

  @CompilerDirectives.TruffleBoundary
  @Override
  public String toString() {
    return "<module "+path+">";
  }
}
