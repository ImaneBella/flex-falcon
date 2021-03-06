/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.flex.compiler.tree.mxml;

/**
 * This AST node represents an MXML {@code <method>} tag inside a
 * {@code <RemoteObject>} tag.
 * <p>
 * The {@code <method>} tag is special in MXML. It doesn't correspond to a
 * property definition in the {@code RemoteObject} class. Instead, it codegens
 * an {@code Operation} in the {@code operations} property. Each
 * {@code <method>} tag codegens one {@code Operation}.
 * <p>
 * A similar scheme applies to {@code <WebService>} and {@code <operation>}.
 */
public interface IMXMLRemoteObjectMethodNode extends IMXMLInstanceNode
{
    /**
     * @return The value of the {@code name} property of a {@code <method>}.
     */
    String getMethodName();
}
