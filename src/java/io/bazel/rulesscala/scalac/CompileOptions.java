package io.bazel.rulesscala.scalac;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompileOptions {
  final public String outputName;
  final public String manifestPath;
  final public String[] scalaOpts;
  final public String[] pluginArgs;
  final public String classpath;
  final public String[] files;
  final public Path outputPath;
  final public String[] sourceJars;
  final public boolean iJarEnabled;
  final public String ijarOutput;
  final public String ijarCmdPath;
  final public Path tmpPath;

  public CompileOptions(List<String> args) throws IOException, FileNotFoundException {
    Map<String, String> argMap = buildArgMap(args);

    outputName = getOrError(argMap, "JarOutput", "Missing required arg JarOutput");
    manifestPath = getOrError(argMap, "Manifest", "Missing required arg Manifest");

    scalaOpts = getOrEmpty(argMap, "ScalacOpts").split(",");
    pluginArgs = buildPluginArgs(getOrEmpty(argMap, "Plugins"));
    classpath = getOrError(argMap, "Classpath", "Must supply the classpath arg");
    outputPath = FileSystems.getDefault().getPath(outputName);

    sourceJars = getOrEmpty(argMap, "SourceJars").split(",");
    List<File> sourceFiles = new ArrayList<File>();

    for(String jarPath : sourceJars) {
      if(jarPath.length() > 0){
        Path tmpPath = Files.createTempDirectory(outputPath.getParent(), "tmp");
        sourceFiles.addAll(extractJar(jarPath, tmpPath.toString()));
      }
    }
    files = appendToString(getOrEmpty(argMap, "Files").split(","), sourceFiles);
    if(files.length == 0) {
      throw new RuntimeException("Must have input files from either source jars or local files.");
    }
    iJarEnabled = booleanGetOrFalse(argMap, "EnableIjar");
    if(iJarEnabled) {
     ijarOutput = getOrError(argMap, "ijarOutput", "Missing required arg ijarOutput when ijar enabled");
     ijarCmdPath = getOrError(argMap, "ijarCmdPath", "Missing required arg ijarCmdPath when ijar enabled");
    }
    else {
      ijarOutput = null;
      ijarCmdPath = null;
    }
    tmpPath = Files.createTempDirectory(outputPath.getParent(),"tmp");
  }

  private static <T> String[] appendToString(String[] init, List<T> rest) {
    String[] tmp = new String[init.length + rest.size()];
    System.arraycopy(init, 0, tmp, 0, init.length);
    int baseIdx = init.length;
    for(T t : rest) {
      tmp[baseIdx] = t.toString();
      baseIdx += 1;
    }
    return tmp;
  }

  private static HashMap<String, String> buildArgMap(List<String> lines) {
    HashMap hm = new HashMap();
    for(String line: lines) {
      String[] lSplit = line.split(": ");
      if(lSplit.length > 2) {
        throw new RuntimeException("Bad arg, should have at most 1 space/2 spans. arg: " + line);
      }
      if(lSplit.length > 1) {
        hm.put(lSplit[0], lSplit[1]);
      }
    }
    return hm;
  }

  private static String getOrEmpty(Map<String, String> m, String k) {
    if(m.containsKey(k)) {
      return m.get(k);
    } else {
      return "";
    }
  }
  private static String getOrError(Map<String, String> m, String k, String errorMessage) {
    if(m.containsKey(k)) {
      return m.get(k);
    } else {
      throw new RuntimeException(errorMessage);
    }
  }

  private static boolean booleanGetOrFalse(Map<String, String> m, String k) {
    if(m.containsKey(k)) {
      String v = m.get(k);
      if(v.trim().equals("True") || v.trim().equals("true")) {
        return true;
      }
    }
    return false;
  }
  public static String[] buildPluginArgs(String packedPlugins) {
    String[] pluginElements = packedPlugins.split(",");
    int numPlugins = 0;
    for(int i =0; i< pluginElements.length; i++){
      if(pluginElements[i].length() > 0) {
        numPlugins += 1;
      }
    }

    String[] result = new String[numPlugins];
    int idx = 0;
    for(int i =0; i< pluginElements.length; i++){
      if(pluginElements[i].length() > 0) {
        result[idx] = "-Xplugin:" + pluginElements[i];
        idx += 1;
      }
    }
    return result;
  }
  private static List<File> extractJar(String jarPath,
      String outputFolder) throws IOException, FileNotFoundException {

    List<File> outputPaths = new ArrayList<File>();
    java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath);
    java.util.Enumeration e = jar.entries();
    while (e.hasMoreElements()) {
      java.util.jar.JarEntry file = (java.util.jar.JarEntry) e.nextElement();
      File f = new File(outputFolder + java.io.File.separator + file.getName());

      if (file.isDirectory()) { // if its a directory, create it
        f.mkdirs();
        continue;
      }

      File parent = f.getParentFile();
      parent.mkdirs();
      outputPaths.add(f);

      java.io.InputStream is = jar.getInputStream(file); // get the input stream
      java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
      while (is.available() > 0) {  // write contents of 'is' to 'fos'
        fos.write(is.read());
      }
      fos.close();
      is.close();
    }
    return outputPaths;
  }
}
