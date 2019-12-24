package com.exsoinn.ie.rule;

import javax.script.ScriptException;
import java.io.FileNotFoundException;

public interface RulesEngine {
	void execute();

	Object executeSelfEvaluatingFunction(String pFileName)
			throws ScriptException;

	Object executeFunctionByName(String pFileName, String pFunctionName, Object... pArgs)
			throws ScriptException;

	Object executeAnonymousFunction(String pFileName, Object... pArgs)
			throws ScriptException;

	Object importJavaScriptFileIntoEngine(String pFileName) throws FileNotFoundException, ScriptException;
}
