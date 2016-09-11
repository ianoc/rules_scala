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
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.lib.worker.WorkerProtocol.Input;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest;
import com.google.devtools.build.lib.worker.WorkerProtocol.WorkResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;


/**
 * A class for creating Jar files. Allows normalization of Jar entries by setting their timestamp to
 * the DOS epoch. All Jar entries are sorted alphabetically.
 */
public class ScalaCInvoker {
    // A UUID that uniquely identifies this running worker process.
  static final UUID workerUuid = UUID.randomUUID();

  // A counter that increases with each work unit processed.
  static int workUnitCounter = 1;

  // If true, returns corrupt responses instead of correct protobufs.
  static boolean poisoned = false;

  // Keep state across multiple builds.
  static final LinkedHashMap<String, String> inputs = new LinkedHashMap<>();


  static class WorkerOptions {
    public int exitAfter = 30;
    public int poisonAfter = 30;
  }

  private static void runPersistentWorker(WorkerOptions workerOptions) throws IOException {
    PrintStream originalStdOut = System.out;
    PrintStream originalStdErr = System.err;

    while (true) {
      try {
        WorkRequest request = WorkRequest.parseDelimitedFrom(System.in);
        if (request == null) {
          break;
        }

        inputs.clear();
        for (Input input : request.getInputsList()) {
          inputs.put(input.getPath(), input.getDigest().toStringUtf8());
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int exitCode = 0;

        try (PrintStream ps = new PrintStream(baos)) {
          System.setOut(ps);
          System.setErr(ps);

          try {
            processRequest(request.getArgumentsList());
          } catch (Exception e) {
            e.printStackTrace();
            exitCode = 1;
          }
        } finally {
          System.setOut(originalStdOut);
          System.setErr(originalStdErr);
        }

        if (poisoned) {
          System.out.println("I'm a poisoned worker and this is not a protobuf.");
        } else {
          WorkResponse.newBuilder()
              .setOutput(baos.toString())
              .setExitCode(exitCode)
              .build()
              .writeDelimitedTo(System.out);
        }
        System.out.flush();

        if (workerOptions.exitAfter > 0 && workUnitCounter > workerOptions.exitAfter) {
          return;
        }

        if (workerOptions.poisonAfter > 0 && workUnitCounter > workerOptions.poisonAfter) {
          poisoned = true;
        }
      } finally {
        // Be a good worker process and consume less memory when idle.
        System.gc();
      }
    }
  }



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



  // public static void dispatch(String[] args) throws Exception {
  //   if (ImmutableSet.copyOf(args).contains("--persistent_worker")) {
  //     OptionsParser parser = OptionsParser.newOptionsParser(ExampleWorkerOptions.class);
  //     parser.setAllowResidue(false);
  //     parser.parse(args);
  //     ExampleWorkerOptions workerOptions = parser.getOptions(ExampleWorkerOptions.class);
  //     Preconditions.checkState(workerOptions.persistentWorker);

  //     runPersistentWorker(workerOptions);
  //   } else {
  //     // This is a single invocation of the example that exits after it processed the request.
  //     processRequest(ImmutableList.copyOf(args));
  //   }
  // }
  private static String getOptionalNamedArg(String line) {
    String[] lSplit = line.split(" ");
    if(lSplit.length > 1) {
      return lSplit[1];
    } else {
      return "";
    }
  }
  private static void processRequest(List<String> args) throws Exception {
    System.out.println("\n\n\n___ARGS_START____\n");



      for (int i = 0; i < args.size(); i++) {
        System.out.println("'''" + args.get(i) + "'''");
      }

      if (args.size() == 1 && args.get(0).startsWith("@")) {
        args = Files.readAllLines(Paths.get(args.get(0).substring(1)), UTF_8);
      }

      List<String> trimmedArgs = new ArrayList<String>();
      for(String arg : args) {
        if(arg.trim().length() > 0){
          trimmedArgs.add(arg);
        }
      }
      args = trimmedArgs;

      for (int i = 0; i < args.size(); i++) {
        System.out.println(args.get(i));
      }

      String outputName = args.get(0);
      String manifestPath = args.get(1);

      String[] scalaOpts = getOptionalNamedArg(args.get(2)).split(",");
      String[] pluginArgs = buildPluginArgs(getOptionalNamedArg(args.get(3)));
      String classpath = args.get(4);
      String[] files = args.get(5).split(",");

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

  public static void main(String[] args) {

    try {
if (ImmutableSet.copyOf(args).contains("--persistent_worker")) {
      runPersistentWorker(new WorkerOptions());
    } else {
processRequest(Arrays.asList(args));
}
}
catch (FileNotFoundException ex){
  throw new RuntimeException("nope", ex);
}
catch (Exception ex){
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
