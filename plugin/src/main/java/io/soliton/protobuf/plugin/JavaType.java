/**
 * Copyright 2013 Julien Silland
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.soliton.protobuf.plugin;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;

/**
 * Represents a fully qualified Java type.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class JavaType {

	private static final Joiner DOT = Joiner.on('.').skipNulls();

  private final String javaPackage;
  private final String enclosingClass;
  private final String className;

  /**
   * Exhaustive constructor.
   *
   * @param javaPackage the type's package, may be {@code null}
   * @param enclosingClass the type's enclosing class, may be {@code null}
   * @param className the type's name
   */
	public JavaType(@Nullable String javaPackage, @Nullable String enclosingClass, String className) {
		this.className = Preconditions.checkNotNull(className);
    this.javaPackage = javaPackage;
    this.enclosingClass = enclosingClass;
	}
	
	@Override
	public String toString() {
		return DOT.join(javaPackage, enclosingClass, className);
	}
}
