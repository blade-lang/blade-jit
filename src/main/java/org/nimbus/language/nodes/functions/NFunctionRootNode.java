package org.nimbus.language.nodes.functions;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import org.nimbus.language.NimbusLanguage;
import org.nimbus.language.nodes.NNode;

public class NFunctionRootNode extends RootNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child private NNode body;

  public NFunctionRootNode(NimbusLanguage language, NNode functionBody) {
    super(language);
    this.body = functionBody;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return body.execute(frame);
  }
}
