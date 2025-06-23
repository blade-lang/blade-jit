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

package org.blade.language.translator;

import com.oracle.truffle.api.bytecode.BytecodeLocal;
import com.oracle.truffle.api.strings.TruffleString;

import java.util.*;

class LocalScope {
  public final int scopeDepth;
  private final LocalScope parent;

  // Maps local names to a unique index.
  private final Map<TruffleString, Integer> locals;
  // Tracks which locals have been initialized in this scope.
  private final Set<TruffleString> initialized;

  public boolean captures = false;

  public LocalScope(LocalScope parent, int scopeDepth) {
    this.parent = parent;
    locals = parent != null ? new HashMap<>(parent.locals) : new HashMap<>();
    initialized = parent != null ? new HashSet<>(parent.initialized) : new HashSet<>();
    this.scopeDepth = scopeDepth;
  }

  public LocalScope() {
    this(null, 0);
  }

  boolean isDeclared(TruffleString name) {
    return locals.containsKey(name);
  }

  void declare(TruffleString name, int index) {
    locals.put(name, index);
  }

  int add(TruffleString name, int index) {
    locals.put(name, index);
    return index;
  }

  Integer getIndex(TruffleString name) {
    Integer i = locals.get(name);
    if (i == null) {
      return -1;
    } else {
      return i;
    }
  }

  boolean initialize(TruffleString name) {
    return initialized.add(name);
  }

//  NFrameMember findFrameMember(String name) {
//    for (Map<String, NFrameMember> scope : stack) {
//      NFrameMember member = scope.get(name);
//      if (member != null) {
//        return member;
//      }
//    }
//
//    return null;
//  }

//  NFrameMember.ClosedVariable findClosedFrameMember(String name, int scope) {
//    if (parent != null) {
//      NFrameMember value = parent.findFrameMember(name);
//
//      if (value != null) {
//        // This scope captures from a surrounding scope.
//        return new NFrameMember.ClosedVariable(value, scope - parent.scopeDepth);
//      }
//
//      return parent.findClosedFrameMember(name, scope);
//    }
//
//    return null;
//  }
//
//  NFrameMember.ClosedVariable findClosedFrameMember(String name) {
//    NFrameMember.ClosedVariable value = findClosedFrameMember(name, scopeDepth);
//
//    if (value != null) {
//      for (
//        LocalScope next = this;
//        next != null && scopeDepth - next.scopeDepth < value.scopeDepth;
//        next = next.parent
//      ) {
//        next.captures = true;
//      }
//    }
//
//    return value;
//  }
}
