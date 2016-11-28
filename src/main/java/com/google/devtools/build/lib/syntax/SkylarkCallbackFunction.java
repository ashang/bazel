// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.syntax;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * A helper class for calling Skylark functions from Java.
 */
public class SkylarkCallbackFunction {

  private final BaseFunction callback;
  private final FuncallExpression ast;
  private final Environment funcallEnv;

  public SkylarkCallbackFunction(
      BaseFunction callback, FuncallExpression ast, Environment funcallEnv) {
    this.callback = callback;
    this.ast = ast;
    this.funcallEnv = funcallEnv;
  }

  public ImmutableList<String> getParameterNames() {
    return callback.signature.getSignature().getNames();
  }

  public Object call(ClassObject ctx, Object... arguments)
      throws EvalException, InterruptedException {
    try (Mutability mutability = Mutability.create("callback %s", callback)) {
      Environment env = Environment.builder(mutability)
          .setSkylark()
          .setEventHandler(funcallEnv.getEventHandler())
          .setGlobals(funcallEnv.getGlobals())
          .build();
      return callback.call(buildArgumentList(ctx, arguments), null, ast, env);
    } catch (ClassCastException | IllegalArgumentException e) {
      throw new EvalException(ast.getLocation(), e.getMessage());
    }
  }

  /**
   * Creates a list of actual arguments that contains the given arguments and all attribute values
   * required from the specified context.
   */
  private ImmutableList<Object> buildArgumentList(ClassObject ctx, Object... arguments) {
    Builder<Object> builder = ImmutableList.<Object>builder();
    ImmutableList<String> names = getParameterNames();
    int requiredParameters = names.size() - arguments.length;
    for (int pos = 0; pos < requiredParameters; ++pos) {
      String name = names.get(pos);
      Object value = ctx.getValue(name);
      if (value == null) {
          throw new IllegalArgumentException(ctx.errorMessage(name));
      }
      builder.add(value);
    }
    return builder.add(arguments).build();
  }
}
