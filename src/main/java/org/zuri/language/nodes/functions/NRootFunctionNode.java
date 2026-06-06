package org.zuri.language.nodes.functions;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import org.zuri.language.ZuriLanguage;
import org.zuri.language.nodes.NNode;

public class NRootFunctionNode extends RootNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NNode body;

  public NRootFunctionNode(ZuriLanguage language, NNode functionBody) {
    super(language);
    this.body = functionBody;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return body.execute(frame);
  }
}
