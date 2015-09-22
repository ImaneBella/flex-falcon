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
package org.apache.flex.compiler.internal.projects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.flex.compiler.common.DependencyType;
import org.apache.flex.compiler.definitions.IDefinition;
import org.apache.flex.compiler.definitions.IScopedDefinition;
import org.apache.flex.compiler.internal.codegen.mxml.flexjs.MXMLFlexJSEmitterTokens;
import org.apache.flex.compiler.internal.css.codegen.CSSCompilationSession;
import org.apache.flex.compiler.internal.definitions.InterfaceDefinition;
import org.apache.flex.compiler.internal.driver.js.flexjs.JSCSSCompilationSession;
import org.apache.flex.compiler.internal.scopes.ASProjectScope.DefinitionPromise;
import org.apache.flex.compiler.internal.scopes.ASScope;
import org.apache.flex.compiler.internal.scopes.PackageScope;
import org.apache.flex.compiler.internal.targets.LinkageChecker;
import org.apache.flex.compiler.internal.tree.mxml.MXMLClassDefinitionNode;
import org.apache.flex.compiler.internal.units.SWCCompilationUnit;
import org.apache.flex.compiler.internal.workspaces.Workspace;
import org.apache.flex.compiler.targets.ITargetSettings;
import org.apache.flex.compiler.tree.as.IASNode;
import org.apache.flex.compiler.units.ICompilationUnit;

/**
 * @author aharui
 * 
 */
public class FlexJSProject extends FlexProject
{

    /**
     * Constructor
     * 
     * @param workspace The {@code Workspace} containing this project.
     */
    public FlexJSProject(Workspace workspace)
    {
        super(workspace);
        MXMLClassDefinitionNode.GENERATED_ID_BASE = MXMLFlexJSEmitterTokens.ID_PREFIX.getToken();
    }

    private HashMap<ICompilationUnit, HashMap<String, String>> interfaces = new HashMap<ICompilationUnit, HashMap<String, String>>();
    private HashMap<ICompilationUnit, HashMap<String, DependencyType>> requires = new HashMap<ICompilationUnit, HashMap<String, DependencyType>>();

    public ICompilationUnit mainCU;

    @Override
    public void addDependency(ICompilationUnit from, ICompilationUnit to,
            DependencyType dt, String qname)
    {
        // ToDo (erikdebruin): add VF2JS conditional -> only use check during full SDK compilation
        List<IDefinition> dp = to.getDefinitionPromises();

        if (dp.size() == 0)
            return;

        IDefinition def = dp.get(0);
        // IDefinition def = to.getDefinitionPromises().get(0);
        IDefinition actualDef = ((DefinitionPromise) def).getActualDefinition();
        boolean isInterface = actualDef instanceof InterfaceDefinition;
        if (!isInterface)
        {
            if (from != to)
            {
                HashMap<String, DependencyType> reqs;
                if (requires.containsKey(from))
                    reqs = requires.get(from);
                else
                {
                    reqs = new HashMap<String, DependencyType>();
                    requires.put(from, reqs);
                }
                if (reqs.containsKey(qname))
                {
                    // inheritance is important so remember it
                    if (reqs.get(qname) != DependencyType.INHERITANCE)
                    {
                        if (!isExternalLinkage(to))
                            reqs.put(qname, dt);
                    }
                }
                else if (!isExternalLinkage(to))
                    reqs.put(qname, dt);
            }
        }
        else
        {
            if (from != to)
            {
                HashMap<String, String> interfacesArr;

                if (interfaces.containsKey(from))
                {
                    interfacesArr = interfaces.get(from);
                }
                else
                {
                    interfacesArr = new HashMap<String, String>();
                    interfaces.put(from, interfacesArr);
                }

                if (!interfacesArr.containsKey(qname))
                {
                	if (qname.equals("org.apache.flex.core.IValuesImpl"))
                		needCSS = true;
                    interfacesArr.put(qname, qname);
                }
            }
        }

        super.addDependency(from, to, dt, qname);
    }

    public boolean needLanguage;
    public boolean needCSS;
    
    private LinkageChecker linkageChecker;
    private ITargetSettings ts;
    
