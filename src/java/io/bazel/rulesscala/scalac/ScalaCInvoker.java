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

package io.bazel.rulesscala.scalac;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import scala.tools.nsc.*;
import java.io.*;
import java.lang.reflect.Field;
import scala.tools.nsc.reporters.ConsoleReporter;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystems;
import io.bazel.rulesscala.jar.JarCreator;

/**
 * A class for creating Jar files. Allows normalization of Jar entries by setting their timestamp to
 * the DOS epoch. All Jar entries are sorted alphabetically.
 */
public class ScalaCInvoker {

  public static String[] buildPluginArgs(String packedPlugins) {
    //     plugin_arg = ""
    // if (len(plugins) > 0):
    //     plugin_arg = " ".join(["-Xplugin:%s" % p for p in plugins])


    String[] result = {};

    return result; // new String[](0);
  }

  public static String[] merge(String[]... arrays) {
    int totalLength = 0;
    for(String[] arr:arrays){
      totalLength += arr.length;
    }

    String[] result = new String[totalLength];
    int offset = 0;
    for(String[] arr:arrays){
      System.arraycopy(arr, 0, result, offset, arr.length);
      offset += arr.length;
    }
    return result;
  }

  public static void main(String[] args) {
    try {
      System.out.println("\n\n\n___ARGS_START____\n");

      if(args.length == 1 && args[0].indexOf("@") == 0) {
        String line;
        BufferedReader in;
        in = new BufferedReader(new FileReader(args[0].substring(1)));
        line = in.readLine();
        args = line.split(" ");
      }

      String outputName = args[0];
      String manifestPath = args[1];
      String[] scalaOpts = args[2].split(",");
      String[] pluginArgs = buildPluginArgs(args[3]);
      String classpath = args[4];
      String[] files = args[5].split(",");

      args = Arrays.copyOfRange(args, 2, args.length);

      Path outputPath = FileSystems.getDefault().getPath(outputName);
      Path tmpPath = Files.createTempDirectory(outputPath.getParent(),"tmp");

      System.out.println("Output path will be: " + outputName);
      System.out.println("Manifest path will be: " + manifestPath);
      System.out.println("tmpPath path will be: " + tmpPath);

      String[] constParams = {
        "-classpath",
        classpath,
        "-d",
        tmpPath.toString()
        };

      String[] compilerArgs = merge(
        scalaOpts,
        pluginArgs,
        constParams,
        files);


      for (int i = 0; i < compilerArgs.length; i++) {
      System.out.println(compilerArgs[i]);
        }




     //  System.out.println("\n\n___ARGS_END____\n");

     // for (int i = 0; i < newArgs.length; i++) {
     //  System.out.println(newArgs[i]);
     //    }

     //  System.out.println("SASDF");
     //  System.out.println("SASDF");
     //  System.out.println("SASDF");
     //  System.out.println("SASDF");
     //  System.out.println("SASDF");
  MainClass comp = new MainClass();
  comp.process(compilerArgs);


  // System.out.println("SASDF");

  Field f = Driver.class.getDeclaredField("reporter"); //NoSuchFieldException
  f.setAccessible(true);
  ConsoleReporter reporter = (ConsoleReporter) f.get(comp); //IllegalAccessException

  if (reporter.hasErrors()) {
      // reportErrors(reporter);
      reporter.flush();
  } else {
    // reportSuccess();
    String[] jarCreatorArgs = {
      "-m",
      manifestPath,
      outputPath.toString(),
      tmpPath.toString()
    };
    JarCreator.buildJar(jarCreatorArgs);

    System.out.println("Success");
  }
}
catch(NoSuchFieldException ex) {
  throw new RuntimeException("nope", ex);
}
catch (IllegalAccessException ex){
  throw new RuntimeException("nope", ex);
}
catch (FileNotFoundException ex){
  throw new RuntimeException("nope", ex);
}

catch (IOException ex){
  throw new RuntimeException("nope", ex);
}


//   Settings s = new Settings();

//   Global g = new Global(s);

//   Global.Run run = g.new Run();

// // run.compile(List("test.scala"))  // invoke compiler. it creates Test.class.

//     for (int i = 0; i < args.length; i++) {
//       System.err.println(i);
//     }
//     System.err.println("Helloooo world!!!");
//     System.exit(-1);
  }
}
