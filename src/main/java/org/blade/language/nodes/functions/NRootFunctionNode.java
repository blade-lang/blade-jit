package org.blade.language.nodes.functions;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import org.blade.language.BladeLanguage;
import org.blade.language.nodes.NNode;

public class NRootFunctionNode extends RootNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode body;

  public NRootFunctionNode(BladeLanguage language, NNode functionBody) {
    super(language);
    this.body = functionBody;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return body.execute(frame);
  }
}
