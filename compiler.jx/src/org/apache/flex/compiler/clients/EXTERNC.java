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

package org.apache.flex.compiler.clients;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.flex.compiler.internal.codegen.externals.emit.ReferenceEmitter;
import org.apache.flex.compiler.internal.codegen.externals.pass.ReferenceCompiler;
import org.apache.flex.compiler.internal.codegen.externals.reference.ReferenceModel;
import org.apache.flex.compiler.internal.codegen.js.JSSharedData;
import org.apache.flex.compiler.problems.ICompilerProblem;

import com.google.javascript.jscomp.Result;

/**
 * @author Michael Schmalle
 */
public class EXTERNC
{
    static enum ExitCode
    {
        SUCCESS(0),
        PRINT_HELP(1),
        FAILED_WITH_PROBLEMS(2),
        FAILED_WITH_EXCEPTIONS(3),
        FAILED_WITH_CONFIG_PROBLEMS(4);

        ExitCode(int code)
        {
            this.code = code;
        }

        final int code;
    }

    private ExternCConfiguration configuration;
    private ReferenceModel model;
    private ReferenceCompiler compiler;
    private ReferenceEmitter emitter;

    public ReferenceModel getModel()
    {
        return model;
    }

    public EXTERNC(ExternCConfiguration configuration)
    {
        this.configuration = configuration;

        model = new ReferenceModel(configuration);
        compiler = new ReferenceCompiler(model);
        emitter = new ReferenceEmitter(model);
    }

    /**
     * Java program entry point.
     * 
     * @param args command line arguments
     */
    public static void main(final String[] args)
    {
        int exitCode = staticMainNoExit(args);
        System.exit(exitCode);
    }

    /**
     * Entry point for the <code>externc</code>.
     *
     * @param args Command line arguments.
     * @return An exit code.
     */
    public static int staticMainNoExit(final String[] args)
    {
        long startTime = System.nanoTime();

        ExternCConfiguration config = new ExternCConfiguration(args);
        final EXTERNC compiler = new EXTERNC(config);
        final Set<ICompilerProblem> problems = new HashSet<ICompilerProblem>();
        final int exitCode = compiler.mainNoExit(args, problems, true);

        long endTime = System.nanoTime();
        JSSharedData.instance.stdout((endTime - startTime) / 1e9 + " seconds");

        return exitCode;
    }

    public int mainNoExit(final String[] args, Set<ICompilerProblem> problems,
            Boolean printProblems)
    {
        int exitCode = -1;

        try
        {
            cleanOutput();
            compile();
            emit();
        }
        catch (IOException e)
        {
            JSSharedData.instance.stderr(e.toString());
        }
        finally
        {
            if (problems != null && !problems.isEmpty())
            {
                if (printProblems)
                {
                }
            }
        }

        return exitCode;
    }

    public void cleanOutput() throws IOException
    {
        FileUtils.deleteDirectory(configuration.getAsRoot());
    }

    public void emit() throws IOException
    {
        emitter.emit();
    }

    public Result compile() throws IOException
    {
        return compiler.compile();
    }

}