package org.blade.language.nodes.imports;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.blade.language.nodes.NNode;
import org.blade.language.nodes.string.NStringLiteralNode;

@SuppressWarnings("unused")
public final class NImportNode extends NNode {
  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NStringLiteralNode path;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NStringLiteralNode name;

  @SuppressWarnings("FieldMayBeFinal")
  @Child
  private NImportProcessorNode importProcessor;

  public NImportNode(NStringLiteralNode path, NStringLiteralNode name, String[] symbols, boolean importsAll) {
    this.path = path;
    this.name = name;
    importProcessor = NImportProcessorNodeGen.create(symbols, importsAll, path, name);
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return importProcessor.execute(frame);
  }
}
