package org.blade.language.nodes.imports;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.strings.TruffleString;
import org.blade.language.nodes.NNode;
import org.blade.language.runtime.*;

@NodeChild("path")
@NodeChild("name")
@ImportStatic(BString.class)
@SuppressWarnings("unused")
public abstract class NImportProcessorNode extends NNode {

  private final String[] importedSymbols;
  private final boolean importsAll;

  public NImportProcessorNode(String[] importedSymbols, boolean importsAll) {
    this.importedSymbols = importedSymbols;
    this.importsAll = importsAll;
  }

  @Specialization(guards = "isBuiltin(modulePath, codePointNode)")
  protected Object doBuiltin(TruffleString modulePath, TruffleString name,
                             @Cached @Cached.Shared("codePointNode") TruffleString.CodePointAtIndexNode codePointNode,
                             @Cached(value = "languageContext()", neverDefault = true) @Cached.Shared("context") BladeContext context,
                             @Cached(value = "context.globalScope", neverDefault = true) DynamicObject globalScope,
                             @Cached @Cached.Shared("nameToStringNode") TruffleString.ToJavaStringNode nameToStringNode,
                             @CachedLibrary(limit = "3") @Cached.Shared("objectLibrary") InteropLibrary objectLibrary) {
    ModuleObject module = context.getBuiltinModule(modulePath);
    if (module == null) {
      throw BladeRuntimeError.error(this, "Cannot find builtin module ", modulePath);
    }

    try {
      String nameString = nameToStringNode.execute(name);

      bindImportedSymbols(module, nameString, globalScope, objectLibrary);
    } catch (UnsupportedMessageException | UnknownIdentifierException | UnsupportedTypeException e) {
      throw BladeRuntimeError.error(this, "Failed to bind module objects");
    }

    return BladeNil.SINGLETON;
  }

  @Specialization(guards = {"equals(modulePath, cachedModulePath, equalNode)", "!isBuiltin(modulePath, codePointNode)"}, limit = "3")
  protected Object doCached(TruffleString modulePath, TruffleString name,
                            @Cached("modulePath") TruffleString cachedModulePath,
                            @Cached("name") TruffleString cachedName,
                            @Cached TruffleString.EqualNode equalNode,
                            @Cached @Cached.Shared("codePointNode") TruffleString.CodePointAtIndexNode codePointNode,
                            @Cached @Cached.Shared("pathToStringNode") TruffleString.ToJavaStringNode pathToStringNode,
                            @Cached @Cached.Shared("nameToStringNode") TruffleString.ToJavaStringNode nameToStringNode,
                            @Cached @Cached.Exclusive TruffleString.ToJavaStringNode cachedPathToStringNode,
                            @Cached @Cached.Exclusive TruffleString.ToJavaStringNode loadCachedPathToStringNode,
                            @Cached("toString(cachedPathToStringNode, cachedName)") String cachedNameString,
                            @Cached("loadModule(cachedName, cachedModulePath, nameToStringNode, pathToStringNode)") ModuleObject cachedModule,
                            @Cached(value = "languageContext().globalScope") DynamicObject globalScope,
                            @CachedLibrary(limit = "3") @Cached.Shared("objectLibrary") InteropLibrary objectLibrary
  ) {
    try {
      bindImportedSymbols(cachedModule, cachedNameString, globalScope, objectLibrary);
    } catch (UnsupportedMessageException | UnknownIdentifierException | UnsupportedTypeException e) {
      throw BladeRuntimeError.error(this, "Failed to bind module objects");
    }

    return BladeNil.SINGLETON;
  }

  @Specialization(replaces = "doCached")
  protected Object doUncached(TruffleString modulePath, TruffleString name,
                              @Cached @Cached.Shared("pathToStringNode") TruffleString.ToJavaStringNode pathToStringNode,
                              @Cached @Cached.Shared("nameToStringNode") TruffleString.ToJavaStringNode nameToStringNode,
                              @Cached(value = "languageContext()", neverDefault = true) @Cached.Shared("context") BladeContext context,
                              @Cached(value = "context.globalScope", neverDefault = true) DynamicObject globalScope,
                              @CachedLibrary(limit = "3") @Cached.Shared("objectLibrary") InteropLibrary objectLibrary) {
    String nameString = nameToStringNode.execute(name);
    ModuleObject module = context.loadModule(this, nameString, pathToStringNode.execute(modulePath));

    try {
      bindImportedSymbols(module, nameString, globalScope, objectLibrary);
    } catch (UnsupportedMessageException | UnknownIdentifierException | UnsupportedTypeException e) {
      throw BladeRuntimeError.error(this, "Failed to bind module objects");
    }

    return BladeNil.SINGLETON;
  }

  private void bindImportedSymbols(ModuleObject module, String moduleName, DynamicObject globalScope, InteropLibrary objectLibrary) throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
    if (!importsAll && importedSymbols.length == 0) {
      objectLibrary.writeMember(globalScope, moduleName, module);
    }

    for (String symbol : importedSymbols) {
      try {
        Object exportedValue = module.getExport(symbol);
        objectLibrary.writeMember(globalScope, symbol, exportedValue);
      } catch (UnknownIdentifierException e) {
        throw BladeRuntimeError.error(this, "Symbol '", symbol, "' not found in module '", module.path, "'");
      }
    }

    if (importsAll) {
      MemberNamesObject moduleMembers = (MemberNamesObject) objectLibrary.getMembers(module, false);
      for (Object name : moduleMembers.getNames()) {
        String originalName = BString.toString(name);

        try {
          Object exportedValue = module.getExport(originalName);
          objectLibrary.writeMember(globalScope, originalName, exportedValue);
        } catch (UnknownIdentifierException e) {
          throw BladeRuntimeError.error(this, "Unknown error");
        }
      }
    }
  }

  protected ModuleObject loadModule(TruffleString name, TruffleString path, TruffleString.ToJavaStringNode nameToStringNode, TruffleString.ToJavaStringNode pathToStringNode) {
    return languageContext().loadModule(
      this,
      BString.toString(nameToStringNode, name),
      BString.toString(pathToStringNode, path)
    );
  }

  protected boolean isBuiltin(TruffleString path, TruffleString.CodePointAtIndexNode codePointNode) {
    return BString.toCodePoint(path, codePointNode, 0) == 95; // 95 == `_`
  }
}