    // definitions that should be considered external linkage
    public Collection<String> unitTestExterns;

    private boolean isExternalLinkage(ICompilationUnit cu)
    {
        if (linkageChecker == null)
        {
            ts = getTargetSettings();
            linkageChecker = new LinkageChecker(this, ts);
        }
        // in unit tests, ts may be null and LinkageChecker NPEs
        if (ts == null)
        {
        	if (unitTestExterns != null)
        	{
        		try {
        			if (!(cu instanceof SWCCompilationUnit))
        				if (unitTestExterns.contains(cu.getQualifiedNames().get(0)))
        					return true;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
            return false;
        }

        try
        {
            return linkageChecker.isExternal(cu);
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    public ArrayList<String> getInterfaces(ICompilationUnit from)
    {
        if (interfaces.containsKey(from))
        {
            HashMap<String, String> map = interfaces.get(from);
            ArrayList<String> arr = new ArrayList<String>();
            Set<String> cus = map.keySet();
            for (String s : cus)
                arr.add(s);
            return arr;
        }
        return null;
    }

    public ArrayList<String> getRequires(ICompilationUnit from)
    {
        if (requires.containsKey(from))
        {
            HashMap<String, DependencyType> map = requires.get(from);
            ArrayList<String> arr = new ArrayList<String>();
            Set<String> cus = map.keySet();
            for (String s : cus)
                arr.add(s);
            return arr;
        }
        return null;
    }

    JSCSSCompilationSession cssSession = new JSCSSCompilationSession();

    @Override
    public CSSCompilationSession getCSSCompilationSession()
    {
        // When building SWFs, each MXML document may have its own styles
        // specified by fx:Style blocks.  The CSS is separately compiled and
        // stored in the class definition for the MXML document.  That helps
        // with deferred loading of classes.  The styles and thus the
        // classes for an MXML document are not initialized until the MXML
        // class is initialized.
        // For JS compilation, the CSS for non-standard CSS could be done the
        // same way, but AFAICT, standard CSS properties are best loaded by
        // specifying a .CSS file in the HTML.  The CSS is probably less text
        // than its codegen'd representation, and the browser can probably
        // load a .CSS file faster than us trying to run code to update the
        // styles.
        // So, for FlexJS, all style blocks from all MXML files are gathered into
        // one .css file and a corresponding codegen block that is output as
        // part of the main .JS file.
        return cssSession;
    }

    private HashMap<IASNode, String> astCache = new HashMap<IASNode, String>();

    @Override
    public void addToASTCache(IASNode ast)
    {
        astCache.put(ast, "");
    }
    
    @Override
    public String getActualPackageName(String packageName)
    {
    	if (packageName.startsWith("window."))
    		return packageName.substring(7);
    	if (packageName.equals("window"))
    		return "";
        return packageName;
    }

    @Override
    public IDefinition doubleCheckAmbiguousDefinition(ASScope scope, String name, IDefinition def1, IDefinition def2)
    {
        IScopedDefinition scopeDef = scope.getContainingDefinition();
        String thisPackage = scopeDef.getPackageName();
        String package1 = def1.getPackageName();
        String package2 = def2.getPackageName();
        // if the conflicts is against a class in the global/window package
        // then return the other one.
        if (package1.equals(thisPackage) && package2.length() == 0)
            return def1;
        if (package2.equals(thisPackage) && package1.length() == 0)
            return def2;
        if (package1.length() == 0 || package2.length() == 0)
        {
            // now check to see if the class was imported in the window package.
            ASScope pkgScope = scope;
            while (!(pkgScope instanceof PackageScope))
                pkgScope = pkgScope.getContainingScope();
            String[] imports = pkgScope.getImports();
            String windowName = "window." + name;
            boolean usingWindow = false;
            for (String imp : imports)
            {
                if (imp.equals(windowName))
                {
                    usingWindow = true;
                    break;
                }
            }
            // if they did not import via the window package, then assume
            // that they meant the one they did import
            if (!usingWindow)
            {
                return package1.length() == 0 ? def2 : def1;
            }
            // otherwise fall through to ambiguous because they need to qualify
            // with the package name, even if it is 'window'
        }
        return null;
    }
}
