package org.nimbus.language.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import org.nimbus.language.runtime.NListObject;
import org.nimbus.language.runtime.NimContext;
import org.nimbus.language.runtime.NimNil;

@TypeSystemReference(NimTypes.class)
public abstract class NBaseNode extends Node {
  protected final NimContext languageContext() {
    return NimContext.get(this);
  }
}
